TextTest Maven Plugin
=====================

Use this plugin in your maven project to run your texttests.

Note that as yet this plugin is not generally downloadable, you'll have to build and install it yourself.

The self-tests should provide a good guide to usage, they are under src/it/texttest.

Basic usage:

	<build>
        <plugins>
            <plugin>
                <groupId>org.texttest</groupId>
                <artifactId>texttest-maven-plugin</artifactId>
                <version>1.0</version>
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

If you have texttest.py on your $PATH then this plugin should work just fine. If you don't, then you'll want
to add this dependency to your POM:

	<dependencies>
		<dependency>
            <groupId>org.texttest</groupId>
            <artifactId>texttest-runner</artifactId>
            <version>3.26</version>
        </dependency>
    </dependencies>

Note that this artifact is not yet published to any central maven repository. We're working on that.