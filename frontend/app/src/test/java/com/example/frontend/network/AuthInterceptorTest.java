package com.example.frontend.network;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Protocol;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okhttp3.Connection;

import org.junit.Test;

public class AuthInterceptorTest {
    // Helper chain để mô phỏng Interceptor.Chain đơn giản
    static class SimpleChain implements Interceptor.Chain {
        private final Request request;

        SimpleChain(Request request) {
            this.request = request;
        }

        @Override
        public Request request() { return request; }

        @Override
        public Response proceed(Request request) throws IOException {
            // Trả về một Response giả để test header của request
            return new Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(MediaType.get("text/plain"), ""))
                    .build();
        }

        @Override
        public Connection connection() { return null; }

        @Override
        public Call call() { return null; }

        @Override
        public int connectTimeoutMillis() { return 0; }

        @Override
        public Interceptor.Chain withConnectTimeout(int timeout, TimeUnit unit) { return this; }

        @Override
        public int readTimeoutMillis() { return 0; }

        @Override
        public Interceptor.Chain withReadTimeout(int timeout, TimeUnit unit) { return this; }

        @Override
        public int writeTimeoutMillis() { return 0; }

        @Override
        public Interceptor.Chain withWriteTimeout(int timeout, TimeUnit unit) { return this; }
    }

    @Test
    public void addsAuthorizationHeader_whenTokenPresent() throws IOException {
        String token = "abc123";
        AuthInterceptor interceptor = new AuthInterceptor(token);

        Request req = new Request.Builder()
                .url("http://example.com")
                .build();

        Interceptor.Chain chain = new SimpleChain(req);
        Response res = interceptor.intercept(chain);

        String header = res.request().header("Authorization");
        assertEquals("Bearer abc123", header);
    }
}
