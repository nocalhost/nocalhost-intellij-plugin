package dev.nocalhost.plugin.intellij.api;

import com.google.inject.Inject;

import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;

import dev.nocalhost.plugin.intellij.api.data.LoginRequest;
import dev.nocalhost.plugin.intellij.api.data.LoginResponse;
import dev.nocalhost.plugin.intellij.utils.JSON;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NocalhostApi {
    private static final String API_V1_SUFFIX = "/v1";
    public static final MediaType MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    @Inject
    private Logger log;

    public String login(String host, String email, String password) {
        OkHttpClient client = new OkHttpClient();

        LoginRequest loginRequest = new LoginRequest(email, password);
        RequestBody body = RequestBody.create(JSON.toJson(loginRequest), MEDIA_TYPE);
        String url = host + API_V1_SUFFIX + "/login";
        Request request = new Request.Builder().url(url).post(body).build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 200) {
                String result = response.body().string();
                NocalhostResponseBody responseBody = JSON.fromJson(result, NocalhostResponseBody.class);
                if (responseBody.getCode() != 0) {
                    log.error("Login response error, " + responseBody.getMessage());
                    return null;
                }
                LoginResponse loginResponse = JSON.fromJson(responseBody.getData(), LoginResponse.class);
                return loginResponse.getToken();
            } else {
                log.error("Login connect error, " + response.code());
            }
        } catch (IOException e) {
            log.error("Login nocalhost error, ", e);
            return null;
        }
        return null;
    }
}
