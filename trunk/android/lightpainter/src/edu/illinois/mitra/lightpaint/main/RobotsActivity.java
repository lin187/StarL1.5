package edu.illinois.mitra.lightpaint.main;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
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
	private static final String PREF_SELECTED_PAINTMODE = "SELECTED_PAINTMODE";
	private static final String PREF_SELECTED_ROBOT = "SELECTED_ROBOT";
	private int selected_robot = 0;
	
	// Logic thread executor
	private ExecutorService executor = Executors.newFixedThreadPool(1);
	private Future<LinkedList<Object>> results;
	
	// TODO: Lightpainting specific
	private CheckBox cbPaintMode;
	private boolean DISPLAY_MODE = true;
	private float overrideBrightness = 1.0f;
	private int reqBrightness = 100;
	private WindowManager.LayoutParams lp;	
	private View vi;
	public static final int MESSAGE_SCREEN = 50;
    public static final int MESSAGE_SCREEN_COLOR = 51;
	// TODO: END
	
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
	    		if(launched) executor.shutdownNow();
    			gvh.plat.moat.halt();
    			launched = false;
    			cbRunning.setChecked(false);
    			cbGPS.setChecked(false);
	    		break;
	    	case Common.MESSAGE_DEBUG:
	    		txtDebug.setText("DEBUG:\n" + (String) msg.obj);
	    		break;
	    	case Common.MESSAGE_BATTERY:
	    		pbBattery.setProgress((Integer) msg.obj);
	    		break;
	    	case MESSAGE_SCREEN:
	    		if(DISPLAY_MODE) {
	    			reqBrightness = (Integer) msg.obj;
		        	lp.screenBrightness = Common.cap(reqBrightness*overrideBrightness, 1f, 100f) / 100.0f;
		        	getWindow().setAttributes(lp);
	    		}
	    		break;
	    	case MESSAGE_SCREEN_COLOR:
	    		String colParse = "#" + (String) msg.obj;
	    		vi.setBackgroundColor(Color.parseColor(colParse));
	    		break;
	    	}	
    	}

    	private void launch(int numWaypoints, int runNum) {  
    		if(!launched) {
    			if(gvh.gps.getWaypointPositions().getNumPositions() == numWaypoints) {
    				if(DISPLAY_MODE) setContentView(vi);
    				
    				if(ENABLE_TRACING) gvh.trace.traceStart(runNum);
    				launched = true;
    				cbRunning.setChecked(true);
    				
    				gvh.trace.traceSync("APPLICATION LAUNCH");
    				
    	    		RobotMessage informLaunch = new RobotMessage("ALL", gvh.id.getName(), Common.MSG_ACTIVITYLAUNCH, new MessageContents(Common.intsToStrings(numWaypoints, runNum)));
    	    		gvh.comms.addOutgoingMessage(informLaunch);
    	    		results = executor.submit(new LogicThread(gvh));
				} else {
	    			gvh.plat.sendMainToast("Should have " + numWaypoints + " waypoints, but I have " + gvh.gps.getWaypointPositions().getNumPositions());
	    		}
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
        connect();
        
        // Set up brightness attribute 
        vi = new View(this);
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
		if(launched) executor.shutdownNow();
		launched = false;

		// Shut down persistent threads
		gvh.log.i(TAG, "Stopping comms");
		gvh.comms.stopComms();
		gvh.gps.stopGps();
		gvh.plat.moat.cancel();
	}

	private void setupGUI() {	
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
				AlertDialog.Builder sel_robot_builder = new AlertDialog.Builder(getApplicationContext());
				sel_robot_builder.setTitle("Who Am I?");
				sel_robot_builder.setItems(participants, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				    	selected_robot = item;
				    	txtRobotName.setText(participants[selected_robot]);
						spe.putInt(PREF_SELECTED_ROBOT, selected_robot);
						spe.commit();
				    }
				});
				AlertDialog sel_robot = sel_robot_builder.create();
				sel_robot.show();
			}
		});
		
		cbPaintMode = (CheckBox) findViewById(R.id.cbDebugMode);
		cbPaintMode.setChecked(prefs.getBoolean("PAINT_MODE", false));		
		
		cbPaintMode.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
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
	protected void onPause() {
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