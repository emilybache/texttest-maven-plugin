package org.texttest;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class AbstractTextTestMojo extends AbstractMojo {

    /**
     * The project currently being built.
     */
    @Parameter(required=true, readonly = true, defaultValue = "${project}")
    protected MavenProject mavenProject;

    /**
     * The current Maven session.
     */
    @Parameter(required=true, readonly=true, defaultValue = "${session}")
    protected MavenSession mavenSession;

    /**
     * The Maven BuildPluginManager component.
     */
    @Component
    protected BuildPluginManager pluginManager;

    /**
     * The short name of the texttest app - ie the file extension of the texttest config file.
     * Defaults to the artifactId of your project.
     */
    @Parameter(property="app_name", defaultValue = "${project.artifactId}")
    protected String appName;

    /**
     * The path to TEXTTEST_ROOT - ie where the texttest runner will find your test cases.
     * If the parameter "install_globally" is set to true, then this must be set, and is expected
     * to be a global location on your machine where you may have many test suites installed.
     * If you don't set this value, we use the environment variable $TEXTTEST_ROOT
     * If that is not set, we will use the environment variable $TEXTTEST_HOME as a fallback option.
     * If neither are set, we use ${basedir}/src/it/texttest
     */
    @Parameter(property="texttest_root")
    private String texttestRoot;

    Path getTexttestRootPath() throws MojoExecutionException {
        String[] defaultsInOrder = new String[]{texttestRoot, System.getenv("TEXTTEST_ROOT"), System.getenv("TEXTTEST_HOME")};
        for (String folder: defaultsInOrder) {
            getLog().debug("checking possible default for texttestRoot " + folder);
            if (folder != null && !"".equals(folder)) {
                getLog().debug("choosing texttestRoot: " + folder);
                return Paths.get(folder);
            }
        }

        return getWhereTheTestsAreInThisMavenProject();
    }

    Path getWhereTheTestsAreInThisMavenProject() {
        return mavenProject.getBasedir().toPath().resolve(Paths.get("src/it/texttest"));
    }

}
