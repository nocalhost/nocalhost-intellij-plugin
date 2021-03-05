package dev.nocalhost.plugin.intellij.exception.report;

import lombok.Data;

@Data
public class Error {
    private String pluginVersion;
    private String intellijVersion;
    private String os;
    private String java;
    private String additionInfo;
    private String exceptionMessage;
    private String exception;

    private Throwable throwable;
}
