package org.texttest;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * This goal will run a texttest test cases, using a command line 'batch session'.
 * For more information about batch sessions, refer to the documentation on http://texttest.org
 */
@Mojo(name = "run-texttests",
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        defaultPhase = LifecyclePhase.INTEGRATION_TEST)
public class RunTextTestsMojo extends AbstractTextTestMojo {

    /**
     * The name of the batch session configured in the texttest config file.
     */
    @Parameter(property="batch_session_name", defaultValue = "all")
    private String batchSessionName;

    /**
     * If this parameter is set to true, failing texttests will not fail the
     * whole build.
     */
    @Parameter(property="test_failure_ignore", defaultValue = "false")
    private boolean testFailureIgnore;

    /**
     * Where texttest should put test result files and files relating to test runs.
     */
    @Parameter(defaultValue = "${project.basedir}/target/sandbox", property="texttest_sandbox")
    private String sandbox;

    /**
     * If you don't have texttest installed already, maven will download the texttest-runner with this version.
     */
    @Parameter(property="texttest_version", defaultValue = "3.26")
    String texttestVersion;

    /**
     * The preferred way is to put the 'texttest' executable on your $PATH, which will happen if you install it with
     *     'pip install texttest'
     * Instead, you can use this property to specify a complete path to the 'texttest' executable.
     */
    @Parameter(property="texttest_executable")
    String texttestExecutable;

    /**
     * If you only want to run a selection of the tests, with test suites that match a certain string, you can use this parameter.
     * The settings 'testPathSelection' and 'testNameSelection'
     * correspond to the command line options '-ts' and '-t' respectively. See also http://texttest.sourceforge.net/index.php?page=documentation_3_26&n=static_gui#-ts
     */
    @Parameter(property="test_path_selection")
    String testPathSelection;

    /**
     * If you only want to run a selection of the tests, with test names that match a certain string, you can use this parameter.
     * The settings 'testPathSelection' and 'testNameSelection'
     * correspond to the command line options '-ts' and '-t' respectively. See also http://texttest.sourceforge.net/index.php?page=documentation_3_26&n=static_gui#-ts
     */
    @Parameter(property="test_name_selection")
    String testNameSelection;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path textTestExecutable = findTextTestExecutable();
        runTextTest(textTestExecutable);
    }

    Path findTextTestExecutable() throws MojoExecutionException {
        if (texttestExecutable != null) {
            File executable = new File(texttestExecutable);
            if (executable.exists()) {
                return executable.toPath();
            } else {
                throw new MojoExecutionException("Unable to run texttest. Parameter 'texttestExecutable' is specified as " + texttestExecutable + " but this file is not found. Please use 'pip install texttest' to install texttest on your system");
            }
        }
        Path textTestOnPath = findTextTestOnPath();
        if (textTestOnPath == null) {
            throw new MojoExecutionException("Unable to run texttest. 'texttestExecutable' parameter is not specified, and 'texttest' was not found on your $PATH. Please use 'pip install texttest' to install texttest on your system");
        }

        return textTestOnPath;
    }

    Path findTextTestOnPath() {
        String PATH = System.getenv("PATH");
        for (String pathDir: PATH.split(System.getProperty("path.separator"))) {
            Path possibleLocation = new File(pathDir).toPath().resolve("texttest");
            if (Files.exists(possibleLocation)) {
                getLog().info("found texttest on PATH at location: " + possibleLocation);
                return possibleLocation;
            }
        }
        getLog().info("texttest not found on PATH " + PATH);
        return null;
    }

    void runTextTest(Path textTestExecutable) throws MojoExecutionException {
        ArrayList<String> arguments = new ArrayList<String>();
        arguments.addAll(Arrays.asList(
                textTestExecutable.toString(),
                "-a", appName,
                "-b", batchSessionName,
                "-c", mavenProject.getBasedir().toString(),
                "-d", texttestLocation + System.getProperty("path.separator") + getTexttestRootPath().toString()
        ));
        if (testPathSelection != null) {
            arguments.addAll(Arrays.asList("-ts", testPathSelection));
        }
        if (testNameSelection != null) {
            arguments.addAll(Arrays.asList("-t", testNameSelection));
        }
        getLog().debug("Will start texttest with this command: " + arguments);
        ProcessBuilder textTest = new ProcessBuilder(arguments);

        textTest.environment().put("TEXTTEST_TMP", sandbox);
        textTest.redirectErrorStream(true);
        try {
            Process process = textTest.start();
            new OutputLogger(process.getInputStream(), getLog()).start();
            final int exitStatus = process.waitFor();
            if (exitStatus != 0 && !testFailureIgnore) {
                throw new MojoExecutionException("There were test failures");
            }
        } catch (IOException e) {
            getLog().error(e);
            throw new MojoExecutionException("TextTest failed to execute");
        } catch (InterruptedException e) {
            getLog().error(e);
            throw new MojoExecutionException("TextTest failed to execute");
        }
    }

    class OutputLogger extends Thread {
        private final Log log;
        private final InputStream inputStream;

        private OutputLogger(InputStream is, Log log) {
            this.inputStream = is;
            this.log = log;
        }

        @Override
        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(this.inputStream);
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                while ((line = br.readLine()) != null) {
                    log.info(line);
                }
            }
            catch (IOException e) {
                log.error(e);
            }
        }
    }

}
