package dev.nocalhost.plugin.intellij.api.data;

import java.io.Serializable;

import lombok.Data;

@Data
public class LoginResponse implements Serializable {
    private String token;
}
