package org.texttest;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This maven plugin will set up your texttests so they can be run by Maven.
 */
@Mojo(name = "install-texttests")
public class InstallTextTestsMojo extends AbstractMojo {


    /**
     * The enclosing Maven project.
     */
    @Parameter( defaultValue = "${project}", readonly = true )
    protected MavenProject project;

    /**
     * Whether to install the texttests globally by putting a link to them under the $TEXTTEST_HOME folder.
     * Defaults to true
     */
    @Parameter(alias="install_globally", defaultValue = "true")
    private boolean installGlobally;

    /**
     * The path to TEXTTEST_HOME - ie where the texttest runner will expect to find test cases
     * Default to the value of the environment variable $TEXTTEST_HOME
     */
    @Parameter(alias="texttest_home")
    private Path texttestHome;

    /**
     * The short name of the texttest app - ie the file extension of the texttest config file.
     * Defaults to the artifactId of your project.
     */
    @Parameter
    private String appName;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (appName == null) {
            appName = project.getArtifactId();
        }
        if (texttestHome == null) {
            texttestHome = Paths.get(System.getenv("TEXTTEST_HOME"));
        }
        installTexttests(appName, texttestHome);
    }

    public void installTexttests(String appName, Path texttestHome) throws MojoExecutionException {
        if (installGlobally) {
            installUnderTexttestHome(appName, texttestHome);
        }
        //String classesDir = project.getBuild().getOutputDirectory();
        //String testsDir = project.getBuild().getTestOutputDirectory();

    }

    public void installUnderTexttestHome(String appName, Path texttestHome) throws MojoExecutionException {
        Path theAppUnderTextTestHome = texttestHome.resolve(appName);
        getLog().info("Installing TextTests under TEXTTEST_HOME " + texttestHome + " with folder name " + appName);

        Path whereTheTestsAre = project.getBasedir().toPath().resolve(Paths.get("src/it/texttest"));
        try {
            if (Files.isSymbolicLink(theAppUnderTextTestHome)) {
                Files.delete(theAppUnderTextTestHome);
            }
            Files.createSymbolicLink(theAppUnderTextTestHome, whereTheTestsAre);
        } catch (IOException x) {
            getLog().error(x);
            throw new MojoExecutionException("unable to install texttests for app " + appName + " under TEXTTEST_HOME " + texttestHome);
        } catch (UnsupportedOperationException x) {
            // Some file systems do not support symbolic links.
            getLog().error(x);
            throw new MojoExecutionException("unable to install texttests for app " + appName + " under TEXTTEST_HOME " + texttestHome);
        }
    }

    public void setGlobalInstall(boolean shouldInstallGlobally) {
        this.installGlobally = shouldInstallGlobally;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

}
