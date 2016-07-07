package fitr.mobile;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import fitr.mobile.google.FitnessClientManager;
import fitr.mobile.google.FitnessHistoryHelper;
import fitr.mobile.google.FitnessHistoryHelperImpl;
import fitr.mobile.google.FitnessRecordingHelper;
import fitr.mobile.google.FitnessRecordingHelperImpl;
import fitr.mobile.google.FitnessSessionHelper;
import fitr.mobile.google.FitnessSessionHelperImpl;
import fitr.mobile.models.AggregatedDistance;
import rx.functions.Func1;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static com.google.android.gms.fitness.data.DataType.AGGREGATE_DISTANCE_DELTA;
import static com.google.android.gms.fitness.data.DataType.TYPE_DISTANCE_DELTA;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    private static final String AUTH_PENDING = "auth_state_pending";

    private static final int REQUEST_OAUTH = 1;

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private static final String DATE_FORMAT_PATTERN_DEFAULT = "yyyy-MM-dd'T'HH:mm:ss";

    // Views
    private Spinner dropDownListActivityType;
    private Button btnStart;
    private Button btnStop;
    private Button btnRefresh;
    private BarChart barChart;
    private TableLayout table;

    // Fitness API helpers
    private FitnessClientManager fcm;
    private FitnessRecordingHelper frh;
    private FitnessSessionHelper fsh;
    private FitnessHistoryHelper fhh;

    private Session session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        fcm = new FitnessClientManager(this);

        // Get views
        dropDownListActivityType = (Spinner) findViewById(R.id.ddl_activity_type);
        btnStart = (Button) findViewById(R.id.btn_start);
        btnStop = (Button) findViewById(R.id.btn_stop);
        btnRefresh = (Button) findViewById(R.id.btn_refresh_chart);
        barChart = (BarChart) findViewById(R.id.chart);
        table = (TableLayout) findViewById(R.id.table);

        // Set up drop down list for activities
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, buildActivitiesList());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dropDownListActivityType.setAdapter(adapter);

        // Configure chart
        barChart.animateX(3000);
        barChart.animateY(3000);

        // Build client
        if (!isPermissionGranted()) {
            requestPermissions();
        } else {
            initialiseClient();
        }

        // Build Fitness API helpers
        frh = new FitnessRecordingHelperImpl(fcm.createClient());
        fsh = new FitnessSessionHelperImpl(fcm.createClient());
        fhh = new FitnessHistoryHelperImpl(fcm.createClient());

        // Button listeners
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                subscribe();
                startSession();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSession();
                unsubscribe();
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshHistoricalData();
            }
        });
    }

    @Override
    protected void onDestroy() {
        fcm.disconnect();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OAUTH) {
            Log.i(TAG, "onActivityResult: REQUEST_OAUTH");
            if (resultCode == Activity.RESULT_OK) {
                Log.i(TAG, "onActivityResult: client.connect()");
                connectClient();
            }
        } else {
            Log.i(TAG, "onActivityResult: request code: " + requestCode);
        }
    }

    private void initialiseClient() {
        fcm.createClient();
        connectClient();
    }

    private void connectClient() {
        fcm.connect()
                .map(new Func1() {
                    @Override
                    public Object call(Object o) {
                        refreshHistoricalData();
                        return null;
                    }
                })
                .onErrorReturn(loggingErrorHandler())
                .subscribe();
    }

    private void subscribe() {
        frh.subscribeIfNotExistingSubscription(TYPE_DISTANCE_DELTA)
                .onErrorReturn(loggingErrorHandler())
                .subscribe();
    }

    private void unsubscribe() {
        frh.unsubscribe(TYPE_DISTANCE_DELTA)
                .onErrorReturn(loggingErrorHandler())
                .subscribe();
    }

    private void startSession() {
        if (session == null) {
            session = buildSession();
            fsh.startSession(session)
                    .onErrorReturn(loggingErrorHandler())
                    .subscribe();
        }
    }

    private Session buildSession() {
        String selectedActivity = dropDownListActivityType.getSelectedItem().toString();
        Date currTime = new Date();
        String sessionName = selectedActivity + "-" + formatTime(currTime.getTime());

        return new Session.Builder()
                .setName(sessionName)
                .setIdentifier(UUID.randomUUID().toString())
                .setDescription("Session of " + selectedActivity)
                .setStartTime(currTime.getTime(), MILLISECONDS)
                .setActivity(selectedActivity)
                .build();
    }

    private void stopSession() {
        if (session == null || !session.isOngoing()) {
            return;
        }

        fsh.stopSession(session.getIdentifier())
                .firstOrDefault(null)
                .map(new Func1<Session, Void>() {
                    @Override
                    public Void call(Session session) {
                        notifySessionStopped(session);
                        return null;
                    }
                })
                .onErrorReturn(loggingErrorHandler())
                .subscribe();
    }

    private void notifySessionStopped(Session session) {
        if (session != null) {
            String toastMsg = "Session stopped." +
                    "\nIt started at " + formatTime(session.getStartTime(MILLISECONDS)) +
                    "\nIt ended at " + formatTime(session.getEndTime(MILLISECONDS));
            Toast.makeText(MainActivity.this, toastMsg, Toast.LENGTH_LONG).show();
        }
    }

    private void refreshHistoricalData() {
        Func1<DataReadResult, Void> action = new Func1<DataReadResult, Void>() {
            @Override
            public Void call(DataReadResult dataReadResult) {
                List<AggregatedDistance> data = extractData(dataReadResult);
                refreshDistanceBarChart(data);
                showTableContent(data);
                return null;
            }
        };

        fhh.readData(buildDataReadRequest())
                .map(action)
                .onErrorReturn(loggingErrorHandler())
                .subscribe();
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
            TableRow tr = new TableRow(this);
            tr.setPadding(0, 10, 0, 0);
            TextView c1 = new TextView(this);
            c1.setPadding(0, 0, 20, 0);
            c1.setText(formatTime(dataItem.getStartDate().getTime()));
            TextView c2 = new TextView(this);
            c2.setPadding(0, 0, 20, 0);
            c2.setText(formatTime(dataItem.getEndDate().getTime()));
            TextView c3 = new TextView(this);
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

    /**
     * Permissions handling
     */
    private boolean isPermissionGranted() {
        int permissionState = ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_FINE_LOCATION);

        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackBar(R.string.permission_rationale, R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    requestPermissionForLocation();
                }
            });
        } else {
            Log.i(TAG, "Requesting permission");
            requestPermissionForLocation();
        }
    }

    private void requestPermissionForLocation() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initialiseClient();
            } else {
                displayPermissionDeniedExplanation();
            }
        }
    }

    private void displayPermissionDeniedExplanation() {
        showSnackBar(R.string.permission_denied_explanation, R.string.settings, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                intent.setData(uri);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
    }

    private void showSnackBar(int message, int actionLabel, View.OnClickListener onClickListener) {
        Snackbar.make(
                findViewById(R.id.main_activity_view),
                message,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(actionLabel, onClickListener).show();
    }

    private String formatTime(long timeMillis) {
        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT_PATTERN_DEFAULT);
        return df.format(new Date(timeMillis));
    }

    private List<String> buildActivitiesList() {
        List<String> activities = new ArrayList<>();
        activities.add(FitnessActivities.RUNNING);
        activities.add(FitnessActivities.WALKING);
        activities.add(FitnessActivities.BIKING);
        return activities;
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