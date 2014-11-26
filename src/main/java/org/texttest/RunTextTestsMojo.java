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
     * Whether to add an environment file containing the CLASSPATH for this application.
     * If you're testing an application on the JVM this is usually needed.
     * <p>
     * This file will be put under ${project.basedir}/target/texttest_extra_config
     * and you should add this line to your config.appName:
     * <pre>
     * extra_search_directory:${TEXTTEST_CHECKOUT}/target/texttest_extra_config/
     * </pre>
     * Note that you can change this folder by setting the "extra_search_directory" property.
     */
    @Parameter(property="add_classpath", defaultValue = "true")
    private boolean addClasspathToTextTestEnvironment;

    /**
     * What folder to use for the "extra_search_directory" setting you may have in your config file
     * This plugin will write an interpreter_options file containing the CLASSPATH to this folder
     * if you set the property 'add_classpath' to true.
     */
    @Parameter(property="extra_search_directory", defaultValue = "${basedir}/target/texttest_extra_config")
    String extraSearchDirectory;

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


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path textTestExecutable = findTextTestExecutable();
        if (addClasspathToTextTestEnvironment) {
            createExtraConfigFiles();
        }
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

    void createExtraConfigFiles() throws MojoExecutionException {
        executeMojo(
                plugin(groupId("org.apache.maven.plugins"), artifactId("maven-dependency-plugin"), version("2.8")),
                goal("build-classpath"),
                configuration(element(name("outputProperty"), "classpath")),
                executionEnvironment(mavenProject, mavenSession, pluginManager));

        String[] classpathElements = new String[]{
                mavenProject.getBuild().getOutputDirectory(),
                mavenProject.getBuild().getTestOutputDirectory(),
                mavenProject.getProperties().getProperty("classpath") // this property has just been set by the maven dependency plugin call above
        };
        getLog().debug("classpath elements for this project: " + Arrays.toString(classpathElements));
        writeClasspathToEnvironmentFile(classpathElements);
    }

    void writeClasspathToEnvironmentFile(String[] classpathElements) throws MojoExecutionException {
        StringBuffer text = new StringBuffer();
        text.append("-cp ");
        for (String path: classpathElements) {
            if (path != null && !"".equals(path)) {
                text.append(path);
                text.append(System.getProperty("path.separator"));
            }
        }
        try {
            Path textTestConfigPath = Paths.get(extraSearchDirectory);
            if (!Files.exists(textTestConfigPath)) {
                Files.createDirectories(textTestConfigPath);
            }
            Path classpathFile = textTestConfigPath.resolve("interpreter_options." + appName);
            List<String> lines = Arrays.asList(new String[]{text.toString()});
            Files.write(classpathFile, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            getLog().error(e);
            throw new MojoExecutionException("Unable to write configuration file for texttest containing the classpath");
        }
    }

    void runTextTest(Path textTestExecutable) throws MojoExecutionException {
        String[] arguments = new String[]{
                textTestExecutable.toString(),
                "-a", appName,
                "-b", batchSessionName,
                "-c", mavenProject.getBasedir().toString(),
                "-d", texttestLocation + System.getProperty("path.separator") + getTexttestRootPath().toString()
        };
        getLog().debug("Will start texttest with this command: " + Arrays.toString(arguments));
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
