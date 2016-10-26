package fitr.mobile.views;

import com.google.android.gms.fitness.data.Session;

public interface ActivityRecordingView extends View {

    void allowStartSession(boolean allow);

    void allowStopSession(boolean allow);

    void sessionStarted(Session session);

    void sessionStopped(Session session);

}
