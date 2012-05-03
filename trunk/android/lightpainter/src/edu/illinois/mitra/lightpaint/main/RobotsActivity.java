package edu.illinois.mitra.lightpaint.main;

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
import android.widget.TextView;
import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.gvh.RealGlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.interfaces.RobotEventListener;
import edu.illinois.mitra.starl.objects.Common;

public class RobotsActivity extends Activity implements MessageListener, RobotEventListener {
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
	private static final String PREF_SELECTED_PAINTMODE = "SELECTED_PAINTMODE";
	private static final String PREF_SELECTED_ROBOT = "SELECTED_ROBOT";
	private int selected_robot = 0;
	
	// Logic thread executor
	public ExecutorService executor = Executors.newFixedThreadPool(1);
	public Future<List<Object>> results;
	
	// TODO: Lightpainting specific
	private CheckBox cbPaintMode;
	public  boolean DISPLAY_MODE = true;
	public  float overrideBrightness = 1.0f;
	public  int reqBrightness = 100;
	public  WindowManager.LayoutParams lp;	
	public	View vi;
	public  static final int MESSAGE_SCREEN = 50;
    public  static final int MESSAGE_SCREEN_COLOR = 51;
    public AppLogic runThread;
	// TODO: END
	
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
        
        main_handler = new MainHandler(this, pbBluetooth, pbBattery, cbGPS, cbBluetooth, cbRunning, txtDebug);
        
        // Create the global variable holder
        HashMap<String,String> hm_participants = new HashMap<String,String>();
        for(int i = 0; i < participants.length; i++) {
        	hm_participants.put(participants[i], ips[i]);
        }        
        gvh = new RealGlobalVarHolder(participants[selected_robot], hm_participants, main_handler, mac[selected_robot]);
        main_handler.setGvh(gvh);
        
        // Connect
        connect();
        
        // Set up brightness attribute 
        vi = new View(this);
        
        runThread = new AppLogic(gvh);
    }
    
    public void launch(int numWaypoints, int runNum) {  
		if(!launched) {
			if(gvh.gps.getWaypointPositions().getNumPositions() == numWaypoints) {
				if(DISPLAY_MODE) setContentView(vi);
				
				if(ENABLE_TRACING) gvh.trace.traceStart(runNum);
				launched = true;
				cbRunning.setChecked(true);
				
				gvh.trace.traceSync("APPLICATION LAUNCH");
				
	    		RobotMessage informLaunch = new RobotMessage("ALL", gvh.id.getName(), Common.MSG_ACTIVITYLAUNCH, new MessageContents(Common.intsToStrings(numWaypoints, runNum)));
	    		gvh.comms.addOutgoingMessage(informLaunch);
	    		results = executor.submit(runThread);
			} else {
    			gvh.plat.sendMainToast("Should have " + numWaypoints + " waypoints, but I have " + gvh.gps.getWaypointPositions().getNumPositions());
    		}
		} 
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
    
	private void disconnect() {
		// Shut down the logic thread if it was running
		if(launched) {
			runThread.cancel();
			executor.shutdownNow();
		}
		launched = false;

		// Shut down persistent threads
		gvh.comms.stopComms();
		gvh.gps.stopGps();
		gvh.plat.moat.cancel();
	}

	public void setupGUI() {	
		lp = getWindow().getAttributes();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		final SharedPreferences.Editor spe = prefs.edit();
		
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
			public void onClick(View arg0) {
				AlertDialog.Builder sel_robot_builder = new AlertDialog.Builder(RobotsActivity.this);
				sel_robot_builder.setTitle("Who Am I?");
				sel_robot_builder.setItems(participants, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				    	selected_robot = item;
				    	txtRobotName.setText(participants[selected_robot]);
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
		
		cbPaintMode = (CheckBox) findViewById(R.id.cbDebugMode);
		cbPaintMode.setChecked(prefs.getBoolean(PREF_SELECTED_PAINTMODE, false));		
		DISPLAY_MODE = cbPaintMode.isChecked();
		
		cbPaintMode.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				DISPLAY_MODE = cbPaintMode.isChecked();
				spe.putBoolean(PREF_SELECTED_PAINTMODE, cbPaintMode.isChecked());	
				spe.commit();
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
	public void onStart()
    {
    	super.onStart();
    }
	
	@Override
	public void onPause() {
		super.onPause();
	}

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
	
	public void robotEvent(int type, int e) {
		if(type == Common.EVENT_MOTION) {
			switch(e) {
			case Common.MOT_TURNING:
				// Illuminate the screen if we're turning in place (assuming phones are in the middle!)
				overrideBrightness = 0.21f;	    			
				break;
			case Common.MOT_ARCING:
				// Illuminate the screen if we're arcing 
				overrideBrightness = 1f;
				break;
			case Common.MOT_STRAIGHT:
				// Illuminate the screen if we're going straight 
				overrideBrightness = 1f;	    
				break;
			case Common.MOT_STOPPED:
				// Darken the screen if we've stopped 
				overrideBrightness = 0f;	    
				break;
			}
			
			if(DISPLAY_MODE && launched) {
				gvh.plat.sendMainMsg(MESSAGE_SCREEN, reqBrightness);
			}
		}
	}
}