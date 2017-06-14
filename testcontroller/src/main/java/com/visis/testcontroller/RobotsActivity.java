package com.visis.testcontroller;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ehang.coptersdk.CopterControl;
import com.ehang.coptersdk.OnSendListener;
import com.ehang.coptersdk.bean.FlightMode;
import com.ehang.coptersdk.connection.OnConnectionListener;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.illinois.mitra.starl.comms.MessageContents;
import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.gvh.RealGlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.interfaces.MessageListener;
import edu.illinois.mitra.starl.models.Model_Mavic;
import edu.illinois.mitra.starl.models.Model_Phantom;
import edu.illinois.mitra.starl.motion.GhostAerialBTI;
import edu.illinois.mitra.starl.objects.Common;
import edu.illinois.mitra.starl.objects.HandlerMessage;

public class RobotsActivity extends Activity implements MessageListener, Joystick.JoystickListener {
	private static final String TAG = "RobotsActivity";
	private static final String ERR = "Critical Error";
	private static final float TAKEOFF_ALTITIDE = 1.0f ;
	private long lastTap_disArmButton = -1;
	private static long DOUBLE_TAP_SENSITIVITY = 1000;

	private static final String IDENTITY_FILE_URL = "https://dl.dropbox.com/s/dwfqdhbf5vdtz18/robots.rif?dl=1";
	private static final String[][] ERROR_PARTICIPANTS = {{"ERROR"}, {"ERROR"}, {"ERROR"}};

	private static final boolean ENABLE_TRACING = false;

	private GlobalVarHolder gvh = null;
	public boolean launched = false;

	// SharedPreferences variables
	private static final String PREF_SELECTED_ROBOT = "SELECTED_ROBOT";
	private int selectedRobot = 0;

	// Logic thread executor
	private ExecutorService executor = Executors.newFixedThreadPool(1);
	private Future<List<Object>> results;
	private LogicThread runThread;
	private MainHandler mainHandler;
	private WifiManager.MulticastLock multicastLock;

	// Row 0 = names
	// Row 1 = MACs
	// Row 2 = IPs
	private String[][] participants;
	private int numRobots;
	private BotInfoSelector[] botInfo;
	private int i;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//testing TURN STRICT MODE OFF
		StrictMode.ThreadPolicy tp = StrictMode.ThreadPolicy.LAX;
		StrictMode.setThreadPolicy(tp);

		//StrictMode.ThreadPolicy()
		super.onCreate(savedInstanceState);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		Joystick joystick = new Joystick(this);
		setContentView(R.layout.testmain);

		// this code allows the MotoE (and probably other phones) to receive broadcast udp packets
		// they don't accept broadcast messages by default to save battery
		WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE); //changed tim to prevent memory leak added getApplicationContext()
		multicastLock = wifi.createMulticastLock("multicastLock");
		multicastLock.setReferenceCounted(true);
		multicastLock.acquire();

		// Load the participants
		//participants = IdentityLoader.loadIdentities(IDENTITY_FILE_URL);
		// Put number of robots being used here
		numRobots = 1;
		botInfo = new BotInfoSelector[numRobots];
		// add color, robot type, and device type for each robot here
		//botInfo[0] = new BotInfoSelector("red", Common.MAVIC, Common.NEXUS7);
		//botInfo[1] = new BotInfoSelector("green", Common.IROBOT, Common.MOTOE);
		//botInfo[2] = new BotInfoSelector("blue", Common.IROBOT, Common.NEXUS7);
		// botInfo[3] = new BotInfoSelector("white", Common.IROBOT, Common.NEXUS7);
		botInfo[0] = new BotInfoSelector("blue", Common.GHOSTAERIAL, Common.NEXUS7);

		participants = new String[3][numRobots];
		for (i = 0; i < numRobots; i++) {
			participants[0][i] = botInfo[i].name;
			participants[1][i] = botInfo[i].bluetooth;
			participants[2][i] = botInfo[i].ip;
		}


		// Initialize preferences holder
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		selectedRobot = prefs.getInt(PREF_SELECTED_ROBOT, 0);

		if (selectedRobot >= participants[0].length) {
			Toast.makeText(this, "Identity error! Reselect robot identity", Toast.LENGTH_LONG).show();
			selectedRobot = 0;
		}

		// Set up the GUI
		setupGUI();

		// Create the main handler
		mainHandler = new MainHandler(this, pbBluetooth, pbBattery, cbGPS, cbBluetooth, cbRunning, txtDebug, cbRegistered);

		if (participants == null) {
			Toast.makeText(this, "Error loading identity file!", Toast.LENGTH_LONG).show();
			participants = ERROR_PARTICIPANTS;
			selectedRobot = 0;
		}

		// Create the global variable holder
		HashMap<String, String> hm_participants = new HashMap<String, String>();
		for (int i = 0; i < participants[0].length; i++) {
			hm_participants.put(participants[0][i], participants[2][i]);
		}

		Log.i("GA", "GVH STARTING");
		gvh = new RealGlobalVarHolder(participants[0][selectedRobot], hm_participants, botInfo[selectedRobot].type, mainHandler, participants[1][selectedRobot], this);
		Log.i("GA", "SET GVH");
		mainHandler.setGvh(gvh);

		Log.i("GA", "CONNECT");
		//Connect
		connect();

		Log.i("GA", "CREATE APP");
		createAppInstance(gvh);
	}

	public void createAppInstance(GlobalVarHolder gvh) {

		runThread = new Test(gvh);    // Instantiate your application here!
		// Example: runThread = new LightPaintActivity(gvh);
		Log.i("GA", "CREATED APP");
	}

	public void launch(int numWaypoints, int runNum) {
		if (!launched) {
			if (gvh.gps.getWaypointPositions().getNumPositions() == numWaypoints) {
				if (ENABLE_TRACING)
					gvh.trace.traceStart(runNum);
				launched = true;
				cbRunning.setChecked(true);

				gvh.trace.traceSync("APPLICATION LAUNCH", gvh.time());

				RobotMessage informLaunch = new RobotMessage("ALL", gvh.id.getName(), Common.MSG_ACTIVITYLAUNCH, new MessageContents(Common.intsToStrings(numWaypoints, runNum)));
				gvh.comms.addOutgoingMessage(informLaunch);
				results = executor.submit(runThread);
			} else {
				gvh.plat.sendMainToast("Should have " + numWaypoints + " waypoints, but I have " + gvh.gps.getWaypointPositions().getNumPositions());
			}
		}
	}

	public void abort() {
		runThread.cancel();
		results.cancel(true);
		executor.shutdownNow();
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
		gvh.comms.addMsgListener(this, Common.MSG_ACTIVITYLAUNCH, Common.MSG_ACTIVITYABORT);
	}

	public void disconnect() {
		gvh.log.i(TAG, "Disconnecting and stopping all background threads");

		// Shut down the logic thread if it was running
		if (launched) {
			runThread.cancel();
			executor.shutdownNow();
		}
		launched = false;

		// Shut down persistent threads
		gvh.comms.stopComms();
		gvh.gps.stopGps();
		gvh.plat.moat.cancel();
	}

	private TextView txtRobotName;
	private TextView txtDebug;
	private TextView txtDestination;
	private ProgressBar pbBluetooth;
	private CheckBox cbGPS;
	private CheckBox cbBluetooth;
	private CheckBox cbRunning;
	private CheckBox cbRegistered; //status of DJI SDK registration
	private ProgressBar pbBattery;
	private Button arm;
	private Button disarm;
	private Button connect;
	private Button manual;
	private Button avatar;
	private Button btnTakeoff;
	private boolean avatarSet;
	private TextView txtMode;

	private void setupGUI() {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		txtRobotName = (TextView) findViewById(R.id.txtRobotName);
		cbGPS = (CheckBox) findViewById(R.id.cbGPS);
		cbBluetooth = (CheckBox) findViewById(R.id.cbBluetooth);
		cbRunning = (CheckBox) findViewById(R.id.cbRunning);
		txtDebug = (TextView) findViewById(R.id.tv_debug);
		pbBluetooth = (ProgressBar) findViewById(R.id.pb_bluetooth);
		pbBattery = (ProgressBar) findViewById(R.id.pbBattery);
		pbBattery.setMax(100);
		cbRegistered = (CheckBox) findViewById(R.id.cbRegistered);
		arm = (Button)findViewById(R.id.arm);
		disarm = (Button)findViewById(R.id.disarm);
		connect = (Button)findViewById(R.id.btnConnect);
		manual = (Button)findViewById(R.id.manual);
		avatar = (Button)findViewById(R.id.avatar);
		btnTakeoff = (Button)findViewById(R.id.btnTakeoff);
		txtMode = (TextView) findViewById(R.id.txtMode);
		avatarSet = false;

		txtMode.setText("Mode: ???");

		if (!(botInfo[selectedRobot].type instanceof Model_Mavic || botInfo[selectedRobot].type instanceof Model_Phantom)) {
			cbRegistered.setVisibility(View.GONE);
		} else {
			cbBluetooth.setText("Drone Connected");
		}

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

		arm.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				if (CopterControl.getInstance().isCopterConnected()){
					CopterControl.getInstance().unLock(new OnSendListener() {
						@Override
						public void onSuccess() {
							Toast.makeText(getApplicationContext(), "UnLock Succeeds", Toast.LENGTH_SHORT).show();
							CopterControl.getInstance().setOnModeChangeListener(new CopterControl.OnModeChangeListener() {
								@Override
								public void onChange(FlightMode flightMode) {
									txtMode.setText("Mode: " + flightMode.toString());
								}
							});
						}
						@Override
						public void onFailure() {
							Toast.makeText(getApplicationContext(), "UnLock Fails", Toast.LENGTH_SHORT).show();
						}
					});
				}
			}
		});


		disarm.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				if (CopterControl.getInstance().isCopterConnected()){
					CopterControl.getInstance().lock(new OnSendListener() {
						@Override
						public void onSuccess() {
							Toast.makeText(getApplicationContext(), "Lock Succeeds", Toast.LENGTH_SHORT).show();

						}
						@Override
						public void onFailure() {
							Toast.makeText(getApplicationContext(), "Lock Fails", Toast
									.LENGTH_SHORT).show();
						}
					});
				}
			}
		});

		connect.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				if (!CopterControl.getInstance().isCopterConnected()){
					CopterControl.getInstance().getConnection().connect(participants[1][selectedRobot], new OnConnectionListener() {
						@Override
						public void onSuccess() {
							Log.d(TAG, "Success");
						}

						@Override
						public void onFailure() {
							Log.d(TAG, "Success");
						}
					});
				}
			}
		});

		manual.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				if (!CopterControl.getInstance().isCopterConnected()){
					CopterControl.getInstance().manual(new OnSendListener() {
						@Override
						public void onSuccess() {
							Toast.makeText(getApplicationContext(), "Set Manual mode Succeeds", Toast.LENGTH_SHORT).show();
						}
						@Override
						public void onFailure() {
							Toast.makeText(getApplicationContext(), "Set Manual mode Fails", Toast.LENGTH_SHORT).show();
						}
					});
				}
			}
		});

		avatar.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				if (!avatarSet){
					CopterControl.getInstance().startAvatar();
				}else
					CopterControl.getInstance().stopAvatar();
			}
		});

		btnTakeoff.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				if (CopterControl.getInstance().isCopterConnected()) {
					CopterControl.getInstance().takeoff(TAKEOFF_ALTITIDE, new OnSendListener() {
						@Override
						public void onSuccess() {
							Toast.makeText(getApplicationContext(), "Takeoff Succeeds", Toast.LENGTH_SHORT).show();

						}
						@Override
						public void onFailure() {
							Toast.makeText(getApplicationContext(), "Takeoff Fails", Toast.LENGTH_SHORT).show();
						}
					});
				}
		}});


	}

	@Override
	public void onBackPressed() {
		gvh.log.e(TAG, "Exiting application");
		disconnect();
		finish();
		if (multicastLock != null) {
			multicastLock.release();
			multicastLock = null;
		}
		return;
	}

	@Override
	public void messageReceived(RobotMessage m) {
		switch (m.getMID()) {
			case Common.MSG_ACTIVITYLAUNCH:
				gvh.plat.sendMainMsg(HandlerMessage.MESSAGE_LAUNCH, Integer.parseInt(m.getContents(0)), Integer.parseInt(m.getContents(1)));
				break;
			case Common.MSG_ACTIVITYABORT:
				gvh.plat.sendMainMsg(HandlerMessage.MESSAGE_ABORT, null);
				break;
		}
	}

	@Override
	public void OnJoystickMoved(float xPercent, float yPercent, int source) {
		switch (source)
		{
			case R.id.leftJoystick:
				Log.d("Joystick", "Left Joystick:"+ "Throttle: " + (int) (yPercent * 100.0) + " Yaw: " + ((int) Math.toDegrees(Math.asin(limiting(xPercent))))/2);
				if (CopterControl.getInstance().isCopterConnected()) {
					CopterControl.getInstance().setThrottle((int) (yPercent * 100.0));
					CopterControl.getInstance().attitudeControl((float)((Math.toDegrees(Math.asin(limiting(xPercent))))/2.0), null); //goes from -90 to 90, but need -45 to 45
				}

				break;
			case R.id.rightJoystick:
				Log.d("Joystick", "Right Joystick:" + " X percent: " + xPercent + " Y percent: " + yPercent);
				break;

		}
	}

	public double limiting (double val)
	{
		if (val < -1)
			return -1.0;
		else if(val > 1)
			return 1.0;
		else
			return (float)val;
	}
}