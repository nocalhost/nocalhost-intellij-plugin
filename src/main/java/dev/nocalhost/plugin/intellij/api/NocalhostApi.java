package dev.nocalhost.plugin.intellij.api;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.api.data.Application;
import dev.nocalhost.plugin.intellij.api.data.LoginRequest;
import dev.nocalhost.plugin.intellij.api.data.LoginResponse;
import dev.nocalhost.plugin.intellij.api.data.NocalhostApiResponse;
import dev.nocalhost.plugin.intellij.api.data.ServiceAccount;
import dev.nocalhost.plugin.intellij.api.data.UserInfo;
import dev.nocalhost.plugin.intellij.exception.NocalhostApiException;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NocalhostApi {

    public static final MediaType MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient.Builder().build();

    public String login(String server, String email, String password) throws IOException, NocalhostApiException {
        LoginRequest loginRequest = new LoginRequest(email, password);
        RequestBody requestBody = RequestBody.create(DataUtils.GSON.toJson(loginRequest), MEDIA_TYPE);
        String url = NocalhostApiUrl.login(server);
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

            return resp.getData().getToken();
        }
    }

    public UserInfo getUserInfo(String server, String jwt) throws IOException, NocalhostApiException {
        String url = NocalhostApiUrl.me(server);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + jwt)
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new NocalhostApiException(url, "get user info", response.code(), "");
            }
            String body = response.body().string();
            NocalhostApiResponse<UserInfo> resp = DataUtils.GSON.fromJson(body,
                    TypeToken.getParameterized(NocalhostApiResponse.class, UserInfo.class).getType());
            if (resp.getCode() != 0) {
                throw new NocalhostApiException(url, "get user info", response.code(), resp.getMessage());
            }

            return resp.getData();
        }
    }

    public List<ServiceAccount> listServiceAccount(String server, String jwt) throws NocalhostApiException, IOException {
        String url = NocalhostApiUrl.serviceAccounts(server);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("authorization", "Bearer " + jwt)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new NocalhostApiException(url, "list serviceAccounts", response.code(), "");
            }
            String body = response.body().string();
            NocalhostApiResponse<List<ServiceAccount>> resp = DataUtils.GSON.fromJson(body,
                    TypeToken.getParameterized(
                            NocalhostApiResponse.class,
                            TypeToken.getParameterized(List.class, ServiceAccount.class).getType()
                    ).getType());
            if (resp.getCode() != 0) {
                throw new NocalhostApiException(url, "list serviceAccounts", response.code(), resp.getMessage());
            }
            return resp.getData();
        }
    }

    public List<Application> listApplications(String server, String jwt, long userId) throws IOException, NocalhostApiException {
        String url = NocalhostApiUrl.applicationsList(server, userId);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("authorization", "Bearer " + jwt)
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
}
