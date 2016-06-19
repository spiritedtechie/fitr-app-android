package fitr.mobile;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import static android.Manifest.permission.*;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DataSourcesResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import fitr.common.logger.Log;
import fitr.common.logger.LogView;
import fitr.common.logger.LogWrapper;
import fitr.common.logger.MessageOnlyLogFilter;

import static com.google.android.gms.fitness.data.DataType.TYPE_STEP_COUNT_CUMULATIVE;


/**
 * This sample demonstrates how to use the Sensors API of the Google Fit platform to find
 * available data sources and to register/unregister listeners to those sources. It also
 * demonstrates how to authenticate a user with Google Play Services.
 */
public class MainActivity extends AppCompatActivity {

    public static final String TAG = "BasicSensorsApi";

    private static final String AUTH_PENDING = "auth_state_pending";

    private static final int REQUEST_OAUTH = 1;

    private GoogleApiClient mClient = null;

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    // Need to hold a reference to this listener, as it's passed into the "unregister"
    // method in order to stop all sensors from sending data to this listener.
    private boolean authInProgress = false;
    private List<OnDataPointListener> mListeners;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Put application specific code here.

        setContentView(R.layout.activity_main);
        // This method sets up our custom logger, which will print all log messages to the device
        // screen, as well as to adb logcat.

        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }

        if (!isPermissionGranted()) {
            requestPermissions();
        } else {
            buildFitnessClient();
        }


        initializeLogging();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // This ensures that if the user denies the permissions then uses Settings to re-enable
        // them, the app will start working.
    }

    @Override
    protected void onStart() {
        super.onStart();
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
    protected void onStop() {
        super.onStop();

        unregisterFitnessDataListener();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(AUTH_PENDING, authInProgress);
    }

    /**
     * Build a {@link GoogleApiClient} that will authenticate the user and allow the application
     * to connect to Fitness APIs. The scopes included should match the scopes your app needs
     * (see documentation for details). Authentication will occasionally fail intentionally,
     * and in those cases, there will be a known resolution, which the OnConnectionFailedListener()
     * can address. Examples of this include the user never having signed in before, or having
     * multiple accounts on the device and needing to specify which account to use, etc.
     */
    private void buildFitnessClient() {

        Log.i(TAG, "buildFitnessClient");
        if (mClient == null) {
            mClient = new GoogleApiClient.Builder(this)
                    .addApi(Fitness.SENSORS_API)
                    .addScope(Fitness.SCOPE_ACTIVITY_READ_WRITE)
                    .addScope(Fitness.SCOPE_LOCATION_READ_WRITE)
                    .addScope(Fitness.SCOPE_NUTRITION_READ_WRITE)
                    .addConnectionCallbacks(onConnection())
                    .enableAutoManage(this, 0, onConnectionFailed())
                    .build();
        }
    }

    @NonNull
    private GoogleApiClient.OnConnectionFailedListener onConnectionFailed() {
        return new GoogleApiClient.OnConnectionFailedListener() {
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
                Log.i(TAG, "Connected!!!");
                // Now you can make calls to the Fitness APIs.
                findFitnessDataSources();
            }

            @Override
            public void onConnectionSuspended(int i) {
                // If your connection to the sensor gets lost at some point,
                // you'll be able to determine the reason and react to it here.
                if (i == CAUSE_NETWORK_LOST) {
                    Log.i(TAG, "Connection lost.  Cause: Network Lost.");
                } else if (i == CAUSE_SERVICE_DISCONNECTED) {
                    Log.i(TAG, "Connection lost.  Reason: Service Disconnected");
                }
            }
        };
    }

    /**
     * Find available data sources and attempt to register on a specific {@link DataType}.
     * If the application cares about a data type but doesn't care about the source of the data,
     * this can be skipped entirely, instead calling
     * {@link com.google.android.gms.fitness.SensorsApi
     * #register(GoogleApiClient, SensorRequest, DataSourceListener)},
     * where the {@link SensorRequest} contains the desired data type.
     */
    private void findFitnessDataSources() {
        // Note: Fitness.SensorsApi.findDataSources() requires the ACCESS_FINE_LOCATION permission.
        Fitness.SensorsApi.findDataSources(mClient, new DataSourcesRequest.Builder()
                // At least one datatype must be specified.
                .setDataTypes(DataType.TYPE_LOCATION_SAMPLE,
                        DataType.TYPE_STEP_COUNT_DELTA,
                        DataType.TYPE_DISTANCE_DELTA)
                // Can specify whether data type is raw or derived.
                .setDataSourceTypes(DataSource.TYPE_RAW, DataSource.TYPE_DERIVED)
                .build())
                .setResultCallback(dataSourceResultCallback());
    }

    @NonNull
    private ResultCallback<DataSourcesResult> dataSourceResultCallback() {
        return new ResultCallback<DataSourcesResult>() {
            @Override
            public void onResult(DataSourcesResult dataSourcesResult) {
                Log.i(TAG, "Result: " + dataSourcesResult.getStatus().toString());

                mListeners = new ArrayList<>();

                for (DataSource dataSource : dataSourcesResult.getDataSources()) {
                    Log.i(TAG, "Data source found: " + dataSource.toString());
                    Log.i(TAG, "*** Data Source type: " + dataSource.getDataType().getName());
                    Log.i(TAG, "Data Source device: " + dataSource.getDevice());


                    //Let's register a listener to receive Activity data!
                    final DataType dataType = dataSource.getDataType();
                    if ((dataType.equals(DataType.TYPE_LOCATION_SAMPLE) ||
                            dataType.equals(DataType.TYPE_STEP_COUNT_DELTA) ||
                            dataType.equals(DataType.TYPE_DISTANCE_DELTA))) {
                        registerFitnessDataListener(dataSource, dataType);
                    }
                }
            }
        };
    }

    /**
     * Register a listener with the Sensors API for the provided {@link DataSource} and
     * {@link DataType} combo.
     */
    private void registerFitnessDataListener(DataSource dataSource, DataType dataType) {

        Log.i(TAG, "Registering for datasource: " + dataSource + ", datatype: " + dataType.getName());

        OnDataPointListener listener = new OnDataPointListener() {
            @Override
            public void onDataPoint(DataPoint dataPoint) {
                for (Field field : dataPoint.getDataType().getFields()) {
                    Value val = dataPoint.getValue(field);
                    Log.i(TAG, "Detected DataPoint field: " + field.getName());
                    Log.i(TAG, "Detected DataPoint value: " + val);
                }
            }
        };

        mListeners.add(listener);

        SensorRequest request = new SensorRequest.Builder()
                .setDataSource(dataSource) // Optional but recommended for custom data sets.
                .setDataType(dataType) // Can't be omitted.
                .setSamplingRate(3, TimeUnit.SECONDS)
                .build();

        Fitness.SensorsApi.add(
                mClient,
                request,
                listener)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Listener registered!");
                        } else {
                            Log.i(TAG, "Listener not registered.");
                        }
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OAUTH) {
            Log.i(TAG, "onActivityResult: REQUEST_OAUTH");
            authInProgress = false;
            if (resultCode == Activity.RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mClient.isConnecting() && !mClient.isConnected()) {
                    Log.i(TAG, "onActivityResult: client.connect()");
                    mClient.connect();
                }
            }
        }
    }

    /**
     * Unregister the listener with the Sensors API.
     */
    private void unregisterFitnessDataListener() {
        if (mListeners ==  null || mListeners.isEmpty()) {
            // This code only activates one listener at a time.  If there's no listener, there's
            // nothing to unregister.
            return;
        }

        // Waiting isn't actually necessary as the unregister call will complete regardless,
        // even if called from within onStop, but a callback can still be added in order to
        // inspect the results.

        for (OnDataPointListener listener : mListeners) {

            if (mClient != null && mClient.isConnected()) {

                Fitness.SensorsApi.remove(
                        mClient,
                        listener)
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status status) {
                                if (status.isSuccess()) {
                                    Log.i(TAG, "Listener was removed!");
                                } else {
                                    Log.i(TAG, "Listener was not removed.");
                                }
                            }
                        });
            }
        }
    }


    /**
     * Initialize a custom log class that outputs both to in-app targets and logcat.
     */
    private void initializeLogging() {
        // Wraps Android's native log framework.
        LogWrapper logWrapper = new LogWrapper();
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        Log.setLogNode(logWrapper);
        // Filter strips out everything except the message text.
        MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
        logWrapper.setNext(msgFilter);
        // On screen logging via a customized TextView.
        LogView logView = (LogView) findViewById(R.id.sample_logview);

        // Fixing this lint errors adds logic without benefit.
        //noinspection AndroidLintDeprecation
        logView.setTextAppearance(this, R.style.Log);

        logView.setBackgroundColor(Color.WHITE);
        msgFilter.setNext(logView);
        Log.i(TAG, "Ready");
    }

    /**
     * Return the current state of the permissions needed.
     */
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
                new String[]{ACCESS_FINE_LOCATION, READ_CONTACTS},
                REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    /**
     * Callback received when a permissions request has been completed.
     */
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