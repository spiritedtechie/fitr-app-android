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
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static com.google.android.gms.fitness.data.DataType.TYPE_DISTANCE_DELTA;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ActivityRecordingPresenter extends BasePresenter<ActivityRecordingView> {

    private static final String TAG = "RecordingPresenter";

    private static final String DATE_FORMAT_PATTERN_DEFAULT = "yyyy-MM-dd'T'HH:mm:ss";

    @Inject
    FitnessRecordingHelper frh;

    @Inject
    FitnessSessionHelper fsh;

    private Session session;

    private Subscription subscribeSubscriber;
    private Subscription unsubscribeSubcriber;
    private Subscription stopSessionSubscriber;
    private Subscription startSessionSubscriber;

    public ActivityRecordingPresenter(FitnessRecordingHelper frh, FitnessSessionHelper fsh) {
        this.frh = frh;
        this.fsh = fsh;
    }

    @Override
    public void attachView(ActivityRecordingView view) {
        super.attachView(view);
    }

    @Override
    public void detachView() {
        unsubscribe(subscribeSubscriber);
        unsubscribe(unsubscribeSubcriber);
        unsubscribe(stopSessionSubscriber);
        unsubscribe(startSessionSubscriber);
        super.detachView();
    }

    public void subscribe() {
        checkViewAttached();

        subscribeSubscriber = frh.subscribeIfNotExistingSubscription(TYPE_DISTANCE_DELTA)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .onErrorReturn(loggingErrorHandler())
                .subscribe();
    }

    public void unsubscribe() {
        checkViewAttached();

        unsubscribeSubcriber = frh.unsubscribe(TYPE_DISTANCE_DELTA)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .onErrorReturn(loggingErrorHandler())
                .subscribe();
    }

    public void startSession(String sessionType) {
        checkViewAttached();

        if (session != null && session.isOngoing()) {
            Log.i(TAG, "Session already ongoing.");
            return;
        }

        this.session = buildSession(sessionType);

        if (getView() != null) {
            getView().sessionStarting(session);
        }

        startSessionSubscriber = fsh.startSession(this.session)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<Session>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        loggingErrorHandler().call(e);
                    }

                    @Override
                    public void onNext(Session session) {
                        getView().sessionStarted(session);
                    }
                });
    }

    public void stopSession() {
        checkViewAttached();

        if (session == null || !session.isOngoing()) {
            Log.i(TAG, "Session is not available or already stopped.");
            return;
        }

        if (getView() != null) {
            getView().sessionStopping(session);
        }

        stopSessionSubscriber = fsh.stopSession(session.getIdentifier())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .firstOrDefault(null)
                .subscribe(new Subscriber<Session>() {
                    @Override
                    public void onCompleted() {
                        ActivityRecordingPresenter.this.session = null;
                    }

                    @Override
                    public void onError(Throwable e) {
                        loggingErrorHandler().call(e);
                    }

                    @Override
                    public void onNext(Session session) {
                        if (session != null) getView().sessionStopped(session);
                    }
                });
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
