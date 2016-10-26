package fitr.mobile.config;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import fitr.mobile.network.AuthenticateApi;
import fitr.mobile.network.ServiceGenerator;
import fitr.mobile.presenters.RegisterPresenter;
import fitr.mobile.presenters.SignInPresenter;

@Module
public class AuthenticationModule {

    @Provides
    @Singleton
    AuthenticateApi providesAuthenticateApi() {
        return ServiceGenerator.createService(AuthenticateApi.class);
    }

    @Provides
    @Singleton
    SignInPresenter providesSignInPresenter(AuthenticateApi authenticateApi) {
        return new SignInPresenter(authenticateApi);
    }

    @Provides
    @Singleton
    RegisterPresenter providesRegisterPresenter(AuthenticateApi authenticateApi) {
        return new RegisterPresenter(authenticateApi);
    }
}
