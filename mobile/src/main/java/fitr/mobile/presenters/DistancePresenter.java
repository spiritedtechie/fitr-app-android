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
import fitr.mobile.models.AggregatedDistance;
import fitr.mobile.views.DistanceView;
import rx.Observable;
import rx.functions.Func1;

import static com.google.android.gms.fitness.data.DataType.AGGREGATE_DISTANCE_DELTA;
import static com.google.android.gms.fitness.data.DataType.TYPE_DISTANCE_DELTA;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class DistancePresenter {

    private static final String TAG = "DistancePresenter";

    private FitnessHistoryHelper fhh;

    public DistancePresenter(FitnessHistoryHelper fhh) {
        this.fhh = fhh;
    }

    public void refreshData(final DistanceView view) {
        this.buildBaseObservable(view).subscribe();
    }

    public void swipeRefreshData(final DistanceView view) {

        view.setRefreshing(true);

        this.buildBaseObservable(view)
                .map(new Func1<Void, Void>() {
                    @Override
                    public Void call(Void o) {
                        view.setRefreshing(false);
                        return null;
                    }
                })
                .onErrorReturn(new Func1<Throwable, Void>() {
                    @Override
                    public Void call(Throwable throwable) {
                        view.setRefreshing(false);
                        loggingErrorHandler().call(throwable);
                        return null;
                    }
                })
                .subscribe();
    }

    private Observable buildBaseObservable(final DistanceView view) {
        Func1<DataReadResult, Void> successAction = new Func1<DataReadResult, Void>() {
            @Override
            public Void call(DataReadResult dataReadResult) {
                List<AggregatedDistance> data = extractData(dataReadResult);
                BarData bd = mapToBarData(data);
                view.setDistanceChartData(bd);
                view.setDistanceTableData(data);
                return null;
            }
        };

        return fhh.readData(buildDataReadRequest())
                .map(successAction)
                .onErrorReturn(loggingErrorHandler());
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

    private List<AggregatedDistance> extractData(DataReadResult dataReadResult) {
        final List<AggregatedDistance> distanceAggregateData = new ArrayList<>();
        for (Bucket bucket : dataReadResult.getBuckets()) {
            DataSet dataSet = bucket.getDataSet(AGGREGATE_DISTANCE_DELTA);
            for (DataPoint dp : dataSet.getDataPoints()) {
                Value value = dp.getValue(Field.FIELD_DISTANCE);

                Date startDate = new Date(dp.getStartTime(MILLISECONDS));
                Date endDate = new Date(dp.getEndTime(MILLISECONDS));
                float distance = value.asFloat();

                distanceAggregateData.add(new AggregatedDistance(startDate, endDate, distance));
            }
        }
        return distanceAggregateData;
    }

    private BarData mapToBarData(List<AggregatedDistance> data) {
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

    private Func1<Throwable, Void> loggingErrorHandler() {
        return new Func1<Throwable, Void>() {
            @Override
            public Void call(Throwable t) {
                Log.e(TAG, t.getMessage(), t);
                return null;
            }
        };
    }
}
