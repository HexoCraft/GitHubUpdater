# GitHubUpdater
This code is an update version of gravitylow updater design for GitHub instead of BukkitDev
This updater is for bukkit developers who want to publish there plugin on GitHub!


##How it works ?
The GitHubUpdater will:
- [x] check your release repository for new version. This mean that you should update your release on GitHub.
- [x] download the new release to the update folder, so it will be installed on the next restart.


##How to use it:
If you are using Maven to manage your project you can use my Maven repository to get the dependency.<br>
To do this, edit your pom.xml to add the following repository:
```java
        <!-- hexosse repository -->
        <repository>
            <id>kexosse-repo</id>
            <url>https://raw.github.com/hexosse/maven-repo/master/</url>
        </repository>
```
Then, add the following dependency:
```java
        <!--hexosse GitHubUpdater-->
        <dependency>
            <groupId>com.github.hexosse</groupId>
            <artifactId>GitHubUpdater</artifactId>
            <version>1.0.5</version>
        </dependency>
```
Otherwise, download the [source code](https://github.com/hexosse/GitHubUpdater/tree/master/src/main/java/com/github/hexosse/githubupdater) for GitHubUpdater. Simply place this somewhere within your plugin's packages.


##How to run the updater :
You just need one line of code to run GitHubUpdater :
```java
GitHubUpdater updater = new GitHubUpdater(this, this.repository, this.getFile(), GitHubUpdater.UpdateType.DEFAULT, true);
```
