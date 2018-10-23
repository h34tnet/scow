package net.h34t;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import org.sql2o.Query;

import javax.lang.model.element.Modifier;
import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Sql2oQueryPojoCreator {


    public Sql2oQueryPojoCreator(
    ) {

    }

    /**
     * Inverts the paramter map, so it's an index -> paramName map again
     *
     * @param paramters the paramName -> index map
     * @return the inverted map
     */
    private static Map<Integer, String> invert(Map<String, int[]> paramters) {
        Map<Integer, String> inv = new HashMap<>();

        paramters.forEach((key, val) -> {
            for (int i : val)
                inv.put(i, key);
        });

        return inv;
    }

    /**
     * @param queries the queries to process
     * @throws SQLException db error
     */
    public List<CompiledQuery> run(Connection conn, List<QuerySource> queries) throws SQLException {
        List<CompiledQuery> compiledQueries = new ArrayList<>(queries.size());

        conn.setAutoCommit(false);

        for (QuerySource queryDefinition : queries) {
            compiledQueries.add(compile(conn, queryDefinition));
        }

        return compiledQueries;
    }

    public CompiledQuery compile(Connection conn, QuerySource queryDefinition) throws SQLException {
        try {
            NamedParameterStatement npStatement = new NamedParameterStatement(conn, queryDefinition.getSql());
            PreparedStatement statement = npStatement.getStatement();

            Map<String, int[]> parameterMap = npStatement.getParameters();

            ParameterMetaData pmd = statement.getParameterMetaData();
            ResultSetMetaData rsmd = statement.getMetaData();

            List<Column> columns = extractColumns(rsmd);

            String table = createDtoClassCode(
                    queryDefinition.getNameSpace(),
                    queryDefinition.getQueryName(),
                    queryDefinition.getSql(),
                    columns,
                    invert(parameterMap),
                    pmd
            );

            return new CompiledQuery(
                    table,
                    queryDefinition.getQueryName(),
                    queryDefinition.getNameSpace());

        } catch (SQLException sqle) {
            System.err.printf("Error for query %s:%n%s%n", queryDefinition.getQueryName(),
                    queryDefinition.getSql());
            throw sqle;
        }
    }

    private List<Column> extractColumns(ResultSetMetaData rsmd) throws SQLException {
        List<Column> columns = new ArrayList<>();

        for (int i = 1; i < rsmd.getColumnCount() + 1; i++) {
            columns.add(new Column(rsmd.getColumnLabel(i), rsmd.getColumnClassName(i)));
            // System.out.println(rsmd.getColumnName(i) + " " + rsmd.getColumnTypeName(i));
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

    private String createDtoClassCode(String namespace,
                                      String viewName,
                                      String sqlQuery,
                                      List<Column> columns,
                                      Map<Integer, String> paramMap,
                                      ParameterMetaData pmd) throws SQLException {

        // a constant that stores the original SQL query
        FieldSpec querySpec = FieldSpec.builder(String.class, "QUERY")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$S", sqlQuery)
                .build();

        // the data fields
        List<FieldSpec> fields = columns.stream()
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

        Map<String, ClassName> paramType = getParamTypes(paramMap, pmd);

        List<ParameterSpec> queryParams = paramType.entrySet().stream()
                .map(entry -> ParameterSpec.builder(entry.getValue(), entry.getKey()).build())
                .collect(Collectors.toList());

        CodeBlock.Builder paramAssignmentCode = CodeBlock.builder()
                .add("return conn.createQuery(QUERY)\n")
                .indent();


        paramType.entrySet().stream()
                .forEach(e -> paramAssignmentCode.add(".addParameter($S, $L)\n", e.getKey(), e.getKey()));


        // the method query, which creates a Query object
        MethodSpec query = MethodSpec.methodBuilder("query")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(Query.class)
                .addParameter(org.sql2o.Connection.class, "conn")
                .addParameters(queryParams)
                .addCode(paramAssignmentCode.build())
                .addCode(";\n")
                .build();

        ClassName dtoClassName = ClassName.bestGuess(viewName + "Dto");

        // the class
        TypeSpec.Builder dtoClassSpec = TypeSpec.classBuilder(dtoClassName);

        ClassName listTypeName = ClassName.get(List.class);

        ParameterizedTypeName returnTypeName = ParameterizedTypeName.get(listTypeName, dtoClassName);

        // the method query, which creates a Query object
        MethodSpec executeAndFetch;
        MethodSpec executeAndFetchSingle;

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


        TypeSpec dtoClass = dtoClassSpec
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addField(querySpec)
                .addFields(fields)
                .addMethod(query)
                .addMethod(executeAndFetch)
                .addMethod(executeAndFetchSingle)
                .build();

        // the file containing the class
        JavaFile javaFile = JavaFile.builder(namespace, dtoClass)
                .build();

        return javaFile.toString();
    }

    private static class Column {

        private final String name;
        private final String type;

        public Column(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getMember() {
            return String.format("public %s %s;", this.type, getName());

        }
    }

}
