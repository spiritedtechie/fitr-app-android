package fitr.mobile.config;

import javax.inject.Singleton;

import dagger.Component;
import fitr.mobile.ActivityRecordingFragment;
import fitr.mobile.DistanceReportsFragment;
import fitr.mobile.FitnessApplication;
import fitr.mobile.MainActivity;

@Singleton
@Component(modules = {
        AppModule.class, FitnessApiModule.class, DistanceModule.class, ActivityRecordingModule.class})
public interface FitnessComponent {

    void inject(FitnessApplication fitnessApplication);

    void inject(MainActivity mainActivity);

    void inject(ActivityRecordingFragment fitnessActivityFragment);

    void inject(DistanceReportsFragment distanceReportsFragment);

}


