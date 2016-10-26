package fitr.mobile.config;

import javax.inject.Singleton;

import dagger.Component;
import fitr.mobile.ActivityRecordingFragment;
import fitr.mobile.DistanceReportsFragment;
import fitr.mobile.FitnessApplication;
import fitr.mobile.MainActivity;
import fitr.mobile.RegisterActivity;
import fitr.mobile.SignInActivity;

@Singleton
@Component(modules = {
        AppModule.class, AuthenticationModule.class,
        FitnessApiModule.class, DistanceModule.class, ActivityRecordingModule.class})
public interface FitnessComponent {

    void inject(FitnessApplication fitnessApplication);

    void inject(RegisterActivity registerActivity);

    void inject(SignInActivity signInActivity);

    void inject(MainActivity mainActivity);

    void inject(ActivityRecordingFragment fitnessActivityFragment);

    void inject(DistanceReportsFragment distanceReportsFragment);

}


