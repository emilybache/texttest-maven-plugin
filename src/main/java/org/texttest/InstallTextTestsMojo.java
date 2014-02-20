package org.texttest;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Mojo(name = "install-texttests", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class InstallTextTestsMojo extends AbstractMojo {


    /**
     * Whether to install the texttests under the global TEXTTEST_HOME folder.
     * Defaults to true - ie they will be installed globally.
     */
    @Parameter(property = "install-texttests.globalInstall")
    private boolean shouldInstallGlobally = true;

    /**
     * The short name of the texttest app - ie the file extension of the config file.
     */
    @Parameter(property="install-texttests.appName", required = true)
    private String appName;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        installTexttests(appName, Paths.get(System.getenv("TEXTTEST_HOME")));
    }

    public void installTexttests(String appName, Path texttestHome) throws MojoExecutionException {
        if (shouldInstallGlobally) {
            installUnderTexttestHome(appName, texttestHome);
        }

    }

    public void installUnderTexttestHome(String appName, Path texttestHome) throws MojoExecutionException {
        Path theAppUnderTextTestHome = texttestHome.resolve(appName);
        getLog().info("Installing TextTests to path " + theAppUnderTextTestHome);

        Path whereTheTestsAre = Paths.get("src/it/texttest");
        try {
            if (Files.isSymbolicLink(theAppUnderTextTestHome)) {
                Files.delete(theAppUnderTextTestHome);
            }
            Files.createSymbolicLink(theAppUnderTextTestHome, whereTheTestsAre);
        } catch (IOException x) {
            x.printStackTrace();
            throw new MojoExecutionException("unable to install texttests for app " + appName + " under TEXTTEST_HOME " + texttestHome);
        } catch (UnsupportedOperationException x) {
            x.printStackTrace();
            // Some file systems do not support symbolic links.
            throw new MojoExecutionException("unable to install texttests for app " + appName + " under TEXTTEST_HOME " + texttestHome);
        }
    }

    public void setGlobalInstall(boolean shouldInstallGlobally) {
        this.shouldInstallGlobally = shouldInstallGlobally;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }
}
