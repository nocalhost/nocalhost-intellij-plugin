package dev.nocalhost.plugin.intellij.service;

import com.github.zafarkhaja.semver.Version;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NocalhostBinService {
    private static final Path NOCALHOST_BIN_PATH = Paths.get(
            System.getProperty("user.home"),
            ".nh/bin/"
    );

    private static final String NHCTL_LINUX_URL = "https://codingcorp-generic.pkg.coding.net/nocalhost/nhctl/nhctl-linux-amd64?version=v%s";
    private static final String NHCTL_MAC_URL = "https://codingcorp-generic.pkg.coding.net/nocalhost/nhctl/nhctl-darwin-amd64?version=v%s";
    private static final String NHCTL_WINDOWS_URL = "https://codingcorp-generic.pkg.coding.net/nocalhost/nhctl/nhctl-windows-amd64.exe?version=v%s";
    private final AtomicBoolean nocalhostBinaryDownloading = new AtomicBoolean(false);
    private final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);

    private final Project project;
    private final String nhctlVersion;
    private final File nocalhostBin;

    public NocalhostBinService(Project project) {
        this.project = project;
        InputStream in = NocalhostBinService.class.getClassLoader().getResourceAsStream("config.properties");
        Properties properties = new Properties();
        try {
            properties.load(in);
        } catch (IOException ignored) {
        }
        nhctlVersion = properties.getProperty("nhctlVersion");
        if (SystemInfo.isWindows) {
            nocalhostBin = new File(NOCALHOST_BIN_PATH.toString() + "/nhctl.exe");
        } else {
            nocalhostBin = new File(NOCALHOST_BIN_PATH.toString() + "/nhctl");
        }
    }

    public void checkBin() {
        if (nocalhostBin.exists()) {
            if (!nocalhostBin.canExecute()) {
                nocalhostBin.setExecutable(true);
            }
            if (StringUtils.isBlank(nocalhostSettings.getNhctlBinary())) {
                nocalhostSettings.setNhctlBinary(nocalhostBin.getAbsolutePath());
            }
        } else {
            downloadNhctl(nhctlVersion);
        }
    }

    public void downloadNhctl(String version) {
        if (!nocalhostBinaryDownloading.compareAndSet(false, true)) {
            return;
        }
        String url;
        if (SystemInfo.isWindows) {
            url = String.format(NHCTL_WINDOWS_URL, version);
        } else if (SystemInfo.isMac) {
            url = String.format(NHCTL_MAC_URL, version);
        } else {
            url = String.format(NHCTL_LINUX_URL, version);
        }

        ProgressManager.getInstance().run(new Task.Modal(project, "Download Nocalhost Command Tool", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                try {
                    download(url, NOCALHOST_BIN_PATH.toString(), indicator);
                    nocalhostBin.setExecutable(true);
                    nocalhostSettings.setNhctlBinary(nocalhostBin.getAbsolutePath());
                } catch (IOException e) {
                    NocalhostNotifier.getInstance(project).notifyError("Download nhctl error", e.getMessage());
                } finally {
                    nocalhostBinaryDownloading.set(false);
                }
            }
        });
    }

    public void checkVersion() {
        final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
        try {
            String versionInfo = nhctlCommand.version();
            String version = "";
            final String[] infos = StringUtils.split(versionInfo, "\n");
            final Optional<String> versionLine = Arrays.stream(infos).filter(s -> s.trim().startsWith("Version")).findFirst();
            if (versionLine.isPresent()) {
                final String[] versionLines = versionLine.get().split("v");
                if (versionLines.length != 2) {
                    return;
                }
                version = versionLines[1];
            }
            if (StringUtils.isBlank(version)) {
                return;
            }
            Version v = Version.valueOf(version);

            Version nhctlV = Version.valueOf(nhctlVersion);
            int compare = v.compareTo(nhctlV);
            if (compare < 0) {
                checkBin();
            } else if (compare > 0) {
                NocalhostNotifier.getInstance(project).notifyVersionTips();
            }
        } catch (InterruptedException | NocalhostExecuteCmdException | IOException e) {
            NocalhostNotifier.getInstance(project).notifyError("Get nhctl version error", e.getMessage());
        }
    }

    public void download(final String url, final String saveDir, final ProgressIndicator indicator) throws IOException {
        String savePath = isExistDir(saveDir);
        Request request = new Request.Builder().url(url).build();
        OkHttpClient client = new OkHttpClient();
        File file = new File(savePath, getName());
        try (FileOutputStream fos = new FileOutputStream(file); Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to download file: " + response);
            }
            String contentLength = response.header("Content-Length");
            long fileSize = Long.parseLong(contentLength);
            byte[] buffer = new byte[1024 * 1024];
            long bytesRead = 0;

            while (bytesRead < fileSize) {
                int len = response.body().byteStream().read(buffer);
                fos.write(buffer, 0, len);
                bytesRead += len;

                double fraction = (double)bytesRead / fileSize;
                indicator.setFraction(fraction);
            }
        }
    }

    private String isExistDir(String saveDir) throws IOException {
        File downloadFile = new File(saveDir);
        if (!downloadFile.mkdirs()) {
            downloadFile.createNewFile();
        }
        return downloadFile.getAbsolutePath();
    }

    private String getName() {
        if (SystemInfo.isWindows) {
            return "nhctl.exe";
        } else {
            return "nhctl";
        }
    }
}
