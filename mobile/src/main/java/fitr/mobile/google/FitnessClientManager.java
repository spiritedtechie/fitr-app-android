package fitr.mobile.google;

import android.app.Activity;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.Fitness;

public class FitnessClientManager {

    public static final String TAG = "FitnessClientManager";

    private static final int REQUEST_OAUTH = 1;

    private Activity activity;
    private GoogleApiClient client;

    public FitnessClientManager(Activity activity) {
        this.activity = activity;
    }

    public GoogleApiClient buildFitnessClient() {
        Log.i(TAG, "buildFitnessClient");

        final GoogleApiClient.ConnectionCallbacks connectionHandler = new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(@Nullable Bundle bundle) {
                Log.i(TAG, "Connected");
            }

            @Override
            public void onConnectionSuspended(int i) {
                if (i == CAUSE_NETWORK_LOST) {
                    Log.i(TAG, "Connection lost. Cause: Network Lost.");
                } else if (i == CAUSE_SERVICE_DISCONNECTED) {
                    Log.i(TAG, "Connection lost. Reason: Service Disconnected");
                }
            }
        };

        final GoogleApiClient.OnConnectionFailedListener connectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(ConnectionResult result1) {
                Log.i(TAG, "Connection Failed");
                try {
                    result1.startResolutionForResult(FitnessClientManager.this.activity, REQUEST_OAUTH);
                } catch (IntentSender.SendIntentException e) {

                }
            }
        };

        client = new GoogleApiClient.Builder(this.activity)
                .addApiIfAvailable(Fitness.RECORDING_API)
                .addApiIfAvailable(Fitness.SESSIONS_API)
                .addApiIfAvailable(Fitness.HISTORY_API)
                .addConnectionCallbacks(connectionHandler)
                .addOnConnectionFailedListener(connectionFailedListener)
                .build();

        return client;
    }

    public void connect() {
        client.connect();
    }

    public GoogleApiClient getClient() {
        return client;
    }
}
