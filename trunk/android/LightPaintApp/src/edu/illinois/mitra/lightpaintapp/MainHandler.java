package edu.illinois.mitra.lightpaintapp;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Toast;
import edu.illinois.mitra.lightpaint.activity.LightPaintActivity;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.objects.HandlerMessage;

public class MainHandler extends Handler {

	private RobotsActivity app;
	private Context appContext;

	private GlobalVarHolder gvh;

	public MainHandler(RobotsActivity app) {
		super();
		this.app = app;
		this.appContext = app.getApplicationContext();

	}

	public void setGvh(GlobalVarHolder gvh) {
		this.gvh = gvh;
	}

	@Override
	public void handleMessage(Message msg) {
		super.handleMessage(msg);
		switch(msg.what) {
		case HandlerMessage.MESSAGE_TOAST:
			Toast.makeText(appContext, msg.obj.toString(), Toast.LENGTH_LONG).show();
			break;
		case HandlerMessage.MESSAGE_LOCATION:
			cbGpsValue = (Integer) msg.obj == HandlerMessage.GPS_RECEIVING;
			app.cbGPS.setChecked(cbGpsValue);
			break;
		case HandlerMessage.MESSAGE_BLUETOOTH:
			app.pbBluetooth.setVisibility((Integer) msg.obj == HandlerMessage.BLUETOOTH_CONNECTING ? View.VISIBLE : View.INVISIBLE);
			cbBluetoothValue = (Integer) msg.obj == HandlerMessage.BLUETOOTH_CONNECTED;
			app.cbBluetooth.setChecked(cbBluetoothValue);
			break;
		case HandlerMessage.MESSAGE_LAUNCH:
			app.launch(msg.arg1, msg.arg2);
			break;
		case HandlerMessage.MESSAGE_ABORT:
			if(app.launched)
				app.abort();
			gvh.plat.moat.motion_stop();
			app.launched = false;
			app.cbRunning.setChecked(false);
			break;
		case HandlerMessage.MESSAGE_DEBUG:
			app.txtDebug.setText("DEBUG:\n" + (String) msg.obj);
			break;
		case HandlerMessage.MESSAGE_BATTERY:
			app.pbBattery.setProgress((Integer) msg.obj);
			break;
		case LightPaintActivity.HANDLER_SCREEN:
			int color = msg.arg1;
			float linewidth = msg.arg2;

			if(linewidth < 0) {
				restoreWindow();
				break;
			} else if(!drawMode) {
				hideWindow();
			}
			System.out.println("Setting screen to " + color + " = " + linewidth);
			app.lp.screenBrightness = (color != 0) ? 1f : 1 / 100f;
			//app.lp.screenBrightness = 1f;
			app.getWindow().setAttributes(app.lp);
			illuminate.setColor(color);
			gvh.log.d("IC", "Set width to " + linewidth + " = " + (linewidth/MAX_LINEWIDTH));
			illuminate.setWidth(linewidth / MAX_LINEWIDTH);
			break;
		case LightPaintActivity.SCREEN_X:
			boolean xOn = (Boolean) msg.obj;
			illuminate.setX(xOn);
			if(xOn) {
				app.lp.screenBrightness = 1f;
				app.getWindow().setAttributes(app.lp);
			}
			break;
		}
	}

	private static final float MAX_LINEWIDTH = 10f;
	private boolean drawMode = false;
	private IlluminationControl illuminate;
	private boolean cbBluetoothValue = false;
	private boolean cbGpsValue = false;

	private void hideWindow() {
		drawMode = true;
		app.setContentView(R.layout.drawview);
		illuminate = (IlluminationControl) app.findViewById(R.id.illuminaitionControl1);
	}

	private void restoreWindow() {
		drawMode = false;
		app.lp.screenBrightness = -1f;
		app.getWindow().setAttributes(app.lp);
		app.setContentView(R.layout.main);
		app.setupGUI();

		app.cbBluetooth.setChecked(cbBluetoothValue);
		app.cbGPS.setChecked(cbGpsValue);
	}
}
