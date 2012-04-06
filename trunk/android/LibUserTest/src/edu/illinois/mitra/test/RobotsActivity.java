package edu.illinois.mitra.test;

import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import edu.illinois.mitra.starl.bluetooth.BluetoothInterface;
import edu.illinois.mitra.starl.bluetooth.MotionAutomaton;
import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.gvh.RealGlobalVarHolder;
import edu.illinois.mitra.starl.objects.Common;

public class RobotsActivity extends Activity {
	private static final String TAG = "RobotsActivity";
	private static final String ERR = "Critical Error";

	private GlobalVarHolder gvh = null;
	private static String gps_host = "192.168.1.100";
	private static int gps_port = 4000;
	private boolean connected = false;
	private boolean launched = false;
	
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
	
    // Persistent threads
    //private BluetoothRobotMotion motion = null;
    private MotionAutomaton moat = null;
    private LogicThread logic = null;
	
    private final Handler main_handler = new Handler() {
    	public void handleMessage(Message msg) {	    	
	    	switch(msg.what) {
	    	case Common.MESSAGE_TOAST:
	    		Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_LONG).show();
	    		break;
	    	case Common.MESSAGE_LOCATION:
	    		cbGPS.setChecked((Integer)msg.obj == Common.GPS_RECEIVING);
	    		break;
	    	case Common.MESSAGE_BLUETOOTH:
	    		pbBluetooth.setVisibility((Integer)msg.obj == Common.BLUETOOTH_CONNECTING?View.VISIBLE:View.INVISIBLE);
	    		cbBluetooth.setChecked((Integer)msg.obj ==  Common.BLUETOOTH_CONNECTED);
	    		break;
	    	case Common.MESSAGE_LAUNCH:
	    		launch(msg.arg1, msg.arg2);
	    		break;
	    	case Common.MESSAGE_ABORT:
    			// Disconnect and reconnect
    			connect_disconnect();
    			launched = false;
    			connect_disconnect();
	    		break;
	    		
	    	case Common.MESSAGE_DEBUG:
	    		txtDebug.setText("DEBUG:\n" + (String) msg.obj);
	    		break;
	    	case Common.MESSAGE_BATTERY:
	    		pbBattery.setProgress((Integer) msg.obj);
	    		break;
	    	}	
    	}

    	private void launch(int numWaypoints, int runNum) {    		
    		//gvh.traceStart(runNum);
    		
    		if(gvh.gps.getWaypointPositions().getNumPositions() == numWaypoints) {
    			if(!launched) {
    				// GUI Updates				
    				launched = true;
    				cbRunning.setChecked(true);
    				
    				gvh.trace.traceSync("APPLICATION LAUNCH");
    				
    				if(!moat.isAlive()) moat.start();
    				logic = new LogicThread(gvh, moat);
    				logic.start();
    				
    	    		RobotMessage informLaunch = new RobotMessage("ALL", gvh.id.getName(), Common.MSG_ACTIVITYLAUNCH, new MessageContents(Common.intsToStrings(numWaypoints, runNum)));
    	    		gvh.comms.addOutgoingMessage(informLaunch);
    			}
    		} else {
    			gvh.plat.sendMainToast("Should have " + numWaypoints + " waypoints, but I have " + gvh.gps.getWaypointPositions().getNumPositions());
    		}
    	}
    };
    
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
        
        // Create the global variable holder
        HashMap<String,String> hm_participants = new HashMap<String,String>();
        for(int i = 0; i < 1; i++) {
        	hm_participants.put(participants[i], ips[i]);
        }        
        gvh = new RealGlobalVarHolder(participants[selected_robot], hm_participants, main_handler, mac[selected_robot]);
        
        // Connect
        connect_disconnect();
    }
    
	private void connect_disconnect() {
		if(!connected) {
			// Update GUI
	        Log.d(TAG, gvh.id.getName());
	        gvh.plat.setDebugInfo("Connected");
	        
	        // Begin persistent background threads
	        gvh.comms.startComms();
	        gvh.gps.startGps();        
	        
	        if(moat == null) {
	        	BluetoothInterface bti = new BluetoothInterface(gvh, mac[selected_robot]);
	        	moat = new MotionAutomaton(gvh, bti);
	        }

		} else {
			// Shut down the logic thread if it was running
			if(launched) {
				logic.cancel();
				logic = null;
			}
			
			// Update GUI
			cbRunning.setChecked(false);
			gvh.plat.sendMainMsg(Common.MESSAGE_LOCATION, 0);
			gvh.plat.setDebugInfo("Disconnected");
			
			// Shut down persistent threads
			gvh.log.i(TAG, "Stopping comms");
			gvh.comms.stopComms();

			launched = false;
			
			moat.motionHalt();
		}	
		connected = !connected;
	}

	// Robot picker
	private void pick_robot() {
		AlertDialog.Builder sel_robot_builder = new AlertDialog.Builder(this);
		sel_robot_builder.setTitle("Who Am I?");
		sel_robot_builder.setItems(participants, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		    	selected_robot = item;
		    	txtRobotName.setText(participants[selected_robot]);
		    	saveToOptions();
		    }
		});
		AlertDialog sel_robot = sel_robot_builder.create();
		sel_robot.show();
	}

	// Save to options file
	private void saveToOptions() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor spe = prefs.edit();
		
		spe.putInt(PREF_SELECTED_ROBOT, selected_robot);
		spe.commit();
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
				pick_robot();
			}
		});
	}
	
	public void onStart()
    {
    	super.onStart();
    }
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	public void onBackPressed() {
		Log.e(TAG, "Exiting application");
		if(moat != null) {
			moat.cancel();
		}
		if(connected) {
			connect_disconnect();
		}
		gvh.gps.stopGps();
		finish();
		return;
	}
}
