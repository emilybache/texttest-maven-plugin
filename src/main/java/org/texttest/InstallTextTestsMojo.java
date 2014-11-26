package org.texttest;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;

import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * This goal will install your texttests globally on your machine, under the TEXTTEST_ROOT folder.
 * This is helpful if you want to run them using the TextTest GUI.
 */
@Mojo(name = "install-texttests",
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class InstallTextTestsMojo extends AbstractTextTestMojo {

    /**
     * Whether to install the texttests globally by putting a softlink to them under the 'texttest_root' folder.
     * If you set this to true then you must also specify 'texttest_root'
     * Defaults to false
     */
    @Parameter(property="install_globally", defaultValue = "false")
    private boolean installGlobally = false;

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
    private boolean addClasspathToTextTestEnvironment = true;

    /**
     * What folder to use for the "extra_search_directory" setting you may have in your config file
     * This plugin will write an interpreter_options file containing the CLASSPATH to this folder
     * if you set the property 'add_classpath' to true.
     */
    @Parameter(property="extra_search_directory", defaultValue = "${basedir}/target/texttest_extra_config")
    String extraSearchDirectory;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path texttestRootPath = getTexttestRootPath();
        if (installGlobally) {
            getLog().info("Will install TextTests globally with name " + appName + " under TEXTTEST_ROOT " + texttestRootPath);
            installUnderTexttestRoot(appName, texttestRootPath);
        }
        if (addClasspathToTextTestEnvironment) {
            getLog().info("Will add classpath to TextTest environment");
            createExtraConfigFiles();
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

}
