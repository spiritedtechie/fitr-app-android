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
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static com.google.android.gms.fitness.data.DataType.TYPE_DISTANCE_DELTA;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ActivityRecordingPresenter extends BasePresenter<ActivityRecordingView> {

    private static final String TAG = ActivityRecordingPresenter.class.getSimpleName();

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

    @Override
    protected void onViewAttached() {
        if (session != null && session.isOngoing()) {
            onView(view -> {
                view.allowStartSession(false);
                view.allowStopSession(true);
            });
        }
    }

    public void subscribe() {
        frh.subscribeIfNotExistingSubscription(TYPE_DISTANCE_DELTA)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .onErrorReturn(loggingErrorHandler())
                .subscribe();
    }

    public void unsubscribe() {
        frh.unsubscribe(TYPE_DISTANCE_DELTA)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .onErrorReturn(loggingErrorHandler())
                .subscribe();
    }

    public void startSession(String sessionType) {
        if (session != null && session.isOngoing()) {
            Log.i(TAG, "Session already ongoing.");
            return;
        }

        Session session = buildSession(sessionType);

        onView(v -> v.allowStartSession(false));

        fsh.startSession(session)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .map(sess -> this.session = sess)
                .subscribe(
                        n -> onView(v -> {
                            v.sessionStarted(n);
                            v.allowStopSession(true);
                        }),
                        e -> loggingErrorHandler().call(e));
    }

    public void stopSession() {
        if (session == null || !session.isOngoing()) {
            Log.i(TAG, "Session is not available or already stopped.");
            return;
        }

        onView(v -> v.allowStopSession(false));

        fsh.stopSession(session.getIdentifier())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .firstOrDefault(null)
                .map(sess -> this.session = null)
                .subscribe(
                        n -> onView(v -> {
                            v.sessionStopped(n);
                            v.allowStartSession(true);
                        }),
                        e -> loggingErrorHandler().call(e));
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

    private Func1<Throwable, Void> loggingErrorHandler() {
        return t -> {
            Log.e(TAG, t.getMessage(), t);
            return null;
        };
    }

    private String formatTime(long timeMillis) {
        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT_PATTERN_DEFAULT);
        return df.format(new Date(timeMillis));
    }
}
