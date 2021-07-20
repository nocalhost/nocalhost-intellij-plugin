package dev.nocalhost.plugin.intellij.api;

public class NocalhostApiUrl {
    private static final String API_V1_SUFFIX = "/v1";

    private static final String LOGIN_URL = "/login";
    private static final String TOKEN_REFRESH_URL = "/token/refresh";
    private static final String ME_URL = "/me";
    private static final String VERSION_URL = "/version";
    @Deprecated
    private static final String DEV_SPACE_URL = "/plugin/dev_space";
    @Deprecated
    private static final String UPDATE_APP_STATUS = "/plugin/application/%d/dev_space/%d/plugin_sync";
    private static final String RECREATE_DEV_SPACE = "/plugin/%d/recreate";

    private static final String DEV_SPACES_LIST = "/users/%d/dev_spaces";
    private static final String APPLICATIONS_LIST = "/users/%d/applications";
    private static final String SERVICE_ACCOUNTS = "/plugin/service_accounts";

    public static String login(String host) {
        return String.format("%s%s%s", host, API_V1_SUFFIX, LOGIN_URL);
    }

    public static String me(String host) {
        return String.format("%s%s%s", host, API_V1_SUFFIX, ME_URL);
    }

    public static String devSpaces(String host) {
        return String.format("%s%s%s", host, API_V1_SUFFIX, DEV_SPACE_URL);
    }

    public static String updateAppStatus(String host, int applicationId, int spaceId) {
        String url = String.format("%s%s%s", host, API_V1_SUFFIX, UPDATE_APP_STATUS);
        return String.format(url, applicationId, spaceId);
    }

    public static String recreateDevSpace(String host, long spaceId) {
        String url = String.format("%s%s%s", host, API_V1_SUFFIX, RECREATE_DEV_SPACE);
        return String.format(url, spaceId);
    }

    public static String devSpacesList(String host, long userId) {
        String url = String.format("%s%s%s", host, API_V1_SUFFIX, DEV_SPACES_LIST);
        return String.format(url, userId);
    }

    public static String applicationsList(String host, long userId) {
        String url = String.format("%s%s%s", host, API_V1_SUFFIX, APPLICATIONS_LIST);
        return String.format(url, userId);
    }

    public static String serviceAccounts(String host) {
        return String.format("%s%s%s", host, API_V1_SUFFIX, SERVICE_ACCOUNTS);
    }

    public static String version(String host) {
        return String.format("%s%s%s", host, API_V1_SUFFIX, VERSION_URL);
    }

    public static String tokenRefresh(String host) {
        return String.format("%s%s%s", host, API_V1_SUFFIX, TOKEN_REFRESH_URL);
    }
}
