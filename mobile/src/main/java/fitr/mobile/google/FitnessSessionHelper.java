package fitr.mobile.google;

import com.google.android.gms.fitness.data.Session;

import rx.Observable;

public interface FitnessSessionHelper {

    String TAG = "FitnessSessionHelper";

    Observable<Void> startSession(Session session);

    Observable<Session> stopSession(String sessionId);

}
