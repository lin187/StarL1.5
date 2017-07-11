//Written by:
//      Tim Liang - @timliang on verivital.slack.com for questions
//      Stirling Carter - @stirlingcarter on verivital.slack.com for questions

package edu.illinois.mitra.starl.motion;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.flightcontroller.ConnectionFailSafeBehavior;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.FlightOrientationMode;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.RobotEventListener;
import edu.illinois.mitra.starl.objects.HandlerMessage;
import dji.sdk.camera.*;
import dji.sdk.gimbal.*;
import dji.common.gimbal.*;

public class DjiController {

    private static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";
    private static final boolean USING_WIFI_BRIDGE = true;
    
    private BaseProduct mProduct;
    private Aircraft mAircraft;
    private Camera mCamera = mAircraft.getCamera();
    private FlightController mFlightController;
    private Handler mHandler;
    private FlightControlData mFlightControlData;
    private Gimbal mGimbal = mAircraft.getGimbal();
    private PlaybackManager mPlaybackManager = mCamera.getPlaybackManager();


    private static String TAG = "DjiController";
    
    private Context context;
    private String mac;
    private GlobalVarHolder gvh;

    public DjiController(GlobalVarHolder gvh, Context context, String mac) {
        this.context = context;
        this.mac = mac;
        this.gvh = gvh;
        initConnection();
    }

    //Check to see if the API key has been registered
    private static boolean getAPIStatus(){
        return DJISDKManager.getInstance().hasSDKRegistered();
    }

    // sets pitch to val percent of max angle
    // positive value moves forward, negative backward
    public void setPitch(double val) {
        val *= .14;
        if (mFlightController != null)
        {
            mFlightControlData.setPitch((float)val);
            mFlightController.sendVirtualStickFlightControlData(mFlightControlData, null);
        }
    }

    // sets roll to val percent of max angle
    // positive value moves right, negative left
    public void setRoll(double val) {
        val *= .14;
        if (mFlightController != null)
        {
            mFlightControlData.setRoll((float)val);
            mFlightController.sendVirtualStickFlightControlData(mFlightControlData, null);
        }
    }

    // sets yaw to val percent of max angular rotation
    // positive value turns right (clockwise from above), negative turns left
    public void setYaw(double val) {
        val *= .14;
        if (mFlightController != null)
        {
            mFlightControlData.setYaw((float)val);
            mFlightController.sendVirtualStickFlightControlData(mFlightControlData, null);
        }
    }

    // moves the drone up (+ val) or down (- val)
    public void setThrottle(double val) {
        if (mFlightController != null)
        {
            mFlightControlData.setVerticalThrottle((float)val);
            mFlightController.sendVirtualStickFlightControlData(mFlightControlData, null);
        }
    }

    public void sendLanding() {
        if (mFlightController != null)
        {
            mFlightController.startLanding(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    gvh.log.e(TAG, "Landing error: " + (djiError == null ? "no errors" : djiError));
                }
            });
        }
    }

    public void sendTakeoff() {
        if (mFlightController != null)
        {
            mFlightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    gvh.log.e(TAG, "Take-off error: "+ (djiError == null ? "no errors" : djiError));
                }
            });
        }
    }

    public void takePicture() {
        if (mFlightController != null)
        {
            mCamera.startShootPhoto(null);
        }
    }

    public void rotateGimbal(float y){
        Rotation rot = new Rotation.Builder().yaw(y).build();
        mGimbal.rotate(rot,null);
    }

    public void rotateGimbal(float p, float y){
        Rotation rot = new Rotation.Builder().pitch(p).yaw(y).build();
        mGimbal.rotate(rot,null);
    }

    public void rotateGimbal(float p, float y, float r){
        Rotation rot = new Rotation.Builder().pitch(p).yaw(y).roll(r).build();
        mGimbal.rotate(rot,null);
    }

    public void downloadPhotos(){
        String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        File dir = new File(baseDir);
        if (!dir.exists() || !dir.isDirectory()){
            dir.mkdirs();
        }
        mPlaybackManager.selectAllFiles();
        mPlaybackManager.downloadSelectedFiles(dir, new PlaybackManager.FileDownloadCallback() {
            @Override
            public void onStart() {

            }

            @Override
            public void onEnd() {

            }

            @Override
            public void onError(Exception e) {

            }

            @Override
            public void onProgressUpdate(int i) {

            }
        });
    }

    // make the drone land immediately
    public void sendEmergency() {
        sendLanding();
    }

    public void hover() {
        //no commands = DJI will automatically hover
    }

    public void setMaxTilt(float maxTilt) {
        //no
    }

    public void initConnection() {
        //Initiates API verification
        mHandler = new Handler(Looper.getMainLooper());
		DJISDKManager.getInstance().registerApp(context, mDJISDKManagerCallback);

        //connects to WIFI bridge app
        if(USING_WIFI_BRIDGE){
            DJISDKManager.getInstance().enableBridgeModeWithBridgeAppIP("10.255.24.152");
        }
    }

    //Executes when the API key is attempted to be registered
    //Tests if the API key registration was successful
    //Attempts to connect to a product if the key registration was successful
    //Creates a listener for the product if a product was detected
    private DJISDKManager.SDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.SDKManagerCallback() {
        @Override
        public void onRegister(DJIError error) {
            Log.d(TAG, error == null ? "success" : error.getDescription());
            if(error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct();
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Register Success", Toast.LENGTH_LONG).show();
                        gvh.plat.sendMainMsg(HandlerMessage.MESSAGE_REGISTERED_DJI, true);
                    }
                });
            } else {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Register SDK failed for package: " + context.getPackageName(), Toast.LENGTH_LONG).show();
                        gvh.plat.sendMainMsg(HandlerMessage.MESSAGE_REGISTERED_DJI, false);
                    }
                });
            }
            gvh.log.d(TAG, "StarLib registered: " + DjiController.getAPIStatus());
            Log.e("TAG", error.toString());
        }
        @Override
        public void onProductChange(BaseProduct oldProduct, BaseProduct newProduct) {
            mProduct = newProduct;
            if(mProduct != null) {
                mProduct.setBaseProductListener(mDJIBaseProductListener);
                //sends a message about bluetooth state but its really the usb connection state,
                //but whoever wrote the gvh class did not anticipate non bluetooth connections
                gvh.plat.sendMainMsg(HandlerMessage.MESSAGE_BLUETOOTH, HandlerMessage.BLUETOOTH_CONNECTED);

                //creates an Aircraft instance
                if(mProduct instanceof Aircraft) {
                    mAircraft = (Aircraft) mProduct;
                    gvh.log.d(TAG, "Connected to DJI drone: " + mAircraft.getModel());

                    //gets the flightcontroller for the aircraft;
                    mFlightController = mAircraft.getFlightController();
                    mFlightControlData = new FlightControlData(0,0,0,0);
                    if(mFlightController != null) {
                        constructFlightController();
                    }else{
                        gvh.log.e(TAG, mAircraft.getModel() + " does not have a flight controller!");
                    }

                } else {
                    gvh.log.e(TAG, "Connected DJI product is not a drone!");
                }

            } else {
                //see above regarding bluetooth confusion
                gvh.plat.sendMainMsg(HandlerMessage.MESSAGE_BLUETOOTH, HandlerMessage.BLUETOOTH_DISCONNECTED);
            }
            notifyStatusChange();
            gvh.log.d(TAG, "onProductChange was called!");
        }
    };

    //Executes when a product's components or connectivity changes
    //Creates a component listener if a new component is detected
    private BaseProduct.BaseProductListener mDJIBaseProductListener = new BaseProduct.BaseProductListener() {
        @Override
        public void onComponentChange(BaseProduct.ComponentKey key, BaseComponent oldComponent, BaseComponent newComponent) {
            if(newComponent != null) {
                newComponent.setComponentListener(mDJIComponentListener);
            }
            notifyStatusChange();
        }
        @Override
        public void onConnectivityChange(boolean isConnected) {
            notifyStatusChange();
        }
    };

    //Executes when a component's connectivity changes
    private BaseComponent.ComponentListener mDJIComponentListener = new BaseComponent.ComponentListener() {
        @Override
        public void onConnectivityChange(boolean isConnected) {
            notifyStatusChange();
        }
    };

    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }

    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            context.sendBroadcast(intent);
        }
    };

    //displays information on the Flight Controller
    private void debug(FlightController mFlightController){
        FlightControllerState fState = mFlightController.getState();
        gvh.log.d(TAG, "Debug for the flight controller of " + mAircraft.getModel());
        gvh.log.d(TAG, "Motor state: " + fState.areMotorsOn());
        gvh.log.d(TAG, "Flying: " + fState.isFlying());
        gvh.log.d(TAG, "Landing confirmation required:" + fState.isLandingConfirmationNeeded());
    }

    //important flight control information.
    private void constructFlightController(){
        mFlightController.setVirtualStickAdvancedModeEnabled(true);
        mFlightController.setConnectionFailSafeBehavior(ConnectionFailSafeBehavior.LANDING, null);
        mFlightController.setRollPitchControlMode(RollPitchControlMode.ANGLE);
        mFlightController.setYawControlMode(YawControlMode.ANGLE);
        mFlightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
        mFlightController.setTerrainFollowModeEnabled(false, null);
        mFlightController.setTripodModeEnabled(false, null);
        mFlightController.setFlightOrientationMode(FlightOrientationMode.AIRCRAFT_HEADING, null);
        if (!mFlightController.isVirtualStickControlModeAvailable()){
            gvh.log.e(TAG, "Virtual Stick Control mode is unavailable, probably because a mission is running.");
        }
    }
}
