package dev.nocalhost.plugin.intellij.exception;

public class NocalhostApiException extends Exception {

    private final String api;
    private final String action;
    private final int code;
    private final String msg;

    public NocalhostApiException(String api, String action, int code, String msg) {
        this.api = api;
        this.action = action;
        this.code = code;
        this.msg = msg;
    }

    @Override
    public String getMessage() {
        return String.format("Failed to access API [%s] to %s, Response Code: [%d]\n%s", api, action, code, msg);
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
