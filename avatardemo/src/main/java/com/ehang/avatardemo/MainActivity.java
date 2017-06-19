package com.ehang.avatardemo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.ehang.coptersdk.CopterControl;
import com.ehang.coptersdk.OnSendListener;
import com.ehang.coptersdk.bean.FlightMode;
import com.ehang.coptersdk.connection.ConnectionListener;
import com.ehang.coptersdk.connection.OnConnectionListener;

public class MainActivity extends Activity implements View.OnClickListener {

    private Handler handler;
    private TextView tv_drone_status;
    private TextView tv_battery_percentage;
    private TextView tv_GPS_Sat;
    private SeekBar sb_throttle;
    private Switch switch_avatar;
    private Button bt_EmergentStop;
    private boolean isEmergentStop = false;

    private long lastTap_disArmButton = -1;
    private static long DOUBLE_TAP_SENSITIVITY = 1000;

    private final static int HEARTBEAT_MSG = 1;
    private final static int STATUS_REFRESH_INTERNAL = 500;

    private final static int TAKEOFF_ALTITIDE = 1;

    private final static String MAC = "98:D3:32:20:58:5B";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initCopterEventListener();
        initView();
        setupHandler();
    }

    @Override
    protected void onStop() {
        super.onStop();
        CopterControl.getInstance().getConnection().disconnect();
    }

    public void initCopterEventListener(){
        //add listener to bluetooth connection
        CopterControl.getInstance().getConnection().addGboxConnectionListener(new ConnectionListener() {
            @Override
            public void onConnect() {
                Toast.makeText(getApplicationContext(), "Bluetooth Connected", Toast
                        .LENGTH_SHORT).show();
            }
            @Override
            public void onDisconnect() {
                Toast.makeText(getApplicationContext(), "Bluetooth Disconnected", Toast
                        .LENGTH_SHORT).show();
            }
        });
        //add listener to copter connection
        CopterControl.getInstance().getConnection().addCopterConnectionListener(new ConnectionListener() {
            @Override
            public void onConnect() {
                Toast.makeText(getApplicationContext(), "Copter Connected", Toast.LENGTH_SHORT)
                        .show();
            }

            @Override
            public void onDisconnect() {
                Toast.makeText(getApplicationContext(), "Copter Disconnected", Toast
                        .LENGTH_SHORT).show();
            }
        });
        CopterControl.getInstance().setOnModeChangeListener(new CopterControl
                .OnModeChangeListener() {
            @Override
            public void onChange(FlightMode flightMode) {
                Toast.makeText(getApplicationContext(), "Changed to " + flightMode.name(), Toast
                        .LENGTH_SHORT).show();
            }
        });
    }


    public void initView() {
        tv_drone_status = (TextView) findViewById(R.id.tv_DroneStatus);
        tv_battery_percentage = (TextView) findViewById(R.id.tv_BatteryPercentage);
        tv_GPS_Sat = (TextView) findViewById(R.id.tv_GPSSat);
        bt_EmergentStop = (Button) findViewById(R.id.bt_Stop);
        findViewById(R.id.bt_Connect).setOnClickListener(this);
        findViewById(R.id.bt_Bind).setOnClickListener(this);
        findViewById(R.id.bt_Disconnect).setOnClickListener(this);
        findViewById(R.id.bt_Arm).setOnClickListener(this);
        findViewById(R.id.bt_Disarm).setOnClickListener(this);
        findViewById(R.id.bt_Takeoff).setOnClickListener(this);
        findViewById(R.id.bt_Return).setOnClickListener(this);
        findViewById(R.id.bt_Stop).setOnClickListener(this);
        findViewById(R.id.bt_Land).setOnClickListener(this);
        findViewById(R.id.bt_GPS).setOnClickListener(this);
        findViewById(R.id.bt_Manual).setOnClickListener(this);
        sb_throttle = (SeekBar) findViewById(R.id.sb_Throttle);
        sb_throttle.setMax(200);
        sb_throttle.setProgress(100);
        sb_throttle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //change throttle as we drag the seekbar
                CopterControl.getInstance().setThrottle(sb_throttle.getProgress() - 100);
                return false;
            }
        });
        sb_throttle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //set seekbar to mid position and set throttle to mid level too
                seekBar.setProgress(100);
                CopterControl.getInstance().setThrottle(0);
            }
        });
        switch_avatar = (Switch) findViewById(R.id.switch_avatar);
        switch_avatar.setChecked(false);
        switch_avatar.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (CopterControl.getInstance().isCopterConnected()){
                    if (isChecked) {
                        System.out.println("Avatar is checked, start");
                        CopterControl.getInstance().startAvatar();


                    } else {
                        System.out.println("Avatar is unchecked, stop avatar");
                        CopterControl.getInstance().stopAvatar();
                    }

                }
            }
        });
    }

    private void initStatusCheckTimer() {
        handler.sendEmptyMessageDelayed(HEARTBEAT_MSG, STATUS_REFRESH_INTERNAL);
    }

    public void setupHandler(){
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what){
                    case HEARTBEAT_MSG:
                        if (CopterControl.getInstance().isCopterConnected()){
                            //Get Flight mode and ARM status
                            boolean armed = CopterControl.getInstance().isArmed();
                            String status_text = CopterControl.getInstance().getCurrentMode().name();
                            if (armed) {
                                status_text = status_text + " Armed";
                            } else {
                                status_text = status_text + " Disarmed";
                            }
                            tv_drone_status.setText(status_text);

                            //Get Battery
                            int remainingBattery;
                            remainingBattery = CopterControl.getInstance().getRemainingBattery();
                            tv_battery_percentage.setText(Integer.toString(remainingBattery));

                            //Get GPS status
                            int satellites;
                            if (CopterControl.getInstance().getGpsStatus()!=null) {
                                satellites = CopterControl.getInstance().getGpsStatus().numAvailableSatellites;
                                tv_GPS_Sat.setText(Integer.toString(satellites));
                            }

                        }
                        //Schedule the next check

                        handler.sendEmptyMessageDelayed(HEARTBEAT_MSG, STATUS_REFRESH_INTERNAL);

                        break;

                }
            }
        };
    }


    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.bt_Connect:
                CopterControl.getInstance().getConnection().connect(MAC, new OnConnectionListener
                        () {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(), "Connection Succeeds", Toast
                                .LENGTH_SHORT).show();
                        initStatusCheckTimer();
                    }

                    @Override
                    public void onFailure() {
                        Toast.makeText(getApplicationContext(), "Connection Fails", Toast
                                .LENGTH_SHORT).show();
                    }
                });

                break;
            case R.id.bt_Bind:
                CopterControl.getInstance().startPair(new OnSendListener() {
                    @Override
                    public void onSuccess() {
                        System.out.println("Pairing , start");
                        Toast.makeText(getApplicationContext(), "Pairing Succeeds", Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onFailure() {
                        Toast.makeText(getApplicationContext(), "Pairing Fails", Toast
                                .LENGTH_SHORT).show();
                    }
                });
                break;
            case R.id.bt_Disconnect:
                if (CopterControl.getInstance().isCopterConnected()){
                    CopterControl.getInstance().getConnection().b();
                }
                break;

            case R.id.bt_Disarm:
                if (CopterControl.getInstance().isCopterConnected()){

                    long now = System.currentTimeMillis();
                    if (now - lastTap_disArmButton >= DOUBLE_TAP_SENSITIVITY) {
                        lastTap_disArmButton = now;
                        return;
                    } else {
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
                break;
            case R.id.bt_Arm:
                if (CopterControl.getInstance().isCopterConnected()){
                    CopterControl.getInstance().unLock(new OnSendListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getApplicationContext(), "UnLock Succeeds", Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onFailure() {
                            Toast.makeText(getApplicationContext(), "UnLock Fails", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                break;
            case R.id.bt_Takeoff:
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
                break;
            case R.id.bt_Stop:
                if (CopterControl.getInstance().isCopterConnected()) {

                    if (!isEmergentStop) {
                        CopterControl.getInstance().emergentStop(false, new OnSendListener() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(getApplicationContext(), "EmergentStop Succeeds",
                                        Toast.LENGTH_SHORT).show();
                                isEmergentStop = true;
                                bt_EmergentStop.setText("Cancel EmergentStop");
                            }

                            @Override
                            public void onFailure() {
                                Toast.makeText(getApplicationContext(), "EmergentStop Fails", Toast
                                        .LENGTH_SHORT).show();
                            }
                        });


                    } else {
                        CopterControl.getInstance().cancelEmergentStop();
                        isEmergentStop = false;
                        bt_EmergentStop.setText("EmergentStop");
                    }
                }
                break;
            case R.id.bt_Return:
                if (CopterControl.getInstance().isCopterConnected()) {
                    CopterControl.getInstance().returnToBase(new OnSendListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getApplicationContext(), "Return Succeeds", Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onFailure() {
                            Toast.makeText(getApplicationContext(), "Return Fails", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                break;
            case R.id.bt_Land:
                if (CopterControl.getInstance().isCopterConnected()) {
                    CopterControl.getInstance().land(new OnSendListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getApplicationContext(), "Landing Succeeds", Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onFailure() {
                            Toast.makeText(getApplicationContext(), "Landing Fails", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                break;

            case R.id.bt_GPS:
                if (CopterControl.getInstance().isCopterConnected()) {
                    CopterControl.getInstance().gps(new OnSendListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getApplicationContext(), "Set GPS mode Succeeds", Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onFailure() {
                            Toast.makeText(getApplicationContext(), "Set GPS mode fails", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                break;

            case R.id.bt_Manual:
                if (CopterControl.getInstance().isCopterConnected()) {
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
    }

}
