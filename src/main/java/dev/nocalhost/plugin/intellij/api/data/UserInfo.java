package dev.nocalhost.plugin.intellij.api.data;

import lombok.Data;

@Data
public class UserInfo {
    private String email;
    private Long exp;
    private Long iat;
    private Short isAdmin;
    private Long nbf;
    private Long userId;
    private String username;
    private String uuid;

    private String token;
}
