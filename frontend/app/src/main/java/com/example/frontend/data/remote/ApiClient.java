package com.example.frontend.data.remote;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.frontend.utils.Constants;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static Retrofit retrofit = null;

    // Truyền Context vào để lấy Token
    public static ApiService getApiService(Context context) {
        if (retrofit == null) {

            // Tạo bộ cài đặt OkHttp để "Bắt" (Intercept) mọi Request và nhét Token vào
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    // 1. Lấy JWT_TOKEN từ SharedPreferences
                    SharedPreferences sharedPref = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
                    String token = sharedPref.getString("JWT_TOKEN", ""); // Nếu không có token thì trả về chuỗi rỗng

                    // 2. Chế tạo Request mới, gắn Token vào Header
                    Request newRequest = chain.request().newBuilder()
                            .addHeader("Authorization", "Bearer " + token) // Gắn chữ Bearer phía trước
                            .build();

                    return chain.proceed(newRequest);
                }
            }).build();

            // Khởi tạo Retrofit với cái client vừa tạo
            retrofit = new Retrofit.Builder()
                    .baseUrl(Constants.BASE_URL)
                    .client(client) // <-- Bắt buộc phải truyền OkHttpClient vào đây
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}