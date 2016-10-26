package fitr.mobile.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServiceGenerator {

    private static final int NETWORK_TIMEOUT_SECONDS = 20;

    private static Retrofit retrofit;

    private static HttpLoggingInterceptor.Level logLevel = HttpLoggingInterceptor.Level.BODY;

    public static <T> T createService(Class<T> serviceClass) {

        return getRetrofit().create(serviceClass);
    }

    public static Retrofit getRetrofit() {
        if (retrofit == null) {
            buildRetrofit();
        }

        return retrofit;
    }

    private static void buildRetrofit() {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.readTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .connectTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(logLevel);
        httpClient.addInterceptor(logging);

        final Gson gson = new GsonBuilder().create();

        retrofit = new Retrofit.Builder()
                // Change IP to service host
                .baseUrl("https://192.168.0.8:8443")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .client(httpClient.build())
                .build();
    }
}