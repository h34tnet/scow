package net.h34t;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class CompilerTest {

    @Test
    public void testTestSql() throws IOException, ClassNotFoundException, SQLException {
        List<QuerySource> qs = Scow.getQuerySources(Paths.get("src/test/resources/sql"));


        Class.forName("org.sqlite.JDBC");

        String dsn = "jdbc:sqlite::memory:";

        Connection connection = DriverManager.getConnection(dsn, null, null);

        connection.prepareStatement("CREATE TABLE users ( userId integer primary key, " +
                "name text not null, " +
                "description text, " +
                "anumber integer not null, " +
                "nother integer " +
                ");").execute();


        Sql2oQueryPojoCreator sql2oQueryPojoCreator = new Sql2oQueryPojoCreator();

        List<CompiledQuery> queries = sql2oQueryPojoCreator.run(connection, qs);

        queries.forEach(System.out::println);

    }
}
