package fitr.mobile.google;

import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import rx.Observable;

public interface FitnessHistoryHelper {

    String TAG = "FitnessHistoryHelper";

    Observable<DataReadResult> readData(DataReadRequest request);

}
