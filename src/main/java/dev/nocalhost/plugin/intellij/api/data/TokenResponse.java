package dev.nocalhost.plugin.intellij.api.data;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import lombok.Data;

@Data
public class TokenResponse implements Serializable {
    private String token;

    @SerializedName("refresh_token")
    private String refreshToken;
}
