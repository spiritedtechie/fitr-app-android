package fitr.mobile.google;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;

import rx.Observable;

import static com.google.android.gms.common.api.GoogleApiClient.Builder;
import static com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;

public class FitnessClientManager {

    private static final String TAG = "FitnessClientManager";

    private static final int REQUEST_OAUTH = 1;

    private Context context;
    private GoogleApiClient client;

    public FitnessClientManager(Context context) {
        this.context = context;
        buildFitnessClient();
    }

    private void buildFitnessClient() {
        client = new Builder(this.context)
                .addApiIfAvailable(Fitness.RECORDING_API)
                .addApiIfAvailable(Fitness.SESSIONS_API)
                .addApiIfAvailable(Fitness.HISTORY_API)
                .addScope(new Scope(Scopes.FITNESS_LOCATION_READ_WRITE))
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addScope(new Scope(Scopes.FITNESS_BODY_READ_WRITE))
                .build();
    }

    public Observable<Void> connect(final Activity activity) {

        return Observable.create(subscriber -> {
            if (client != null && !client.isConnected() && !client.isConnecting()) {

                if (subscriber.isUnsubscribed()) return;

                client.registerConnectionCallbacks(new ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        Log.i(TAG, "Connected");
                        subscriber.onNext(null);
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        if (i == CAUSE_NETWORK_LOST) {
                            Log.i(TAG, "Connection lost. Cause: Network Lost.");
                        } else if (i == CAUSE_SERVICE_DISCONNECTED) {
                            Log.i(TAG, "Connection lost. Reason: Service Disconnected");
                        } else {
                            Log.i(TAG, "Connection lost. Reason: Unknown");
                        }
                    }
                });

                client.registerConnectionFailedListener(result -> {
                    Log.i(TAG, "Connection failed");
                    subscriber.onError(new IllegalStateException("Connection failed: "));
                    if (activity != null) {
                        try {
                            result.startResolutionForResult(activity, REQUEST_OAUTH);
                        } catch (IntentSender.SendIntentException e) {
                        }
                    }
                });

                Log.i(TAG, "Connecting");
                client.connect();
            } else {
                // Already connected/connecting
                Log.i(TAG, "Already connected/connecting");
                subscriber.onNext(null);
                subscriber.onCompleted();
            }
        });
    }

    public void disconnect() {
        if (client != null) {
            Log.i(TAG, "disconnect");
            client.disconnect();
        }
    }

    public GoogleApiClient getClient() {
        if (client == null) {
            this.buildFitnessClient();
        }

        return client;
    }
}
