package dev.nocalhost.plugin.intellij.api;

import com.google.gson.reflect.TypeToken;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;

import org.apache.groovy.util.Maps;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.api.data.LoginRequest;
import dev.nocalhost.plugin.intellij.api.data.LoginResponse;
import dev.nocalhost.plugin.intellij.api.data.NocalhostApiResponse;
import dev.nocalhost.plugin.intellij.api.data.UserInfo;
import dev.nocalhost.plugin.intellij.exception.NocalhostApiException;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.topic.NocalhostAccountChangedNotifier;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NocalhostApi {

    public static final MediaType MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient.Builder().build();

    public void login(String host, String email, String password) throws IOException {
        LoginRequest loginRequest = new LoginRequest(email, password);
        RequestBody requestBody = RequestBody.create(DataUtils.GSON.toJson(loginRequest), MEDIA_TYPE);
        String url = NocalhostApiUrl.login(host);
        Request request = new Request.Builder().url(url).post(requestBody).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new NocalhostApiException(String.format("Failed to login, connect server API %s, respond HTTP %d", url, response.code()));
            }
            NocalhostApiResponse<LoginResponse> resp = DataUtils.GSON.fromJson(response.body().charStream(),
                    TypeToken.getParameterized(NocalhostApiResponse.class, LoginResponse.class).getType());
            if (resp.getCode() != 0) {
                throw new NocalhostApiException(String.format("Failed to login nocalhost server: %s", resp.getMessage()));
            }

            final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);
            nocalhostSettings.setBaseUrl(host);
            nocalhostSettings.setJwt(resp.getData().getToken());

            refreshUserInfo();

            final Application application = ApplicationManager.getApplication();
            NocalhostAccountChangedNotifier publisher = application.getMessageBus()
                    .syncPublisher(NocalhostAccountChangedNotifier.NOCALHOST_ACCOUNT_CHANGED_NOTIFIER_TOPIC);
            publisher.action();
        }
    }

    private void refreshUserInfo() throws IOException {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);

        String url = NocalhostApiUrl.me(nocalhostSettings.getBaseUrl());
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + nocalhostSettings.getJwt())
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new NocalhostApiException(String.format("Failed to get user info, connect server API %s, respond HTTP %d", url, response.code()));
            }
            NocalhostApiResponse<UserInfo> resp = DataUtils.GSON.fromJson(response.body().charStream(),
                    TypeToken.getParameterized(NocalhostApiResponse.class, UserInfo.class).getType());
            if (resp.getCode() != 0) {
                throw new NocalhostApiException(String.format("Failed to get user info : %s", resp.getMessage()));
            }

            nocalhostSettings.setUserInfo(resp.getData());
        }
    }

    public List<DevSpace> listDevSpace() throws IOException {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);

        String url = NocalhostApiUrl.devSpaces(nocalhostSettings.getBaseUrl());

        Request request = new Request.Builder()
                .url(url)
                .addHeader("authorization", "Bearer " + nocalhostSettings.getJwt())
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new NocalhostApiException(String.format("Failed to get devSpaces, connect server API %s, respond HTTP %d", url, response.code()));
            }
            String body = response.body().string();
            NocalhostApiResponse<List<DevSpace>> resp = DataUtils.GSON.fromJson(body,
                    TypeToken.getParameterized(
                            NocalhostApiResponse.class,
                            TypeToken.getParameterized(List.class, DevSpace.class).getType()
                    ).getType());
            if (resp.getCode() != 0) {
                throw new NocalhostApiException(String.format("Failed to get devSpaces, %s", resp.getMessage()));
            }
            List<DevSpace> devSpaces = resp.getData();
            return devSpaces.stream().peek(devSpace -> devSpace.setContext(DataUtils.GSON.fromJson(devSpace.getContextStr(), DevSpace.Context.class))).collect(Collectors.toList());
        }
    }

    public void syncInstallStatus(DevSpace devSpace, int status) throws IOException {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);

        String url = NocalhostApiUrl.updateAppStatus(nocalhostSettings.getBaseUrl(), devSpace.getId(), devSpace.getDevSpaceId());
        Request request = new Request.Builder()
                .url(url)
                .addHeader("authorization", "Bearer " + nocalhostSettings.getJwt())
                .put(RequestBody.create(DataUtils.GSON.toJson(Maps.of("status", status)).getBytes(StandardCharsets.UTF_8), MEDIA_TYPE))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new NocalhostApiException(String.format("Failed to sync install status, connect server API %s, respond HTTP %d", url, response.code()));
            }
            NocalhostApiResponse<Object> resp = DataUtils.GSON.fromJson(response.body().charStream(),
                    TypeToken.getParameterized(NocalhostApiResponse.class, Object.class).getType());
            if (resp.getCode() != 0) {
                throw new NocalhostApiException(String.format("Failed to sync install status, %s", resp.getMessage()));
            }
        }
    }

    public void recreate(DevSpace devSpace) throws IOException {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);

        String url = NocalhostApiUrl.recreateDevSpace(nocalhostSettings.getBaseUrl(), devSpace.getDevSpaceId());
        Request request = new Request.Builder()
                .url(url)
                .addHeader("authorization", "Bearer " + nocalhostSettings.getJwt())
                .post(RequestBody.create("".getBytes()))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new NocalhostApiException(String.format("Failed to reset application , connect server API %s, respond HTTP %d", url, response.code()));
            }
            NocalhostApiResponse<Object> resp = DataUtils.GSON.fromJson(response.body().charStream(),
                    TypeToken.getParameterized(NocalhostApiResponse.class, Object.class).getType());
            if (resp.getCode() != 0) {
                throw new NocalhostApiException(String.format("Failed to reset application, %s", resp.getMessage()));
            }
        }
    }
}
