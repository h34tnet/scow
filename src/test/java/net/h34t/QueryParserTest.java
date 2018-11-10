package net.h34t;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class QueryParserTest {

    @Test
    public void parseQuery() {
        ParsedQuery pq = new QueryParser().parse("SELECT a, b, c FROM d");
        Assert.assertEquals("SELECT a, b, c FROM d", pq.getFirstQuery().trim());
    }

    @Test
    public void parseQueryWithModifier() {
        ParsedQuery pq = new QueryParser().parse("SELECT a, b, c FROM d\n" +
                "---: matchc\n" +
                "WHERE c=:bar");
        Assert.assertEquals(Whitespace.normalize("SELECT a, b, c FROM d WHERE c=:bar"),
                Whitespace.normalize(pq.getFirstQuery().trim()));
        Assert.assertEquals("matchc", pq.getModifiers().get(0).name.trim());
        Assert.assertEquals("WHERE c=:bar", pq.getModifiers().get(0).body.trim());
    }

    @Test
    public void parseQueryWithTwoModifiers() {
        ParsedQuery pq = new QueryParser().parse("SELECT a,\n b, \nc FROM d\n" +
                "---: getByC\n" +
                "WHERE c=:bar\n" +
                "---: getAllOrdered\n" +
                "ORDER BY a");

        List<ParsedQuery.Modifier> modifiers = pq.getModifiers();

        Assert.assertEquals(Whitespace.normalize("SELECT a, b, c FROM d WHERE c=:bar"),
                Whitespace.normalize(pq.getFirstQuery()));

        Assert.assertEquals("getByC", modifiers.get(0).name.trim());
        Assert.assertEquals("WHERE c=:bar", modifiers.get(0).body.trim());
        Assert.assertEquals("getAllOrdered", modifiers.get(1).name.trim());
        Assert.assertEquals("ORDER BY a", modifiers.get(1).body.trim());
    }

}
