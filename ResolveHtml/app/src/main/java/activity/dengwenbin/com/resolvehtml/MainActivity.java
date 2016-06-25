package activity.dengwenbin.com.resolvehtml;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.FileDownloader;
import org.wlf.filedownloader.listener.OnDetectBigUrlFileListener;
import org.wlf.filedownloader.listener.OnDownloadFileChangeListener;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private Document doc = null;
    private Context context;
    private String url = "https://www.zhihu.com/explore";
    private WebView webView;
    String oldEtag;
    String oldLastModified;
    String newEtag;
    String newLastModified;

    private OkHttpClient client;

    private String imgUrl = "file:///data/data/activity.dengwenbin.com.resolvehtml/files/HTML/assets/images";
    private String cssUrl = "file:///data/data/activity.dengwenbin.com.resolvehtml/files/HTML/assets/stylesheets";
    private String jsUrl = "file:///data/data/activity.dengwenbin.com.resolvehtml/files/HTML/assets/javascripts";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        try {
            isFileDir();
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileDownloader.registerDownloadStatusListener(onFileDownloadStatusListener);
        FileDownloader.registerDownloadFileChangeListener(onDownloadFileChangeListener);
        webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webView.setWebChromeClient(new WebChromeClient() {

        });
        downloadHtml();
    }

    //解析html
    public void getHtmlInfo(final String htmlInfo) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("再一次加载");
                    doc = Jsoup.parse(htmlInfo);
                    if (doc.equals(null)) {
                        doc = Jsoup.parse(htmlInfo);
                    } else if (!doc.equals(null)) {
                        getImgLink(doc);
                        getStylesheetLink(doc);
                        getJavaScript(doc);
                        Thread.sleep(3000);
                        loadWebView();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //解析html图片信息
    public void getImgLink(Document doc) throws IOException {
        Elements imgLink = doc.select("img[src]");
        for (Element imgLinks : imgLink) {
            StringBuffer oldSb = new StringBuffer(imgLinks.attr("src"));
            System.out.println(oldSb.toString());
            String str = oldSb.substring(oldSb.lastIndexOf("/") + 1, oldSb.length());
            if (oldSb.indexOf("http", 0) != -1) {
                startDownload(imgLinks.attr("src"), "img", str);
            } else {
                startDownload(url + "/" + imgLinks.attr("src"), "img", str);
            }
            imgLinks.attr("src", imgUrl + "/" + str);
        }
        saveIndexHtml();
    }


    //解析html样式信息
    public void getStylesheetLink(Document doc) throws IOException {
        Elements cssLink = doc.select("link[href]");
        for (Element cssLinks : cssLink) {
            if (cssLinks.attr("rel").equals("stylesheet")) {
                StringBuffer oldSb = new StringBuffer(cssLinks.attr("href"));
                String str = oldSb.substring(oldSb.lastIndexOf("/") + 1, oldSb.length());
                if (oldSb.indexOf("http", 0) != -1) {
                    startDownload(cssLinks.attr("href"), "css", str);
                } else {
                    startDownload(url + "/" + cssLinks.attr("href"), "css", str);
                }
                cssLinks.attr("href", cssUrl + "/" + str);
            }
        }


    }

    //解析htmlJs信息
    public void getJavaScript(Document doc) throws IOException {
        Elements javascriptLink = doc.select("script[src]");
        for (Element javascriptLinks : javascriptLink) {
            StringBuffer oldSb = new StringBuffer(javascriptLinks.attr("src"));
            String str = oldSb.substring(oldSb.lastIndexOf("/") + 1, oldSb.length());
            if (oldSb.indexOf("http", 0) != -1) {
                startDownload(javascriptLinks.attr("src"), "js", str);
            } else {
                startDownload(url + "/" + javascriptLinks.attr("src"), "js", str);
            }
            javascriptLinks.attr("src", jsUrl + "/" + str);
        }
        saveIndexHtml();

    }


    //下载状态监听
    private OnFileDownloadStatusListener onFileDownloadStatusListener = new OnFileDownloadStatusListener() {
        @Override
        public void onFileDownloadStatusWaiting(DownloadFileInfo downloadFileInfo) {

        }

        @Override
        public void onFileDownloadStatusPreparing(DownloadFileInfo downloadFileInfo) {

        }

        @Override
        public void onFileDownloadStatusPrepared(DownloadFileInfo downloadFileInfo) {

        }

        @Override
        public void onFileDownloadStatusDownloading(DownloadFileInfo downloadFileInfo, float downloadSpeed, long remainingTime) {

        }

        @Override
        public void onFileDownloadStatusPaused(DownloadFileInfo downloadFileInfo) {

        }

        @Override
        public void onFileDownloadStatusCompleted(DownloadFileInfo downloadFileInfo) {
            Log.e("links", "下载完成");


        }

        @Override
        public void onFileDownloadStatusFailed(String url, DownloadFileInfo downloadFileInfo, FileDownloadStatusFailReason failReason) {

        }
    };

    //下载文件改变监听
    private OnDownloadFileChangeListener onDownloadFileChangeListener = new OnDownloadFileChangeListener() {
        @Override
        public void onDownloadFileCreated(DownloadFileInfo downloadFileInfo) {

        }

        @Override
        public void onDownloadFileUpdated(DownloadFileInfo downloadFileInfo, Type type) {

        }

        @Override
        public void onDownloadFileDeleted(DownloadFileInfo downloadFileInfo) {

        }
    };


    //启动一个下载
    public void startDownload(String url, final String fileType, final String filesName) {
        FileDownloader.detect(url, new OnDetectBigUrlFileListener() {
            @Override
            public void onDetectNewDownloadFile(String url, String fileName, String saveDir, long fileSize) {
                if (fileType.equals("img")) {
                    FileDownloader.createAndStart(url, context.getFilesDir().getPath() + "//HTML//assets//images", filesName);
                } else if (fileType.equals("css")) {
                    FileDownloader.createAndStart(url, context.getFilesDir().getPath() + "//HTML//assets//stylesheets", filesName);
                } else if (fileType.equals("js")) {
                    FileDownloader.createAndStart(url, context.getFilesDir().getPath() + "//HTML//assets//javascripts", filesName);
                }
            }

            @Override
            public void onDetectUrlFileExist(String url) {

            }

            @Override
            public void onDetectUrlFileFailed(String url, DetectBigUrlFileFailReason failReason) {

            }
        });
    }

    //判断文件夹是否存在，不存在创建
    public void isFileDir() throws IOException {
        File htmlFileDir = new File(context.getFilesDir().getPath() + "//HTML");
        if (!htmlFileDir.exists()) {
            htmlFileDir.mkdirs();
        }
        File htmlFile = new File(context.getFilesDir() + "//HTML//index.html");
        if (!htmlFile.exists()) {
            htmlFile.createNewFile();
        }


        File imgFileDir = new File(context.getFilesDir().getPath() + "//HTML//assets//images");
        if (!imgFileDir.exists()) {
            imgFileDir.mkdirs();
        }

        File cssFileDir = new File(context.getFilesDir().getPath() + "//HTML//assets//stylesheets");
        if (!cssFileDir.exists()) {
            cssFileDir.mkdirs();
        }

        File jsFileDir = new File(context.getFilesDir().getPath() + "//HTML//assets//javascripts");
        if (!jsFileDir.exists()) {
            jsFileDir.mkdirs();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FileDownloader.unregisterDownloadStatusListener(onFileDownloadStatusListener);
        FileDownloader.unregisterDownloadFileChangeListener(onDownloadFileChangeListener);
    }


    public void downloadHtml() {
        int cacheSize = 10 * 1024 * 1024;
        Cache cache = new Cache(getCacheDir(), cacheSize);
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.cache(cache);
        client = builder.build();
        Request request = new Request.Builder().url("https://www.zhihu.com/explore")
                .header("User-Agent", "OkHttp Headers.java")
                .addHeader("Accept", "application/json; q=0.5")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                showDialog();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    newEtag = response.header("ETag");
                    if (newEtag == null) {
                        newEtag = "";
                    }
                    newLastModified = response.header("Last-Modified");
                    if (newLastModified.equals(null)) {
                        newLastModified = "";
                    }
                    SharedPreferences getCathe = context.getSharedPreferences("CACHE", MODE_PRIVATE);
                    oldEtag = getCathe.getString("ETag", "");
                    oldLastModified = getCathe.getString("Last-Modified", "");
                    System.out.println("  最后一次修改的时间" + oldLastModified);

                    //判读是否是第一次访问网页
                    if (oldEtag.equals(null) && oldLastModified.equals(null)) {
                        saveCache(newEtag, newLastModified);
                        getHtmlInfo(response.body().string());
                    } else if (newEtag.equals("") && newLastModified.equals("")) {
                        saveCache(newEtag, newLastModified);
                        getHtmlInfo(response.body().string());
                    } else if (newEtag.equals("")) {
                        if (newLastModified.equals(oldLastModified)) {
                            mHandler.obtainMessage(0).sendToTarget();
                        } else {
                            saveCache(newEtag, newLastModified);
                            getHtmlInfo(response.body().string());
                        }
                    } else if (oldLastModified.equals("")) {
                        if (newEtag.equals(oldEtag)) {
                            mHandler.obtainMessage(0).sendToTarget();
                        } else {
                            saveCache(newEtag, newLastModified);
                            getHtmlInfo(response.body().string());
                        }
                    }

                }

            }
        });
    }

    private void saveCache(String sTag, String lastModified) {

        SharedPreferences saveCache = context.getSharedPreferences("CACHE", MODE_PRIVATE);
        SharedPreferences.Editor editor = saveCache.edit();
        editor.putString("ETag", sTag);
        editor.putString("Last-Modified", lastModified);
        editor.commit();

    }

    public void loadWebView() {
        webView.post(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl("file:///" + context.getFilesDir() + "/HTML/index.html");
            }
        });
    }


    public void saveIndexHtml() throws IOException {
        String str = doc.html();
        InputStream is = new ByteArrayInputStream(str.getBytes());
        File file = new File(context.getFilesDir().getPath() + "//HTML", "index.html");

        FileOutputStream fos = new FileOutputStream(file);
        byte[] bytes = new byte[2048];
        int len = 0;
        while ((len = is.read(bytes)) != -1) {
            fos.write(bytes, 0, len);
        }
        fos.flush();
        fos.close();
        is.close();
    }

    public void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("网页未更新");
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                loadWebView();
            }
        });
        builder.create().show();
    }

    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg) { //该方法是在UI主线程中执行
            switch(msg.what) {
                case 0:
                    showDialog();
                    break;
            }
        };
    };


}
