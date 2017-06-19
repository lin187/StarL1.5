package com.visis.testcontroller;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ehang.coptersdk.CopterControl;

import java.util.ArrayList;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.objects.HandlerMessage;

public class MainHandler extends Handler {

	private RobotsActivity app;
	private Context appContext;
	private ProgressBar pbBluetooth;
	private ProgressBar pbBattery;
	private CheckBox cbGPS;
	private CheckBox cbBluetooth;
	private CheckBox cbRunning;
	private CheckBox cbRegistered; //status of DJI SDK registration
	private TextView txtDebug;
	private CheckBox cbArm;
	private CheckBox manual;
	private CheckBox avatar;
	private TextView txtMode;
	private CheckBox copterConn;

	private GlobalVarHolder gvh;

	public MainHandler(RobotsActivity app, ProgressBar pbBluetooth, ProgressBar pbBattery, CheckBox cbGPS, CheckBox cbBluetooth, CheckBox cbRunning, TextView txtDebug, CheckBox cbRegistered, ArrayList<TextView> ga) {
		super();
		this.app = app;
		this.appContext = app.getApplicationContext();
		this.pbBluetooth = pbBluetooth;
		this.pbBattery = pbBattery;
		this.cbGPS = cbGPS;
		this.cbBluetooth = cbBluetooth;
		this.cbRunning = cbRunning;
		this.txtDebug = txtDebug;
		this.cbRegistered = cbRegistered;
		this.cbArm = (CheckBox)ga.get(0);
		this.manual= (CheckBox)ga.get(1);
		this.avatar= (CheckBox)ga.get(2);
		this.txtMode= ga.get(3);
		this.copterConn= (CheckBox)ga.get(4);
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
				cbGPS.setChecked((Integer) msg.obj == HandlerMessage.GPS_RECEIVING);
				break;
			case HandlerMessage.MESSAGE_BLUETOOTH:
				pbBluetooth.setVisibility((Integer) msg.obj == HandlerMessage.BLUETOOTH_CONNECTING ? View.VISIBLE : View.INVISIBLE);
				cbBluetooth.setChecked((Integer) msg.obj == HandlerMessage.BLUETOOTH_CONNECTED);
				break;
			case HandlerMessage.MESSAGE_LAUNCH:
		//		app.launch(msg.arg1, msg.arg2);
				break;
			case HandlerMessage.MESSAGE_ABORT:
				//if (app.launched)
				//	app.abort();
				gvh.plat.moat.motion_stop();
				//app.launched = false;
				cbRunning.setChecked(false);
				break;
			case HandlerMessage.MESSAGE_DEBUG:
				txtDebug.setText("DEBUG:\n" + (String) msg.obj);
				break;
			case HandlerMessage.MESSAGE_BATTERY:
				pbBattery.setProgress((Integer) msg.obj);
				break;
			case HandlerMessage.MESSAGE_REGISTERED_DJI:
				cbRegistered.setChecked((boolean) msg.obj);
				break;
			case HandlerMessage.MESSAGE_REGISTERED_3DR:
				cbRegistered.setChecked((boolean) msg.obj);
				break;
			case HandlerMessage.COPTER_CONNECTED:
				copterConn.setChecked((boolean) msg.obj);
				break;
			case HandlerMessage.MANUAL_MODE:
				manual.setChecked((boolean) msg.obj);
				break;
			case HandlerMessage.AVATAR_MODE:
				avatar.setChecked((boolean) msg.obj);
				break;
			case HandlerMessage.ARM_TOGGLE:
				cbArm.setChecked((boolean) msg.obj);
				break;
			case HandlerMessage.HEARTBEAT_MODE:
				if (CopterControl.getInstance().isCopterConnected()){
					gvh.plat.sendMainMsg(HandlerMessage.ARM_TOGGLE, CopterControl.getInstance().isArmed());
					gvh.plat.sendMainMsg(HandlerMessage.COPTER_CONNECTED, CopterControl.getInstance().isCopterConnected());
					txtMode.setText((String) msg.obj);
				}
		}
	}
}
