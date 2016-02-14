## Dependency JAR files within an executable JAR are not so easy
<br/>
##### Problem description

Last time I needed to create executable JAR dependent on other JARs. More precisely, I wanted to build executable JAR which
runs flyway migration (check *requirements & notes* paragraph). To achieve this goal I use *apache maven-assembly-plugin* [1].
I configured it to the result JAR contains all required dependencies (flyway and postgresql). I wrote custom descriptor
for plugin (*assembly/src.xml*) [2] and I added *Main-Class* and *Class-Path* manifest entries [3,4]. But when I tried to
run app I got an error (you can investigate it or recreate error by downloading sources, build it with maven and run *exec-jar-test-1.0.0-assembly.jar*):


```
$ java -jar exec-jar-test-1.0.0-assembly.jar <path-to-db-conf-file>
Exception in thread "main" java.lang.NoClassDefFoundError: org/flywaydb/core/Flyway
        at navi.blog.ExecJarTest.main(ExecJarTest.java:28)
Caused by: java.lang.ClassNotFoundException: org.flywaydb.core.Flyway
        at java.net.URLClassLoader.findClass(URLClassLoader.java:381)
        at java.lang.ClassLoader.loadClass(ClassLoader.java:424)
        at sun.misc.Launcher$AppClassLoader.loadClass(Launcher.java:331)
        at java.lang.ClassLoader.loadClass(ClassLoader.java:357)
        ... 1 more
```

*exec-jar-test-1.0.0-assembly.jar* structure:

<div align="center">
    <img src="https://drive.google.com/uc?export=view&id=0BxtqUI0hQlhbU1Z4SFVCVGxnd1U"
         alt="assembly plugin JAR - packed dependencies"
         title="assembly plugin JAR - packed dependencies" />
</div>

*MANIFEST.MF* file:

```
Manifest-Version: 1.0
Archiver-Version: Plexus Archiver
Built-By: narvi
Class-Path: postgresql-9.4.1207.jre7.jar flyway-core-3.2.1.jar
Created-By: Apache Maven 3.3.9
Build-Jdk: 1.8.0_72
Main-Class: navi.blog.ExecJarTest
```
What is the reason of the error?
<br/><br/><br/>


##### Requirements & notes (in braces I handed my versions)
- jdk (1.8.0_72) [1R]
- maven (3.3.9) [2R]
- db server (postgresql 9.4 [3R]). For example configuration check *db.conf.example* file.
- flyway (3.2.1) [4R], flyway is db versioning tool. To run example code you don't need to install anything,
maven dependency is enough. When flyway migration starts, it runs all migration scripts from given path
(by default it checks *db.migration* package in classpath).
<br/><br/><br/>


##### Investigation
To investigate the problem I added second plugin to *pom.xml*: *maven-shade-plugin* [5]. Thanks to it I got other JAR
file which works fine:

```
$ java -jar exec-jar-test-1.0.0.jar <path-to-db-conf-file>
Feb 13, 2016 3:45:32 PM org.flywaydb.core.internal.util.VersionPrinter printVersion
INFO: Flyway 3.2.1 by Boxfuse
Feb 13, 2016 3:45:32 PM org.flywaydb.core.internal.dbsupport.DbSupportFactory createDbSupport
INFO: Database: jdbc:postgresql://localhost:5432/exec-jar-test (PostgreSQL 9.4)
Feb 13, 2016 3:45:32 PM org.flywaydb.core.internal.command.DbValidate validate
INFO: Validated 1 migration (execution time 00:00.016s)
Feb 13, 2016 3:45:32 PM org.flywaydb.core.internal.metadatatable.MetaDataTableImpl createIfNotExists
INFO: Creating Metadata table: "public"."schema_version"
Feb 13, 2016 3:45:32 PM org.flywaydb.core.internal.command.DbMigrate migrate
INFO: Current version of schema "public": << Empty Schema >>
Feb 13, 2016 3:45:32 PM org.flywaydb.core.internal.command.DbMigrate applyMigration
INFO: Migrating schema "public" to version 1 - test-table
Feb 13, 2016 3:45:32 PM org.flywaydb.core.internal.command.DbMigrate logSummary
INFO: Successfully applied 1 migration to schema "public" (execution time 00:00.078s).
```

When I checked JAR it found out that all dependencies were unpacked:

<div align="center">
    <img src="https://drive.google.com/uc?export=view&id=0BxtqUI0hQlhbRGh3Q0ZiMnpMTXM"
         alt="shade plugin JAR - unpacked dependencies"
         title="shade plugin JAR - unpacked dependencies" />
</div>

Because all dependencies were given directly, *Manifest* file now needs only *Main-Class* entry, *Class-Path* is not needed:

```
Manifest-Version: 1.0
Archiver-Version: Plexus Archiver
Built-By: narvi
Created-By: Apache Maven 3.3.9
Build-Jdk: 1.8.0_72
Main-Class: navi.blog.ExecJarTest
```

First I tried to achieve the same result with *maven-assembly-plugin*. I deleted element `<addClasspath>` in *pom.xml*
and added `<unpack>` element to *src.xml*:

*pom.xml*:
```xml
<plugins>
    ...
        <plugin>
            <artifactId>maven-assembly-plugin</artifactId>
            <version>2.6</version>
            <configuration>
                <descriptors>
                    <descriptor>src/assembly/src.xml</descriptor>
                </descriptors>
                <archive>
                    <manifest>
                        <mainClass>navi.blog.ExecJarTest</mainClass>
                    </manifest>
                </archive>
            </configuration>
            <executions>
                <execution>
                    <id>make-assembly</id>
                    <phase>package</phase>
                    <goals>
                        <goal>single</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    ...
</plugins>
```

*src.xml*:
```xml
...
    <dependencySets>
        <dependencySet>
            <unpack>true</unpack>
            <outputDirectory>./</outputDirectory>
            <includes>
                <include>org.postgresql:postgresql</include>
                <include>org.flywaydb:flyway-core</include>
            </includes>
        </dependencySet>
    </dependencySets>
...
```

Result JAR file structure:
<div align="center">
    <img src="https://drive.google.com/uc?export=view&id=0BxtqUI0hQlhbOFZZTm1MbklZNm8"
         alt="assembly plugin JAR - unpacked dependencies"
         title="assembly plugin JAR - unpacked dependencies" />
</div>

and *MANIFEST.MF* file:
```
Manifest-Version: 1.0
Archiver-Version: Plexus Archiver
Built-By: narvi
Created-By: Apache Maven 3.3.9
Build-Jdk: 1.8.0_72
Main-Class: navi.blog.ExecJarTest
```

After it I got working *exec-jar-test-1.0.0-assembly.jar*. But there still was a question why the first JAR
with *Class-Path* manifest entry and packed dependencies is not working. I found answer in java documentation [6]:

> Note: The Class-Path header points to classes or JAR files on the local network, not JAR files within the JAR file
or classes accessible over Internet protocols. To load classes in JAR files within a JAR file into the class path,
you must write custom code to load those classes. For example, if MyJar.jar contains another JAR file called MyUtils.jar,
you cannot use the Class-Path header in MyJar.jar's manifest to load classes in MyUtils.jar into the class path.

I didn't want to write extra code so I chose to leave all dependencies unpacked.
<br/><br/><br/>

##### Sources
[1] http://maven.apache.org/plugins/maven-assembly-plugin/<br/>
[2] https://maven.apache.org/plugins/maven-assembly-plugin/advanced-descriptor-topics.html<br/>
[3] https://docs.oracle.com/javase/tutorial/deployment/jar/appman.html<br/>
[4] https://docs.oracle.com/javase/tutorial/deployment/jar/downman.html<br/>
[5] https://maven.apache.org/plugins/maven-shade-plugin/<br/>
[6] https://docs.oracle.com/javase/tutorial/deployment/jar/downman.html<br/>

[1R] http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html<br/>
[2R] https://maven.apache.org/<br/>
[3R] http://www.postgresql.org/<br/>
[4R] https://flywaydb.org/<br/>

<br/><br/>
##### Contact
I look forward to your comments, please contact:
* by email: narvi.blog@gmail.com
* by twitter: *@narviblog* user, https://twitter.com/narviblog
