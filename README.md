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
                <version>1.1</version>
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

Build this project with

    mvn clean install

Run the self tests with

    mvn -Prun-its verify

