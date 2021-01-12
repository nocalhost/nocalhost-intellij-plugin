package dev.nocalhost.plugin.intellij.api.data;

import lombok.Data;

@Data
public class UserInfo {
    private long id;
    private String name;
    private String username;
    private long phone;
    private String email;
    private boolean isAdmin;
    private int state;
    private String avatar;
}
