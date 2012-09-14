package edu.illinois.mitra.lightpaintapp;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import edu.illinois.mitra.lightpaintlib.activity.LightPaintActivity;
import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.gvh.RealGlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.objects.Common;
import edu.illinois.mitra.starl.objects.HandlerMessage;

public class RobotsActivity extends Activity implements MessageListener {
	private static final String TAG = "RobotsActivity";
	private static final String ERR = "Critical Error";

	private static final boolean ENABLE_TRACING = false;

	private GlobalVarHolder gvh = null;
	public boolean launched = false;

	// THESE NAMES AND ASSOCIATED IP ADDRESSES ARE CONSTANT FOR TESTING.
	//									ALICE				BOB					CARLOS				DIANA
	private static final String[] mac = { "00:0A:3A:2E:C9:CA", "00:0A:3A:2E:C8:21", "00:0A:3A:2E:CB:73", "00:0A:3A:2E:CB:43" };
	private static final String[] participants = { "Alice", "Bob", "Carlos", "Diana" };
	private static final String[] ips = { "192.168.1.101", "192.168.1.102", "192.168.1.103", "192.168.1.104" };

	// SharedPreferences variables
	private static final String PREF_SELECTED_ROBOT = "SELECTED_ROBOT";
	private int selectedRobot = 0;

	// Logic thread executor
	private ExecutorService executor = Executors.newFixedThreadPool(1);
	private Future<List<Object>> results;
	private LogicThread runThread;
	private MainHandler mainHandler;
	
	// TODO: Set lp to be the window parameters!!
	public WindowManager.LayoutParams lp;
	public RelativeLayout mainLayout;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.main);

		// Initialize preferences holder
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		selectedRobot = prefs.getInt(PREF_SELECTED_ROBOT, 0);

		// Set up the GUI
		setupGUI();

		// Create the main handler
		mainHandler = new MainHandler(this, pbBluetooth, pbBattery, cbGPS, cbBluetooth, cbRunning, txtDebug);

		// Create the global variable holder
		HashMap<String, String> hm_participants = new HashMap<String, String>();
		for(int i = 0; i < participants.length; i++) {
			hm_participants.put(participants[i], ips[i]);
		}
		gvh = new RealGlobalVarHolder(participants[selectedRobot], hm_participants, mainHandler, mac[selectedRobot]);
		mainHandler.setGvh(gvh);

		// Connect
		connect();

		createAppInstance(gvh);
	}

	public void createAppInstance(GlobalVarHolder gvh) {
		runThread = new LightPaintActivity(gvh);
	}

	public void launch(int numWaypoints, int runNum) {
		if(!launched) {
			if(gvh.gps.getWaypointPositions().getNumPositions() == numWaypoints) {
				if(ENABLE_TRACING)
					gvh.trace.traceStart(runNum);
				launched = true;
				cbRunning.setChecked(true);

				gvh.trace.traceSync("APPLICATION LAUNCH");

				RobotMessage informLaunch = new RobotMessage("ALL", gvh.id.getName(), Common.MSG_ACTIVITYLAUNCH, new MessageContents(Common.intsToStrings(numWaypoints, runNum)));
				gvh.comms.addOutgoingMessage(informLaunch);
				
				mainHandler.obtainMessage(LightPaintActivity.HANDLER_SCREEN, 0, 0);
				
				results = executor.submit(runThread);
			} else {
				gvh.plat.sendMainToast("Should have " + numWaypoints + " waypoints, but I have " + gvh.gps.getWaypointPositions().getNumPositions());
			}
		}
	}
	
	public void abort() {
		results.cancel(true);
		executor.shutdownNow();
		runThread.cancel();
		gvh.plat.moat.motion_stop();
		gvh.plat.moat.motion_resume();
		executor = Executors.newSingleThreadExecutor();
		createAppInstance(gvh);
	}

	public void connect() {
		// Update GUI
		gvh.log.d(TAG, gvh.id.getName());

		// Begin persistent background threads
		gvh.comms.startComms();
		gvh.gps.startGps();

		// Register this as a listener
		gvh.comms.addMsgListener(Common.MSG_ACTIVITYLAUNCH, this);
		gvh.comms.addMsgListener(Common.MSG_ACTIVITYABORT, this);
	}

	public void disconnect() {
		gvh.log.i(TAG, "Disconnecting and stopping all background threads");

		// Shut down the logic thread if it was running
		if(launched) {
			runThread.cancel();
			executor.shutdownNow();
		}
		launched = false;

		// Shut down persistent threads
		gvh.comms.stopComms();
		gvh.gps.stopGps();
		gvh.plat.moat.motion_stop();
		gvh.plat.moat.cancel();
	}

	private TextView txtRobotName;
	private TextView txtDebug;
	private ProgressBar pbBluetooth;
	private CheckBox cbGPS;
	private CheckBox cbBluetooth;
	private CheckBox cbRunning;
	private ProgressBar pbBattery;

	private void setupGUI() {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		mainLayout = (RelativeLayout) findViewById(R.id.mainLayout);
		txtRobotName = (TextView) findViewById(R.id.txtRobotName);
		cbGPS = (CheckBox) findViewById(R.id.cbGPS);
		cbBluetooth = (CheckBox) findViewById(R.id.cbBluetooth);
		cbRunning = (CheckBox) findViewById(R.id.cbRunning);
		txtDebug = (TextView) findViewById(R.id.tv_debug);
		pbBluetooth = (ProgressBar) findViewById(R.id.pb_bluetooth);
		pbBattery = (ProgressBar) findViewById(R.id.pbBattery);
		pbBattery.setMax(100);

		txtRobotName.setText(participants[selectedRobot]);
		txtRobotName.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				AlertDialog.Builder sel_robot_builder = new AlertDialog.Builder(RobotsActivity.this);
				sel_robot_builder.setTitle("Who Am I?");
				sel_robot_builder.setItems(participants, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						selectedRobot = item;
						txtRobotName.setText(participants[selectedRobot]);
						SharedPreferences.Editor spe = prefs.edit();
						spe.putInt(PREF_SELECTED_ROBOT, selectedRobot);
						spe.commit();
						// Restart the application
						Intent restart = getIntent();
						disconnect();
						finish();
						startActivity(restart);
					}
				});
				AlertDialog sel_robot = sel_robot_builder.create();
				sel_robot.show();
			}
		});
	}

	@Override
	public void onBackPressed() {
		gvh.log.e(TAG, "Exiting application");
		disconnect();
		finish();
		return;
	}

	public void messageReceied(RobotMessage m) {
		switch(m.getMID()) {
		case Common.MSG_ACTIVITYLAUNCH:
			gvh.plat.sendMainMsg(HandlerMessage.MESSAGE_LAUNCH, Integer.parseInt(m.getContents(0)), Integer.parseInt(m.getContents(1)));
			break;
		case Common.MSG_ACTIVITYABORT:
			gvh.plat.sendMainMsg(HandlerMessage.MESSAGE_ABORT, null);
			break;
		}
	}
}
