package fitr.mobile;

import android.app.Application;

import fitr.mobile.config.ActivityRecordingModule;
import fitr.mobile.config.AppModule;
import fitr.mobile.config.DaggerFitnessComponent;
import fitr.mobile.config.DistanceModule;
import fitr.mobile.config.FitnessApiModule;
import fitr.mobile.config.FitnessComponent;

public class FitnessApplication extends Application {

    private FitnessComponent fitnessComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        fitnessComponent = DaggerFitnessComponent.builder()
                .appModule(new AppModule(this))
                .fitnessApiModule(new FitnessApiModule())
                .distanceModule(new DistanceModule())
                .activityRecordingModule(new ActivityRecordingModule())
                .build();
    }

    public FitnessComponent getFitnessComponent() {
        return fitnessComponent;
    }

}
