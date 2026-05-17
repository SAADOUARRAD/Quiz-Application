package com.example.quiz_application;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import org.json.JSONObject;

public class ApiClient {

    // URL du serveur FastAPI
    // Sur émulateur : 10.0.2.2 = localhost de ton PC
    private static final String BASE_URL = "http://10.0.2.2:8000";

    private static OkHttpClient client = new OkHttpClient();

    // ─── GET ───────────────────────────────────────
    public static String get(String endpoint) {
        try {
            Request request = new Request.Builder()
                    .url(BASE_URL + endpoint)
                    .build();

            Response response = client.newCall(request).execute();
            return response.body().string();

        } catch (Exception e) {
            return null;
        }
    }

    // ─── POST ──────────────────────────────────────
    public static String post(String endpoint, JSONObject body) {
        try {
            MediaType JSON = MediaType.parse("application/json");
            RequestBody requestBody = RequestBody.create(body.toString(), JSON);

            Request request = new Request.Builder()
                    .url(BASE_URL + endpoint)
                    .post(requestBody)
                    .build();

            Response response = client.newCall(request).execute();
            return response.body().string();

        } catch (Exception e) {
            return null;
        }
    }
}