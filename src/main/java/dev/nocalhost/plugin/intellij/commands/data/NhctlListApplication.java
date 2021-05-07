package dev.nocalhost.plugin.intellij.commands.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlListApplication {
    @SerializedName("Namespace")
    private String namespace;

    @SerializedName("Application")
    private List<Application> application;

    @Getter
    @Setter
    public static class Application {
        @SerializedName("Name")
        private String name;

        @SerializedName("Type")
        private String type;
    }
}
