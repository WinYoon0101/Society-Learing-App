package com.example.frontend.data.remote;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.frontend.utils.Constants;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static Retrofit retrofit = null;
    // Lưu application context để interceptor luôn đọc token mới nhất
    private static Context appContext = null;

    public static ApiService getApiService(Context context) {
        // Lưu applicationContext (không bị leak)
        if (appContext == null) {
            appContext = context.getApplicationContext();
        }

        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .addInterceptor(logging)
                    .addInterceptor(chain -> {
                        // Đọc token mỗi lần gọi API → luôn lấy token mới nhất
                        SharedPreferences prefs = appContext.getSharedPreferences(
                                "MyAppPrefs", Context.MODE_PRIVATE);
                        String token = prefs.getString("JWT_TOKEN", "");

                        Request newRequest = chain.request().newBuilder()
                                .addHeader("Authorization", "Bearer " + token)
                                .build();
                        return chain.proceed(newRequest);
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(Constants.BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }

    /** Gọi sau khi logout để clear token */
    public static void reset() {
        retrofit = null;
        appContext = null;
    }
}
