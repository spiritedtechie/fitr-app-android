package fitr.mobile.google;

import com.google.android.gms.fitness.data.DataType;

import rx.Observable;

public interface FitnessRecordingHelper {

    String TAG = "FitnessRecordingHelper";

    Observable<Void> subscribeIfNotExistingSubscription(DataType dataType);

    Observable<Void> subscribe(DataType dataType);

    Observable<Void> unsubscribe(DataType dataType);
}
