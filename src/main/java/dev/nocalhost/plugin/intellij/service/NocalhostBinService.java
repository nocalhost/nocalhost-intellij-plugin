package dev.nocalhost.plugin.intellij.service;

import com.github.zafarkhaja.semver.Version;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.system.CpuArch;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.exception.NocalhostUnsupportedCpuArchitectureException;
import dev.nocalhost.plugin.intellij.exception.NocalhostUnsupportedOperatingSystemException;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NocalhostBinService {
    private static final HttpUrl NHCTL_BASE_URL = HttpUrl.parse("https://codingcorp-generic.pkg.coding.net/nocalhost/nhctl");
    private static final Pattern NHCTL_VERSION_PATTERN = Pattern.compile("Version\\:\\sv(.+)");

    private final AtomicBoolean nocalhostBinaryDownloading = new AtomicBoolean(false);
    private final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

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
        nocalhostBin = new File(NhctlUtil.binaryPath());
    }

    public void checkBin() {
        boolean shouldDownload = false;
        if (nocalhostBin.exists()) {
            if (!nocalhostBin.canExecute()) {
                nocalhostBin.setExecutable(true);
            }
            try {
                nhctlCommand.version();
            } catch (Exception e) {
                shouldDownload = true;
            }
        } else {
            shouldDownload = true;
        }

        if (shouldDownload) {
            downloadNhctl(nhctlVersion, "Download Nocalhost Command Tool");
        }
    }

    public void checkVersion() {
        try {
            Matcher matcher = NHCTL_VERSION_PATTERN.matcher(nhctlCommand.version());
            if (!matcher.find()) {
                return;
            }

            Version currentVersion = Version.valueOf(matcher.group(1));
            Version requiredVersion = Version.valueOf(nhctlVersion);
            int compare = currentVersion.compareTo(requiredVersion);
            if (compare < 0) {
                downloadNhctl(nhctlVersion, "Upgrade Nocalhost Command Tool");
            }
        } catch (InterruptedException | NocalhostExecuteCmdException | IOException e) {
            NocalhostNotifier.getInstance(project).notifyError("Get nhctl version error", e.getMessage());
        }
    }

    private void downloadNhctl(String version, String title) {
        if (!nocalhostBinaryDownloading.compareAndSet(false, true)) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Modal(project, title, false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                try {
                    startDownload(version, Paths.get(NhctlUtil.binaryDir()), indicator);
                    nocalhostBin.setExecutable(true);
                } catch (IOException e) {
                    NocalhostNotifier.getInstance(project).notifyError("Download nhctl error", e.getMessage());
                } finally {
                    nocalhostBinaryDownloading.set(false);
                }
            }
        });
    }

    private void startDownload(final String version, final Path binDir, final ProgressIndicator indicator) throws IOException {
        final String downloadFilename = getDownloadFilename();
        final HttpUrl url = NHCTL_BASE_URL.newBuilder().addPathSegment(downloadFilename)
                .addQueryParameter("version", "v" + version).build();

        indicator.setText(String.format("downloading %s v%s", downloadFilename, version));

        Files.createDirectories(binDir);

        Request request = new Request.Builder().url(url).build();
        OkHttpClient client = new OkHttpClient();
        Path downloadingPath = binDir.resolve(getDownloadingTempFilename());
        try (FileOutputStream fos = new FileOutputStream(downloadingPath.toFile());
             Response response = client.newCall(request).execute()) {
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

                double fraction = (double) bytesRead / fileSize;
                indicator.setFraction(fraction);
            }
        }
        Path destPath = Paths.get(NhctlUtil.binaryPath());
        Files.deleteIfExists(destPath);
        Files.move(downloadingPath, destPath);
    }

    private String getDownloadingTempFilename() {
        return NhctlUtil.getName() + ".downloading";
    }

    private String getDownloadFilename() {
        String filename = String.format("nhctl-%s-amd64", os());
        if (SystemInfo.isWindows) {
            filename += ".exe";
        }
        return filename;
    }

    private String os() {
        if (SystemInfo.isMac) {
            return "darwin";
        }
        if (SystemInfo.isWindows) {
            return "windows";
        }
        if (SystemInfo.isLinux) {
            return "linux";
        }
        throw new NocalhostUnsupportedOperatingSystemException(SystemInfo.OS_NAME);
    }

    private String arch() {
        switch (CpuArch.CURRENT) {
            case ARM64:
                return "arm64";
            case X86_64:
                return "amd64";
            default:
        }
        throw new NocalhostUnsupportedCpuArchitectureException(SystemInfo.OS_ARCH);
    }
}
