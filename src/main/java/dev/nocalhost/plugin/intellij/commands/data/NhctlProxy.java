package dev.nocalhost.plugin.intellij.commands.data;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class NhctlProxy {
    @SerializedName("status")
    private String status;

    @SerializedName("belongsToMe")
    private boolean belongsToMe;
}
