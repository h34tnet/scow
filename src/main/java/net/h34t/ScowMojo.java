package net.h34t;


import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "generate-dto", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class ScowMojo
        extends AbstractMojo {

    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    @Parameter(property = "scow.dsn", required = true)
    private String dsn;

    @Parameter(property = "scow.schema", required = true)
    private String schema;

    @Parameter(property = "scow.username")
    private String username;

    @Parameter(property = "scow.password")
    private String password;

    @Parameter(property = "scow.inputPath", defaultValue = "${project.directory}/sql-tpl", required = true)
    private String inputPath;

    @Parameter(property = "scow.outputPath", defaultValue = "${project.build.directory}/generated-sources/scow")
    private String outputPath;

    @Parameter(property = "scow.jdbcdriver", defaultValue = "org.postgresql.Driver", required = true)
    private String jdbcDriver;

    public void execute() throws MojoExecutionException {
        try {
            project.addCompileSourceRoot(outputPath);
            new Scow(getLog())
                    .run(inputPath,
                            outputPath,
                            dsn,
                            schema,
                            username,
                            password,
                            jdbcDriver
                    );

        } catch (Exception e) {
            throw new MojoExecutionException("An error occurred", e);
        }
    }
}
