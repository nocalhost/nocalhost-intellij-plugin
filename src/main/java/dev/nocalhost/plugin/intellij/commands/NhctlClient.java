package dev.nocalhost.plugin.intellij.commands;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;

public class NhctlClient extends AbstractClient {
    private static final String NHCTL_COMMAND = "/usr/local/bin/nhctl";

    public NhctlClient() {
    }

    public String version() throws IOException {
        String[] command = new String[] {
                NHCTL_COMMAND,
                "version"
        };
        ProcessBuilder processBuilder = createProcessBuilder(command);
        Process process = processBuilder.start();
        InputStream in = process.getInputStream();

        List<String> inStr = IOUtils.readLines(in, Charset.defaultCharset());
        return StringUtils.join(inStr, "\n");
    }

    public String install(DevSpace devSpace) throws IOException {
        DevSpace.Context context = devSpace.getContext();
        String resourcePath = Arrays.stream(context.getResourceDir()).map(dir -> "--resource-path " + dir).collect(Collectors.joining(" "));
        // TODO: helm values.yaml
        String[] command = new String[] {
                NHCTL_COMMAND,
                "install",
                context.getApplicationName(),
                "-u",
                context.getApplicationUrl(),
                "-t",
                context.getInstallType(),
                resourcePath
        };
        ProcessBuilder processBuilder = createProcessBuilder(command);
        Process process = processBuilder.start();
        InputStream in = process.getInputStream();

        List<String> inStr = IOUtils.readLines(in, Charset.defaultCharset());
        return StringUtils.join(inStr, "\n");
    }
}
