package fitr.mobile.config;

import javax.inject.Singleton;

import dagger.Component;
import fitr.mobile.ActivityRecordingFragment;
import fitr.mobile.DistanceReportsFragment;
import fitr.mobile.MainActivity;

@Singleton
@Component(modules = {AppModule.class, FitnessApiModule.class, DistanceModule.class})
public interface FitnessComponent {

    void inject(MainActivity mainActivity);

    void inject(ActivityRecordingFragment fitnessActivityFragment);

    void inject(DistanceReportsFragment distanceReportsFragment);
}
