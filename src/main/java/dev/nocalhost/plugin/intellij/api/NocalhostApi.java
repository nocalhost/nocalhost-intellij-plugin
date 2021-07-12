package dev.nocalhost.plugin.intellij.api;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.gson.reflect.TypeToken;

import com.github.zafarkhaja.semver.Version;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.api.data.Application;
import dev.nocalhost.plugin.intellij.api.data.LoginRequest;
import dev.nocalhost.plugin.intellij.api.data.NocalhostApiResponse;
import dev.nocalhost.plugin.intellij.api.data.ServerVersion;
import dev.nocalhost.plugin.intellij.api.data.ServiceAccount;
import dev.nocalhost.plugin.intellij.api.data.TokenResponse;
import dev.nocalhost.plugin.intellij.api.data.UserInfo;
import dev.nocalhost.plugin.intellij.exception.NocalhostApiException;
import dev.nocalhost.plugin.intellij.exception.NocalhostServerVersionOutDatedException;
import dev.nocalhost.plugin.intellij.service.NocalhostBinService;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NocalhostApi {

    public static final MediaType MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient.Builder().build();

    public TokenResponse login(String server, String email, String password) throws IOException, NocalhostApiException {
        LoginRequest loginRequest = new LoginRequest(email, password);
        RequestBody requestBody = RequestBody.create(DataUtils.GSON.toJson(loginRequest), MEDIA_TYPE);
        String url = NocalhostApiUrl.login(server);
        Request request = new Request.Builder().url(url).post(requestBody).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new NocalhostApiException(url, "login", response.code(), "");
            }
            String body = CharStreams.toString(new InputStreamReader(response.body().byteStream(), Charsets.UTF_8));
            NocalhostApiResponse<TokenResponse> resp = DataUtils.GSON.fromJson(body,
                    TypeToken.getParameterized(NocalhostApiResponse.class, TokenResponse.class).getType());
            if (resp.getCode() != 0) {
                throw new NocalhostApiException(url, "login", response.code(), resp.getMessage());
            }

            return resp.getData();
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
            String body = CharStreams.toString(new InputStreamReader(response.body().byteStream(), Charsets.UTF_8));
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
            String body = CharStreams.toString(new InputStreamReader(response.body().byteStream(), Charsets.UTF_8));
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

    public List<Application> listApplications(String server, String jwt, long userId) throws IOException, NocalhostApiException, NocalhostServerVersionOutDatedException {
        checkServerVersion(server);

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
            String body = CharStreams.toString(new InputStreamReader(response.body().byteStream(), Charsets.UTF_8));
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

    public void recreate(String server, String jwt, long devSpaceId) throws IOException, NocalhostApiException {
        String url = NocalhostApiUrl.recreateDevSpace(server, devSpaceId);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("authorization", "Bearer " + jwt)
                .post(RequestBody.create("".getBytes()))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new NocalhostApiException(url, "reset application", response.code(), "");
            }
            String body = CharStreams.toString(new InputStreamReader(response.body().byteStream(), Charsets.UTF_8));
            NocalhostApiResponse<Object> resp = DataUtils.GSON.fromJson(body,
                    TypeToken.getParameterized(NocalhostApiResponse.class, Object.class).getType());
            if (resp.getCode() != 0) {
                throw new NocalhostApiException(url, "reset application", response.code(), resp.getMessage());
            }
        }
    }

    public TokenResponse refreshToken(String server, String jwt, String refreshToken) throws IOException, NocalhostApiException {
        String url = NocalhostApiUrl.tokenRefresh(server);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("authorization", "Bearer " + jwt)
                .addHeader("Reraeb", refreshToken)
                .post(RequestBody.create("".getBytes()))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new NocalhostApiException(url, "refresh token", response.code(), "");
            }
            String body = CharStreams.toString(new InputStreamReader(response.body().byteStream(), Charsets.UTF_8));
            NocalhostApiResponse<TokenResponse> resp = DataUtils.GSON.fromJson(body,
                    TypeToken.getParameterized(NocalhostApiResponse.class, TokenResponse.class).getType());
            if (resp.getCode() != 0) {
                throw new NocalhostApiException(url, "refresh token", response.code(), resp.getMessage());
            }
            return resp.getData();
        }
    }

    private void checkServerVersion(String server) throws NocalhostApiException, IOException, NocalhostServerVersionOutDatedException {
        String url = NocalhostApiUrl.version(server);
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new NocalhostApiException(url, "get server version", response.code(), "");
            }
            String body = CharStreams.toString(new InputStreamReader(response.body().byteStream(), Charsets.UTF_8));
            NocalhostApiResponse<ServerVersion> resp = DataUtils.GSON.fromJson(body,
                    TypeToken.getParameterized(NocalhostApiResponse.class, ServerVersion.class).getType());
            if (resp.getCode() != 0) {
                throw new NocalhostApiException(url, "get server version", response.code(), resp.getMessage());
            }

            InputStream configInputStream = NocalhostBinService.class.getClassLoader().getResourceAsStream("config.properties");
            Properties properties = new Properties();
            properties.load(configInputStream);

            String serverVersion = resp.getData().getVersion();
            if (!StringUtils.isNotEmpty(serverVersion)) {
                return;
            }

            Version currentServerVersion = Version.valueOf(serverVersion.substring(1));
            Version requiredMinimalServerVersion = Version.valueOf(properties.getProperty("serverVersion"));

            if (currentServerVersion.lessThan(requiredMinimalServerVersion)) {
                throw new NocalhostServerVersionOutDatedException(server, resp.getData().getVersion(), properties.getProperty("serverVersion"));
            }
        }
    }
}
