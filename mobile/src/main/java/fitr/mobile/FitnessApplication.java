package fitr.mobile;

import android.app.Application;

import fitr.mobile.config.AppModule;
import fitr.mobile.config.DaggerFitnessApiComponent;
import fitr.mobile.config.FitnessApiComponent;
import fitr.mobile.config.FitnessApiModule;

public class FitnessApplication extends Application {

    private FitnessApiComponent fitnessApiComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        fitnessApiComponent = DaggerFitnessApiComponent.builder()
                .appModule(new AppModule(this))
                .fitnessApiModule(new FitnessApiModule())
                .build();
    }

    public FitnessApiComponent getFitnessApiComponent() {
        return fitnessApiComponent;
    }

}
