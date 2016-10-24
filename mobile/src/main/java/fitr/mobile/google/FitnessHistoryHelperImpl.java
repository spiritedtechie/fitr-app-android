package fitr.mobile.google;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import rx.Observable;
import rx.Subscriber;

public class FitnessHistoryHelperImpl implements FitnessHistoryHelper {

    private GoogleApiClient client;

    public FitnessHistoryHelperImpl(GoogleApiClient client) {
        this.client = client;
    }

    @Override
    public Observable<DataReadResult> readData(final DataReadRequest request) {
        return Observable.create(new Observable.OnSubscribe<DataReadResult>() {

            @Override
            public void call(final Subscriber<? super DataReadResult> subscriber) {
                if (clientUnavailable(subscriber)) return;

                Fitness.HistoryApi.readData(client, request).setResultCallback(dataReadResult -> {
                    if (subscriber.isUnsubscribed()) return;
                    if (dataReadResult.getStatus().isSuccess()) {
                        Log.i(TAG, "Successfully read data!");
                        subscriber.onNext(dataReadResult);
                        subscriber.onCompleted();
                    } else {
                        Log.i(TAG, "There was a problem reading data.");
                        subscriber.onError(new IllegalStateException("Problem reading data: " + dataReadResult.getStatus().toString()));
                    }
                });
            }
        });
    }

    private boolean clientUnavailable(Subscriber<? super DataReadResult> subscriber) {
        if (client == null || !client.isConnected()) {
            subscriber.onError(new IllegalStateException("Client is not available or not connected"));
            return true;
        }
        return false;
    }
}
