package dev.nocalhost.plugin.intellij.api.data;

import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class UserInfo {
    private String email;
    private Long exp;
    private Long iat;
    @SerializedName("is_admin")
    private Short isAdmin;
    private Long nbf;
    @SerializedName("user_id")
    private Long userId;
    private String username;
    private String uuid;
}
