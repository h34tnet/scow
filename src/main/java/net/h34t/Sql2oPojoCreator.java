package net.h34t;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.sql2o.Query;

import javax.lang.model.element.Modifier;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Sql2oPojoCreator {


    public Sql2oPojoCreator() {
    }

    /**
     * Inverts the paramter map, so it's an index -> paramName map again
     *
     * @param parameters the paramName -> index map
     * @return the inverted map
     */
    private static Map<Integer, String> invert(Map<String, int[]> parameters) {
        Map<Integer, String> inv = new HashMap<>();

        parameters.forEach((key, val) -> {
            for (int i : val)
                inv.put(i, key);
        });

        return inv;
    }

    /**
     * @param queries the queries to process
     * @throws SQLException db error
     */
    List<CompiledQuery> run(Connection conn, List<SourceQuery> queries) throws Exception {
        List<CompiledQuery> compiledQueries = new ArrayList<>(queries.size());

        conn.setAutoCommit(false);

        for (SourceQuery queryDefinition : queries) {
            try {
                JavaFile jf = compile(conn,
                        queryDefinition.getNameSpace(),
                        queryDefinition.getClassName(),
                        queryDefinition.getSql(),
                        queryDefinition.getQueryFile());

                compiledQueries.add(new CompiledQuery(
                        jf.toString(),
                        jf.typeSpec.name,
                        jf.packageName
                ));

            } catch (Exception e) {
                throw new Exception("An error occurred while parsing " + queryDefinition.getQueryFile().toString(), e);
            }
        }

        return compiledQueries;
    }

    public JavaFile compile(
            Connection con,
            String packageName,
            String className,
            String rawQuery,
            Path sourceFilePath) throws SQLException {
        ParsedQuery parsedQuery = new QueryParser().parse(rawQuery);

        NamedParameterStatement npStatement = new NamedParameterStatement(con, parsedQuery.getFirstQuery());
        PreparedStatement statement = npStatement.getStatement();
        ResultSetMetaData rsmd = statement.getMetaData();

        List<FieldSpec> fields = this.createFields(extractColumns(rsmd));

        ClassName cs = ClassName.bestGuess(className);

        List<TypeSpec> executeAndFetchMethods = parsedQuery.getModifiers().stream()
                .map(pqm -> createQuerySubclass(con, cs, pqm))
                .collect(Collectors.toList());

        CodeBlock javaDoc = sourceFilePath != null
                ? CodeBlock.of("see <a href=\"$L\">$L</a>", sourceFilePath.getFileName().toUri().toString(),
                sourceFilePath.getFileName().toString())
                : CodeBlock.of("");

        TypeSpec typeSpec = TypeSpec.classBuilder(cs)
                .addJavadoc(javaDoc)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addFields(fields)
                .addTypes(executeAndFetchMethods)
                .build();

        return JavaFile.builder(packageName, typeSpec).build();

    }

    private TypeSpec createQuerySubclass(Connection con, ClassName cs, ParsedQuery.Modifier mod) {
        try {
            TypeSpec.Builder specBuilder = TypeSpec.classBuilder(mod.name)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

            String sql = mod.getFullBody().trim();

            NamedParameterStatement npStatement = new NamedParameterStatement(con, sql);
            PreparedStatement statement = npStatement.getStatement();

            Map<String, int[]> parameterMap = npStatement.getParameters();

            ParameterMetaData pmd = statement.getParameterMetaData();
            ResultSetMetaData rsmd = statement.getMetaData();

            List<Column> columns = extractColumns(rsmd);

            Map<String, ClassName> paramType = getParamTypes(invert(parameterMap), pmd);

            List<ParameterSpec> queryParams = paramType.entrySet().stream()
                    .map(entry -> ParameterSpec.builder(entry.getValue(), entry.getKey()).build())
                    .collect(Collectors.toList());

            CodeBlock.Builder paramAssignmentCode = CodeBlock.builder()
                    .add("return conn.createQuery(QUERY)\n")
                    .indent();


            paramType.forEach((key, value) -> paramAssignmentCode.add(".addParameter($S, $L)\n", key, key));

            // a constant that stores the original SQL query
            specBuilder.addField(FieldSpec.builder(String.class, "QUERY")
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$S", sql)
                    .build());

            // the method query, which creates a Query object
            MethodSpec query = MethodSpec.methodBuilder("query")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(Query.class)
                    .addParameter(org.sql2o.Connection.class, "conn")
                    .addParameters(queryParams)
                    .addCode(paramAssignmentCode.build())
                    .addCode(";\n")
                    .build();

            specBuilder.addMethod(query);

            specBuilder.addMethods(createFetchers(columns,
                    cs,
                    paramType));


            return specBuilder.build();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<MethodSpec> createFetchers(
            List<Column> columns,
            TypeName dtoClassName,
            Map<String, ClassName> paramType
    ) {
        // the method query, which creates a Query object
        MethodSpec executeAndFetch;
        MethodSpec executeAndFetchSingle;
        ClassName listTypeName = ClassName.get(List.class);
        ParameterizedTypeName returnTypeName = ParameterizedTypeName.get(listTypeName, dtoClassName);


        List<ParameterSpec> queryParams = paramType.entrySet().stream()
                .map(entry -> ParameterSpec.builder(entry.getValue(), entry.getKey()).build())
                .collect(Collectors.toList());

        CodeBlock.Builder paramAssignmentCode = CodeBlock.builder()
                .add("return conn.createQuery(QUERY)\n")
                .indent();


        paramType.forEach((key, value) -> paramAssignmentCode.add(".addParameter($S, $L)\n", key, key));

        if (columns.size() == 1) {
            ClassName colClassName = ClassName.bestGuess(columns.get(0).getType());
            executeAndFetch = MethodSpec.methodBuilder("fetchAll")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(ParameterizedTypeName.get(listTypeName, colClassName))
                    .addParameter(org.sql2o.Connection.class, "conn")
                    .addParameters(queryParams)
                    .addCode(paramAssignmentCode.build())
                    .addCode(".executeScalarList($T.class);\n", colClassName)
                    .build();

            executeAndFetchSingle = MethodSpec.methodBuilder("fetch")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(colClassName)
                    .addParameter(org.sql2o.Connection.class, "conn")
                    .addParameters(queryParams)
                    .addCode(paramAssignmentCode.build())
                    .addCode(".executeScalar($T.class);\n", colClassName)
                    .build();
        } else {
            executeAndFetch = MethodSpec.methodBuilder("fetchAll")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(returnTypeName)
                    .addParameter(org.sql2o.Connection.class, "conn")
                    .addParameters(queryParams)
                    .addCode(paramAssignmentCode.build())
                    .addCode(".executeAndFetch($T.class);\n", dtoClassName)
                    .build();

            executeAndFetchSingle = MethodSpec.methodBuilder("fetch")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(dtoClassName)
                    .addParameter(org.sql2o.Connection.class, "conn")
                    .addParameters(queryParams)
                    .addCode(paramAssignmentCode.build())
                    .addCode(".executeAndFetchFirst($T.class);\n", dtoClassName)
                    .build();
        }

        return Arrays.asList(executeAndFetch, executeAndFetchSingle);
    }

    private List<Column> extractColumns(ResultSetMetaData rsmd) throws SQLException {
        List<Column> columns = new ArrayList<>();

        for (int i = 1; i < rsmd.getColumnCount() + 1; i++) {
            columns.add(new Column(rsmd.getColumnLabel(i), rsmd.getColumnClassName(i)));
        }

        return columns;
    }

    private Map<String, ClassName> getParamTypes(Map<Integer, String> paramMap, ParameterMetaData pmd) throws SQLException {
        Map<String, ClassName> paramType = new HashMap<>();

        for (int i = 1; i < pmd.getParameterCount() + 1; i++) {
            String name = paramMap.get(i);
            paramType.putIfAbsent(name, ClassName.bestGuess(pmd.getParameterClassName(i)));
        }

        return paramType;
    }

    /**
     * @param columns the column definitions as given by the resultsetmetadata
     * @return the list of properties
     */
    private List<FieldSpec> createFields(List<Column> columns) {
        // the data fields
        return columns.stream()
                .map(c -> {
                    try {
                        return FieldSpec.builder(
                                ClassName.bestGuess(c.getType()),
                                c.getName())
                                .addModifiers(Modifier.PUBLIC)
                                .build();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    private static class Column {

        private final String name;
        private final String type;

        Column(String name, String type) {
            this.name = name;
            this.type = type;
        }

        String getName() {
            return name;
        }

        String getType() {
            return type;
        }

//        public String getMember() {
//            return String.format("public %s %s;", this.type, getName());
//        }
    }

}
