package fitr.mobile.google;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Session;

import rx.Observable;
import rx.Subscriber;

public class FitnessSessionHelperImpl implements FitnessSessionHelper {

    private GoogleApiClient client;

    public FitnessSessionHelperImpl(GoogleApiClient client) {
        this.client = client;
    }

    @Override
    public Observable<Session> startSession(final Session session) {

        return Observable.create(subscriber -> {
            if (clientUnavailable(subscriber)) return;

            if (session == null) {
                subscriber.onError(new IllegalStateException("Session is not available"));
                return;
            }

            Fitness.SessionsApi.startSession(client, session).setResultCallback(status -> {
                if (subscriber.isUnsubscribed()) return;
                if (status.isSuccess()) {
                    Log.i(TAG, "Successfully started session " + session.getIdentifier());
                    subscriber.onNext(session);
                    subscriber.onCompleted();
                } else {
                    Log.i(TAG, "There was a problem starting session.");
                    subscriber.onError(new IllegalStateException("Problem starting session: " + status.toString()));
                }
            });
        });
    }

    @Override
    public Observable<Session> stopSession(final String sessionId) {

        return Observable.create(subscriber -> {
            if (clientUnavailable(subscriber)) return;

            Fitness.SessionsApi.stopSession(client, sessionId).setResultCallback(result -> {
                if (subscriber.isUnsubscribed()) return;
                if (result.getStatus().isSuccess()) {
                    for (Session s : result.getSessions()) {
                        Log.i(TAG, "Successfully stopped session " + sessionId);
                        subscriber.onNext(s);
                    }
                    subscriber.onCompleted();
                } else {
                    Log.i(TAG, "There was a problem stopping session.");
                    subscriber.onError(new IllegalStateException("Problem stopping session: " + sessionId));
                }
            });
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
