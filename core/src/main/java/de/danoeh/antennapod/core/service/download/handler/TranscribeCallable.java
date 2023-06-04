package de.danoeh.antennapod.core.service.download.handler;

/**
 * <pre>
 *     author : whatsthat
 *     date   : 09/05/2023
 *     desc   :
 * </pre>
 */

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.Date;
import java.util.concurrent.Callable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import de.danoeh.antennapod.core.util.TransApi;
import de.danoeh.antennapod.core.util.Translator;

public class TranscribeCallable implements Callable<String> {
    private String downloadUrl;

    private int fragment;
    private String action;

    private long feedMediaId;
    private long fileSize;

    private String podcastTitle;
    private String podcastWebsite;
    private String episodeTitle;
    private Date episodePubDate;

    public TranscribeCallable(String podcastTitle, String podcastWebsite, String episodeTitle, Date episodePubDate, String downloadUrl, long feedMediaId, long fileSize, int fragment, String action) {

        this.podcastTitle = podcastTitle;
        this.podcastWebsite = podcastWebsite;
        this.episodeTitle = episodeTitle;
        this.episodePubDate = episodePubDate;

        this.downloadUrl = downloadUrl;
        this.feedMediaId = feedMediaId;
        this.fileSize = fileSize;
        this.fragment = fragment;
        this.action = action;
    }
    @Override
    public String call() {
        TransApi api = new TransApi();
        return api.podcastTranscribe(podcastTitle,
                podcastWebsite,
                episodeTitle,
                episodePubDate,
                downloadUrl,
                feedMediaId,
                fileSize,
                action,
                fragment);
    }
}
