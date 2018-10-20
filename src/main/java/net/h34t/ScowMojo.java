package net.h34t;


import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Collectors;

@Mojo(name = "scow", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class ScowMojo
        extends AbstractMojo {

    @Parameter(property = "scow.dsn", required = true)
    private String dsn;

    @Parameter(property = "scow.schema", required = true)
    private String schema;

    @Parameter(property = "scow.username")
    private String username;

    @Parameter(property = "scow.password")
    private String password;

    @Parameter(property = "scow.inputPath", defaultValue = "${project.directory}/sql-tpl", required = true)
    private String inputDirectory;


    @Parameter(property = "scow.outputPath", defaultValue = "${project.directory}/src/main/java", required = true)
    private String outputDirectory;

//    @Parameter(property = "scow.packagename", defaultValue = "scowgen", required = true)
//    private String packageName;

    @Parameter(property = "scow.jdbcdriver", defaultValue = "org.postgresql.Driver", required = true)
    private String jdbcDriver;

    public void execute() throws MojoExecutionException {
        try {
            Class.forName(jdbcDriver);

            Path queryDir = Paths.get(inputDirectory);
            Path outputDir = Paths.get(outputDirectory);

            getLog().info("query directory: " + queryDir.toString());
            getLog().info("output directory: " + outputDir.toString());

            // collect all query files
            Map<String, String> queries = Files.list(queryDir)
                    .filter(p -> p.toString().endsWith(".sql"))
                    .collect(Collectors.toMap(
                            path -> path.getFileName().toString().substring(0, path.getFileName().toString().length() - 4),
                            path -> {
                                try {
                                    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                                } catch (IOException ioe) {
                                    throw new RuntimeException(ioe);
                                }
                            }
                    ));

            new Sql2oQueryPojoCreator(dsn, schema, username, password)
                    .run(queries, outputDir);

        } catch (SQLException | ClassNotFoundException | IOException ioe) {
            throw new MojoExecutionException("IO Error", ioe);
        }
    }
}
