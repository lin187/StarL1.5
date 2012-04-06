package edu.illinois.mitra.lightpaint.main;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import edu.illinois.mitra.starl.bluetooth.BluetoothRobotMotion;
import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.gvh.RealGlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.Cancellable;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.interfaces.RobotEventListener;
import edu.illinois.mitra.starl.objects.Common;

public class RobotsActivity extends Activity implements MessageListener, RobotEventListener {
	private static final String TAG = "RobotsActivity";
	private static final String ERR = "Critical Error";
	
	private boolean DISPLAY_MODE = true;
	
	private GlobalVarHolder gvh = null;
	private boolean connected = false;
	private boolean launched = false;
	private Collection<Cancellable> created;
	
	// THESE NAMES AND ASSOCIATED IP ADDRESSES ARE CONSTANT FOR TESTING.
	//									ALICE				BOB					CARLOS				DIANA
	private static final String [] mac = {"00:0A:3A:2E:C9:CA","00:0A:3A:2E:C8:21","00:0A:3A:2E:CB:73","00:0A:3A:2E:CB:43"};
	private static final String [] participants = {"Alice", "Bob", "Carlos", "Diana"};
	private static final String [] ips = {"192.168.1.101", "192.168.1.102", "192.168.1.103", "192.168.1.104"};
	
	// GUI variables
	private TextView txtRunNumber;
	private TextView txtRobotName;
	private TextView txtDebug;
	private ProgressBar pbBluetooth;
	private CheckBox cbGPS;
	private CheckBox cbBluetooth;
	private CheckBox cbRunning;
	private CheckBox cbPaintMode;
	private ProgressBar pbBattery;
	private int batteryPercent = 0;
	private int bluetoothStatus = 0;
	
	// SharedPreferences variables
	SharedPreferences prefs;
	private static final String PREF_SELECTED_ROBOT = "SELECTED_ROBOT";
	private static final String PREF_SELECTED_PAINTMODE = "PAINT_MODE";
	private int selected_robot = 0;
	
	// Lightpainting specific
	private float overrideBrightness = 1;
	private int reqBrightness = 100; 
	private WindowManager.LayoutParams lp;	
	private View vi;
	private int defaultBrightness = -1;
	
    public static final int MESSAGE_SCREEN = 50;
    public static final int MESSAGE_SCREEN_COLOR = 51;
	
    private final Handler debug_handler = new Handler() {
    	public void handleMessage(Message msg) {	    	
	    	switch(msg.what) {
	    	case Common.MESSAGE_TOAST:
	    		Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_LONG).show();
	    		break;
	    	case Common.MESSAGE_LOCATION:
	    		cbGPS.setChecked((Integer)msg.obj == Common.GPS_RECEIVING);
	    		break;
	    	case Common.MESSAGE_BLUETOOTH:
	    		bluetoothStatus = (Integer)msg.obj;
	    		updateGUI();
	    		break;
	    	case Common.MESSAGE_LAUNCH:
	    		launch(msg.arg1, msg.arg2);
	    		break;
	    	case Common.MESSAGE_ABORT:
    			// Disconnect and reconnect
	    		abort();
	    		break;
	    	case Common.MESSAGE_DEBUG:
	    		txtDebug.setText("DEBUG:\n" + (String) msg.obj);
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
	    	case Common.MESSAGE_BATTERY:
	    		batteryPercent = (Integer) msg.obj;
	    		Log.d(TAG, "Battery is at " + batteryPercent + "%");
	    		updateGUI();
	    		break;	
	    	}
    	}
    	
    	private void launch(int numWaypoints, int runNum) {    		
    		gvh.trace.traceStart(runNum);
    		txtRunNumber.setText("Run: " + runNum);
    		
    		if(gvh.gps.getWaypointPositions().getNumPositions() == numWaypoints) {
    			if(!launched) {
    				// GUI Updates				
    				launched = true;
    				cbRunning.setChecked(true);
    				DISPLAY_MODE = cbPaintMode.isChecked();
    				if(DISPLAY_MODE) setContentView(vi);
    				
    				gvh.trace.traceSync("APPLICATION LAUNCH");
    				
    				logic = new LogicThread(gvh, motion);
    				created.add(logic);
    				logic.start();
    				
    	    		RobotMessage informLaunch = new RobotMessage("ALL", gvh.id.getName(), Common.MSG_ACTIVITYLAUNCH, new MessageContents(Common.intsToStrings(numWaypoints, runNum)));
    	    		gvh.comms.addOutgoingMessage(informLaunch);
    			}
    		} else {
    			gvh.plat.sendMainToast("Should have " + numWaypoints + " waypoints, but I have " + gvh.gps.getWaypointPositions().getNumPositions());
    		}
    	}
    	
    	private void abort() {
    		if(launched) {
    			gvh.trace.traceSync("APPLICATION ABORT");
    			connect_disconnect();
    			launched = false;
    			connect_disconnect();
    			
        		RobotMessage informAbort = new RobotMessage("ALL", gvh.id.getName(), Common.MSG_ACTIVITYABORT, (MessageContents) null);
        		gvh.comms.addOutgoingMessage(informAbort);
    		}
    	}
    };
	
    // Persistent threads
    private BluetoothRobotMotion motion = null;
    private LogicThread logic = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.main);

        created = new HashSet<Cancellable>();
        
        // Initialize preferences holder
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        selected_robot = prefs.getInt(PREF_SELECTED_ROBOT, 0);
        
        // Set up the GUI
        setupGUI();
        
        // Create the global variable holder
        HashMap<String,String> hm_participants = new HashMap<String,String>();
        for(int i = 0; i < participants.length; i++) {
        	hm_participants.put(participants[i], ips[i]);
        }        
        gvh = new RealGlobalVarHolder(participants[selected_robot], hm_participants, debug_handler, mac[selected_robot]);
        gvh.addEventListener(this);
        
        // Connect
        connect_disconnect();
        
        // Register message listener
        gvh.comms.addMsgListener(Common.MSG_ACTIVITYABORT, this);
        gvh.comms.addMsgListener(Common.MSG_ACTIVITYLAUNCH, this);
        
        // Set up brightness attribute 
        vi = new View(this);
    }
    
    private void updateGUI() {
    	pbBluetooth.setVisibility(bluetoothStatus == Common.BLUETOOTH_CONNECTING?View.VISIBLE:View.INVISIBLE);
		cbBluetooth.setChecked(bluetoothStatus ==  Common.BLUETOOTH_CONNECTED);
		pbBattery.setProgress(batteryPercent);
    }
    
	private void connect_disconnect() {
		if(!connected) {
			// CONNECT
			
			// Update GUI
			gvh.plat.setDebugInfo("");
	        Log.d(TAG, gvh.id.getName());
	        
	        // Begin persistent background threads
	        gvh.comms.startComms();
	        gvh.gps.startGps();
	        
	        if(motion == null) {
	        	Log.d(TAG, "Creating new RobotMotion");
	        	motion = new BluetoothRobotMotion(gvh, mac[selected_robot]);
	        } else {
	        	motion.resume();
	        }
		} else {
			// DISCONNECT
			
			// Shut down any created Cancellable items
			for(Cancellable c: created) {
				try {
					c.cancel();
				} catch(NullPointerException e) {}
			}
			created.clear();

			// Reset GVH's persistent threads
			gvh.comms.stopComms();
			gvh.trace.traceEnd();
			
			// Update GUI
			gvh.plat.setDebugInfo("");
			txtRunNumber.setText("");
			cbRunning.setChecked(false);

			launched = false;
			
			// LIGHTPAINTING SPECIFIC:
			// Restore the view to the main GUI
			if(DISPLAY_MODE) {
				setContentView(R.layout.main);
				setupGUI();
				updateGUI();
			}

			motion.halt();
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
		spe.putBoolean(PREF_SELECTED_PAINTMODE, cbPaintMode.isChecked());	
		spe.commit();
	}

	private void setupGUI() {
		// Set the brightness to the default level
		defaultBrightness();

		txtRunNumber = (TextView) findViewById(R.id.txtRunNumber);
		txtRobotName = (TextView) findViewById(R.id.txtRobotName);
		cbGPS = (CheckBox) findViewById(R.id.cbGPS);
		cbBluetooth = (CheckBox) findViewById(R.id.cbBluetooth);
		cbRunning = (CheckBox) findViewById(R.id.cbRunning);
		txtDebug = (TextView) findViewById(R.id.tv_debug);
		pbBluetooth = (ProgressBar) findViewById(R.id.pb_bluetooth);
		pbBattery = (ProgressBar) findViewById(R.id.pbBattery);
		pbBattery.setMax(100);
		
		txtRunNumber.setText(" ");
		txtRobotName.setText(participants[selected_robot]);
		txtRobotName.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				if(connected) connect_disconnect();
 				pick_robot();
 				connect_disconnect();
			}
		});
		
		cbPaintMode = (CheckBox) findViewById(R.id.cbDebugMode);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		cbPaintMode.setChecked(prefs.getBoolean("PAINT_MODE", false));		
		
		cbPaintMode.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
		    	saveToOptions();
			}
		});
	}
	
	private void defaultBrightness() {
		lp = getWindow().getAttributes();
		lp.screenBrightness = defaultBrightness;
		getWindow().setAttributes(lp);
	}

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
				overrideBrightness = 1;
				break;
			case Common.MOT_STRAIGHT:
				// Illuminate the screen if we're going straight 
				overrideBrightness = 1;	    
				break;
			case Common.MOT_STOPPED:
				// Darken the screen if we've stopped 
				overrideBrightness = 0;	    
				break;
			}
			
			if(DISPLAY_MODE && launched) {
				gvh.plat.sendMainMsg(MESSAGE_SCREEN, reqBrightness);
			}
		}
	}
	
	@Override
	public void onBackPressed() {
		Log.e(TAG, "Exiting application");
		if(motion != null) {
			motion.cancel();
		}
		if(connected) connect_disconnect();
		finish();
		return;
	}
}
