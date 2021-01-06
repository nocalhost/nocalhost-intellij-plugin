package dev.nocalhost.plugin.intellij.api;

import com.google.gson.JsonElement;

import java.io.Serializable;

import lombok.Data;

@Data
public class NocalhostResponseBody implements Serializable {

    private long code;
    private String message;
    private JsonElement data;
}
