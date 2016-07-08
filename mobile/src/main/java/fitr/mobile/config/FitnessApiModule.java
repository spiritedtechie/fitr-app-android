package fitr.mobile.config;

import android.app.Application;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import fitr.mobile.google.FitnessClientManager;
import fitr.mobile.google.FitnessHistoryHelper;
import fitr.mobile.google.FitnessHistoryHelperImpl;
import fitr.mobile.google.FitnessRecordingHelper;
import fitr.mobile.google.FitnessRecordingHelperImpl;
import fitr.mobile.google.FitnessSessionHelper;
import fitr.mobile.google.FitnessSessionHelperImpl;

@Module
public class FitnessApiModule {

    @Provides
    @Singleton
    FitnessClientManager providesFitnessClient(Application application) {
        return new FitnessClientManager(application);
    }

    @Provides
    @Singleton
    FitnessRecordingHelper providesFitnessRecordingHelper(FitnessClientManager fitnessClientManager) {
        return new FitnessRecordingHelperImpl(fitnessClientManager.getClient());
    }

    @Provides
    @Singleton
    FitnessSessionHelper providesFitnessSessionHelper(FitnessClientManager fitnessClientManager) {
        return new FitnessSessionHelperImpl(fitnessClientManager.getClient());
    }

    @Provides
    @Singleton
    FitnessHistoryHelper providesFitnessHistoryHelper(FitnessClientManager fitnessClientManager) {
        return new FitnessHistoryHelperImpl(fitnessClientManager.getClient());
    }

}
