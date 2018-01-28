package duling;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class dulingDelayedTask extends Service implements LocationListener {
    private static final String TAG = "DulingDelayedTask";
    public long timeToNextUpdate = 0;
    public long lastLocationTime = 0;
    public long lastActivityTime = 0;
    public long lastEventTime = 0;
    public double lastSpeed = 0;
    public boolean activityReceived = false;
    public double travelDistance = 0;
    public double lastAccuracy = 0;
    public boolean isWalkingStill = false;

    LocationManager DulingLocManager;

    // timer for delayed task
    public Timer timer = new Timer();
    myTimer mTask = new myTimer();

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {

        Log.e(TAG, "onCreate");

        // start activity recognition service
        Intent serviceIntent = new Intent(this, duling.dulingActivityRecognition.class);
        startService(serviceIntent);
        Log.e(TAG,"Starting Activity Service with intent");

        // register broadcast receivers for activity data
        LocalBroadcastManager.getInstance(this).registerReceiver(DulingActivityDataReceiver, new IntentFilter("DulingActivityRecognition"));

        requestLocation();
    }

    /*
    Method to calculate speed and execute delayed task based on location data
     */
    public void handleLocationUpdate(Location location){

//        double accuracy = location.getAccuracy();
        long locationTime = location.getTime();

        // Assumption: the accuracy is constant 7.8m
        double accuracy = 7.8;

        // initialize speed to driving speed before any activity is received
        if (activityReceived==false){
            lastSpeed = 1.39;
        }

        // schedule a delayed task to send message (if already exist a delayed task, remove it)
        if (mTask!=null){
            mTask.cancel();
            //debug
            Log.e(TAG,"Scheduled Task Canceled");
        }

        // if the user is not still, request location updates
        if (lastSpeed>0){
            // update time to next update
            timeToNextUpdate = (long) (accuracy/lastSpeed*1000 - 2000);

            // debug
            Log.e(TAG, "Time to next update is " + String.valueOf(timeToNextUpdate) + " millisecond");

            mTask = new myTimer();
            timer.schedule(mTask,timeToNextUpdate);

            // update last activity Time
            lastLocationTime = locationTime;

            // update last accuracy
            lastAccuracy = accuracy;
        }
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        // stop activity recognition service
        Intent serviceIntent = new Intent(this, duling.dulingActivityRecognition.class);
        stopService(serviceIntent);
        Log.e(TAG,"Stop Activity Service with intent");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(DulingActivityDataReceiver);
        DulingLocManager.removeUpdates(this);
    }

//    private void sendBroadcastMessage(String intentFilterName, Boolean isUpdate) {
//        Intent intent = new Intent(intentFilterName);
//        intent.putExtra("DelayedLocationTask", isUpdate);
//        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
//    }


    private BroadcastReceiver DulingActivityDataReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // debug
            Log.e(TAG, "Activity Data Received in DelayedTask");

            activityReceived = true;
            double speed = intent.getDoubleExtra("DulingSpeed",lastSpeed);
            long activityTime = intent.getLongExtra("DulingActivityTimestamp", lastActivityTime);

            // schedule a delayed task to send message (if already exist a delayed task, remove it)
            if (mTask!=null){
                mTask.cancel();
                //debug
                Log.e(TAG,"Scheduled Task Canceled");
            }

            // update time to next update
            if (lastActivityTime<lastLocationTime){
                lastEventTime = lastLocationTime;
            } else {
                lastEventTime = lastActivityTime;
            }

            // update distance traveled
            if (lastSpeed>0){
                travelDistance = travelDistance+(activityTime-lastEventTime)*lastSpeed;
                if (speed>0){
                    timeToNextUpdate = (long)((timeToNextUpdate-lastActivityTime+lastEventTime)*(lastSpeed/speed) - 2000);
                    mTask = new myTimer();
                    timer.schedule(mTask,timeToNextUpdate);
                }
            } else if (lastSpeed==0){
                if (speed>0) {
                    timeToNextUpdate = (long)((lastAccuracy*1000-travelDistance)/speed);
                    Log.e(TAG,"Schedule a new location update after " + timeToNextUpdate + " seconds");
                    mTask = new myTimer();
                    timer.schedule(mTask,timeToNextUpdate);
                }
            }

            // update the last speed
            lastSpeed = speed;
            // update last activity Time
            lastActivityTime = activityTime;

        }
    };

    @Override
    public void onLocationChanged(Location location) {
        if (isWalkingStill) {
            handleLocationUpdate(location);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.e(TAG,"onStatusChanged");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.e(TAG,"onProviderEnabled");
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.e(TAG,"onProviderDisabled");
    }

    public class myTimer extends TimerTask
    {
        @Override
        public void run() {
//            LocationManager lmgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//            LocationListener myLocListener = new DulingLocationService().DulingLocationListener;
//            lmgr.requestSingleUpdate(LocationManager.GPS_PROVIDER,myLocListener,null);

            Log.e(TAG,"Timer expires, request a new location update");
//            DulingLocationListener dulingLocListener = new DulingLocationListener();
//            LocationManager lmgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//            lmgr.requestSingleUpdate(LocationManager.GPS_PROVIDER,dulingLocListener,null);

//            sendBroadcastMessage(TAG,true);
            //TODO replace with requestSingleUpdate
            requestLocation();

        }
    }

    public void requestLocation(){
        DulingLocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // TODO replace the provider, listener and looper thread to the ones we extract from the original class

        // TODO we create an instance of the Location Listeners in the calling class
//        DulingLocationListener origLocListener1 = new DulingLocationListener();
//        origLocListener1.setGPSLogger(GPSLogger.this);
//        LocationListener origLocListener1 = new com.gabm.fancyplaces.functional.LocationHandler(DulingLocManager, this);

//        DulingLocManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, origLocListener1, Looper.getMainLooper());
        DulingLocManager.requestSingleUpdate(LocationManager.GPS_PROVIDER,this,null);
    }
}

