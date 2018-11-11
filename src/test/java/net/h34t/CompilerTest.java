package net.h34t;

import org.junit.Test;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.util.List;

public class CompilerTest {

    @Test
    public void testTestSql() throws Exception {
        List<SourceQuery> qs = Scow.getQuerySources(Paths.get("src/test/resources/sql"));


        Class.forName("org.sqlite.JDBC");

        String dsn = "jdbc:sqlite::memory:";

        Connection connection = DriverManager.getConnection(dsn, null, null);

        connection.prepareStatement("CREATE TABLE users ( " +
                "id integer primary key, " +
                "name text not null, " +
                "password integer not null, " +
                "created datetime" +
                ");").execute();

        PreparedStatement pps = connection.prepareStatement("SELECT * FROM users\n" +
                "WHERE id=:id\n");
        ResultSetMetaData msmd = pps.getMetaData();

        for (int i = 1; i < msmd.getColumnCount() + 1; i++) {
            System.out.println(String.format("%s / %s / %s",
                    msmd.getColumnClassName(i),
                    msmd.getColumnType(i),
                    msmd.getColumnTypeName(i)));

            java.sql.Types
        }

        Sql2oPojoCreator sql2oQueryPojoCreator = new Sql2oPojoCreator();

        List<CompiledQuery> queries = sql2oQueryPojoCreator.run(connection, qs);

        queries.forEach(System.out::println);

    }
}
