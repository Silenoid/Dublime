package com.silenoids.utils;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;

public class HttpClient {

    private static void executeNoBodyGetRequest(String token, String value1, String value2) throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://maker.ifttt.com/trigger/audio_worked/with/key/" + token + "?" +
                        "value1=" + value1 +
                        "&value2=" + value2)
                .build();
        Response response = client.newCall(request).execute();
        System.out.println(response.toString());
    }

    private static void executeWithBodyGetRequest(String token, String bodyString) throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://maker.ifttt.com/trigger/error_occurred/json/with/key/" + token)
                .post(RequestBody.create(MediaType.parse("application/json"), bodyString))
                .header("Content-Type","application/json")
                .build();
        Response response = client.newCall(request).execute();
        System.out.println(request.toString());
        System.out.println(response.toString());
    }

}
