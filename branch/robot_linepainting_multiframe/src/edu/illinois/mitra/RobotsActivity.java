package edu.illinois.mitra;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import edu.illinois.mitra.Objects.common;
import edu.illinois.mitra.Objects.globalVarHolder;
import edu.illinois.mitra.bluetooth.RobotMotion;
import edu.illinois.mitra.comms.GPSReceiver;

public class RobotsActivity extends Activity {
	private static final String TAG = "RobotsActivity";
	private static final String ERR = "Critical Error";
	
	private boolean DISPLAY_MODE = true;
	
	private globalVarHolder gvh = null;
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
	private Button btnConnect;
	private TextView txtRobotName;
	private TextView txtDebug;
	private ProgressBar pbBluetooth;
	private CheckBox cbGPS;
	private CheckBox cbBluetooth;
	private CheckBox cbRunning;
	private CheckBox cbPaintMode;
	private ProgressBar pbBattery;
	
	// SharedPreferences variables
	SharedPreferences prefs;
	private static final String PREF_SELECTED_ROBOT = "SELECTED_ROBOT";
	private static final String PREF_SELECTED_PAINTMODE = "PAINT_MODE";
	private int selected_robot = 0;
	
	// GUI Message handler
	public static final int MESSAGE_TOAST = 0;
	public static final int MESSAGE_LOCATION = 1;
	public static final int MESSAGE_BLUETOOTH = 2;
	public static final int MESSAGE_LAUNCH = 3;
	public static final int MESSAGE_DEBUG = 4;
	public static final int MESSAGE_SCREEN = 5;
	public static final int MESSAGE_SCREEN_COLOR = 6;
	public static final int MESSAGE_BATTERY = 7;
	public static final int MESSAGE_MOTION = 8;
	
	// Motion types
	public static final int MOT_TURNING		= 0;
	public static final int MOT_ARCING		= 1;
	public static final int MOT_STRAIGHT	= 2;
	public static final int MOT_STOPPED		= 3;
	
	// Lightpainting specific
	private float overrideBrightness = 1;
	private int reqBrightness = 100; 
	private WindowManager.LayoutParams lp;	
	private View vi;
	private int defaultBrightness = -1;
	
    private final Handler debug_handler = new Handler() {
    	public void handleMessage(Message msg) {	    	
	    	switch(msg.what) {
	    	case MESSAGE_TOAST:
	    		Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_LONG).show();
	    		break;
	    	case MESSAGE_LOCATION:
	    		cbGPS.setChecked((Integer)msg.obj == 1);
	    		break;
	    	case MESSAGE_BLUETOOTH:
	    		pbBluetooth.setVisibility((Integer)msg.obj == 2?View.VISIBLE:View.INVISIBLE);
	    		cbBluetooth.setChecked((Integer)msg.obj == 1);
	    		
	    		if(cbBluetooth.isChecked()) {
					// Update the battery
					gvh.sendMainMsg(RobotsActivity.MESSAGE_BATTERY, motion.getBatteryPercentage());
	    		}
	    		break;
	    	case MESSAGE_LAUNCH:
	    		String cmd = (String) msg.obj;
	    		if(cmd.substring(0, 2).equals("GO")) {
	    			int wptcount = Integer.parseInt(cmd.substring(3));
	    			if(gvh.getWaypointPositions().getNumPositions() == wptcount) {
		    			if(!launched) {
		    				launch();
		    			}	    				
	    			} else {
	    				gvh.sendMainToast("Should have " + wptcount + " waypoints, but I have " + gvh.getWaypointPositions().getNumPositions());
	    			}
	    		} else if(cmd.equals("ABORT")) {
	    			Toast.makeText(getApplicationContext(), "Aborting!", Toast.LENGTH_SHORT).show();
	    			// Disconnect
	    			attempt_connect();
	    			launched = false;
	    			
	    			// Reconnect
	    			attempt_connect();
	    		}
	    		break;
	    	case MESSAGE_DEBUG:
	    		txtDebug.setText("DEBUG:\n" + (String) msg.obj);
	    		break;
	    	case MESSAGE_SCREEN:
	    		if(DISPLAY_MODE) {
	    			reqBrightness = (Integer) msg.obj;
		        	lp.screenBrightness = common.cap(reqBrightness*overrideBrightness, 1f, 100f) / 100.0f;
		        	getWindow().setAttributes(lp);
	    		}
	    		break;
	    	case MESSAGE_SCREEN_COLOR:
	    		String colParse = "#" + (String) msg.obj;
	    		vi.setBackgroundColor(Color.parseColor(colParse));
	    		break;
	    	case MESSAGE_BATTERY:
	    		pbBattery.setProgress((Integer) msg.obj);
	    		break;
	    	case MESSAGE_MOTION:
	    		switch((Integer) msg.obj) {
	    		case MOT_TURNING:
	    			// Illuminate the screen if we're turning in place (assuming phones are in the middle!)
	    			overrideBrightness = 0.21f;	    			
	    			break;
	    		case MOT_ARCING:
	    			// Illuminate the screen if we're arcing 
	    			overrideBrightness = 1;
	    			break;
	    		case MOT_STRAIGHT:
	    			// Illuminate the screen if we're going straight 
	    			overrideBrightness = 1;	    
	    			break;
	    		case MOT_STOPPED:
	    			// Darken the screen if we've stopped 
	    			overrideBrightness = 0;	    
	    			break;
	    		}
	    		
	    		if(DISPLAY_MODE && launched) {
	    			gvh.sendMainMsg(MESSAGE_SCREEN, reqBrightness);
	    		}
	    		break;
	    	}	
    	}

		private void launch() {
			DISPLAY_MODE = cbPaintMode.isChecked();
			launched = true;
			cbRunning.setChecked(true);
			logic = new LogicThread(gvh, motion);
			logic.start();
			if(DISPLAY_MODE) {
				setContentView(vi);
			}
		}
    };
	
    // Persistent threads
    private GPSReceiver gps = null;
    private RobotMotion motion = null;
    private LogicThread logic = null;
    
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
        for(int i = 0; i < participants.length; i++) {
        	hm_participants.put(participants[i], ips[i]);
        }        
        gvh = new globalVarHolder(hm_participants, debug_handler);
        
        // Connect
        attempt_connect();
        
        // Set up brightness attribute
        vi = new View(this);
    }
    
	private void attempt_connect() {
		if(!connected) {
			// Update GUI
			btnConnect.setText("Disconnect");
			gvh.setName(participants[selected_robot]);
	        Log.d(TAG, gvh.getName());
	        gvh.setDebugInfo("[Not Connected]");
	        
	        // Begin persistent background threads
	        gvh.startComms();
	        gps = new GPSReceiver(gvh, gps_host, gps_port);
	        gps.start();
	        motion = new RobotMotion(gvh, mac[selected_robot]);
		} else {
			// Update GUI
			btnConnect.setText("Connect");
			cbRunning.setChecked(false);
			gvh.sendMainMsg(MESSAGE_LOCATION, 0);
			
			// Shut down persistent threads
			gvh.stopComms();
			gps.cancel();

			// Shut down the logic thread if it was running
			if(launched) {
				logic.cancel();
			}

			launched = false;
			
			// Restore the view to the main GUI
			if(DISPLAY_MODE) {
				setContentView(R.layout.main);
				setupGUI();
			}

			motion.cancel();
		}	
		connected = !connected;
	}

	// Handle menu creation and exiting
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    // Handle menu selections
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.menu_connect:
			attempt_connect();
			break;
		case R.id.menu_robot:
			pick_robot();
			break;
		}
		return super.onOptionsItemSelected(item);
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
		lp = getWindow().getAttributes();
		lp.screenBrightness = defaultBrightness;
		getWindow().setAttributes(lp);
		
		btnConnect = (Button) findViewById(R.id.btnConnect);
		txtRobotName = (TextView) findViewById(R.id.txtRobotName);
		cbGPS = (CheckBox) findViewById(R.id.cbGPS);
		cbBluetooth = (CheckBox) findViewById(R.id.cbBluetooth);
		cbRunning = (CheckBox) findViewById(R.id.cbRunning);
		txtDebug = (TextView) findViewById(R.id.tv_debug);
		pbBluetooth = (ProgressBar) findViewById(R.id.pb_bluetooth);
		pbBattery = (ProgressBar) findViewById(R.id.pbBattery);
		pbBattery.setMax(100);
		
		txtRobotName.setText(participants[selected_robot]);
		
		cbPaintMode = (CheckBox) findViewById(R.id.cbDebugMode);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		cbPaintMode.setChecked(prefs.getBoolean("PAINT_MODE", false));		
		
		cbPaintMode.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
		    	saveToOptions();
			}
		});
		
		btnConnect.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				attempt_connect();
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
		if(connected) attempt_connect();
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				int pid = android.os.Process.myPid(); 
				android.os.Process.killProcess(pid);
			}
		}, 300);
		return;
	}
	

}
