package com.example.frontend.data.remote;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.frontend.data.model.Message;
import com.example.frontend.utils.Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
                                // Dùng header() thay vì addHeader() để tránh gửi trùng Authorization
                                // khi một số API đã truyền @Header("Authorization") từ Retrofit.
                                .header("Authorization", "Bearer " + token)
                                .build();
                        return chain.proceed(newRequest);
                    })
                    .build();

            // Gson chịu được replyTo dạng STRING (ObjectId chưa populate) — tránh crash
            // "Expected BEGIN_OBJECT but was STRING" khi load conversations/messages.
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Message.class, new MessageDeserializer())
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(Constants.BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
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