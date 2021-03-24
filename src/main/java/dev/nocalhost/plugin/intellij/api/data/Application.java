package dev.nocalhost.plugin.intellij.api.data;

import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class Application {
    private int id;
    @SerializedName("user_id")
    private int userId;
    private int status;
    @SerializedName("public")
    private int isPublic;
    private int editable;
    @SerializedName("context")
    private String contextStr;
    private transient Context context;

    @Data
    public static class Context {
        private String source;
        @SerializedName("install_type")
        private String installType;
        @SerializedName("resource_dir")
        private String[] resourceDir;
        @SerializedName("application_name")
        private String applicationName;
        @SerializedName("application_url")
        private String applicationUrl;
        @SerializedName("application_config_path")
        private String applicationConfigPath;
        @SerializedName("nocalhost_config")
        private String nocalhostConfig;
    }
}
