package com.github.hexosse.githubupdater;


import org.apache.commons.io.FilenameUtils;

import org.bukkit.Bukkit;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Class that tries to find updates on a GitHub repository.
 *
 * @author Connor Spencer Harries, hexosse
 * @version 1.0.2
 */
@SuppressWarnings("unused")
public class Updater {

    /**
     * Pattern used to match semantic versioning compliant strings.
     * <p/>
     * Major: matcher.group(1) Minor: matcher.group(2) Patch: matcher.group(3)
     * <p/>
     * Does detect suffixes such as RC though they're unused as of now.
     */
    protected static Pattern regex = Pattern.compile("(?:[v]?)([0-9]+)\\.([0-9]+)\\.([0-9]+)(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+[0-9A-Za-z-]+)?", Pattern.CASE_INSENSITIVE);

    /**
     * URL template for all API calls
     */
    protected static String api = "https://api.github.com/repos/{{ REPOSITORY }}/releases";

    /**
     * Store the query result.
     */
    private Response result = Response.NO_UPDATE;

    /**
     * User agent to use when making requests,
     * according to the API it's preferred if this is your username.
     *
     * See https://developer.github.com/v3/#user-agent-required
     */
    private String agent = "githubupdater";

    /**
     * Store the repository to lookup.
     */
    private String repository = null;

    /**
     * Should I print log?
     */
    private boolean verbose = false;

    /**
     * Store the <strong>latest</strong> version.
     */
    private Version version;

    /**
     * Store the version passed in the constructor.
     */
    private Version current;

    /**
     * Thread that does the heavy lifting.
     */
    private Thread thread;

    /**
     * URL to query.
     */
    private URL url;

    // BeYkeRYkt
    private String body;

    // hexosse
    // The folder that downloads will be placed in
    private final File updateFolder = Bukkit.getServer().getUpdateFolderFile();
    // The plugin update file url
    private String updateLink;
    // Used for downloading files
    private static final int BYTE_SIZE = 1024;

    /**
     * Create a new {@link Updater} using integers as the major, minor and
     * patch.
     *
     * @param major current major
     * @param minor current minor
     * @param patch current patch
     * @param repository github repository to query
     */
    public Updater(int major, int minor, int patch, String repository) throws Exception {
        this(Version.parse(major + "." + minor + "." + patch), repository, false);
    }

    /**
     * Create a new {@link Updater} using integers as the major, minor and
     * patch.
     *
     * @param major current major
     * @param minor current minor
     * @param patch current patch
     * @param repository github repository to query
     */
    public Updater(int major, int minor, int patch, String repository, boolean verbose) throws Exception {
        this(Version.parse(major + "." + minor + "." + patch), repository, verbose);
    }

    /**
     * Create a new {@link Updater} using a {@link java.lang.String}
     *
     * @param version string containing valid semver string
     * @param repository  github repository to query
     * @throws Exception error whilst parsing semver string
     */
    public Updater(String version, String repository) throws Exception {
        this(Version.parse(version), repository, false);
    }

    /**
     * Create a new {@link Updater} using a {@link java.lang.String}
     *
     * @param version string containing valid semver string
     * @param repository *            github repository to query
     * @param verbose print information to System.out
     * @throws Exception error whilst parsing semver string
     */
    public Updater(String version, String repository, boolean verbose) throws Exception {
        this(Version.parse(version), repository, verbose);
    }

    /**
     * Create a new {@link Updater} using a {@link Version}
     * object.
     *
     * @param repository github repository to query
     */
    public Updater(Version version, String repository) throws Exception {
        this(version, repository, false);
    }

    /**
     * Create a new {@link Updater} using a {@link java.lang.String}
     *
     * @param version string containing valid semver string
     * @param repository github repository to query
     * @param verbose print information to console
     * @throws Exception error whilst parsing semver string
     */
    public Updater(Version version, String repository, boolean verbose) throws Exception {
        if (version == null) {
            throw new Exception("Provided version is not semver compliant!");
        }

        this.repository = repository;
        this.current = version;
        this.verbose = verbose;

        try {
            this.url = new URL(api.replace("{{ REPOSITORY }}", this.repository));
            log(Level.INFO, "Set the URL to get");
        } catch (NumberFormatException ex) {
            log(Level.SEVERE, "Unable to parse semver string!");
            throw new Exception("Unable to parse semver string!");
        } catch (MalformedURLException ex) {
            log(Level.SEVERE, "Invalid URL, return failed response.");
            result = Response.FAILED;
            throw new Exception(ex.getMessage());
        }

        if (this.result != Response.FAILED) {
            this.thread = new Thread(new UpdaterRunnable(this));
            this.thread.start();
        }
    }

    /**
     * Actually lookup the JSON data.
     */
    private void run()
    {
        try
        {
            final URLConnection connection = url.openConnection();
            connection.setConnectTimeout(6000);

            connection.addRequestProperty("Accept", "application/vnd.github.v3+json");
            log(Level.INFO, "Opening connection to API");

            final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            final StringBuilder buffer = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null){
                buffer.append(line);
            }

            JSONArray releases = (JSONArray) JSONValue.parse(buffer.toString());
            log(Level.INFO, "Parsing the returned JSON");

            if (releases.isEmpty()) {
                log(Level.WARNING, "Appears there were no releases, setting result");
                this.result = Response.REPO_NO_RELEASES;
                return;
            }

            JSONObject release = (JSONObject) releases.get(0);
            String tag = release.get("tag_name").toString();

            // BeYkeRYkt
            String body = release.get("body").toString();

            // Hexosse
            JSONArray assets = (JSONArray) JSONValue.parse(release.get("assets").toString());
            JSONObject asset = (JSONObject) assets.get(0);
            String asset_url = asset.get("browser_download_url").toString();

            if (isSemver(tag))
            {
                this.version = Version.parse(tag);
                // BeYkeRYkt
                this.body = body;
                // Hexosse
                if(asset_url.length()>0 && asset_url.endsWith(".jar"))
                    this.updateLink = asset_url;

                if (current.compare(version)) {
                    log(Level.INFO, "Hooray, we found a semver compliant update!");
                    this.result = Response.SUCCESS;
                } else {
                    log(Level.INFO, "The version you specified is the latest version available!");
                    this.result = Response.NO_UPDATE;
                }
            } else {
                log(Level.WARNING, "Version string is not semver compliant!");
                this.result = Response.REPO_NOT_SEMVER;
            }
        }
        catch (Exception e)
        {
            if (e.getMessage().contains("HTTP response code: 403")) {
                log(Level.WARNING, "GitHub denied our HTTP request!");
                this.result = Response.GITHUB_DENY;
            } else if (e.getMessage().contains("HTTP response code: 404")) {
                log(Level.WARNING, "The specified repository could not be found!");
                this.result = Response.REPO_NOT_FOUND;
            } else if (e.getMessage().contains("HTTP response code: 500")) {
                log(Level.WARNING, "Internal server error");
                this.result = Response.GITHUB_ERROR;
            } else {
                log(Level.SEVERE, "Failed to check for updates!");
                this.result = Response.FAILED;
                this.version = null;
            }
        }
    }

    private void exit(Response response)
    {
        if (response != Response.SUCCESS) {
            this.result = response;
            this.version = null;
        }
    }

    /**
     * @return {@link java.lang.String} the version that GitHub tells us about.
     */
    public String getLatestVersion() {
        waitForThread();

        if (version == null) {
            log(Level.INFO, "Latest version is undefined, return message.");
            return "Please check #getResult()";
        }

        log(Level.INFO, "Somebody queried the latest version");
        return version.toString();
    }

    /**
     * @return {@link Response}
     */
    public Response getResult()
    {
        log(Level.INFO, "Somebody queried the update result");
        waitForThread();
        return this.result;
    }

    /**
     * @return the update repository
     */
    public String getRepository()
    {
        log(Level.INFO, "Somebody queried the repository");
        return this.repository;
    }

    // BeYkeRYkt
    public String getChangesDescription()
    {
        waitForThread();
        return body;
    }

    /**
     * Try and wait for the thread to finish executing.
     */
    private void waitForThread()
    {
        if ((this.thread != null) && this.thread.isAlive()) {
            try {
                this.thread.join();
                log(Level.INFO, "Trying to join thread");
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Test if the version string contains a valid semver string
     *
     * @param version version to test
     * @return true if valid
     */
    public static boolean isSemver(String version) {
        return regex.matcher(version).matches();
    }

    private void log(Level level, String message)
    {
        if(this.verbose)
            Bukkit.getLogger().log(level, message);
    }

    private void log(Level level, String message, Throwable thrown)
    {
        if(this.verbose)
            Bukkit.getLogger().log(level, message, thrown);
    }

        /**
         * Hexosse : Save an update from GitHub into the server's update folder.
         *
         */
    public void saveFile()
    {
        final File folder = this.updateFolder;

        deleteOldFiles();
        if (!folder.exists()) {
            this.fileIOOrError(folder, folder.mkdir(), true);
        }
        downloadFile();

        log(Level.INFO, "Finished updating.");
    }

    /**
     * Download a file and save it to the specified folder.
     */
    private void downloadFile()
    {
        BufferedInputStream in = null;
        FileOutputStream fout = null;
        try
        {
            URL fileUrl = new URL(this.updateLink);
            final int fileLength = fileUrl.openConnection().getContentLength();
            in = new BufferedInputStream(fileUrl.openStream());
            fout = new FileOutputStream(new File(this.updateFolder, FilenameUtils.getName(fileUrl.toString())));

            final byte[] data = new byte[Updater.BYTE_SIZE];
            int count;
            log(Level.INFO, "About to download a new update: " + getLatestVersion());

            long downloaded = 0;
            while ((count = in.read(data, 0, Updater.BYTE_SIZE)) != -1)
            {
                downloaded += count;
                fout.write(data, 0, count);
                final int percent = (int) ((downloaded * 100) / fileLength);
                if (((percent % 10) == 0)) {
                    log(Level.INFO, "Downloading update: " + percent + "% of " + fileLength + " bytes.");
                }
            }
            this.result = Response.SUCCESS_DOWNLOAD;
        } catch (Exception ex) {
            log(Level.WARNING, "The auto-updater tried to download a new update, but was unsuccessful.", ex);
            this.result = Response.FAILED_DOWNLOAD;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (final IOException ex) {
                log(Level.SEVERE, null, ex);
            }
            try {
                if (fout != null) {
                    fout.close();
                }
            } catch (final IOException ex) {
                log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Remove possibly leftover files from the update folder.
     */
    private void deleteOldFiles() {
        //Just a quick check to make sure we didn't leave any files from last time...
        File[] list = listFilesOrError(this.updateFolder);
        for (final File xFile : list) {
            if (xFile.getName().endsWith(".zip")) {
                this.fileIOOrError(xFile, xFile.mkdir(), true);
            }
        }
    }


    private File[] listFilesOrError(File folder) {
        File[] contents = folder.listFiles();
        if (contents == null) {
            log(Level.SEVERE, "The updater could not access files at: " + this.updateFolder.getAbsolutePath());
            return new File[0];
        } else {
            return contents;
        }
    }


    /**
     * Perform a file operation and log any errors if it fails.
     * @param file file operation is performed on.
     * @param result result of file operation.
     * @param create true if a file is being created, false if deleted.
     */
    private void fileIOOrError(File file, boolean result, boolean create) {
        if (!result) {
            log(Level.SEVERE, "The updater could not " + (create ? "create" : "delete") + " file at: " + file.getAbsolutePath());
        }
    }
}