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
 * This goal will run your texttests.
 */
@Mojo(name = "run-texttests",
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        defaultPhase = LifecyclePhase.INTEGRATION_TEST)
public class RunTextTestsMojo extends AbstractTextTestMojo {

    /**
     * The name of the batch session configured in the texttest config file.
     */
    @Parameter(alias="batch_session_name", defaultValue = "all")
    private String batchSessionName;

    /**
     * Where texttest should put test result files and files relating to test runs.
     */
    @Parameter(defaultValue = "${project.basedir}/target/sandbox")
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
     * This plugin will write an environment file containing the CLASSPATH to this folder
     * if you set the property 'add_classpath' to true.
     */
    @Parameter(property="extra_search_directory", defaultValue = "${basedir}/target/texttest_extra_config")
    String extraSearchDirectory;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path textTestExecutable = findTextTestExecutable();
        createExtraConfigFiles();
        runTextTest(textTestExecutable);
    }

    void createExtraConfigFiles() throws MojoExecutionException {
        if (addClasspathToTextTestEnvironment) {

            executeMojo(
                    plugin(groupId("org.apache.maven.plugins"), artifactId("maven-dependency-plugin"), version("2.8")),
                    goal("build-classpath"),
                    configuration(element(name("outputProperty"), "classpath")),
                    executionEnvironment(mavenProject, mavenSession, pluginManager));

            String[] classpathElements = new String[]{
                    mavenProject.getBuild().getOutputDirectory(),
                    mavenProject.getProperties().getProperty("classpath") // this property has just been set by the maven dependency plugin call above
            };
            getLog().debug("classpath elements for this project: " + Arrays.toString(classpathElements));
            writeClasspathToEnvironmentFile(classpathElements);
        }

    }

    void writeClasspathToEnvironmentFile(String[] classpathElements) throws MojoExecutionException {
        StringBuffer text = new StringBuffer();
        text.append("CLASSPATH:");
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
            Path classpathFile = textTestConfigPath.resolve("environment." + appName);
            List<String> lines = Arrays.asList(new String[]{text.toString()});
            Files.write(classpathFile, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            getLog().error(e);
            throw new MojoExecutionException("Unable to write configuration file for texttest containing the classpath");
        }
    }

    Path findTextTestExecutable() throws MojoExecutionException {

        Path textTestExecutable = findTextTestOnPath();
        if (textTestExecutable == null) {
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
            if (Files.exists(possibleLocation)) {
                getLog().info("found texttest on PATH at location: " + possibleLocation);
                return possibleLocation;
            }
        }
        getLog().info("texttest.py not found on PATH " + PATH);
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
            if (exitStatus != 0) {
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
