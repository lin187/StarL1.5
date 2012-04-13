package edu.illinois.mitra.lightpaint.main;

import java.util.concurrent.ExecutorService;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.objects.Common;

public class MainHandler extends Handler {

	private RobotsActivity app;
	private Context appContext;
	private ProgressBar pbBluetooth;
	private ProgressBar pbBattery;
	private CheckBox cbGPS;
	private CheckBox cbBluetooth;
	private CheckBox cbRunning;
	private TextView txtDebug;
	
	private GlobalVarHolder gvh;
	private ExecutorService executor;
	
	public MainHandler(RobotsActivity app, ProgressBar pbBluetooth,
			ProgressBar pbBattery, CheckBox cbGPS, CheckBox cbBluetooth,
			CheckBox cbRunning, TextView txtDebug, ExecutorService executor) {
		super();
		this.app = app;
		this.appContext = app.getApplicationContext();
		this.pbBluetooth = pbBluetooth;
		this.pbBattery = pbBattery;
		this.cbGPS = cbGPS;
		this.cbBluetooth = cbBluetooth;
		this.cbRunning = cbRunning;
		this.txtDebug = txtDebug;
		this.executor = executor;
	}
	
	public void setGvh(GlobalVarHolder gvh) {
		this.gvh = gvh;
	}
	
	@Override
	public void handleMessage(Message msg) {
		super.handleMessage(msg);
    	switch(msg.what) {
    	case Common.MESSAGE_TOAST:
    		Toast.makeText(appContext, msg.obj.toString(), Toast.LENGTH_LONG).show();
    		break;
    	case Common.MESSAGE_LOCATION:
    		cbGPS.setChecked((Integer)msg.obj == Common.GPS_RECEIVING);
    		break;
    	case Common.MESSAGE_BLUETOOTH:
    		pbBluetooth.setVisibility((Integer)msg.obj == Common.BLUETOOTH_CONNECTING?View.VISIBLE:View.INVISIBLE);
    		cbBluetooth.setChecked((Integer)msg.obj ==  Common.BLUETOOTH_CONNECTED);
    		break;
    	case Common.MESSAGE_LAUNCH:
    		app.launch(msg.arg1, msg.arg2);
    		break;
    	case Common.MESSAGE_ABORT:
    		if(app.launched) executor.shutdownNow();
			gvh.plat.moat.halt();
			app.launched = false;
			cbRunning.setChecked(false);
			cbGPS.setChecked(false);
    		break;
    	case Common.MESSAGE_DEBUG:
    		txtDebug.setText("DEBUG:\n" + (String) msg.obj);
    		break;
    	case Common.MESSAGE_BATTERY:
    		pbBattery.setProgress((Integer) msg.obj);
    		break;
 		case RobotsActivity.MESSAGE_SCREEN:
			if(app.DISPLAY_MODE) {
				app.reqBrightness = (Integer) msg.obj;
	        	app.lp.screenBrightness = Common.cap(app.reqBrightness*app.overrideBrightness, 1f, 100f) / 100.0f;
	        	app.getWindow().setAttributes(app.lp);
			}
			break;
		case RobotsActivity.MESSAGE_SCREEN_COLOR:
			String colParse = "#" + (String) msg.obj;
			app.vi.setBackgroundColor(Color.parseColor(colParse));
			break;
    	}	
	}
}
