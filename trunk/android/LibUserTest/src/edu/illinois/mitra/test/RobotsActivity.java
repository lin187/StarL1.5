package edu.illinois.mitra.test;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import edu.illinois.mitra.starl.bluetooth.RobotMotion;
import edu.illinois.mitra.starl.comms.GPSReceiver;
import edu.illinois.mitra.starl.objects.common;
import edu.illinois.mitra.starl.objects.globalVarHolder;

public class RobotsActivity extends Activity {
	private static final String TAG = "RobotsActivity";
	private static final String ERR = "Critical Error";

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
	private ProgressBar pbBattery;
	
	// SharedPreferences variables
	SharedPreferences prefs;
	private static final String PREF_SELECTED_ROBOT = "SELECTED_ROBOT";
	private int selected_robot = 0;
	
    private final Handler main_handler = new Handler() {
    	public void handleMessage(Message msg) {	    	
	    	switch(msg.what) {
	    	case common.MESSAGE_TOAST:
	    		Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_LONG).show();
	    		break;
	    	case common.MESSAGE_LOCATION:
	    		cbGPS.setChecked((Integer)msg.obj == common.GPS_RECEIVING);
	    		break;
	    	case common.MESSAGE_BLUETOOTH:
	    		pbBluetooth.setVisibility((Integer)msg.obj == common.BLUETOOTH_CONNECTING?View.VISIBLE:View.INVISIBLE);
	    		cbBluetooth.setChecked((Integer)msg.obj ==  common.BLUETOOTH_CONNECTED);
	    		break;
	    	case common.MESSAGE_LAUNCH:
	    		int wptcount = (Integer) msg.obj;
    			if(gvh.getWaypointPositions().getNumPositions() == wptcount) {
    				launch();   				
    			} else {
    				gvh.sendMainToast("Should have " + wptcount + " waypoints, but I have " + gvh.getWaypointPositions().getNumPositions());
    			}
	    		break;
	    	case common.MESSAGE_ABORT:
    			// Disconnect and reconnect
    			connect_disconnect();
    			launched = false;
    			connect_disconnect();
	    		break;
	    		
	    	case common.MESSAGE_DEBUG:
	    		txtDebug.setText("DEBUG:\n" + (String) msg.obj);
	    		break;
	    	case common.MESSAGE_BATTERY:
	    		pbBattery.setProgress((Integer) msg.obj);
	    		break;
	    	}	
    	}

		private void launch() {
			if(!launched) {
				launched = true;
				cbRunning.setChecked(true);
				logic.start();
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
        gvh = new globalVarHolder(hm_participants, main_handler);
        //gvh = new globalVarHolder(main_handler);
        
        // Connect
        connect_disconnect();
    }
    
	private void connect_disconnect() {
		if(!connected) {
			// Update GUI
			btnConnect.setText("Disconnect");
			gvh.setName(participants[selected_robot]);
	        Log.d(TAG, gvh.getName());
	        gvh.setDebugInfo("Connected");
	        
	        // Begin persistent background threads
	        gvh.startComms();
	        gps = new GPSReceiver(gvh, gps_host, gps_port);
	        gps.start();
	        
	        if(motion == null) {
	        	motion = new RobotMotion(gvh, mac[selected_robot]);
	        } else {
	        	motion.resume();
	        }
	        
	        logic = new LogicThread(gvh, motion);
		} else {
			// Shut down the logic thread if it was running
			if(launched) {
				logic.cancel();
				logic = null;
			}
			
			// Update GUI
			btnConnect.setText("Connect");
			cbRunning.setChecked(false);
			gvh.sendMainMsg(common.MESSAGE_LOCATION, 0);
			gvh.setDebugInfo("Disconnected");
			
			// Shut down persistent threads
			gvh.stopComms();
			gps.cancel();

			launched = false;

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
		spe.commit();
	}

	private void setupGUI() {	
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
		txtRobotName.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				pick_robot();
			}
		});
		
		btnConnect.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				connect_disconnect();
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
		if(connected) connect_disconnect();
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
