package dev.nocalhost.plugin.intellij.api.data;

import org.jetbrains.annotations.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthData {
    @NotNull
    private final String host;
    @NotNull
    private final String email;

    private UserInfo user;

    private String token;

    public AuthData(@NotNull String host, @NotNull String email) {
        this.host = host;
        this.email = email;
    }
}
