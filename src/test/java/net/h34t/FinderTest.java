package net.h34t;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FinderTest {

    @Test
    public void findFiles() throws IOException {
        Path inputPath = Paths.get("src/test/resources/sql");

        List<QuerySource> sources = Scow.getQuerySources(inputPath);

        QuerySource s = sources.get(0);

        Assert.assertEquals("Test", s.getQueryName());
        Assert.assertEquals("SELECT * FROM users;", s.getSql());
        Assert.assertEquals("foobar.baz", s.getNameSpace());
    }

    @Test
    public void testTestSql() throws IOException {
        Path inputPath = Paths.get("src/test/resources/sql");

        List<QuerySource> sources = Scow.getQuerySources(inputPath);

        QuerySource s = sources.get(0);

        Assert.assertEquals("Test", s.getQueryName());
        Assert.assertEquals("SELECT * FROM users;", s.getSql());
        Assert.assertEquals("foobar.baz", s.getNameSpace());
    }

    @Test
    public void testOutputPath() throws IOException {
        Path inputPath = Paths.get("src/test/resources/sql");

        CompiledQuery cp = new CompiledQuery(
                "florb",
                "Test",
                "foo.bar.baz");

        Path p = Scow.getOutputPath(Paths.get("out"), cp);

        Assert.assertEquals(Paths.get("out/foo/bar/baz/TestDto.java"), p);
    }

}
