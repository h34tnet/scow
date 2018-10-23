package net.h34t;

import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class QuerySource {

    private final Path sourceDirectory;
    private final Path queryFile;
    private final String sql;

    public QuerySource(Path sourceDirectory, Path queryFile, String sql) {
        this.sourceDirectory = sourceDirectory;
        this.queryFile = queryFile;
        this.sql = sql;
    }

    public String getQueryName() {
        String path = queryFile.getFileName().toString();
        return path.substring(0, path.length() - 4);
    }

    public String getSql() {
        return sql;
    }

    public String getNameSpace() {
        Path sub = sourceDirectory.relativize(queryFile.getParent());

        String namespace = StreamSupport.stream(sub.spliterator(), false)
                .map(p -> p.toString())
                .collect(Collectors.joining("."));

        return namespace;
    }

    public Path getQueryFile() {
        return queryFile;
    }

    @Override
    public String toString() {
        return "QuerySource{" +
                "queryFile=" + queryFile +
                ", sql='" + sql + '\'' +
                '}';
    }
}
