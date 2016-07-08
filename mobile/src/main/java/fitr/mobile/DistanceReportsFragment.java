package fitr.mobile;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import fitr.mobile.google.FitnessHistoryHelper;
import fitr.mobile.models.AggregatedDistance;
import rx.Observable;
import rx.functions.Func1;

import static com.google.android.gms.fitness.data.DataType.AGGREGATE_DISTANCE_DELTA;
import static com.google.android.gms.fitness.data.DataType.TYPE_DISTANCE_DELTA;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class DistanceReportsFragment extends Fragment {

    private static final String TAG = "DistanceReports";

    private static final String DATE_FORMAT_PATTERN_DEFAULT = "yyyy-MM-dd'T'HH:mm:ss";

    private BarChart barChart;
    private TableLayout table;
    private SwipeRefreshLayout swipeLayout;

    @Inject
    FitnessHistoryHelper fhh;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ((Injector) getActivity()).inject(this);

        View view = inflater.inflate(R.layout.fragment_report_distance, container, false);

        barChart = (BarChart) view.findViewById(R.id.bc_distance_chart);
        table = (TableLayout) view.findViewById(R.id.tl_distance_table);
        swipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.srl_swipe_container);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeLayout.setRefreshing(true);
                refreshHistoricalData()
                        .map(new Func1<Void, Void>() {
                            @Override
                            public Void call(Void o) {
                                swipeLayout.setRefreshing(false);
                                return null;
                            }
                        })
                        .onErrorReturn(new Func1<Throwable, Void>() {
                            @Override
                            public Void call(Throwable throwable) {
                                swipeLayout.setRefreshing(false);
                                loggingErrorHandler().call(throwable);
                                return null;
                            }
                        })
                        .subscribe();
            }
        });

        // Configure chart
        barChart.animateX(3000);
        barChart.animateY(3000);

        // Refresh chart
        refreshHistoricalData().onErrorReturn(loggingErrorHandler()).subscribe();

        return view;
    }

    private Observable refreshHistoricalData() {
        Func1<DataReadResult, Void> action = new Func1<DataReadResult, Void>() {
            @Override
            public Void call(DataReadResult dataReadResult) {
                List<AggregatedDistance> data = extractData(dataReadResult);
                refreshDistanceBarChart(data);
                showTableContent(data);
                return null;
            }
        };

        return fhh.readData(buildDataReadRequest())
                .map(action)
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

    private void showTableContent(List<AggregatedDistance> data) {
        table.removeAllViews();
        for (AggregatedDistance dataItem : data) {
            TableRow tr = new TableRow(getContext());
            tr.setPadding(0, 10, 0, 0);
            TextView c1 = new TextView(getContext());
            c1.setPadding(0, 0, 20, 0);
            c1.setText(formatTime(dataItem.getStartDate().getTime()));
            TextView c2 = new TextView(getContext());
            c2.setPadding(0, 0, 20, 0);
            c2.setText(formatTime(dataItem.getEndDate().getTime()));
            TextView c3 = new TextView(getContext());
            c3.setPadding(0, 0, 20, 0);
            c3.setText(String.valueOf(dataItem.getDistanceInMeters()));
            tr.addView(c1);
            tr.addView(c2);
            tr.addView(c3);
            table.addView(tr);
        }
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

    private void refreshDistanceBarChart(List<AggregatedDistance> data) {
        if (barChart == null) return;
        barChart.invalidate();

        List<String> xValues = new ArrayList<>();
        List<BarEntry> barEntries = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            BarEntry be = new BarEntry(data.get(i).getDistanceInMeters(), i);
            barEntries.add(be);
            xValues.add(String.valueOf(i));
        }
        BarDataSet bds = new BarDataSet(barEntries, "Total distance (m)");
        BarData bd = new BarData(xValues, bds);

        barChart.setData(bd);
        barChart.notifyDataSetChanged();
    }

    private String formatTime(long timeMillis) {
        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT_PATTERN_DEFAULT);
        return df.format(new Date(timeMillis));
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

    interface Injector {
        void inject(DistanceReportsFragment frag);
    }

}
