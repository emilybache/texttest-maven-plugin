package org.texttest;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * This goal will run your texttests.
 */
@Mojo(name = "run-texttests")
public class RunTextTestsMojo extends AbstractTextTestMojo {

    @Parameter(alias="batch_session_name", defaultValue = "all")
    private String batchSessionName;

    @Parameter(defaultValue = "${project.reporting.outputDirectory}/sandbox")
    private String sandbox;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path textTestExecutable = findTextTestExecutable();
        runTextTest(textTestExecutable);
    }

    Path findTextTestExecutable() throws MojoExecutionException {

        Path textTestExecutable = findTextTestOnPath();
        if (textTestExecutable == null) {
            getLog().debug("texttest.py was not found on the PATH, will extract it from maven dependency");
            executeMojo(
                    plugin(groupId("org.apache.maven.plugins"), artifactId("maven-dependency-plugin"), version("2.8")),
                    goal("unpack"),
                    configuration(element(name("artifactItems"),
                            element(name("artifactItem"),
                                    element(name("artifactId"), "texttest-runner"),
                                    element(name("groupId"), "org.texttest"),
                                    element(name("version"), "2.26")))),
                    executionEnvironment(mavenProject, mavenSession, pluginManager));
            textTestExecutable = mavenProject.getBasedir().toPath().resolve("target/dependency/bin/texttest.py");
            if (!Files.exists(textTestExecutable)) {
                throw new MojoExecutionException("unpacked dependency 'texttest-runner' but it did not contain the texttest.py executable");
            }
        }

        return textTestExecutable;
    }

    Path findTextTestOnPath() {
        String PATH = System.getenv("PATH");
        for (String pathDir: PATH.split(System.getProperty("path.separator"))) {
            Path possibleLocation = new File(pathDir).toPath().resolve("texttest.py");
            getLog().debug("checking location: " + possibleLocation);
            if (Files.exists(possibleLocation)) {
                getLog().debug("found texttest on PATH at location: " + possibleLocation);
                return possibleLocation;
            }
        }
        return null;
    }

    void runTextTest(Path textTestExecutable) throws MojoExecutionException {
        String[] arguments = new String[]{
                textTestExecutable.toString(),
                "-a", appName,
                "-b", batchSessionName,
                "-c", mavenProject.getBasedir().toString(),
                "-d", getTexttestRootPath().toString()
        };
        getLog().debug("Will start texttest with this command: " + Arrays.toString(arguments));
        ProcessBuilder textTest = new ProcessBuilder(arguments);

        textTest.environment().put("TEXTTEST_TMP", sandbox);
        textTest.redirectErrorStream(true);
        try {
            Process process = textTest.start();
            new OutputLogger(process.getInputStream(), getLog()).start();
            final int exitStatus = process.waitFor();
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
