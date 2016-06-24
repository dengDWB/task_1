package activity.dengwenbin.com.resolvehtml;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
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
    private Document content = null;
    private TextView showTv;
    private Context context;
    private ProgressDialog pd;
    private String url = "https://www.zhihu.com/explore";
    private List<String> imgList = new ArrayList<>();
    private List<String> cssList = new ArrayList<>();
    private List<String> jsList = new ArrayList<>();
    private WebView webView;
    String oldEtag;
    String oldLastModified;
    String newEtag;
    String newLastModified;
    boolean flag = true;

    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        showTv = (TextView) findViewById(R.id.showContent);
        context = this;
        isFileDir();
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
//        webView.loadUrl("https://www.zhihu.com/explore");


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
                        Thread.sleep(4000);
                        doc = Jsoup.parse(htmlInfo);
                    } else if (!doc.equals(null)) {
                        getImgLink(doc);
                        getStylesheetLink(doc);
                        getJavaScript(doc);
                        String str = doc.html();
                        InputStream is = new ByteArrayInputStream(str.getBytes());
                        Log.e("info", str);
                        File file = new File(context.getFilesDir().getPath() + "//HTML", "index.html");
                        if (!file.exists()) {
                            file.createNewFile();
                        }

                        FileOutputStream fos = new FileOutputStream(file);
                        byte[] bytes = new byte[2048];
                        int len = 0;
                        while ((len = is.read(bytes)) != -1) {
                            fos.write(bytes, 0, len);
                        }
                        fos.flush();
                        fos.close();
                        is.close();
                        webView.post(new Runnable() {
                            @Override
                            public void run() {
                                webView.loadUrl("file:///" + context.getFilesDir() + "/HTML/index.html");
                            }
                        });

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
    public void getImgLink(Document doc) {
        Elements imgLink = doc.select("img[src]");
        for (Element imgLinks : imgLink) {
            if (imgLinks.tagName().equals("img")) {
                Log.e("imgLinks", imgLinks.attr("src"));
                StringBuffer sb = new StringBuffer(imgLinks.attr("src"));
                if (sb.indexOf("http", 0) != -1) {
                    startDownload(imgLinks.attr("src"), "img");
                } else {
                    startDownload(url +"/"+imgLinks.attr("src"), "img");
                }
            }
        }
    }

    //解析html样式信息
    public void getStylesheetLink(Document doc) {
        Elements cssLink = doc.select("link[href]");
        for (Element cssLinks : cssLink) {
            if (cssLinks.attr("rel").equals("stylesheet")) {
                Log.e("cssLinks", cssLinks.attr("href"));
                StringBuffer sb = new StringBuffer(cssLinks.attr("href"));
                if (sb.indexOf("http", 0) != -1) {
                    startDownload(cssLinks.attr("href"), "css");
                } else {
                    startDownload(url+"/"+cssLinks.attr("href"), "css");
                    System.out.println(url+"/"+cssLinks.attr("href"));
                }
            }
        }
    }

    //解析htmlJs信息
    public void getJavaScript(Document doc) {
        Elements javascriptLink = doc.select("script[src]");
        for (Element javascriptLinks : javascriptLink) {
            if (javascriptLinks.attr("type").equals("text/javascript")) {
                Log.e("javascriptLinks", javascriptLinks.attr("src"));
                StringBuffer sb = new StringBuffer(javascriptLinks.attr("src"));
                if (sb.indexOf("http", 0) != -1) {
                    startDownload(javascriptLinks.attr("src"), "js");
                } else {
                    startDownload(url+"/"+javascriptLinks.attr("src"), "js");
                }
            }
        }

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
    public void startDownload(String url, final String fileType) {
        FileDownloader.detect(url, new OnDetectBigUrlFileListener() {
            @Override
            public void onDetectNewDownloadFile(String url, String fileName, String saveDir, long fileSize) {
                if (fileType.equals("img")) {
                    FileDownloader.createAndStart(url, context.getFilesDir().getPath() + "//HTML//assets//images", fileName);
                    imgList.add(fileName);
                } else if (fileType.equals("css")) {
                    FileDownloader.createAndStart(url, context.getFilesDir().getPath() + "//HTML//assets//stylesheets", fileName);
                    cssList.add(fileName);
                } else if (fileType.equals("js")) {
                    FileDownloader.createAndStart(url, context.getFilesDir().getPath() + "//HTML//assets//javascripts", fileName);
                    jsList.add(fileName);
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
    public void isFileDir() {
        File htmlFileDir = new File(context.getFilesDir().getPath() + "//HTML");
        if (!htmlFileDir.exists()) {
            htmlFileDir.mkdirs();
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

                    //判读是否是第一次访问网页
                    if (oldEtag.equals(null) && oldLastModified.equals(null)) {
                        saveCache(newEtag, newLastModified);
                        getHtmlInfo(response.body().string());
                    } else if (newEtag.equals("") && newLastModified.equals("")) {
                        getHtmlInfo(response.body().string());
                    } else if (newEtag.equals("")) {
                        if (newLastModified.equals(oldLastModified)) {
                            loadWebView();
                        } else {
                            saveCache(newEtag, newLastModified);
                            getHtmlInfo(response.body().string());
                        }
                    } else if (oldLastModified.equals("")) {
                        if (newEtag.equals(oldEtag)) {
                            loadWebView();
                        } else {
                            saveCache(newEtag, newLastModified);
                            getHtmlInfo(response.body().string());
                        }
                    }
                    System.out.println("newETag:" + newEtag + "oldETag:" + oldEtag);
                    System.out.println("newLast-Modified:" + newLastModified + "oldLast-Modified:" + oldEtag);

                }

            }
        });
    }

    private void saveCache(String sTag,String lastModified){

        SharedPreferences saveCache =context.getSharedPreferences("CACHE", MODE_PRIVATE);
        SharedPreferences.Editor editor=saveCache.edit();
        editor.putString("ETag", sTag);
        editor.putString("Last-Modified",lastModified);
        editor.commit();

    }

    public void loadWebView(){
        webView.post(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl("file:///" + context.getFilesDir() + "/HTML/index.html");
            }
        });
    }


}
