package edu.illinois.mitra.template;

import java.util.HashMap;
import java.util.LinkedList;
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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.gvh.RealGlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.objects.Common;

public class RobotsActivity extends Activity implements MessageListener {
	private static final String TAG = "RobotsActivity";
	private static final String ERR = "Critical Error";

	private static final boolean ENABLE_TRACING = false;
	
	private GlobalVarHolder gvh = null;
	public boolean launched = false;
	
	// THESE NAMES AND ASSOCIATED IP ADDRESSES ARE CONSTANT FOR TESTING.
	//									ALICE				BOB					CARLOS				DIANA
	private static final String [] mac = {"00:0A:3A:2E:C9:CA","00:0A:3A:2E:C8:21","00:0A:3A:2E:CB:73","00:0A:3A:2E:CB:43"};
	private static final String [] participants = {"Alice", "Bob", "Carlos", "Diana"};
	private static final String [] ips = {"192.168.1.101", "192.168.1.102", "192.168.1.103", "192.168.1.104"};
	
	// GUI variables
	private TextView txtRobotName;
	private TextView txtDebug;
	private ProgressBar pbBluetooth;
	private CheckBox cbGPS;
	private CheckBox cbBluetooth;
	private CheckBox cbRunning;
	private ProgressBar pbBattery;
	
	// SharedPreferences variables
	SharedPreferences prefs;
	private static final String PREF_SELECTED_ROBOT = "SELECTED_ROBOT";
	private int selected_robot = 0;
	
	// Logic thread executor
	private ExecutorService executor = Executors.newFixedThreadPool(1);
	private Future<LinkedList<Object>> results;
	
	private MainHandler main_handler;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.main);

        // Initialize preferences holder
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        selected_robot = prefs.getInt(PREF_SELECTED_ROBOT, 0);
        
        // Set up the GUI
        setupGUI();
        
        // Create the main handler
        main_handler = new MainHandler(this, pbBluetooth, pbBattery, cbGPS, cbBluetooth, cbRunning, txtDebug, executor);
        
        
        // Create the global variable holder
        HashMap<String,String> hm_participants = new HashMap<String,String>();
        for(int i = 0; i < 1; i++) {
        	hm_participants.put(participants[i], ips[i]);
        }        
        gvh = new RealGlobalVarHolder(participants[selected_robot], hm_participants, main_handler, mac[selected_robot]);
        // TODO: Determine if this works
        main_handler.setGvh(gvh);
        
        // Connect
        connect();
    }
    
    public void connect() {
		// Update GUI
        Log.d(TAG, gvh.id.getName());
        
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
		if(launched) executor.shutdownNow();
		launched = false;

		// Shut down persistent threads
		gvh.comms.stopComms();
		gvh.gps.stopGps();
		gvh.plat.moat.cancel();
	}
	
    public void launch(int numWaypoints, int runNum) {  
		if(!launched) {
			if(gvh.gps.getWaypointPositions().getNumPositions() == numWaypoints) {
				if(ENABLE_TRACING) gvh.trace.traceStart(runNum);
				launched = true;
				cbRunning.setChecked(true);
				
				gvh.trace.traceSync("APPLICATION LAUNCH");
				
	    		RobotMessage informLaunch = new RobotMessage("ALL", gvh.id.getName(), Common.MSG_ACTIVITYLAUNCH, new MessageContents(Common.intsToStrings(numWaypoints, runNum)));
	    		gvh.comms.addOutgoingMessage(informLaunch);
	    		results = executor.submit(new AppLogic(gvh));
			} else {
    			gvh.plat.sendMainToast("Should have " + numWaypoints + " waypoints, but I have " + gvh.gps.getWaypointPositions().getNumPositions());
    		}
		} 
	}

	private void setupGUI() {	
		txtRobotName = (TextView) findViewById(R.id.txtRobotName);
		cbGPS = (CheckBox) findViewById(R.id.cbGPS);
		cbBluetooth = (CheckBox) findViewById(R.id.cbBluetooth);
		cbRunning = (CheckBox) findViewById(R.id.cbRunning);
		txtDebug = (TextView) findViewById(R.id.tv_debug);
		pbBluetooth = (ProgressBar) findViewById(R.id.pb_bluetooth);
		pbBattery = (ProgressBar) findViewById(R.id.pbBattery);
		pbBattery.setMax(100);
		
		txtRobotName.setText(participants[selected_robot]);
		txtRobotName.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				AlertDialog.Builder sel_robot_builder = new AlertDialog.Builder(RobotsActivity.this);
				sel_robot_builder.setTitle("Who Am I?");
				sel_robot_builder.setItems(participants, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				    	selected_robot = item;
				    	txtRobotName.setText(participants[selected_robot]);
						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
						SharedPreferences.Editor spe = prefs.edit();
						spe.putInt(PREF_SELECTED_ROBOT, selected_robot);
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
		Log.e(TAG, "Exiting application");
		disconnect();
		finish();
		return;
	}
	
	@Override
	public void onStart()
    {
    	super.onStart();
    }
	
	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void messageReceied(RobotMessage m) {
		switch(m.getMID()) {
		case Common.MSG_ACTIVITYLAUNCH:
			gvh.plat.sendMainMsg(Common.MESSAGE_LAUNCH, Integer.parseInt(m.getContents(0)),  Integer.parseInt(m.getContents(1)));
			break;
		case Common.MSG_ACTIVITYABORT:
			gvh.plat.sendMainMsg(Common.MESSAGE_ABORT, null);
			break;
		}
	}
}
