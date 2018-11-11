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

        List<SourceQuery> sources = Scow.getQuerySources(inputPath);

        Assert.assertEquals(2, sources.size());

        SourceQuery s = sources.get(0);

        Assert.assertEquals("Test", s.getQueryName());
        Assert.assertEquals(Whitespace.normalize("SELECT * FROM users;"), Whitespace.normalize(s.getSql()));
        Assert.assertEquals("foobar.baz", s.getNameSpace());
    }

    @Test
    public void testTestSql() throws IOException {
        Path inputPath = Paths.get("src/test/resources/sql");

        List<SourceQuery> sources = Scow.getQuerySources(inputPath);

        SourceQuery s =
                sources.stream().filter(q -> q.getQueryName().equals("Test")).findAny()
                        .orElseThrow(RuntimeException::new);

        Assert.assertEquals("Test", s.getQueryName());
        Assert.assertEquals(Whitespace.normalize("SELECT * FROM users;"), Whitespace.normalize(s.getSql()));
        Assert.assertEquals("foobar.baz", s.getNameSpace());
    }

    @Test
    public void testOutputPath() {
        CompiledQuery cp = new CompiledQuery(
                "florb",
                "TestDto",
                "foo.bar.baz");

        Path p = Scow.getOutputPath(Paths.get("out"), cp);

        Assert.assertEquals(Paths.get("out/foo/bar/baz/TestDto.java"), p);
    }

}
