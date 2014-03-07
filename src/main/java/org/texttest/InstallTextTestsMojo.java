package org.texttest;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * This goal will set up your texttests so they can be run by the run-texttests goal.
 */
@Mojo(name = "install-texttests", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class InstallTextTestsMojo extends AbstractTextTestMojo {

    /**
     * Whether to install the texttests globally by putting a softlink to them under the 'texttest_root' folder.
     * If you set this to true then you must also specify 'texttest_root'
     * Defaults to false
     */
    @Parameter(property="install_globally", defaultValue = "false")
    private boolean installGlobally;

    /**
     * Whether to add the classpath to texttest's environment file for this application.
     * If you're testing an application on the JVM this is usually needed.
     */
    @Parameter(property="add_classpath", defaultValue = "true")
    private boolean addClasspathToTextTestEnvironment;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path texttestRootPath = getTexttestRootPath();
        if (installGlobally) {
            getLog().info("Will install texttests globally with name " + appName + " under TEXTTEST_ROOT " + texttestRootPath);
            installUnderTexttestHome(appName, texttestRootPath);
        }

        if (addClasspathToTextTestEnvironment) {
            String classesDir = mavenProject.getBuild().getOutputDirectory();
            executeMojo(
                    plugin(groupId("org.apache.maven.plugins"), artifactId("maven-dependency-plugin"), version("2.8")),
                    goal("build-classpath"),
                    configuration(element(name("outputFile"), new File(classesDir).toPath().resolve("classpath.txt").toString())),
                    executionEnvironment(mavenProject, mavenSession, pluginManager));
        }

    }

    public void installUnderTexttestHome(String appName, Path texttestRoot) throws MojoExecutionException {

        try {
            if (!Files.exists(texttestRoot)) {
                getLog().warn("TEXTTEST_ROOT did not exist, creating " + texttestRoot);
                Files.createDirectories(texttestRoot);
            }

            Path theAppUnderTextTestHome = texttestRoot.resolve(appName);
            Path whereTheTestsAre = getWhereTheTestsAreInThisMavenProject();

            if (Files.isSymbolicLink(theAppUnderTextTestHome)) {
                Files.delete(theAppUnderTextTestHome);
            }
            Files.createSymbolicLink(theAppUnderTextTestHome, whereTheTestsAre);
        } catch (IOException x) {
            getLog().error(x);
            throw new MojoExecutionException("unable to install texttests for app " + appName + " under TEXTTEST_ROOT " + texttestRoot);
        } catch (UnsupportedOperationException x) {
            // Some file systems do not support symbolic links.
            getLog().error(x);
            throw new MojoExecutionException("unable to install texttests for app " + appName + " under TEXTTEST_ROOT " + texttestRoot);
        }
    }


}
