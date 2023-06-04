package de.danoeh.antennapod.core.util;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.util.download.FeedUpdateManager;

public class UpdateApp {
    private Context context;
    private Handler handler;
    private ExecutorService executorService;
    private Future newVersionDownloadingFuture;
    private Object checkUpdateServiceToken = new Object();
    private Object downloadNewVersionServiceToken = new Object();

    private AlertDialog alertDialog;
    private ProgressBar progressBar;

    private TextView progressRatio;

    private String PREF_NAME = "UpdateAppPrefs";

    private String PREF_IGNORED_VERSION = "prefUpdateAppIgnoredVersion";

    private String TAG = "UpdateApp";

    private CheckUpdateService checkUpdateService;

    public UpdateApp(Context context){
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        this.handler = new Handler(Looper.getMainLooper()) {
            @SuppressLint("SetTextI18n")
            @Override
            public void handleMessage(Message msg) {
                Log.d(TAG, "handler receive message with " + msg.what);
                progressBar.setProgress(msg.what);
                progressRatio.setText(msg.what + "%");
                if(msg.what == 100){
                    alertDialog.dismiss();
                    installApk();
                }
            }
        };
    }

    public void checkUpdate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            handler.postDelayed(new CheckUpdateService(), checkUpdateServiceToken, 10000);
        }

    }

    public class AppUpdateAvailableEvent{
        public String apkUrl;
        public String updateSummary;
        AppUpdateAvailableEvent(String apkUrl, String updateSummary){
            this.apkUrl = apkUrl;
            this.updateSummary = updateSummary;
        }
    }

    private class CheckUpdateCallable implements Callable<JSONObject>{

        @Override
        public JSONObject call() throws Exception {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(TransApi.APP_VERSION_UPDATE_URL);
                conn = (HttpURLConnection) url.openConnection();
                InputStream in = new BufferedInputStream(conn.getInputStream());
                String res = convertStreamToString(in);
                return new JSONObject(res);
            }catch (Exception e){
                Log.d("UpdateApp", "failed to get new version info " + e.toString());
                return null;
            }
        }

        private String convertStreamToString(InputStream is) {
            java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }

    private boolean checkIgnoredVersion(String newVersion) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, 0);
        return prefs.getString(PREF_IGNORED_VERSION, "").equals(newVersion);
    }

    private void ignoredVersion(String newVersion) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, 0);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(PREF_IGNORED_VERSION, newVersion);
        edit.apply();
    }

    private class CheckUpdateService implements  Runnable {

        @Override
        public void run() {
            Future<JSONObject> future = executorService.submit(new CheckUpdateCallable());
            try {
                JSONObject json = future.get(5, TimeUnit.SECONDS);
                if(json != null){
                    String latestVersion = json.getString("version_name");
                    String apkUrl = json.getString("apk_url");
                    String updateSummary = json.getString("update_summary");
                    String currentVersion = getCurrentVersion();
                    int apkSize = json.getInt("apk_size");
                    if (latestVersion.compareTo(currentVersion) > 0 && !checkIgnoredVersion(latestVersion)) {
                        showUpdateDialog(apkUrl, updateSummary, latestVersion, apkSize);
                    }
                }
            } catch (Exception e) {
                Log.d("UpdateApp", "failed to get app update info " + e.toString());
            }
        }
    }

    public void showUpdateDialog(final String apkUrl, final String updateSummary, final String latestVersion, final int apkSize) {
        if(alertDialog == null){
            alertDialog = new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.transcript_update_app_title))
                    .setView(R.layout.app_update_dialog)
                    .setPositiveButton(context.getString(R.string.transcript_update_app_yes), null)  // null as the OnClickListener
                    .setNegativeButton(context.getString(R.string.transcript_update_app_no), null)  // null as the OnClickListener
                    .setNeutralButton(context.getString(R.string.transcript_update_app_later), null)
                    .create();

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    TextView message = alertDialog.findViewById(R.id.dialog_message);
                    message.setText(updateSummary);
                    progressBar = alertDialog.findViewById(R.id.dialog_progress_bar);
                    progressRatio = alertDialog.findViewById(R.id.dialog_progress_ratio);

                    Button positiveButton = ((AlertDialog) alertDialog).getButton(AlertDialog.BUTTON_POSITIVE);
                    positiveButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View view) {
                            if(progressBar.getVisibility() == View.INVISIBLE){
                                progressBar.setVisibility(View.VISIBLE);
                                DownloadNewVersionService downloadNewVersion = new DownloadNewVersionService(apkUrl, apkSize);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    handler.postDelayed(downloadNewVersion, downloadNewVersionServiceToken, 100);
                                }
                                positiveButton.setText(R.string.transcript_update_app_cancel);
                            }else{
                                newVersionDownloadingFuture.cancel(true);
                                alertDialog.dismiss();
                            }
                        }
                    });

                    Button negativeButton = ((AlertDialog) alertDialog).getButton(AlertDialog.BUTTON_NEGATIVE);
                    negativeButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View view) {
                            ignoredVersion(latestVersion);
                            alertDialog.dismiss();
                        }
                    });

                    Button neutralButton = ((AlertDialog) alertDialog).getButton(AlertDialog.BUTTON_NEUTRAL);
                    neutralButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View view) {
                            alertDialog.dismiss();
                        }
                    });
                }
            }, 100);
        }
        alertDialog.show();
    }

    private class DownloadNewVersionCallable implements Callable<Boolean>{

        private String apkUrl ;
        private int apkSize;

        DownloadNewVersionCallable(String apkUrl, int apkSize){
            this.apkUrl = apkUrl;
            this.apkSize = apkSize;
        }

        @Override
        public Boolean call() {
            String path = HttpGet.download(context,apkUrl, apkSize,"app_update", "app.apk", handler);
            return path != null;
        }

    }

    private class DownloadNewVersionService implements  Runnable {

        private  String apkUrl;
        private int apkSize;
        DownloadNewVersionService(String apkUrl, int apkSize){
            this.apkUrl = apkUrl;
            this.apkSize = apkSize;
        }

        @Override
        public void run() {
            Log.d(TAG, "DownloadNewVersionService run invoked");
            newVersionDownloadingFuture = executorService.submit(new DownloadNewVersionCallable(apkUrl,apkSize));
        }
    }

    private void installApk() {

        Log.d(TAG, "installApk invoked");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        File directory = new File(context.getFilesDir(), "app_update");
        String fileName = "app.apk";
        File apkFile = new File(directory, fileName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri apkUri = FileProvider.getUriForFile(context, context.getString(R.string.provider_authority), apkFile);
            intent.setData(apkUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
        }
        context.startActivity(intent);
    }

    private String getCurrentVersion() {
        PackageManager pm = context.getPackageManager();
        PackageInfo pInfo = null;

        try {
            pInfo = pm.getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e1) {
            e1.printStackTrace();
        }

        return pInfo.versionName;
    }

}
