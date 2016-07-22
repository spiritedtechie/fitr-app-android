package fitr.mobile.google;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.result.SessionStopResult;

import rx.Observable;
import rx.Subscriber;

public class FitnessSessionHelperImpl implements FitnessSessionHelper {

    private GoogleApiClient client;

    public FitnessSessionHelperImpl(GoogleApiClient client) {
        this.client = client;
    }

    @Override
    public Observable<Session> startSession(final Session session) {

        return Observable.create(new Observable.OnSubscribe<Session>() {
            @Override
            public void call(final Subscriber<? super Session> subscriber) {
                if (clientUnavailable(subscriber)) return;

                if (session == null) {
                    subscriber.onError(new IllegalStateException("Session is not available"));
                    return;
                }

                Fitness.SessionsApi.startSession(client, session).setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (subscriber.isUnsubscribed()) return;
                        if (status.isSuccess()) {
                            Log.i(TAG, "Successfully started session " + session.getIdentifier());
                            subscriber.onNext(session);
                            subscriber.onCompleted();
                        } else {
                            Log.i(TAG, "There was a problem starting session.");
                            subscriber.onError(new IllegalStateException("Problem starting session: " + status.toString()));
                        }
                    }
                });
            }
        });
    }

    @Override
    public Observable<Session> stopSession(final String sessionId) {

        return Observable.create(new Observable.OnSubscribe<Session>() {
            @Override
            public void call(final Subscriber<? super Session> subscriber) {
                if (clientUnavailable(subscriber)) return;

                Fitness.SessionsApi.stopSession(client, sessionId).setResultCallback(new ResultCallback<SessionStopResult>() {
                    @Override
                    public void onResult(@NonNull SessionStopResult sessionStopResult) {
                        if (subscriber.isUnsubscribed()) return;
                        if (sessionStopResult.getStatus().isSuccess()) {
                            for (Session s : sessionStopResult.getSessions()) {
                                Log.i(TAG, "Successfully stopped session " + sessionId);
                                subscriber.onNext(s);
                            }
                            subscriber.onCompleted();
                        } else {
                            Log.i(TAG, "There was a problem stopping session.");
                            subscriber.onError(new IllegalStateException("Problem stopping session: " + sessionId));
                        }
                    }
                });
            }
        });
    }

    private boolean clientUnavailable(Subscriber<? super Session> subscriber) {
        if (client == null || !client.isConnected()) {
            subscriber.onError(new IllegalStateException("Client is not available or not connected"));
            return true;
        }
        return false;
    }

}
