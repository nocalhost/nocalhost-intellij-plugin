package dev.nocalhost.plugin.intellij.api;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;

import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;

import dev.nocalhost.plugin.intellij.api.data.AuthData;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.api.data.LoginRequest;
import dev.nocalhost.plugin.intellij.api.data.LoginResponse;
import dev.nocalhost.plugin.intellij.exception.NocalhostApiException;
import dev.nocalhost.plugin.intellij.utils.JSON;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NocalhostApi {

    public static final MediaType MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient.Builder().callTimeout(1, TimeUnit.SECONDS).build();

    @Inject
    private Logger log;
    @Inject
    private NocalhostApiUrl nocalhostApiUrl;

    public String login(AuthData authData, String password) {
        LoginRequest loginRequest = new LoginRequest(authData.getEmail(), password);
        RequestBody requestBody = RequestBody.create(JSON.toJson(loginRequest), MEDIA_TYPE);
        String url = nocalhostApiUrl.login(authData);
        Request request = new Request.Builder().url(url).post(requestBody).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new NocalhostApiException(String.format("Failed to login, connect server API %s, respond HTTP %d", url, response.code()));
            }
            String body = response.body().string();
            NocalhostApiResponse responseBody = JSON.fromJson(body, NocalhostApiResponse.class);
            if (responseBody.getCode() != 0) {
                log.error("Login response error, " + responseBody.getMessage());
                return null;
            }
            LoginResponse loginResponse = JSON.fromJson(responseBody.getData(), LoginResponse.class);
            return loginResponse.getToken();
        } catch (IOException e) {
            log.error("Login nocalhost error, ", e);
            return null;
        }
    }

    public List<DevSpace> listDevSpace(AuthData authData) {
        String url = nocalhostApiUrl.devSpaces(authData);
        Request request = new Request.Builder().url(url).addHeader("authorization", "Bearer " + authData.getToken()).get().build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new NocalhostApiException(String.format("Failed to get devSpaces, connect server API %s, respond HTTP %d", url, response.code()));
            }

            String result = response.body().string();
            NocalhostApiResponse responseBody = JSON.fromJson(result, NocalhostApiResponse.class);
            if (responseBody.getCode() != 0) {
                log.error("list application response error, " + responseBody.getMessage());
                return null;
            }
            Type listType = new TypeToken<List<DevSpace>>() {}.getType();

            List<DevSpace> devSpaces = new Gson().fromJson(responseBody.getData(), listType);
            devSpaces.forEach(devSpace -> {
                try {
                    devSpace.setContext(JSON.fromJson(devSpace.getContextStr(), DevSpace.Context.class));
                } catch (IOException e) {
                    log.error("gson convert context error, ", e);
                }
            });
            return devSpaces;
        } catch (IOException e) {
            log.error("list application error, ", e);
            return null;
        }
    }
}
