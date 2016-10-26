package fitr.mobile.presenters;

import android.util.Log;

import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import fitr.mobile.google.FitnessHistoryHelper;
import fitr.mobile.models.Distance;
import fitr.mobile.models.DistanceData;
import fitr.mobile.views.DistanceView;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static com.google.android.gms.fitness.data.DataType.AGGREGATE_DISTANCE_DELTA;
import static com.google.android.gms.fitness.data.DataType.TYPE_DISTANCE_DELTA;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class DistancePresenter extends BasePresenter<DistanceView> {

    private static final String TAG = DistancePresenter.class.getSimpleName();

    private FitnessHistoryHelper fhh;

    private DistanceData cachedDistanceData;

    public DistancePresenter(FitnessHistoryHelper fhh) {
        this.fhh = fhh;
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();

        if (cachedDistanceData != null) {
            refreshViewDistanceData(cachedDistanceData);
        } else {
            refreshData();
        }
    }

    public void refreshData() {
        onView(v -> v.setRefreshing(true));

        fhh.readData(buildDataReadRequest())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .map(dataReadResult -> {
                    List<Distance> data = extractData(dataReadResult);
                    BarData barData = mapToBarData(data);
                    return new DistanceData(data, barData);
                })
                .map(data -> {
                    cachedDistanceData = data;
                    return data;
                })
                .subscribe(
                        n -> refreshViewDistanceData(n),
                        e -> {
                            loggingErrorHandler().call(e);
                            onView(v -> v.setRefreshing(false));
                        },
                        () -> onView(v -> v.setRefreshing(false))
                );
    }

    private DataReadRequest buildDataReadRequest() {
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.WEEK_OF_YEAR, -2);
        long startTime = cal.getTimeInMillis();

        return new DataReadRequest.Builder()
                .aggregate(TYPE_DISTANCE_DELTA, AGGREGATE_DISTANCE_DELTA)
                .bucketByTime(1, DAYS)
                .setTimeRange(startTime, endTime, MILLISECONDS)
                .build();
    }

    private List<Distance> extractData(DataReadResult dataReadResult) {
        final List<Distance> distanceAggregateData = new ArrayList<>();
        for (Bucket bucket : dataReadResult.getBuckets()) {
            DataSet dataSet = bucket.getDataSet(AGGREGATE_DISTANCE_DELTA);
            for (DataPoint dp : dataSet.getDataPoints()) {
                Value value = dp.getValue(Field.FIELD_DISTANCE);

                Date startDate = new Date(dp.getStartTime(MILLISECONDS));
                Date endDate = new Date(dp.getEndTime(MILLISECONDS));
                float distance = value.asFloat();

                distanceAggregateData.add(new Distance(startDate, endDate, distance));
            }
        }
        return distanceAggregateData;
    }

    private BarData mapToBarData(List<Distance> data) {
        List<String> xValues = new ArrayList<>();
        List<BarEntry> barEntries = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            BarEntry be = new BarEntry(data.get(i).getDistanceInMeters(), i);
            barEntries.add(be);
            xValues.add(String.valueOf(i));
        }
        BarDataSet bds = new BarDataSet(barEntries, "Total distance (m)");
        return new BarData(xValues, bds);
    }

    private void refreshViewDistanceData(DistanceData data) {
        onView(v -> {
            v.setDistanceChartData(data.getDistanceBarData());
            v.setDistanceTableData(data.getDistances());
        });
    }

    private Func1<Throwable, Void> loggingErrorHandler() {
        return t -> {
            Log.e(TAG, t.getMessage(), t);
            return null;
        };
    }
}
