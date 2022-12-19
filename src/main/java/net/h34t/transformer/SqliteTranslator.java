package net.h34t.transformer;

import net.h34t.DatatypeTranslator;

public class SqliteTranslator implements DatatypeTranslator {
    @Override
    public String transform(String type) {
        switch (type) {
            case "NULL":
                return "Void";
            case "INTEGER":
                return "Long";
            case "REAL":
                return "Double";
            case "TEXT":
                return "String";
            case "BLOB":
                return "Object";
            case "DATETIME":
                return "java.util.Date";
            case "NUMERIC":
                return "java.math.BigDecimal";
            default:
                throw new RuntimeException("Couldn't convert type \"" + type + "\"");
        }
    }
}
