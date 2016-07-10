package fitr.mobile.views;

import com.google.android.gms.fitness.data.Session;

public interface ActivityRecordingView {

    void sessionStarting(Session session);

    void sessionStarted(Session session);

    void sessionStopping(Session session);

    void sessionStopped(Session session);

}
