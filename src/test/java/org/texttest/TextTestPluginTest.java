package org.texttest;


import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static junit.framework.Assert.*;

public class TextTestPluginTest {

    Path texttestHome;

    @Before
    public void createTempDir() throws IOException {
        texttestHome = Files.createTempDirectory("texttestHome");
    }


    @Test
    public void install_the_app_in_texttest_home() throws Exception {
        InstallTextTestsMojo target = new InstallTextTestsMojo();

        Path expectedLink = texttestHome.resolve("myapp");

        assertFalse(Files.isSymbolicLink(expectedLink));

        target.installTexttests("myapp", texttestHome);

        assertTrue(Files.isSymbolicLink(expectedLink));

    }

    @Test
    public void dont_install_app_if_parameter_is_set() throws MojoFailureException, MojoExecutionException {
        InstallTextTestsMojo target = new InstallTextTestsMojo();
        target.setGlobalInstall(false);

        target.installTexttests("myapp", texttestHome);

        Path expectedLink = texttestHome.resolve("myapp");
        assertFalse(Files.isSymbolicLink(expectedLink));

    }

    @Test
    public void execute() throws MojoFailureException, MojoExecutionException {
        InstallTextTestsMojo target = new InstallTextTestsMojo();
        target.setAppName("myapp");
        // no exception should be thrown
        target.execute();
    }
}