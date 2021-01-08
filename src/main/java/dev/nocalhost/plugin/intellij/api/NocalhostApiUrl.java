package dev.nocalhost.plugin.intellij.api;

import dev.nocalhost.plugin.intellij.api.data.AuthData;

public class NocalhostApiUrl {
    private static final String API_V1_SUFFIX = "/v1";

    private static final String LOGIN_URL = "/login";
    private static final String DEV_SPACE_URL = "/plugin/dev_space";
    private static final String UPDATE_APP_STATUS = "/plugin/application/%d/dev_space/%d/plugin_sync";
    private static final String RECREATE_DEV_SPACE = "/plugin/%d/recreate";

    public String login(AuthData authData) {
        return String.format("%s%s%s", authData.getHost(), API_V1_SUFFIX, LOGIN_URL);
    }

    public String devSpaces(AuthData authData) {
        return String.format("%s%s%s", authData.getHost(), API_V1_SUFFIX, DEV_SPACE_URL);
    }

    public String updateAppStatus(AuthData authData, int applicationId, int spaceId) {
        String url = String.format("%s%s%s", authData.getHost(), API_V1_SUFFIX, UPDATE_APP_STATUS);
        return String.format(url, applicationId, spaceId);
    }

    public String recreateDevSpace(AuthData authData, int spaceId) {
        String url = String.format("%s%s%s", authData.getHost(), API_V1_SUFFIX, RECREATE_DEV_SPACE);
        return String.format(url, spaceId);
    }
}
