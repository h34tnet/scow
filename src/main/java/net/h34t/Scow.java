package net.h34t;

import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.stream.Collectors;

class Scow {

    private final Log log;

    Scow(Log log) {
        this.log = log;
    }

    static List<SourceQuery> getQuerySources(Path queryDir) throws IOException {
        return Files.find(queryDir, 64, (f, a) -> true)
                .filter(p -> p.toString().endsWith(".sql"))
                .map(path -> {
                    try {
                        return new SourceQuery(
                                queryDir,
                                path,
                                new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
                        );
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    static Path getOutputPath(Path outputDir, CompiledQuery cp) {
        Path pkg = Paths.get("", cp.getPackageName().split("\\."));
        return outputDir.resolve(pkg).resolve(cp.getClassname() + ".java");
    }

    void run(
            String inputPath,
            String outputPath,
            String dsn,
            String schema,
            String username,
            String password,
            String jdbcDriver
    ) {
        try {
            Class.forName(jdbcDriver);

            Path queryDir = Paths.get(inputPath);
            Path outputDir = Paths.get(outputPath);

            queryDir.getFileSystem().getPathMatcher("glob:**/*.sql");

            // collect all query files
            List<SourceQuery> queries = getQuerySources(queryDir);

            Connection con = DriverManager.getConnection(dsn, username, password);
            con.setSchema(schema);

            List<CompiledQuery> compiledQueries = new Sql2oPojoCreator()
                    .run(con, queries);

            compiledQueries.forEach(cp -> {
                try {
                    Path output = getOutputPath(outputDir, cp);

                    Files.createDirectories(output.getParent());

                    log.info("Writing " + cp.getPackageName() + "." + cp.getClassname() + " to " + output.toString());

                    Files.write(
                            output,
                            cp.getJavaSource().getBytes(StandardCharsets.UTF_8),
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
