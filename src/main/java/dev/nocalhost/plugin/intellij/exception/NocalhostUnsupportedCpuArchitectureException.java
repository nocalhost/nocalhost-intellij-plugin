package dev.nocalhost.plugin.intellij.exception;

import dev.nocalhost.plugin.intellij.i18n.NocalhostI18n;

public class NocalhostUnsupportedCpuArchitectureException extends RuntimeException {
    private String arch;

    public NocalhostUnsupportedCpuArchitectureException(String arch) {
        this.arch = arch;
    }

    @Override
    public String getMessage() {
        return NocalhostI18n.format("error.unsupportedArch", arch);
    }
}
