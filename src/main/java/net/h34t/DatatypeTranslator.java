package net.h34t;

public interface DatatypeTranslator {

    /**
     * @param type the type name as reported by the database
     * @return an appropriate java class for this database type
     */
    String transform(String type);

}
