package net.h34t;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

class Scow {

    private final Log log;

    Scow(Log log) {
        this.log = log;
    }

    /**
     * Finds all files with an sql ending in the queryDir and all its sub
     * directories, and reads the files.
     *
     * @param queryDir the directory to search
     * @return a list of SourceQuery objects (paths and file contents)
     * @throws IOException if reading a file fails
     */
    static List<SourceQuery> getQuerySources(Path queryDir) throws IOException {
        final PathMatcher pm = queryDir.getFileSystem().getPathMatcher("glob:**/*.sql");

        return Files.find(queryDir, 64, (f, a) -> true)
                .filter(pm::matches)
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

    /**
     * Creates a path and a filename for the output file (java) from a
     * CompileQuery instance (path, package name, class name).
     *
     * @param outputDir the output directory for the java files
     * @param cp        the compiled query
     * @return the path for the java file
     */
    static Path getOutputPath(Path outputDir, CompiledQuery cp) {
        Path pkg = Paths.get("", cp.getPackageName().split("\\."));
        return outputDir.resolve(pkg).resolve(cp.getClassname() + ".java");
    }

    /**
     * This is the main program.
     * <p>
     * Read all input files, compile them, write the output files.
     *
     * @param inputPath  the base path to search for sql files
     * @param outputPath the base path to put the generated java files in
     * @param dsn        the database dsn
     * @param schema     the database schema
     * @param username   the database username (null if no user)
     * @param password   the database password (null if no password)
     * @param jdbcDriver the jdbc driver to use
     */
    void run(
            String inputPath,
            String outputPath,
            String dsn,
            String schema,
            String username,
            String password,
            String jdbcDriver) throws MojoFailureException {

        try {
            Class.forName(jdbcDriver);
        } catch (ClassNotFoundException e) {
            log.error("Could not find driver " + jdbcDriver);
            throw new MojoFailureException("Could not find driver " + jdbcDriver);
        }

        Path queryDir = Paths.get(inputPath);
        Path outputDir = Paths.get(outputPath);

        // collect all query files
        List<SourceQuery> queries;
        try {
            queries = getQuerySources(queryDir);

        } catch (IOException e) {
            throw new MojoFailureException("Could not read sql input files", e);
        }

        // open a database connection
        Connection con;
        try {
            con = DriverManager.getConnection(dsn, username, password);
            con.setSchema(schema);

        } catch (SQLException e) {
            String error =
                    String.format("Couldn't create database connection to \"%s\" with username \"%s\"" +
                                    ", using password: %s",
                            dsn,
                            username != null ? username : "<no username>",
                            password != null ? "<yes>" : "<no>");
            throw new MojoFailureException(error, e);

        }

        // compile all queries
        List<CompiledQuery> compiledQueries;
        try {
            compiledQueries = new Sql2oPojoCreator()
                    .run(con, queries);

        } catch (Exception e) {
            throw new MojoFailureException("Failed compiling a query", e);
        }

        // write all queries
        // this is done after compiling has succeeded for all input files, to avoid writing a partial new state
        // note that we _do_ have partial state if an IO error occurs
        try {
            compiledQueries.forEach(cp -> {
                Path output = null;
                try {
                    output = getOutputPath(outputDir, cp);

                    Files.createDirectories(output.getParent());

                    log.info("Writing " + cp.getPackageName() + "." + cp.getClassname() + " to " + output.toString());

                    Files.write(
                            output,
                            cp.getJavaSource().getBytes(StandardCharsets.UTF_8),
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

                } catch (IOException e) {
                    String error = String.format("Failed writing java output file to %s", output.toString());
                    log.error(error, e);
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            throw new MojoFailureException("Failure writing java files", e);
        }
    }
}
