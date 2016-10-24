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
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static com.google.android.gms.fitness.data.DataType.AGGREGATE_DISTANCE_DELTA;
import static com.google.android.gms.fitness.data.DataType.TYPE_DISTANCE_DELTA;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class DistancePresenter extends BasePresenter<DistanceView> {

    private static final String TAG = "DistancePresenter";

    private FitnessHistoryHelper fhh;

    // Subscriptions
    private Subscription refreshSubscription;

    public DistancePresenter(FitnessHistoryHelper fhh) {
        this.fhh = fhh;
    }

    @Override
    public void attachView(DistanceView view) {
        super.attachView(view);
    }

    @Override
    public void detachView() {
        unsubscribe(refreshSubscription);
        super.detachView();
    }

    public void refreshData() {
        checkViewAttached();

        if (getView() != null) {
            getView().setRefreshing(true);
        }

        refreshSubscription = fhh.readData(buildDataReadRequest())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .map(dataReadResult -> {
                    List<Distance> data = extractData(dataReadResult);
                    BarData barData = mapToBarData(data);
                    return new DistanceData(data, barData);
                })
                .subscribe(
                        n -> {
                            getView().setDistanceChartData(n.getDistanceBarData());
                            getView().setDistanceTableData(n.getDistances());
                        },
                        e -> {
                            loggingErrorHandler().call(e);
                            getView().setRefreshing(false);
                        },
                        () -> getView().setRefreshing(false)
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

    private Func1<Throwable, Void> loggingErrorHandler() {
        return t -> {
            Log.e(TAG, t.getMessage(), t);
            return null;
        };
    }
}
