package fitr.mobile;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;

import fitr.common.logger.Log;
import fitr.common.logger.LogView;
import fitr.common.logger.LogWrapper;
import fitr.common.logger.MessageOnlyLogFilter;

import static com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks.*;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private GoogleApiClient mClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeLogging();

        if (!checkPermissions()) {
            requestPermissions();
        }

        final EditText et_name = (EditText) findViewById(R.id.et_name);
        Button btn_submit = (Button) findViewById(R.id.btn_submit);

        btn_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent acceptedIntent = new Intent(MainActivity.this, AcceptedActivity.class);
                acceptedIntent.putExtra("NAME", et_name.getText().toString());
                startActivity(acceptedIntent);
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        buildFitnessClient();
    }

    private void buildFitnessClient() {
        if (mClient == null && checkPermissions()) {
            mClient = new GoogleApiClient.Builder(this)
                    .addApi(Fitness.SENSORS_API)
                    .addScope(new Scope(Scopes.FITNESS_LOCATION_READ))
                    .addConnectionCallbacks(getConnectionCallbacks())
                    .enableAutoManage(this, 0, getOnConnectionFailedListener())
                    .build();
        }
    }

    @NonNull
    private GoogleApiClient.OnConnectionFailedListener getOnConnectionFailedListener() {
        return new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(ConnectionResult result) {
                Log.i(TAG, "Google Play services connection failed. Cause: " + result.toString());
                Snackbar.make(
                        MainActivity.this.findViewById(R.id.main_activity_view),
                        "Exception while connecting to Google Play services: " +
                                result.getErrorMessage(),
                        Snackbar.LENGTH_INDEFINITE).show();
            }
        };
    }

    @NonNull
    private GoogleApiClient.ConnectionCallbacks getConnectionCallbacks() {
        return new GoogleApiClient.ConnectionCallbacks() {
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

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }
}
