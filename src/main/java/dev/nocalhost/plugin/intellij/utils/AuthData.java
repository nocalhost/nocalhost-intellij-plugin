package dev.nocalhost.plugin.intellij.utils;

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
    @NotNull
    private final String token;

    public AuthData(@NotNull String host, @NotNull String email, @NotNull String token) {
        this.host = host;
        this.email = email;
        this.token = token;
    }
}
