package dev.nocalhost.plugin.intellij.commands.data;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlGetResource {
    @SerializedName("description")
    private NhctlDescribeService nhctlDescribeService;
    @SerializedName("info")
    private KubeResource kubeResource;
}
