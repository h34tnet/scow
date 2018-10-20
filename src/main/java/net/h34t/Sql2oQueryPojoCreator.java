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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
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

    private final String jdbcDsn;
    private final String schema;
    private final String user;
    private final String password;

    public Sql2oQueryPojoCreator(
            String jdbcDsn,
            String schema,
            String user,
            String password
    ) {
        this.jdbcDsn = jdbcDsn;
        this.schema = schema;
        this.user = user;
        this.password = password;
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
     * @param queries         the queries to process
     * @param outputDirectory the directory to save the generated java code
     * @throws SQLException db error
     * @throws IOException  write error
     */
    public void run(Map<String, String> queries, Path outputDirectory) throws SQLException,
            IOException {
        try (Connection conn = DriverManager.getConnection(jdbcDsn, user, password)) {
            conn.setSchema(schema);
            conn.setAutoCommit(false);

            for (Map.Entry<String, String> queryDefinition : queries.entrySet()) {

                try {
                    NamedParameterStatement npStatement = new NamedParameterStatement(conn, queryDefinition.getValue());
                    PreparedStatement statement = npStatement.getStatement();

                    Map<String, int[]> parameterMap = npStatement.getParameters();

                    ParameterMetaData pmd = statement.getParameterMetaData();
                    ResultSetMetaData rsmd = statement.getMetaData();

                    List<Column> columns = extractColumns(rsmd);

                    String table = createDtoClassCode(
                            queryDefinition.getKey(),
                            queryDefinition.getValue(),
                            columns,
                            invert(parameterMap),
                            pmd
                    );

                    Files.write(
                            outputDirectory.resolve(queryDefinition.getKey() + "Dto.java"),
                            table.getBytes(StandardCharsets.UTF_8),
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

                } catch (SQLException sqle) {
                    System.err.printf("Error for query %s:%n%s%n", queryDefinition.getKey(), queryDefinition.getValue());
                    throw sqle;
                }
            }
        }
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

    private String createDtoClassCode(String viewName, String sqlQuery, List<Column> columns, Map<Integer, String> paramMap, ParameterMetaData pmd) throws SQLException {

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

//        paramType.forEach((name, className) -> queryParams.add(ParameterSpec.builder(className, name).build()));

        CodeBlock.Builder paramAssignmentCode = CodeBlock.builder()
                .add("return conn.createQuery(QUERY)\n")
                .indent();


        paramType.entrySet().stream()
                .forEach(e -> paramAssignmentCode.add(".addParameter($S, $L)\n", e.getKey(), e.getKey()));

        // paramAssignmentCode.add("");


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


        // TypeSpec.classBuilder(ClassName.get(List.class));

        ClassName listTypeName = ClassName.get(List.class);
        TypeName classTypeName = dtoClassName;

        ParameterizedTypeName returnTypeName = ParameterizedTypeName.get(listTypeName, classTypeName);

        // the method query, which creates a Query object
        MethodSpec executeAndFetch = MethodSpec.methodBuilder("executeAndFetch")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(returnTypeName)
                .addParameter(org.sql2o.Connection.class, "conn")
                .addParameters(queryParams)
                .addCode(paramAssignmentCode.build())
                .addCode(".executeAndFetch($T.class);\n", classTypeName)
                .build();


        TypeSpec dtoClass = dtoClassSpec
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addField(querySpec)
                .addFields(fields)
                .addMethod(query)
                .addMethod(executeAndFetch)
                .build();

        // the file containing the class
        JavaFile javaFile = JavaFile.builder("at.sra.app.model.dto", dtoClass)
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
