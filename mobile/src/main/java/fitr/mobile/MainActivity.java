package fitr.mobile;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;

import fitr.mobile.google.FitnessClientManager;
import rx.functions.Func1;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainActivity extends AppCompatActivity implements
        ActivityRecordingFragment.Injector,
        DistanceReportsFragment.Injector {

    public static final String TAG = "Main";

    private static final int REQUEST_OAUTH = 1;

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    // Views
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navViewMain;
    private ActionBarDrawerToggle drawerToggle;

    @Inject
    FitnessClientManager fcm;

    @Override
    public void inject(ActivityRecordingFragment frag) {
        ((FitnessApplication) getApplication()).getFitnessApiComponent().inject(frag);
    }

    @Override
    public void inject(DistanceReportsFragment frag) {
        ((FitnessApplication) getApplication()).getFitnessApiComponent().inject(frag);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Inject
        ((FitnessApplication) getApplication()).getFitnessApiComponent().inject(this);

        // Set a Toolbar to replace the ActionBar.
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get views
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navViewMain = (NavigationView) findViewById(R.id.nv_main);

        initialiseNavigationDrawer();

        // Build client
        if (!isPermissionGranted()) {
            requestPermissions();
        } else {
            initialiseClient();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    protected void onDestroy() {
        fcm.disconnect();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START);
            return true;
        }

        return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OAUTH) {
            Log.i(TAG, "onActivityResult: REQUEST_OAUTH");
            if (resultCode == Activity.RESULT_OK) {
                Log.i(TAG, "onActivityResult: client.connect()");
                initialiseClient();
            }
        } else {
            Log.i(TAG, "onActivityResult: request code: " + requestCode);
        }
    }

    private void initialiseNavigationDrawer() {
        navViewMain.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        navigationItemSelected(menuItem);
                        return true;
                    }
                });
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
    }

    public void navigationItemSelected(MenuItem menuItem) {
        getSupportFragmentManager().beginTransaction().replace(R.id.fl_content, getFragment(menuItem)).commit();
        menuItem.setChecked(true);
        setTitle(menuItem.getTitle());
        drawerLayout.closeDrawers();
    }

    private Fragment getFragment(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.nav_first_fragment:
                return new WorkoutTabsFragment();
            case R.id.nav_second_fragment:
                return new DistanceReportsFragment();
            default:
                return new WorkoutTabsFragment();
        }
    }

    private void initialiseClient() {
        fcm.getClient();
        connectClient();
    }

    private void connectClient() {
        fcm.connect(this)
                .onErrorReturn(loggingErrorHandler())
                .subscribe();
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
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
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
                findViewById(R.id.nv_main),
                message,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(actionLabel, onClickListener).show();
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