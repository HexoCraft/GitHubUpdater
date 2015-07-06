# GitHubUpdater
This code is an update version of gravitylow updater design for GitHub instead of BukkitDev
This updater is for bukkit developers who want to publish there plugin on GitHub!


##How it works ?
The updater will :
- [x] check your release repository for new version. This mean that you should update your release on GitHub.
- [x] download the new release to the update folder, so it will be installed on the next restart.


##How to run the updater ?
Include GitHubUpdater class to your plugin then you just need one line of code to run the updater :
```java
GitHubUpdater updater = new GitHubUpdater(this, this.repository, this.getFile(), GitHubUpdater.UpdateType.DEFAULT, true);
```
