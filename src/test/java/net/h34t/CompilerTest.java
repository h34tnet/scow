package net.h34t;

import com.squareup.javapoet.JavaFile;
import net.h34t.transformer.SqliteTranslator;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.util.Collections;

public class CompilerTest {

    @Rule
    public TemporaryFolder outputFolder = new TemporaryFolder();

    @Test
    public void testAdHocCompilation() throws Exception {
        Class.forName("org.sqlite.JDBC");

        String dsn = "jdbc:sqlite::memory:";

        Connection connection = DriverManager.getConnection(dsn, null, null);

        connection.prepareStatement("CREATE TABLE users ( " +
                "id integer primary key, " +
                "name text not null, " +
                "password blob not null, " +
                "points real, " +
                "quirks null, " +
                "created datetime" +
                ");").execute();

        String sql = "SELECT * FROM users WHERE id=:id\n";

        PreparedStatement pps = connection.prepareStatement(sql);
        ResultSetMetaData msmd = pps.getMetaData();

        for (int i = 1; i < msmd.getColumnCount() + 1; i++) {
            System.out.println(String.format("%s: %s / %s / %s / %d",
                    msmd.getColumnName(i),
                    msmd.getColumnClassName(i),
                    msmd.getColumnType(i),
                    msmd.getColumnTypeName(i),
                    msmd.getPrecision(i)));
        }

        Sql2oPojoCreator sql2oQueryPojoCreator = new Sql2oPojoCreator();

        JavaFile javaFile = sql2oQueryPojoCreator.compile(
                connection, new SqliteTranslator(),
                "foo.bar", "Baz", sql, Paths.get("foo/bar/baz")
        );

        // this tests the finder and the compiler
        Assert.assertNotNull(javaFile);

        System.out.println(javaFile.toString());

        File outputFile = outputFolder.newFile("Baz.java");
        Files.write(outputFile.toPath(), javaFile.toString().getBytes(StandardCharsets.UTF_8));

        boolean result = TestCompiler.test(Collections.singletonList(outputFile));

        Assert.assertTrue("Compilation failed", result);
    }
}
