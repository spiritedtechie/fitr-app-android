package fitr.mobile.network;

import fitr.mobile.models.AuthToken;
import fitr.mobile.models.Credentials;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.POST;
import rx.Observable;

public interface AuthenticateApi {

    @POST("authenticate")
    Observable<AuthToken> authenticate(@Body Credentials credentials);

    @POST("signup")
    Observable<Void> signup(@Body Credentials credentials);
}
