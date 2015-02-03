package com.weimj.imagedownloader;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.types.Photo;
import com.tumblr.jumblr.types.PhotoPost;
import com.tumblr.jumblr.types.PhotoSize;
import com.tumblr.jumblr.types.Post;
import com.tumblr.jumblr.types.User;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

public class DownloadService extends Service {
    private final static String LOG_TAG = "DownloadService";

    private CountDownTimer timer = null;

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class DownloadServiceBinder extends Binder {
        DownloadService getService() {
            return DownloadService.this;
        }
    }
    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new DownloadServiceBinder();

    public DownloadService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("DownloadService", "onBind");
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d("DownloadService", "onDestroy()");
    }

    @Override
    public void onCreate() {
        Log.d("DownloadService", "onCreate");
        timer = new CountDownTimer(1000, 1000) {
            public void onTick(long millisUntilFinished) {
                return;
            }
            public void onFinish() {
                startDownload();
            }
        };

        startDownload();
    }

    private void startDownload() {
        final SqliteManager sqliteManager = new SqliteManager(getApplicationContext());

        //final String type = DownloadAsync.DOWNLOAD_BASH_BOARD;
        final String type = TumblrPost.VIDEO_TYPE;

        String count = "100";

        DownloadAsync async = new DownloadAsync(sqliteManager) {
            @Override
            public void onDownloadFinish() {
                //timer.start();

                List<TumblrPost> posts = sqliteManager.select(type, 0L);
                if (posts == null || posts.isEmpty()) {
                    Log.e(LOG_TAG, "Error, nothing is download");
                }
                for (TumblrPost post : posts) {
                    Log.d(LOG_TAG, post.toString());
                }
            }
        };

        async.execute(new String[] {DownloadAsync.DOWNLOAD_BASH_BOARD, type, "0", "0", "0", count});
        /*
        DownloadThread dlThread = new DownloadThread(sqliteManager);
        dlThread.start();
        */
    }
}
