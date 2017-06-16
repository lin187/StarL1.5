package importsdkdemo.dji.com.ehangtemplate;

import android.app.Application;

import com.ehang.coptersdk.CopterControl;

/**
 * Created by alexaad1 on 6/7/2017.
 */

public class myApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // do stuff (prefs, etc)
        CopterControl.getInstance().init(this);
    }

}
