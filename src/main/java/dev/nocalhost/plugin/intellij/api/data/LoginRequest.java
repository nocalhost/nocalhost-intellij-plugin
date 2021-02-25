package dev.nocalhost.plugin.intellij.api.data;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

import lombok.Data;

@Data
public class LoginRequest implements Serializable {
    @NotNull
    private String email;
    @NotNull
    private String password;
    private String from = "plugin";

    public LoginRequest(@NotNull String email, @NotNull String password) {
        this.email = email;
        this.password = password;
    }
}
