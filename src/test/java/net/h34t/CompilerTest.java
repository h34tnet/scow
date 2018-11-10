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

        connection.prepareStatement("CREATE TABLE users ( userId integer primary key, " +
                "name text not null, " +
                "description text, " +
                "anumber integer not null, " +
                "nother integer " +
                ");").execute();

        PreparedStatement ps = connection.prepareStatement("INSERT INTO users (userId, name, description, anumber, " +
                "nother) VALUES (?, ?, ?, ?, ?)");

        ps.setInt(1, 1);
        ps.setString(2, "Pietro");
        ps.setString(3, "A lanky man");
        ps.setInt(4, 77);
        ps.setObject(5, null);
        ps.execute();


        PreparedStatement pps = connection.prepareStatement("SELECT * FROM users");
        ResultSetMetaData msmd = pps.getMetaData();

        for (int i = 1; i < msmd.getColumnCount() + 1; i++) {
            System.out.println(String.format("%s / %s / %s", msmd.getColumnClassName(i), msmd.getColumnType(i),
                    msmd.getColumnTypeName(i)));
        }

        Sql2oPojoCreator sql2oQueryPojoCreator = new Sql2oPojoCreator();

        List<CompiledQuery> queries = sql2oQueryPojoCreator.run(connection, qs);

        queries.forEach(System.out::println);

    }
}
