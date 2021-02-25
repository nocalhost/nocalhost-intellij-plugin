package dev.nocalhost.plugin.intellij.api.data;

import lombok.Data;

@Data
public class NocalhostApiResponse<T> {

    private long code;
    private String message;
    private T data;
}
