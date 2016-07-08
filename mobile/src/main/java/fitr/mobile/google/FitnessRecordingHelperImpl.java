package fitr.mobile.google;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Subscription;
import com.google.android.gms.fitness.result.ListSubscriptionsResult;

import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

import static com.google.android.gms.fitness.FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED;

public class FitnessRecordingHelperImpl implements FitnessRecordingHelper {

    private GoogleApiClient client;

    public FitnessRecordingHelperImpl(GoogleApiClient client) {
        this.client = client;
    }

    @Override
    public Observable<Void> subscribeIfNotExistingSubscription(final DataType dataType) {
        return subscriptionExists(dataType)
                .flatMap(new Func1<Boolean, Observable<Void>>() {
                    @Override
                    public Observable<Void> call(Boolean isTrue) {
                        if (isTrue) {
                            Log.i(TAG, "Existing subscriptions found, so not subscribing");
                            return Observable.empty();
                        } else {
                            Log.i(TAG, "No subscriptions found, so subscribing");
                            return subscribe(dataType);
                        }
                    }
                });
    }

    private Observable<Boolean> subscriptionExists(final DataType dataType) {
        Log.i(TAG, "Checking subscriptions exist for: " + dataType);

        return this.getSubscriptions()
                .filter(new Func1<Subscription, Boolean>() {
                    @Override
                    public Boolean call(Subscription subscription) {
                        return subscription.getDataType() != dataType;
                    }
                })
                .firstOrDefault(null)
                .map(new Func1<Subscription, Boolean>() {
                    @Override
                    public Boolean call(Subscription subscription) {
                        return subscription != null;
                    }
                });
    }

    private Observable<Subscription> getSubscriptions() {

        return Observable.create(new Observable.OnSubscribe<Subscription>() {
            @Override
            public void call(final Subscriber<? super Subscription> subscriber) {

                if (client == null || !client.isConnected()) {
                    subscriber.onError(new IllegalStateException("Client is not available or not connected"));
                    return;
                }
                Fitness.RecordingApi.listSubscriptions(client).setResultCallback(new ResultCallback<ListSubscriptionsResult>() {
                    @Override
                    public void onResult(@NonNull ListSubscriptionsResult listSubscriptionsResult) {
                        if (subscriber.isUnsubscribed()) return;
                        if (listSubscriptionsResult.getStatus().isSuccess()) {
                            Log.i(TAG, "Subscriptions retrieved.");
                            List<Subscription> subscriptions = listSubscriptionsResult.getSubscriptions();
                            for (Subscription s : subscriptions) {
                                subscriber.onNext(s);
                            }
                            subscriber.onCompleted();
                        } else {
                            Log.i(TAG, "Failed to retrieve subscriptions.");
                            subscriber.onError(new IllegalStateException("Problem getting subscriptions: " + listSubscriptionsResult.getStatus().toString()));
                        }
                    }
                });
            }
        });
    }

    @Override
    public Observable<Void> subscribe(final DataType dataType) {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(final Subscriber<? super Void> subscriber) {

                if (client == null || !client.isConnected()) {
                    subscriber.onError(new IllegalStateException("Client is not available or not connected"));
                    return;
                }
                Fitness.RecordingApi.subscribe(client, dataType).setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (subscriber.isUnsubscribed()) return;
                        if (status.isSuccess()) {
                            if (status.getStatusCode() == SUCCESS_ALREADY_SUBSCRIBED) {
                                Log.i(TAG, "Existing subscription detected.");
                            } else {
                                Log.i(TAG, "Successfully subscribed!");
                                subscriber.onNext(null);
                            }
                            subscriber.onCompleted();
                        } else {
                            Log.i(TAG, "There was a problem subscribing.");
                            subscriber.onError(new IllegalStateException("Problem subscribing: " + status.toString()));
                        }
                    }
                });
            }
        });
    }

    @Override
    public Observable<Void> unsubscribe(final DataType dataType) {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(final Subscriber<? super Void> subscriber) {

                if (client == null || !client.isConnected()) {
                    subscriber.onError(new IllegalStateException("Client is not available or not connected"));
                    return;
                }
                Fitness.RecordingApi.unsubscribe(client, dataType).setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (subscriber.isUnsubscribed()) return;
                        if (status.isSuccess()) {
                            Log.i(TAG, "Successfully unsubscribed!");
                            subscriber.onNext(null);
                            subscriber.onCompleted();
                        } else {
                            Log.i(TAG, "There was a problem unsubscribing.");
                            subscriber.onError(new IllegalStateException("Problem unsubscribing: " + status.toString()));
                        }
                    }
                });
            }
        });
    }
}