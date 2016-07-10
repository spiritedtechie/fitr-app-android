package fitr.mobile.presenters;

import android.util.Log;

import com.google.android.gms.fitness.data.Session;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import javax.inject.Inject;

import fitr.mobile.google.FitnessRecordingHelper;
import fitr.mobile.google.FitnessSessionHelper;
import fitr.mobile.views.ActivityRecordingView;
import rx.functions.Func1;

import static com.google.android.gms.fitness.data.DataType.TYPE_DISTANCE_DELTA;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ActivityRecordingPresenter {

    private static final String TAG = "RecordingPresenter";

    private static final String DATE_FORMAT_PATTERN_DEFAULT = "yyyy-MM-dd'T'HH:mm:ss";

    @Inject
    FitnessRecordingHelper frh;
    @Inject
    FitnessSessionHelper fsh;

    private Session session;

    public ActivityRecordingPresenter(FitnessRecordingHelper frh, FitnessSessionHelper fsh) {
        this.frh = frh;
        this.fsh = fsh;
    }

    public void subscribe() {
        frh.subscribeIfNotExistingSubscription(TYPE_DISTANCE_DELTA)
                .onErrorReturn(loggingErrorHandler())
                .subscribe();
    }

    public void unsubscribe() {
        frh.unsubscribe(TYPE_DISTANCE_DELTA)
                .onErrorReturn(loggingErrorHandler())
                .subscribe();
    }

    public void startSession(final ActivityRecordingView view, String sessionType) {
        if (session != null && session.isOngoing()) {
            Log.i(TAG, "Session already ongoing.");
            return;
        }

        this.session = buildSession(sessionType);
        view.sessionStarting(session);

        fsh.startSession(this.session)
                .map(new Func1<Session, Void>() {
                    @Override
                    public Void call(Session session) {
                        view.sessionStarted(session);
                        return null;
                    }
                })
                .onErrorReturn(loggingErrorHandler())
                .subscribe();

    }

    private Session buildSession(String activityType) {
        Date currTime = new Date();
        String sessionName = activityType + "-" + formatTime(currTime.getTime());

        return new Session.Builder()
                .setName(sessionName)
                .setIdentifier(UUID.randomUUID().toString())
                .setDescription("Session of " + activityType)
                .setStartTime(currTime.getTime(), MILLISECONDS)
                .setActivity(activityType)
                .build();
    }

    public void stopSession(final ActivityRecordingView view) {
        if (session == null || !session.isOngoing()) {
            Log.i(TAG, "Session is not available or already stopped.");
            return;
        }

        view.sessionStopping(session);

        fsh.stopSession(session.getIdentifier())
                .firstOrDefault(null)
                .map(new Func1<Session, Void>() {
                    @Override
                    public Void call(Session session) {
                        ActivityRecordingPresenter.this.session = null;
                        view.sessionStopped(session);
                        return null;
                    }
                })
                .onErrorReturn(loggingErrorHandler())
                .subscribe();
    }

    private Func1<Throwable, Void> loggingErrorHandler() {
        return new Func1<Throwable, Void>() {
            @Override
            public Void call(Throwable t) {
                Log.e(TAG, t.getMessage(), t);
                return null;
            }
        };
    }

    private String formatTime(long timeMillis) {
        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT_PATTERN_DEFAULT);
        return df.format(new Date(timeMillis));
    }
}
