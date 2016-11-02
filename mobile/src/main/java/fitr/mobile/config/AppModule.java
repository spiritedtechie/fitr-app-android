package fitr.mobile.config;

import android.app.Application;
import android.content.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import fitr.mobile.R;
import fitr.mobile.network.ServiceGenerator;

@Module
public class AppModule {

    @Inject
    Context context;

    private Application mApplication;

    public AppModule(Application application) {
        mApplication = application;
    }

    @Provides
    @Singleton
    Application providesApplication() {
        return mApplication;
    }

    @Provides
    @Singleton
    ServiceGenerator providesServiceGenerator() {
        return new ServiceGenerator(context.getResources().openRawResource(R.raw.fitr));
    }
}