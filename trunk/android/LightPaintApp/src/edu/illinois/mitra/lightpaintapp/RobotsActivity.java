package edu.illinois.mitra.lightpaintapp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import android.widget.Toast;
import edu.illinois.mitra.lightpaint.activity.LightPaintActivity;
import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.gvh.RealGlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.interfaces.TrackedRobot;
import edu.illinois.mitra.starl.models.Model_iRobot;
import edu.illinois.mitra.starl.objects.Common;
import edu.illinois.mitra.starl.objects.HandlerMessage;

public class RobotsActivity extends Activity implements MessageListener {
	private static final String TAG = "RobotsActivity";
	private static final String ERR = "Critical Error";

	private static final boolean ENABLE_TRACING = false;

	private GlobalVarHolder gvh = null;
	public boolean launched = false;

	private static final String IDENTITY_FILE_URL = "http://dl.dropbox.com/u/3255051/robots.rif"; // TODO: add to config file
	
	//private static final String IDENTITY_FILE_URL = "https://dl.dropbox.com/s/dwfqdhbf5vdtz18/robots.rif?dl=1";
	// http://dl.dropbox.com/u/3255051/robots.rif
	private static final String[][] ERROR_PARTICIPANTS = {{"ERROR"}, {"ERROR"}, {"ERROR"}};
	
	// Row 0 = names
	// Row 1 = MACs
	// Row 2 = IPs
	private String[][] participants;


	// SharedPreferences variables
	private static final String PREF_SELECTED_ROBOT = "SELECTED_ROBOT";
	private int selectedRobot = 0;

	// Logic thread executor
	private ExecutorService executor = Executors.newFixedThreadPool(1);
	private Future<List<Object>> results;
	private LogicThread runThread;
	private MainHandler mainHandler;

	public WindowManager.LayoutParams lp;
	public RelativeLayout mainLayout;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.main);

		// Load the participants
		participants = IdentityLoader.loadIdentities(IDENTITY_FILE_URL);
		if(participants == null)
			participants = ERROR_PARTICIPANTS;
		
		// Initialize preferences holder
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		selectedRobot = prefs.getInt(PREF_SELECTED_ROBOT, 0);

		if(selectedRobot >= participants[0].length) {
			Toast.makeText(this, "Identity error! Reselect robot identity", Toast.LENGTH_LONG).show();
			selectedRobot = 0;
		}
		
		// Set up the GUI
		setupGUI();

		// Create the main handler
		mainHandler = new MainHandler(this);

		// Create the global variable holder
		Map<String, String> mapParticipants = new HashMap<String, String>();
		for(int i = 0; i < participants[0].length; i++) {
			System.out.println("Putting " + participants[0][i] + "->" + participants[2][i]);
			mapParticipants.put(participants[0][i], participants[2][i]);
		}
		System.out.println("Creating a GVH for " + participants[0][selectedRobot] + ", " + participants[1][selectedRobot]);
		//gvh = new RealGlobalVarHolder(participants[0][selectedRobot], mapParticipants, mainHandler, participants[1][selectedRobot], this);
        // TD_NATHAN: you need to figure out what this initpos thing is, as the null in the next is probably going to crash it
        // initpos is the initial position of the robot (i think). it has type Model_irobot, Model_quadrotor, so it is also used to check the bot type
        // for now I'm passing a Model_irobot with position (0,0). Not 100% sure this will work since I can't really test it.
        TrackedRobot initpos = new Model_iRobot(participants[0][selectedRobot], 0,0);
        gvh = new RealGlobalVarHolder(participants[0][selectedRobot], mapParticipants, initpos, mainHandler, participants[1][selectedRobot], this);
		
		System.out.println(gvh.id.getParticipants());
		
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

				gvh.trace.traceSync("APPLICATION LAUNCH", gvh.time());

				RobotMessage informLaunch = new RobotMessage("ALL", gvh.id.getName(), Common.MSG_ACTIVITYLAUNCH, new MessageContents(Common.intsToStrings(numWaypoints, runNum)));
				gvh.comms.addOutgoingMessage(informLaunch);
				
				mainHandler.obtainMessage(LightPaintActivity.HANDLER_SCREEN, 0, 0);

				gvh.plat.sendMainMsg(LightPaintActivity.HANDLER_SCREEN, 0,0);
				
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
		gvh.plat.sendMainMsg(LightPaintActivity.HANDLER_SCREEN, 0,-1);
		gvh.gps.getWaypointPositions().clear();
	}

	public void connect() {
		// Update GUI
		gvh.log.d(TAG, gvh.id.getName());

		// Begin persistent background threads
		gvh.comms.startComms();
		gvh.gps.startGps();

		// Register this as a listener
		gvh.comms.addMsgListener(this, Common.MSG_ACTIVITYLAUNCH, Common.MSG_ACTIVITYABORT);
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

	public TextView txtRobotName;
	public TextView txtDebug;
	public ProgressBar pbBluetooth;
	public CheckBox cbGPS;
	public CheckBox cbBluetooth;
	public CheckBox cbRunning;
	public ProgressBar pbBattery;

	public void setupGUI() {
		lp = getWindow().getAttributes();
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

		txtRobotName.setText(participants[0][selectedRobot]);
		txtRobotName.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				AlertDialog.Builder sel_robot_builder = new AlertDialog.Builder(RobotsActivity.this);
				sel_robot_builder.setTitle("Who Am I?");
				sel_robot_builder.setItems(participants[0], new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						selectedRobot = item;
						txtRobotName.setText(participants[0][selectedRobot]);
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

	@Override
	public void messageReceived(RobotMessage m) {
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
