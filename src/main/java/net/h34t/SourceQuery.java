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

        return StreamSupport.stream(sub.spliterator(), false)
                .map(Path::toString)
                .collect(Collectors.joining("."));
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
