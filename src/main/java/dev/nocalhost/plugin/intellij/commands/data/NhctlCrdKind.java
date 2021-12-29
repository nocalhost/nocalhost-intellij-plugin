package dev.nocalhost.plugin.intellij.commands.data;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class NhctlCrdKind {
    @SerializedName("info")
    private Spec info;

    @Getter
    public class Spec {
        @SerializedName("Kind")
        private String kind;

        @SerializedName("Group")
        private String group;

        @SerializedName("Version")
        private String version;

        @SerializedName("Resource")
        private String resource;

        @SerializedName("Namespaced")
        private boolean namespaced;
    }
}
