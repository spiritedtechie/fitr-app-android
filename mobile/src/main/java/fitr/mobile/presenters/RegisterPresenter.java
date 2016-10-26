package fitr.mobile.presenters;

import android.util.Log;

import fitr.mobile.models.Credentials;
import fitr.mobile.network.AuthenticateApi;
import fitr.mobile.views.RegisterView;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class RegisterPresenter extends BasePresenter<RegisterView> {

    private static final String TAG = RegisterPresenter.class.getSimpleName();

    private AuthenticateApi authenticateApi;

    public RegisterPresenter(AuthenticateApi authenticateApi) {
        this.authenticateApi = authenticateApi;
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
    }

    public void register(String userName, String password) {

        authenticateApi.signup(new Credentials(userName, password))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        n -> onView(v -> v.registrationSuccessful()),
                        e -> {
                            Log.e(TAG, "Error registering user", e);
                            onView(v -> v.registrationFailure());
                        },
                        () -> {
                        }
                );
    }

}
