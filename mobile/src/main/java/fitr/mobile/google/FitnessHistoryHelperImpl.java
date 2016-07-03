package fitr.mobile.google;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import rx.Observable;
import rx.Subscriber;

public class FitnessHistoryHelperImpl implements FitnessHistoryHelper {

    public GoogleApiClient client;

    public FitnessHistoryHelperImpl(GoogleApiClient client) {
        this.client = client;
    }

    @Override
    public Observable<DataReadResult> readData(final DataReadRequest request) {

        return Observable.create(new Observable.OnSubscribe<DataReadResult>() {

            @Override
            public void call(final Subscriber<? super DataReadResult> subscriber) {

                try {
                    Fitness.HistoryApi.readData(client, request).setResultCallback(new ResultCallback<DataReadResult>() {
                        @Override
                        public void onResult(@NonNull DataReadResult dataReadResult) {
                            if (subscriber.isUnsubscribed()) return;
                            if (dataReadResult.getStatus().isSuccess()) {
                                Log.i(TAG, "Successfully read data!");
                                subscriber.onNext(dataReadResult);
                                if (!subscriber.isUnsubscribed()) {
                                    subscriber.onCompleted();
                                }
                            } else {
                                Log.i(TAG, "There was a problem reading data.");
                                subscriber.onError(new IllegalStateException("Problem reading data: " + dataReadResult.getStatus().toString()));
                            }

                        }
                    });
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });

    }
}
