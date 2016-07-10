package fitr.mobile;

import android.app.Application;

import fitr.mobile.config.AppModule;
import fitr.mobile.config.DaggerFitnessApiComponent;
import fitr.mobile.config.FitnessApiComponent2;
import fitr.mobile.config.FitnessApiModule;

public class FitnessApplication extends Application {

    private FitnessApiComponent2 fitnessApiComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        fitnessApiComponent = DaggerFitnessApiComponent.builder()
                .appModule(new AppModule(this))
                .fitnessApiModule(new FitnessApiModule())
                .build();
    }

    public FitnessApiComponent2 getFitnessApiComponent() {
        return fitnessApiComponent;
    }

}
