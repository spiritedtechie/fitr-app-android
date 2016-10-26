package fitr.mobile.presenters;

import fitr.mobile.models.Credentials;
import fitr.mobile.network.AuthenticateApi;
import fitr.mobile.views.SignInView;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class SignInPresenter extends BasePresenter<SignInView> {

    private AuthenticateApi authenticateApi;

    public SignInPresenter(AuthenticateApi authenticateApi) {
        this.authenticateApi = authenticateApi;
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
    }

    public void signIn(String userName, String password) {

        authenticateApi.authenticate(new Credentials(userName, password))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        n -> {

                        },
                        e -> {

                        },
                        () -> {

                        }
                );
    }

}
