# GitHubUpdater
This code is a rewrite of Connor Spencer Harries's code which is not available anymore<br>
This updater is for bukkit developer who want to publish there plugin on GitHub


##How it works ?
The updater while :
- [x] check your release repository for new version
- [x] download the new release to the update folder
- [ ] ~~install the new release on server stop~~


##How to run the updater ?
first declare the actual plugin version and the repository of your plugin in your main class :
```java
private static Version version;
private static String repository;
```

Initialise those variables:
```java
version = Version.parse(getDescription().getVersion());
repository = "MyAccount/MyPlugin";
```

then call the updater :
```java
boolean download = true;

new BukkitRunnable()
{
    @Override
    public void run()
    {
        try
        {
            // Check for update
            Updater updater = new Updater(version, repository);

            // Update available
            Response response = updater.getResult();
            if (response == Response.SUCCESS)
                getServer().getLogger().info(ChatColor.GREEN + "[" + getDescription().getName() + "] New update is available: " + ChatColor.YELLOW + updater.getLatestVersion() + ChatColor.GREEN + "!");
            else
                return;

            // Download update to the update folder
            if(!download) return;
            updater.saveFile();
            response = updater.getResult();
            if (response == Response.SUCCESS_DOWNLOAD)
                getServer().getLogger().info(ChatColor.GREEN + "[" + getDescription().getName() + "] New update downloaded: " + ChatColor.YELLOW + updater.getLatestVersion() + ChatColor.GREEN + "!");
            else
                return;
        }
        catch (Exception e) { e.printStackTrace(); }
    }

}.runTaskLaterAsynchronously(this, 60);
```