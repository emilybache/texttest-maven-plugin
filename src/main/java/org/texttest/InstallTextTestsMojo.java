package org.texttest;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;

import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This goal will set up your texttests so they can be run by the run-texttests goal.
 */
@Mojo(name = "install-texttests", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class InstallTextTestsMojo extends AbstractTextTestMojo {

    /**
     * Whether to install the texttests globally by putting a softlink to them under the 'texttest_root' folder.
     * If you set this to true then you must also specify 'texttest_root'
     * Defaults to false
     */
    @Parameter(property="install_globally", defaultValue = "false")
    private boolean installGlobally;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path texttestRootPath = getTexttestRootPath();
        if (installGlobally) {
            getLog().info("Will install texttests globally with name " + appName + " under TEXTTEST_ROOT " + texttestRootPath);
            installUnderTexttestRoot(appName, texttestRootPath);
        }
    }

    public void installUnderTexttestRoot(String appName, Path texttestRoot) throws MojoExecutionException {

        try {
            if (!Files.exists(texttestRoot)) {
                getLog().warn("TEXTTEST_ROOT did not exist, creating " + texttestRoot);
                Files.createDirectories(texttestRoot);
            }

            Path theAppUnderTextTestHome = texttestRoot.resolve(appName);
            Path whereTheTestsAre = Paths.get(texttestLocation);

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
