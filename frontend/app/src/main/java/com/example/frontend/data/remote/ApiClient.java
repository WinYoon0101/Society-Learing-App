package com.example.frontend.data.remote;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.frontend.utils.Constants;
import java.io.IOException;
import java.util.concurrent.TimeUnit; // Cần thêm cái này
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static Retrofit retrofit = null;

    public static ApiService getApiService(Context context) {
        // Lưu ý: Nếu bạn đổi IP ở Constants, hãy tắt hẳn App rồi mở lại
        // để nó chạy vào phần khởi tạo mới này nhé.
        if (retrofit == null) {

            // 1. Cấu hình OkHttp với thời gian chờ (Timeout) cực dài cho việc Upload
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS) // Đợi kết nối 60 giây
                    .writeTimeout(60, TimeUnit.SECONDS)   // Đợi gửi file 60 giây
                    .readTimeout(60, TimeUnit.SECONDS)    // Đợi server phản hồi 60 giây
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            SharedPreferences sharedPref = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
                            String token = sharedPref.getString("JWT_TOKEN", "");

                            Request newRequest = chain.request().newBuilder()
                                    .addHeader("Authorization", "Bearer " + token)
                                    .build();

                            return chain.proceed(newRequest);
                        }
                    })
                    .build();

            // 2. Khởi tạo Retrofit
            retrofit = new Retrofit.Builder()
                    .baseUrl(Constants.BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}