package fitr.mobile.google;

import android.app.Activity;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.Fitness;

import rx.Observable;
import rx.Subscriber;

import static com.google.android.gms.common.api.GoogleApiClient.Builder;
import static com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import static com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

public class FitnessClientManager {

    public static final String TAG = "FitnessClientManager";

    private static final int REQUEST_OAUTH = 1;

    private Activity activity;
    private GoogleApiClient client;

    public FitnessClientManager(Activity activity) {
        this.activity = activity;
        buildFitnessClient();
    }

    private void buildFitnessClient() {
        Log.i(TAG, "buildFitnessClient");

        client = new Builder(this.activity)
                .addApiIfAvailable(Fitness.RECORDING_API)
                .addApiIfAvailable(Fitness.SESSIONS_API)
                .addApiIfAvailable(Fitness.HISTORY_API)
                .build();
    }

    public Observable connect() {
        Log.i(TAG, "connect");

        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(final Subscriber<? super Void> subscriber) {
                if (client != null && !client.isConnected() && !client.isConnecting()) {
                    if (subscriber.isUnsubscribed()) return;

                    client.registerConnectionCallbacks(new ConnectionCallbacks() {
                        @Override
                        public void onConnected(@Nullable Bundle bundle) {
                            Log.i(TAG, "Connected");
                            subscriber.onNext(null);
                        }

                        @Override
                        public void onConnectionSuspended(int i) {
                            if (i == CAUSE_NETWORK_LOST) {
                                Log.i(TAG, "Connection lost. Cause: Network Lost.");
                            } else if (i == CAUSE_SERVICE_DISCONNECTED) {
                                Log.i(TAG, "Connection lost. Reason: Service Disconnected");
                            }
                        }
                    });

                    client.registerConnectionFailedListener(new OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult result) {
                            Log.i(TAG, "Connection failed");
                            subscriber.onError(new IllegalStateException("Connection failed"));
                            try {
                                result.startResolutionForResult(FitnessClientManager.this.activity, REQUEST_OAUTH);
                            } catch (IntentSender.SendIntentException e) {
                            }
                        }
                    });

                    client.connect();
                }
            }
        });
    }

    public void disconnect() {
        Log.i(TAG, "disconnect");

        if (client != null) {
            client.disconnect();
        }
    }

    public GoogleApiClient createClient() {
        if (client == null) {
            this.buildFitnessClient();
        }

        return client;
    }
}
