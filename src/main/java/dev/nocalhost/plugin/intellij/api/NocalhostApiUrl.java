package dev.nocalhost.plugin.intellij.api;

public class NocalhostApiUrl {
    private static final String API_V1_SUFFIX = "/v1";

    private static final String LOGIN_URL = "/login";
    private static final String ME_URL = "/me";
    private static final String DEV_SPACE_URL = "/plugin/dev_space";
    private static final String UPDATE_APP_STATUS = "/plugin/application/%d/dev_space/%d/plugin_sync";
    private static final String RECREATE_DEV_SPACE = "/plugin/%d/recreate";

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

    public static String recreateDevSpace(String host, int spaceId) {
        String url = String.format("%s%s%s", host, API_V1_SUFFIX, RECREATE_DEV_SPACE);
        return String.format(url, spaceId);
    }
}
