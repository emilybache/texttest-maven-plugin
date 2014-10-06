TextTest Maven Plugin
=====================

Use this plugin in your maven project to run your texttests.

The self-tests should provide a good guide to usage, they are under src/it/texttest.

Basic usage:

	<build>
        <plugins>
            <plugin>
                <groupId>org.texttest</groupId>
                <artifactId>texttest-maven-plugin</artifactId>
                <version>1.6</version>
                <executions>
                    <execution>
                        <id>run-texttests</id>
                        <goals>
                            <goal>install-texttests</goal>
                            <goal>run-texttests</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

Then run it with:

	mvn verify

You'll get a lot more information about how to configure this plugin from maven itself:

	mvn help:describe -Ddetail -Dplugin=org.texttest:texttest-maven-plugin


For Developers
--------------

Build this project with:

    mvn clean install

Run the self tests with:

    mvn -Prun-its verify

Set up a personal config file
-----------------------------

This is an optional step. If you do this, you will be able to run test interactively from the texttest gui. Your personal
config file is kept on this path by default:

    ~/.texttest/config

You might want some settings similar to these:

    [view_program]
    default:gedit
    [end]

    [diff_program]
    default:tkdiff
    [end]

    [checkout_location]
    texttest-maven-plugin:${HOME}/workspace/texttest-maven-plugin/
    [end]

If you have multiple monitors, texttest might not display its window in an appropriate width. You can adjust the
default window width and height by adding a setting like this:

    [window_size]
    width_screen:0.2
    [end]

This sets the proportion of the screen width it should use. There is more documentation on the personal config file
 on the texttest website: [http://texttest.sourceforge.net/index.php?page=documentation_3_27&n=personalpreffile](link)
