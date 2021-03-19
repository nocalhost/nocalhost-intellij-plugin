package dev.nocalhost.plugin.intellij.api;

import com.google.gson.reflect.TypeToken;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.api.data.Application;
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

    public void login(String host, String email, String password) throws IOException, NocalhostApiException {
        LoginRequest loginRequest = new LoginRequest(email, password);
        RequestBody requestBody = RequestBody.create(DataUtils.GSON.toJson(loginRequest), MEDIA_TYPE);
        String url = NocalhostApiUrl.login(host);
        Request request = new Request.Builder().url(url).post(requestBody).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new NocalhostApiException(url, "login", response.code(), "");
            }
            NocalhostApiResponse<LoginResponse> resp = DataUtils.GSON.fromJson(response.body().charStream(),
                    TypeToken.getParameterized(NocalhostApiResponse.class, LoginResponse.class).getType());
            if (resp.getCode() != 0) {
                throw new NocalhostApiException(url, "login", response.code(), resp.getMessage());
            }

            final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);
            nocalhostSettings.setBaseUrl(host);
            nocalhostSettings.setJwt(resp.getData().getToken());

            refreshUserInfo();

            final com.intellij.openapi.application.Application application = ApplicationManager.getApplication();
            NocalhostAccountChangedNotifier publisher = application.getMessageBus()
                    .syncPublisher(NocalhostAccountChangedNotifier.NOCALHOST_ACCOUNT_CHANGED_NOTIFIER_TOPIC);
            publisher.action();
        }
    }

    private void refreshUserInfo() throws IOException, NocalhostApiException {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);

        String url = NocalhostApiUrl.me(nocalhostSettings.getBaseUrl());
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + nocalhostSettings.getJwt())
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new NocalhostApiException(url, "get user info", response.code(), "");
            }
            NocalhostApiResponse<UserInfo> resp = DataUtils.GSON.fromJson(response.body().charStream(),
                    TypeToken.getParameterized(NocalhostApiResponse.class, UserInfo.class).getType());
            if (resp.getCode() != 0) {
                throw new NocalhostApiException(url, "get user info", response.code(), resp.getMessage());
            }

            nocalhostSettings.setUserInfo(resp.getData());
        }
    }

    public List<DevSpace> listDevSpaces() throws IOException, NocalhostApiException {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);

        String url = NocalhostApiUrl.devSpacesList(nocalhostSettings.getBaseUrl(), nocalhostSettings.getUserInfo().getId());

        Request request = new Request.Builder()
                .url(url)
                .addHeader("authorization", "Bearer " + nocalhostSettings.getJwt())
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new NocalhostApiException(url, "list devSpaces", response.code(), "");
            }
            String body = response.body().string();
            NocalhostApiResponse<List<DevSpace>> resp = DataUtils.GSON.fromJson(body,
                    TypeToken.getParameterized(
                            NocalhostApiResponse.class,
                            TypeToken.getParameterized(List.class, DevSpace.class).getType()
                    ).getType());
            if (resp.getCode() != 0) {
                throw new NocalhostApiException(url, "list devSpaces", response.code(), resp.getMessage());
            }
            List<DevSpace> devSpaces = resp.getData();
            return devSpaces.stream().peek(devSpace -> devSpace.setSpaceResourceLimit(DataUtils.GSON.fromJson(devSpace.getSpaceResourceLimitStr(), DevSpace.SpaceResourceLimit.class))).collect(Collectors.toList());
        }
    }

    public List<Application> listApplications() throws IOException, NocalhostApiException {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);

        String url = NocalhostApiUrl.applicationsList(nocalhostSettings.getBaseUrl(), nocalhostSettings.getUserInfo().getId());

        Request request = new Request.Builder()
                .url(url)
                .addHeader("authorization", "Bearer " + nocalhostSettings.getJwt())
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new NocalhostApiException(url, "list applications", response.code(), "");
            }
            String body = response.body().string();
            NocalhostApiResponse<List<Application>> resp = DataUtils.GSON.fromJson(body,
                    TypeToken.getParameterized(
                            NocalhostApiResponse.class,
                            TypeToken.getParameterized(List.class, Application.class).getType()
                    ).getType());
            if (resp.getCode() != 0) {
                throw new NocalhostApiException(url, "list applications", response.code(), resp.getMessage());
            }
            List<Application> applications = resp.getData();
            return applications.stream().peek(app -> app.setContext(DataUtils.GSON.fromJson(app.getContextStr(), Application.Context.class))).collect(Collectors.toList());
        }
    }

    public void recreate(DevSpace devSpace) throws IOException, NocalhostApiException {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);

        String url = NocalhostApiUrl.recreateDevSpace(nocalhostSettings.getBaseUrl(), devSpace.getId());
        Request request = new Request.Builder()
                .url(url)
                .addHeader("authorization", "Bearer " + nocalhostSettings.getJwt())
                .post(RequestBody.create("".getBytes()))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new NocalhostApiException(url, "reset application", response.code(), "");
            }
            NocalhostApiResponse<Object> resp = DataUtils.GSON.fromJson(response.body().charStream(),
                    TypeToken.getParameterized(NocalhostApiResponse.class, Object.class).getType());
            if (resp.getCode() != 0) {
                throw new NocalhostApiException(url, "reset application", response.code(), resp.getMessage());
            }
        }
    }
}
