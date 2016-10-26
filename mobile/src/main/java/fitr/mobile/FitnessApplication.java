package fitr.mobile;

import android.app.Application;
import android.util.Log;

import javax.inject.Inject;

import fitr.mobile.config.ActivityRecordingModule;
import fitr.mobile.config.AppModule;
import fitr.mobile.config.DaggerFitnessComponent;
import fitr.mobile.config.DistanceModule;
import fitr.mobile.config.FitnessApiModule;
import fitr.mobile.config.FitnessComponent;
import fitr.mobile.google.FitnessClientManager;

public class FitnessApplication extends Application {

    private static final String TAG = FitnessApplication.class.getSimpleName();

    private FitnessComponent fitnessComponent;

    @Inject
    FitnessClientManager fcm;

    @Override
    public void onCreate() {
        super.onCreate();

        fitnessComponent = DaggerFitnessComponent.builder()
                .appModule(new AppModule(this))
                .fitnessApiModule(new FitnessApiModule())
                .distanceModule(new DistanceModule())
                .activityRecordingModule(new ActivityRecordingModule())
                .build();

        fitnessComponent.inject(this);

        fcm.connect(null)
                .subscribe(
                        n -> {},
                        t -> Log.e(TAG, "Failed to connect fitness client", t));
    }

    @Override
    public void onTerminate() {
        fcm.disconnect();

        super.onTerminate();
    }

    public FitnessComponent getFitnessComponent() {
        return fitnessComponent;
    }

}
