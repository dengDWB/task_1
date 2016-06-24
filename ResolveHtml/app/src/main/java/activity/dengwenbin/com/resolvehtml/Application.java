package activity.dengwenbin.com.resolvehtml;

import android.content.Context;
import android.os.Environment;

import org.wlf.filedownloader.FileDownloadConfiguration;
import org.wlf.filedownloader.FileDownloader;

import java.io.File;

/**
 * Created by 40284 on 2016/6/23.
 */
public class Application extends android.app.Application {

    public Context context;
    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        FileDownloader.init(FileDownloadConfiguration.createDefault(getApplicationContext()));
    }
}
