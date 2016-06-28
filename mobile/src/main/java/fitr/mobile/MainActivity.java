package fitr.mobile;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
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
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.fitness.result.ListSubscriptionsResult;
import com.google.android.gms.fitness.result.SessionStopResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static com.google.android.gms.fitness.FitnessStatusCodes.*;
import static com.google.android.gms.fitness.data.DataType.*;
import static java.util.concurrent.TimeUnit.*;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "SensorActivity";

    private static final String AUTH_PENDING = "auth_state_pending";

    private static final int REQUEST_OAUTH = 1;

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    // Views
    private Spinner dropDownListActivityType;

    private Button btnStart;
    private Button btnStop;
    private Button btnRefresh;
    private BarChart barChart;
    private TableLayout table;

    private boolean authInProgress = false;

    private GoogleApiClient mClient = null;
    private Session session;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }

        // Get views
        btnStart = (Button) findViewById(R.id.btn_start);
        btnStop = (Button) findViewById(R.id.btn_stop);
        btnRefresh = (Button) findViewById(R.id.btn_refresh_chart);
        dropDownListActivityType = (Spinner) findViewById(R.id.ddl_activity_type);
        barChart = (BarChart) findViewById(R.id.chart);
        table = (TableLayout) findViewById(R.id.table);

        // Set up drop down list for activities
        List<String> activities = new ArrayList<>();
        activities.add(FitnessActivities.RUNNING);
        activities.add(FitnessActivities.WALKING);
        activities.add(FitnessActivities.BIKING);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, activities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dropDownListActivityType.setAdapter(adapter);

        // Configure chart
        barChart.animateX(3000);
        barChart.animateY(3000);

        // Build client
        if (!isPermissionGranted()) {
            requestPermissions();
        } else {
            buildFitnessClient();
        }

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
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (client != null) {
            client.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        disconnectClient();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(AUTH_PENDING, authInProgress);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OAUTH) {
            Log.i(TAG, "onActivityResult: REQUEST_OAUTH");
            authInProgress = false;
            if (resultCode == Activity.RESULT_OK) {
                if (mClient != null && !mClient.isConnecting() && !mClient.isConnected()) {
                    Log.i(TAG, "onActivityResult: client.connect()");
                    mClient.connect();
                }
            }
        } else {
            Log.i(TAG, "onActivityResult: request code: " + requestCode);
        }
    }

    private void disconnectClient() {
        Log.i(TAG, "Disconnecting client");
        if (mClient != null) mClient.disconnect();
    }

    private void buildFitnessClient() {
        Log.i(TAG, "buildFitnessClient");
        if (mClient == null) {
            mClient = new GoogleApiClient.Builder(this)
                    .addApiIfAvailable(Fitness.RECORDING_API)
                    .addApiIfAvailable(Fitness.SESSIONS_API)
                    .addApiIfAvailable(Fitness.HISTORY_API)
                    .addConnectionCallbacks(onConnection())
                    .enableAutoManage(this, 0, onConnectionFailed())
                    .build();
        }
    }

    @NonNull
    private OnConnectionFailedListener onConnectionFailed() {
        return new OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(ConnectionResult result) {
                if (!authInProgress) {
                    try {
                        authInProgress = true;
                        result.startResolutionForResult(MainActivity.this, REQUEST_OAUTH);
                    } catch (IntentSender.SendIntentException e) {

                    }
                } else {
                    Log.e("GoogleFit", "authInProgress");
                }
            }
        };
    }

    @NonNull
    private ConnectionCallbacks onConnection() {
        return new ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
                Log.i(TAG, "Client connected");
                subscribe();
            }

            @Override
            public void onConnectionSuspended(int i) {
                if (i == CAUSE_NETWORK_LOST) {
                    Log.i(TAG, "Client connection lost.  Cause: Network Lost.");
                } else if (i == CAUSE_SERVICE_DISCONNECTED) {
                    Log.i(TAG, "Client connection lost.  Reason: Service Disconnected");
                }
            }
        };
    }

    private void subscribe() {

        if (mClient != null) {
            Fitness.RecordingApi.listSubscriptions(mClient, TYPE_DISTANCE_DELTA)
                    .setResultCallback(new ResultCallback<ListSubscriptionsResult>() {
                        @Override
                        public void onResult(ListSubscriptionsResult listSubscriptionsResult) {
                            if (listSubscriptionsResult.getSubscriptions().isEmpty()) {
                                _subscribe();
                            }
                        }
                    });
        }
    }

    public void _subscribe() {

        if (mClient != null && mClient.isConnected()) {
            Fitness.RecordingApi.subscribe(mClient, TYPE_DISTANCE_DELTA)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            if (status.isSuccess()) {
                                if (status.getStatusCode() == SUCCESS_ALREADY_SUBSCRIBED) {
                                    Log.i(TAG, "Existing subscription for activity detected.");
                                } else {
                                    Log.i(TAG, "Successfully subscribed!");
                                    refreshHistoricalData();
                                }
                            } else {
                                Log.i(TAG, "There was a problem subscribing.");
                            }
                        }
                    });
        }
    }

    private void unsubscribe() {

        if (mClient != null && mClient.isConnected()) {
            Fitness.RecordingApi.unsubscribe(mClient, TYPE_DISTANCE_DELTA)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            if (status.isSuccess()) {
                                Log.i(TAG, "Successfully unsubscribed!");
                            } else {
                                Log.i(TAG, "There was a problem unsubscribing.");
                            }
                        }
                    });
        }
    }

    private void startSession() {

        if (mClient != null && mClient.isConnected() && session == null) {
            String selectedActivity = dropDownListActivityType.getSelectedItem().toString();
            Date currTime = new Date();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            String sessionName = selectedActivity + "-" + df.format(currTime);

            session = new Session.Builder()
                    .setName(sessionName)
                    .setIdentifier(UUID.randomUUID().toString())
                    .setDescription("Session of " + selectedActivity)
                    .setStartTime(currTime.getTime(), MILLISECONDS)
                    .setActivity(selectedActivity)
                    .build();

            Fitness.SessionsApi.startSession(mClient, session).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    if (status.isSuccess()) {
                        Log.i(TAG, "Successfully started session " + session.getIdentifier());
                    } else {
                        Log.i(TAG, "There was a problem starting session.");
                    }
                }
            });
        }
    }

    private void stopSession() {

        if (mClient != null && mClient.isConnected() && session != null && session.isOngoing()) {
            Fitness.SessionsApi.stopSession(mClient, session.getIdentifier()).setResultCallback(new ResultCallback<SessionStopResult>() {
                @Override
                public void onResult(@NonNull SessionStopResult sessionStopResult) {
                    if (sessionStopResult.getStatus().isSuccess()) {
                        Log.i(TAG, "Successfully stopped session " + session.getIdentifier());
                        notifySessionStopped(sessionStopResult);
                        MainActivity.this.session = null;
                    } else {
                        Log.i(TAG, "There was a problem stopping session.");
                    }
                }
            });
        }
    }

    private void notifySessionStopped(@NonNull SessionStopResult sessionStopResult) {
        if (!sessionStopResult.getSessions().isEmpty()) {
            Session session = sessionStopResult.getSessions().get(0);
            String toastMsg = "Session stopped." +
                    "\nIt started at " + formatTime(session.getStartTime(MILLISECONDS)) +
                    "\nIt ended at " + formatTime(session.getEndTime(MILLISECONDS));
            Toast.makeText(MainActivity.this, toastMsg, Toast.LENGTH_LONG).show();
        }
    }

    private void refreshHistoricalData() {

        if (mClient != null && mClient.isConnected() && barChart != null) {

            Calendar cal = Calendar.getInstance();
            Date now = new Date();
            cal.setTime(now);
            long endTime = cal.getTimeInMillis();
            cal.add(Calendar.WEEK_OF_YEAR, -2);
            long startTime = cal.getTimeInMillis();

            DataReadRequest readRequest = new DataReadRequest.Builder()
                    .aggregate(TYPE_DISTANCE_DELTA, AGGREGATE_DISTANCE_DELTA)
                    .bucketByTime(1, DAYS)
                    .setTimeRange(startTime, endTime, MILLISECONDS)
                    .build();

            Fitness.HistoryApi.readData(mClient, readRequest).setResultCallback(new ResultCallback<DataReadResult>() {
                @Override
                public void onResult(@NonNull DataReadResult dataReadResult) {
                    List<DistanceAggregate> distanceAggregateData = extractData(dataReadResult);
                    updateBarChart(distanceAggregateData);
                    createTable(distanceAggregateData);
                }
            });
        }
    }

    private void createTable(List<DistanceAggregate> data) {

        for (DistanceAggregate dataItem : data) {
            TableRow tr = new TableRow(this);
            tr.setPadding(0, 10, 0, 0);
            TextView c1 = new TextView(this);
            c1.setPadding(0,0,20,0);
            c1.setText(formatTime(dataItem.getStartDate().getTime()));
            TextView c2 = new TextView(this);
            c2.setPadding(0,0,20,0);
            c2.setText(formatTime(dataItem.getEndDate().getTime()));
            TextView c3 = new TextView(this);
            c3.setPadding(0,0,20,0);
            c3.setText(String.valueOf(dataItem.getDistanceInMeters()));
            tr.addView(c1);
            tr.addView(c2);
            tr.addView(c3);
            table.addView(tr);
        }
    }

    @NonNull
    private List<DistanceAggregate> extractData(@NonNull DataReadResult dataReadResult) {
        List<DistanceAggregate> distanceAggregateData = new ArrayList<>();

        for (Bucket bucket : dataReadResult.getBuckets()) {
            DataSet dataSet = bucket.getDataSet(AGGREGATE_DISTANCE_DELTA);
            for (DataPoint dp : dataSet.getDataPoints()) {
                Value value = dp.getValue(Field.FIELD_DISTANCE);

                Date startDate = new Date(dp.getStartTime(MILLISECONDS));
                Date endDate = new Date(dp.getEndTime(MILLISECONDS));
                float distance = value.asFloat();

                distanceAggregateData.add(new DistanceAggregate(startDate, endDate, distance));
            }
        }
        return distanceAggregateData;
    }

    private void updateBarChart(List<DistanceAggregate> data) {
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
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return df.format(new Date(timeMillis));
    }

    private boolean isPermissionGranted() {
        int permissionState1 = ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION);
        return permissionState1 == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            Snackbar.make(
                    findViewById(R.id.main_activity_view),
                    R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            requestPermissionForLocation();
                        }
                    })
                    .show();
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            requestPermissionForLocation();
        }
    }

    private void requestPermissionForLocation() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{ACCESS_FINE_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                buildFitnessClient();
                mClient.connect();
            } else {
                displayPermissionDeniedExplanation();
            }
        }
    }

    /**
     * // Permission denied.
     * <p/>
     * // In this Activity we've chosen to notify the user that they
     * // have rejected a core permission for the app since it makes the Activity useless.
     * // We're communicating this message in a Snackbar since this is a sample app, but
     * // core permissions would typically be best requested during a welcome-screen flow.
     * <p/>
     * // Additionally, it is important to remember that a permission might have been
     * // rejected without asking the user for permission (device policy or "Never ask
     * // again" prompts). Therefore, a user interface affordance is typically implemented
     * // when permissions are denied. Otherwise, your app could appear unresponsive to
     * // touches or interactions which have required permissions.
     */
    private void displayPermissionDeniedExplanation() {

        Snackbar.make(
                findViewById(R.id.main_activity_view),
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.settings, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Build intent that displays the App settings screen.
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                        intent.setData(uri);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                })
                .show();
    }
}