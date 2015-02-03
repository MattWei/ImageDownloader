package com.weimj.imagedownloader;

import android.os.Environment;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by weimj on 15/1/27.
 */
public class DownloadThread extends Thread {
    private static final String LOG_TAG = "DownloadThread";

    public static final String SAVE_PATH =
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/ImageDownloader/";

    public final static String DOWNLOAD_BASH_BOARD = "downloadDashBoard";
    public final static String GET_USER_INFO = "getUserInfo";

    private JumblrClient mClient = null;

    private boolean mIsStop = false;

    private SqliteManager mSqlManager = null;
    private String mJob = null;

    private static final int QUEUE_SIZE = 100;
    private ArrayBlockingQueue mFairQueue = null;

    private int mThreadSize = 10;
    private ArrayList<FileDownloadThread> mThreadList = null;

    private enum DOWNLOAD_STATUS {
        START,
        WAITING,
        DOWNLOADING,
        STOPPED
    };

    DownloadThread(SqliteManager sqliteManager) {
        this(sqliteManager, DOWNLOAD_BASH_BOARD, 0);
    }

    DownloadThread(SqliteManager sqlManager, String job, int threadSize) {
        // Authenticate via OAuth
        mClient = new JumblrClient(
                "i6ZbpJnTsiz4bYCVvFjNI3qBsXigY10HiTvBjihRRKKFCS0s4Y",
                "zFx6ibpCqASZuzJSHqCXumKycvo0l4eG5oaCcjrk6lydRsWEva"
        );
        mClient.setToken(
                "DkjGQ93vm8znDs4GH4ER5A8RUtl0qqgVUxewg1Wuhg1aBRI2HU",
                "VOKUVEAspQLL7bn615qS7a7Tq1k39PGVh3I99e8WmJxsPBjp1r"
        );

        mSqlManager = sqlManager;

        mJob = job;
        mFairQueue = new  ArrayBlockingQueue(QUEUE_SIZE);

        if (threadSize > 0)
            mThreadSize = threadSize;
    }

    class FileDownloadThread extends Thread {
        private DOWNLOAD_STATUS mStatus = DOWNLOAD_STATUS.START;

        @Override
        public void run() {
            if (mFairQueue == null) {
                Log.e(LOG_TAG, "mFairQueue is null");
                return;
            }

            try {
                while(!mIsStop || !mFairQueue.isEmpty()) {
                    mStatus = DOWNLOAD_STATUS.WAITING;
                    Log.e(LOG_TAG, "waiting mFairQueue");
                    TumblrPost post = (TumblrPost) mFairQueue.take();
                    Log.e(LOG_TAG, "Downloading");
                    mStatus = DOWNLOAD_STATUS.DOWNLOADING;
                    if (post != null)
                        DownloadFromUrl(post);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Log.e(LOG_TAG, "Download finish");
            mStatus = DOWNLOAD_STATUS.STOPPED;
        }

        public DOWNLOAD_STATUS getDownloadStatus() {
            return mStatus;
        }
    };

    private boolean createDownloadThread() {
        if (mThreadList == null) {
            mThreadList = new ArrayList<FileDownloadThread>(mThreadSize);
            for (int i = 0; i < mThreadSize; i++) {
                FileDownloadThread thread = new FileDownloadThread();
                mThreadList.add(thread);
                thread.start();
            }
        }
        return true;
    }

    @Override
    public void run() {
        mIsStop = false;

        Log.d(LOG_TAG, mJob);

        if (mJob.equals(DOWNLOAD_BASH_BOARD)) {
            if (!createDownloadThread())
                return;

            downloadDashboard("photo", 0, 0, 0);
        } else if (mJob.equals(GET_USER_INFO)) {
            getTumblrUser();
            Log.d(LOG_TAG, DOWNLOAD_BASH_BOARD);
        }
    }

    public void stopDownload() {
        mIsStop = true;
    }

    public void getTumblrUser() {
        User user = mClient.user();
        // Write the user's name
        Log.d(LOG_TAG, "User name:" + user.getName());

    }

    public boolean downloadDashboard(String type, int limit, int offset, int sinceId) {
        Map<String, Object> params = new HashMap<String, Object>();
        if (limit <= 0)
            limit = 20;

        params.put("limit", limit);

        if (offset > 0)
            params.put("offset", offset);
        else
            offset = 0;

        if (type != null)
            params.put("type", type);

        if (sinceId > 0)
            params.put("since_id", sinceId);

        List<Post> posts = mClient.userDashboard(params);
        if (posts.size() <= 0)
            return false;

        boolean downloadRes = downloadPosts(posts, type);

        //TODO just download once in test
        //mIsStop = true;
        while (downloadRes && !mIsStop) {
            offset += posts.size();
            downloadRes = downloadPosts(posts, type);
        }

        waitingForDownloadFinish();

        return downloadRes;
    }

    private void waitingForDownloadFinish() {
        Log.d(LOG_TAG, "Waiting all download finish");
        for (FileDownloadThread thread : mThreadList) {
            try {
                if (mIsStop && mFairQueue.isEmpty() &&
                        thread.getDownloadStatus() == DOWNLOAD_STATUS.WAITING) {
                    thread.interrupt();
                } else {
                    thread.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Log.d(LOG_TAG, "All downloads are finish");
    }

    public boolean downloadPosts(List<Post> posts, String type) {
        List<Photo> photos = null;
        PhotoSize photoSize = null;
        Log.d(LOG_TAG, "Download from id=" + posts.get(0).getId() +
                " to id=" + posts.get(posts.size() - 1).getId());

        Date dNow = new Date( );
        SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd");
        String saveDir = SAVE_PATH + type + "/" + ft.format(dNow) + "/";
        File file = new File(saveDir);

        if (file.exists() && !file.isDirectory()) {
            Log.d(LOG_TAG, "Remove and re-mkdir: " + saveDir + "\n");
            if (!file.delete() || !file.mkdir())
                return false;
        } else if (!file.exists()) {
            if (!file.mkdirs()) {
                Log.e(LOG_TAG, "Mkdir: " + saveDir + " error \n");
                return false;
            }
        }

        Log.d(LOG_TAG, "Download files:" + posts.size());

        for (int i = 0; i < posts.size(); i++) {
            photos = ((PhotoPost)posts.get(i)).getPhotos();
            for (Photo photo : photos) {
                photoSize = photo.getOriginalSize();
                Log.d(LOG_TAG, i + " Caption:" + photo.getCaption() +
                        " Width:" + photoSize.getWidth() +
                        " Height:" + photoSize.getHeight() +
                        " Url:" + photoSize.getUrl());

                try {
                    URL url = new URL(photoSize.getUrl()); //you can write here any link
                    String[] urlFile = url.getFile().split("/");
                    String fileName = urlFile[urlFile.length - 1];

                    TumblrPost post = new TumblrPost(
                            posts.get(i).getId(),
                            url,
                            fileName,
                            type,
                            saveDir + fileName,
                            0);

                    mFairQueue.put(post);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return false;
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }

        Log.d(LOG_TAG, "Download finish");
        return true;
    }

    public void DownloadFromUrl(TumblrPost post) {  //this is the downloader method
        try {

            URL url = post.getUrl();
            File file = new File(post.getPath());

            Log.d(LOG_TAG, "download " + post.getFileName() +
                    " from " + url + " to " + file.getAbsolutePath());

            /*
             * Define InputStreams to read from the URLConnection.
            */
            InputStream is = url.openConnection().getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);

            if (file.exists() ||
                    mSqlManager.isInserted(
                            post.getTumblrId(),
                            post.getFileName(),
                            post.getType())) {
                System.out.print("File exits, return\n");

                return;
            }

            FileOutputStream fos = new FileOutputStream(file);

            long startTime = System.currentTimeMillis();

            /*
            * Read bytes to the Buffer until there is nothing more to read(-1).
            */
            byte[] baf = new byte[1024];
            int read;
            int fileSize = 0;
            while ((read = bis.read(baf)) != -1) {
                fos.write(baf, 0, read);
                fileSize += read;
            }

            bis.close();
            fos.close();

            post.setPath(file.getAbsolutePath());
            post.setFileSize(fileSize);
            mSqlManager.insert(post);

            Log.d(LOG_TAG, "download ready in"
                    + ((System.currentTimeMillis() - startTime) / 1000)
                    + " sec \n");

        } catch (IOException e) {
            Log.d(LOG_TAG, "Error: " + e);
        }

    }
}