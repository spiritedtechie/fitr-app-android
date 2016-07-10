package fitr.mobile.config;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import fitr.mobile.google.FitnessRecordingHelper;
import fitr.mobile.google.FitnessSessionHelper;
import fitr.mobile.presenters.ActivityRecordingPresenter;

@Module
public class ActivityRecordingModule {

    @Provides
    @Singleton
    ActivityRecordingPresenter providesActivityRecordingPresenter(FitnessRecordingHelper frh, FitnessSessionHelper fsh) {
        return new ActivityRecordingPresenter(frh, fsh);
    }
}
