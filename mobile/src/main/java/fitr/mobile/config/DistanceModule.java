package fitr.mobile.config;

import dagger.Module;
import dagger.Provides;
import fitr.mobile.google.FitnessHistoryHelper;
import fitr.mobile.presenters.DistancePresenter;

@Module
public class DistanceModule {

    @Provides
    DistancePresenter providesDistancePresenter(FitnessHistoryHelper fhh) {
        return new DistancePresenter(fhh);
    }
}
