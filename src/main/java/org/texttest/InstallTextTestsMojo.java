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
     * Whether to install the texttests globally by putting a softlink to them under the 'texttest_root' folder.
     * If you set this to true then you must also specify 'texttest_root'
     * Defaults to false
     */
    @Parameter(property="install_globally", defaultValue = "false")
    private boolean installGlobally;

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

    /**
     * Whether to add the classpath to texttest's environment file for this application.
     * If you're testing an application on the JVM this is usually needed.
     */
    @Parameter(property="add_classpath", defaultValue = "true")
    private boolean addClasspathToTestEnvironment;

    /**
     * The short name of the texttest app - ie the file extension of the texttest config file.
     * Defaults to the artifactId of your project.
     */
    @Parameter(property="app_name", defaultValue = "${artifactId}")
    private String appName;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (appName == null) {
            appName = project.getArtifactId();
        }
        Path texttestRootPath = getTexttestRootPath();
        if (installGlobally) {
            getLog().info("Will install texttests globally with name " + appName + " under TEXTTEST_ROOT " + texttestRootPath);
            installUnderTexttestHome(appName, texttestRootPath);
        }
        //String classesDir = project.getBuild().getOutputDirectory();
        //String testsDir = project.getBuild().getTestOutputDirectory();

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

    Path getTexttestRootPath() throws MojoExecutionException {
        String[] defaultsInOrder = new String[]{texttestRoot, System.getenv("TEXTTEST_ROOT"), System.getenv("TEXTTEST_HOME")};
        for (String folder: defaultsInOrder) {
            getLog().debug("checking possible default for texttestRoot " + folder);
            if (folder != null && !"".equals(folder)) {
                getLog().debug("choosing texttestRoot: " + folder);
                return Paths.get(folder);
            }
        }
        if (installGlobally) {
            throw new MojoExecutionException("If you want to install the tests globally then you must specify 'texttest_root'");
        }
        return getWhereTheTestsAreInThisMavenProject();
    }

    Path getWhereTheTestsAreInThisMavenProject() {
        return project.getBasedir().toPath().resolve(Paths.get("src/it/texttest"));
    }


    public void setGlobalInstall(boolean shouldInstallGlobally) {
        this.installGlobally = shouldInstallGlobally;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

}
