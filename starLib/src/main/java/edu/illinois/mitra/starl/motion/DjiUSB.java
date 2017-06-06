//Written by Tim Liang - @timliang on verivital.slack.com for questions
package edu.illinois.mitra.starl.motion;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.objects.HandlerMessage;

public class DjiUSB {

    private static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";
    private static final boolean USING_WIFI_BRIDGE = true;
    
    private BaseProduct mProduct;
    private Aircraft mAircraft;
    private FlightController mFlightController;
    private Handler mHandler;

    private static String TAG = "DjiUSB";
    
    private Context context;
    private String mac;
    private GlobalVarHolder gvh;

    public DjiUSB(GlobalVarHolder gvh, Context context, String mac) {
        this.context = context;
        this.mac = mac;
        this.gvh = gvh;
        gvh.log.d(TAG, "HEY THE MAVICBTI CONSTRUCTOR ACTUALLY GOT CALLED!!!!!!!!!!");
        initConnection();
    }

    //Check to see if the API key has been registered
    private static boolean getAPIStatus(){
        return DJISDKManager.getInstance().hasSDKRegistered();
    }

    // sets pitch to val percent of max angle
    // positive value moves forward, negative backward
    public void setPitch(double val) {
        if (mFlightController != null)
        {
        }
    }

    // sets roll to val percent of max angle
    // positive value moves right, negative left
    public void setRoll(double val) {
        if (mFlightController != null)
        {
        }
    }

    // sets yaw to val percent of max angular rotation
    // positive value turns right (clockwise from above), negative turns left
    public void setYaw(double val) {
        if (mFlightController != null)
        {
        }
    }

    // moves the drone up (+ val) or down (- val)
    public void setThrottle(double val) {
        if (mFlightController != null)
        {
        }
    }

    public void sendLanding() {
        if (mFlightController != null)
        {
            mFlightController.startLanding(null);
        }
    }

    public void sendTakeoff() {
        if (mFlightController != null)
        {
            gvh.log.d(TAG, "Taking off!");
            mFlightController.startTakeoff(takeOffCallBack);
        }
    }

    // make the drone stop and fall to the ground
    public void sendEmergency() {
        if (mFlightController != null)
        {
        }
    }

    public void hover() {
        // setting this flag to 0 causes the drone to ignore roll/pitch commands and attempt to hover
    }

    public void setMaxTilt(float maxTilt) {
    }

    public void initConnection() {
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
                        gvh.plat.sendMainMsg(HandlerMessage.MESSAGE_REGISTERED, true);
                    }
                });
            } else {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Register SDK failed for package: " + context.getPackageName(), Toast.LENGTH_LONG).show();
                        gvh.plat.sendMainMsg(HandlerMessage.MESSAGE_REGISTERED, false);
                    }
                });
            }
            gvh.log.d(TAG, "StarLib registered: " + DjiUSB.getAPIStatus());
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
                    if(mFlightController != null) {
                        mFlightController.startTakeoff(takeOffCallBack);
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

    //todo comment this and updateRunnable
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

    //Executes when take off completes (drone arrives at .5 meters)
    private CommonCallbacks.CompletionCallback takeOffCallBack = new CommonCallbacks.CompletionCallback() {
        @Override
        public void onResult(DJIError djiError) {
            gvh.log.e(TAG, "Take off errors: " + djiError);
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

}
