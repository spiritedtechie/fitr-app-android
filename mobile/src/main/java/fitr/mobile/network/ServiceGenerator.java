package fitr.mobile.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServiceGenerator {

    private static HttpLoggingInterceptor.Level logLevel = HttpLoggingInterceptor.Level.BODY;

    private static final int NETWORK_TIMEOUT_SECONDS = 20;

    private static Retrofit retrofit;

    private InputStream keystoreStream;

    public ServiceGenerator(InputStream keystoreStream) {
        this.keystoreStream = keystoreStream;
    }

    public <T> T createService(Class<T> serviceClass) {

        return getRetrofit().create(serviceClass);
    }

    public Retrofit getRetrofit() {
        if (retrofit == null) {
            buildRetrofit();
        }

        return retrofit;
    }

    private void buildRetrofit() {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.readTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .connectTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        httpClient.sslSocketFactory(buildSslSocketFactory());


        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(logLevel);
        httpClient.addInterceptor(logging);

        final Gson gson = new GsonBuilder().create();

        retrofit = new Retrofit.Builder()
                // Change IP to service host
                .baseUrl("https://172.17.243.132:8443")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .client(httpClient.build())
                .build();
    }

    private SSLSocketFactory buildSslSocketFactory() {
        try {
            KeyStore ksTrust = KeyStore.getInstance("BKS");
            ksTrust.load(keystoreStream, "".toCharArray());
            // TrustManager decides which certificate authorities to use.
            TrustManagerFactory tmf = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ksTrust);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

        } catch (KeyStoreException | CertificateException
                | NoSuchAlgorithmException | IOException
                | KeyManagementException e) {
            e.printStackTrace();
        }

        return null;
    }
}