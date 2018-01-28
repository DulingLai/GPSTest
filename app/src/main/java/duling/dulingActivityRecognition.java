package duling;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

public class dulingActivityRecognition extends IntentService implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    public dulingActivityRecognition() {
        super("ActivityRecognitionService");
    }

    public GoogleApiClient mGoogleApiClient;
    private static final long ACTIVITY_UPDATE_INTERVAL = 1000;
    public double speed = 0;
    public double lastSpeed = 15;

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.e("DulingActivity","Received New Activity");
        DetectedActivity mostProbableActivity = null;
        long timestamp = 0;

        if(ActivityRecognitionResult.hasResult(intent)) {
            // Get the update
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

            // Get the most probable activity from the list of activities in the update
            List<DetectedActivity> probableActivity = result.getProbableActivities();
            for (DetectedActivity a : probableActivity){
                Log.e("DulingActivity","Current Activity is "+a.getType()+" with probability "+a.getConfidence());
                if (a.getConfidence()>68 && (a.getType()==DetectedActivity.IN_VEHICLE || a.getType()==DetectedActivity.ON_BICYCLE ||
                        a.getType()==DetectedActivity.RUNNING || a.getType()==DetectedActivity.STILL || a.getType()==DetectedActivity.WALKING)){
                    mostProbableActivity = a;
                    Log.e("DulingActivity","Current Activity satisfies the requirement");
                    break;
                }
            }

            // get the speed and timestamp
            if (mostProbableActivity!=null){
                timestamp  = result.getTime();
                switch( mostProbableActivity.getType() ) {
                    case DetectedActivity.IN_VEHICLE: {
                        speed = 15;
                        Log.e("DulingActivity","Current Activity is IN_VEHICLE");
                        break;
                    }
                    case DetectedActivity.ON_BICYCLE: {
                        speed = 4.17;
                        Log.e("DulingActivity","Current Activity is ON_BICYCLE");
                        break;
                    }
                    case DetectedActivity.RUNNING: {
                        speed = 2.78;
                        Log.e("DulingActivity","Current Activity is RUNNING");
                        break;
                    }
                    case DetectedActivity.STILL: {
                        speed = 0;
                        Log.e("DulingActivity","Current Activity is STILL");
                        break;
                    }
                    case DetectedActivity.WALKING: {
                        speed = 1.39;
                        Log.e("DulingActivity","Current Activity is WALKING");
                        break;
                    }
                    default: {
                        Log.e("DulingActivity","Oops, Something wrong");
                        break;
                    }
                }
                sendBroadcastMessage("DulingActivityRecognition", speed, timestamp);
            }
            lastSpeed = speed;
        }

    }

    @Override
    public void onCreate(){
        super.onCreate();

        Log.e("DulingActivity","Starting Activity Service");
        connectGoogleApiClient();

    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.e("DulingActivity","Stop Activity Service");
        Intent intent = new Intent( this, dulingActivityRecognition.class );
        PendingIntent pendingIntent = PendingIntent.getService( this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT );
        if (mGoogleApiClient.isConnected()) {
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, pendingIntent);
        }
    }

    /*
 Google API Client setup for Activity Recognition
 */
    protected synchronized void connectGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(ActivityRecognition.API)
                .build();

        mGoogleApiClient.connect();
    }

    private void  sendBroadcastMessage(String intentFilterName, double msg1, long msg2) {
        Intent intent = new Intent(intentFilterName);
        intent.putExtra("DulingSpeed",msg1);
        intent.putExtra("DulingActivityTimestamp",msg2);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.e("DulingActivity","broadcast speed and timestamp");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Intent intent = new Intent( this, dulingActivityRecognition.class );
        PendingIntent pendingIntent = PendingIntent.getService( this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT );
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates( mGoogleApiClient,ACTIVITY_UPDATE_INTERVAL, pendingIntent );
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("DulingActivity","connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("DulingActivity","connection failed");
    }
}

