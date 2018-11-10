package net.h34t;

import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * An unparsed query
 */
public class SourceQuery {

    private final Path sourceDirectory;
    private final Path queryFile;
    private final String sql;

    SourceQuery(Path sourceDirectory, Path queryFile, String sql) {
        this.sourceDirectory = sourceDirectory;
        this.queryFile = queryFile;
        this.sql = sql;
    }

    String getQueryName() {
        String path = queryFile.getFileName().toString();
        return path.substring(0, path.length() - 4);
    }

    String getClassName() {
        return getQueryName() + "Dto";
    }

    String getSql() {
        return sql;
    }

    String getNameSpace() {
        Path sub = sourceDirectory.relativize(queryFile.getParent());

        String namespace = StreamSupport.stream(sub.spliterator(), false)
                .map(p -> p.toString())
                .collect(Collectors.joining("."));

        return namespace;
    }

    Path getQueryFile() {
        return queryFile;
    }

    @Override
    public String toString() {
        return "SourceQuery{" +
                "queryFile=" + queryFile +
                ", sql='" + sql + '\'' +
                '}';
    }
}
