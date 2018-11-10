package net.h34t;

public final class Whitespace {

    public static String normalize(String sql) {
        return sql.replaceAll("\\s*", " ").trim();
    }
}
