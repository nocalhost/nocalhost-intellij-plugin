package dev.nocalhost.plugin.intellij.service;

import com.github.zafarkhaja.semver.Version;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.exception.NocalhostUnsupportedCpuArchitectureException;
import dev.nocalhost.plugin.intellij.exception.NocalhostUnsupportedOperatingSystemException;
import dev.nocalhost.plugin.intellij.i18n.NocalhostI18n;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.Request;
import okhttp3.HttpUrl;

public class NocalhostBinService {
    private static final Pattern NHCTL_VERSION_PATTERN = Pattern.compile("Version:\\sv(.+)");

    /** 上一次下载失败的版本号 / 时间戳（毫秒），用于失败后的冷却，避免每次打开项目都重复下载并弹框。 */
    private static final String KEY_DOWNLOAD_FAILED_VERSION = "nhctlDownloadFailedVersion";
    private static final String KEY_DOWNLOAD_FAILED_AT = "nhctlDownloadFailedAt";

    /** 下载失败后的冷却时间。 */
    private static final long DOWNLOAD_FAILURE_COOLDOWN_MILLIS = 6 * 60 * 60 * 1000L;

    private final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);
    private static NocalhostSettings settings = ApplicationManager.getApplication().getService(NocalhostSettings.class);

    /** 随插件发布的 nhctl 版本（{@code config.properties} 里的 {@code nhctlVersion}）。 */
    private final String bundledNhctlVersion;
    private final File nocalhostBin;

    private volatile boolean triedCoding = false;
    private volatile boolean triedGithub = false;

    public NocalhostBinService() {
        InputStream in = NocalhostBinService.class.getClassLoader().getResourceAsStream("config.properties");
        Properties properties = new Properties();
        try {
            properties.load(in);
        } catch (IOException ignore) {
        }
        bundledNhctlVersion = properties.getProperty("nhctlVersion");
        nocalhostBin = new File(NhctlUtil.binaryPath());
    }

    /**
     * 期望安装（或升级）到的 nhctl 版本。
     *
     * <p>默认使用随插件发布的 {@code config.properties: nhctlVersion}；如果该版本在
     * <a href="https://github.com/nocalhost/nocalhost/releases">nocalhost release</a> 里已经不存在
     * （下载始终失败），用户可以在 Settings | Tools | Nocalhost 里填写一个可用版本兜底，
     * 不需要重新安装插件。
     */
    public String getExpectedNhctlVersion() {
        String override = settings.getNhctlVersionOverride();
        if (StringUtils.isNotBlank(override)) {
            return override.trim();
        }
        return bundledNhctlVersion;
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
            tryDownload(getExpectedNhctlVersion(), NocalhostI18n.get("progress.downloadNhctl"));
        }
    }

    public void checkVersion() {
        if (!settings.getCheckNhctlVersion()) {
            return;
        }
        final String expectedVersion = getExpectedNhctlVersion();
        if (StringUtils.isEmpty(expectedVersion)) {
            return;
        }
        try {
            Matcher matcher = NHCTL_VERSION_PATTERN.matcher(nhctlCommand.version());
            if (!matcher.find()) {
                return;
            }

            Version currentVersion = Version.valueOf(matcher.group(1));
            Version requiredVersion = Version.valueOf(expectedVersion);
            int compare = currentVersion.compareTo(requiredVersion);
            if (compare < 0) {
                tryDownload(expectedVersion, NocalhostI18n.get("progress.upgradeNhctl"));
            }
        } catch (InterruptedException | NocalhostExecuteCmdException | IOException | IllegalArgumentException e) {
            Messages.showErrorDialog(e.getMessage(), NocalhostI18n.get("error.getNhctlVersion"));
        }
    }

    private void tryDownload(String version, String title) {
        if (StringUtils.isEmpty(version)) {
            return;
        }
        if (isDownloadCoolingDown(version)) {
            // 不久前已经失败过：继续使用现有的 nhctl，不再重复下载/弹框打扰用户
            return;
        }

        Exception downloadException;
        do {
            try {
                downloadException = null;
                downloadNhctl(version, title);
            } catch (Exception e) {
                downloadException = e;
            }
        } while ((!triedCoding || !triedGithub) && downloadException != null);

        if (downloadException == null) {
            clearDownloadFailure(version);
            return;
        }
        if (triedCoding || triedGithub) {
            rememberDownloadFailure(version);
            final Exception finalDownloadException = downloadException;
            ApplicationManager.getApplication().invokeLater(() ->
                    Messages.showErrorDialog(buildDownloadFailureMessage(version, finalDownloadException),
                            NocalhostI18n.get("error.downloadNhctl")));
        }
    }

    private String buildDownloadFailureMessage(String version, Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(NocalhostI18n.format("error.downloadNhctl.message", version));
        sb.append(System.lineSeparator()).append(System.lineSeparator());
        sb.append(NocalhostI18n.get("error.downloadNhctl.hint"));
        if (StringUtils.isNotBlank(e.getMessage())) {
            sb.append(System.lineSeparator()).append(System.lineSeparator()).append(e.getMessage());
        }
        return sb.toString();
    }

    private static boolean isDownloadCoolingDown(String version) {
        String failedVersion = settings.get(KEY_DOWNLOAD_FAILED_VERSION);
        if (!StringUtils.equals(version, failedVersion)) {
            return false;
        }
        String failedAt = settings.get(KEY_DOWNLOAD_FAILED_AT);
        if (StringUtils.isEmpty(failedAt)) {
            return false;
        }
        try {
            return System.currentTimeMillis() - Long.parseLong(failedAt) < DOWNLOAD_FAILURE_COOLDOWN_MILLIS;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void rememberDownloadFailure(String version) {
        settings.set(KEY_DOWNLOAD_FAILED_VERSION, version);
        settings.set(KEY_DOWNLOAD_FAILED_AT, String.valueOf(System.currentTimeMillis()));
    }

    private static void clearDownloadFailure(String version) {
        if (StringUtils.equals(version, settings.get(KEY_DOWNLOAD_FAILED_VERSION))) {
            settings.del(KEY_DOWNLOAD_FAILED_VERSION);
            settings.del(KEY_DOWNLOAD_FAILED_AT);
        }
    }

    private HttpUrl pickDownloadUrl(String version) {
        if (!triedCoding) {
            triedCoding = true;
            return nhctlCodingDownloadUrl(version);
        }
        if (!triedGithub) {
            triedGithub = true;
            return nhctlGithubDownloadUrl(version);
        }
        return null;
    }

    private void downloadNhctl(String version, String title) throws Exception {
        ProgressManager.getInstance().run(new Task.WithResult<Void, Exception>(null, title, false) {
            @Override
            protected Void compute(@NotNull ProgressIndicator indicator) throws Exception {
                indicator.setIndeterminate(false);
                try {
                    startDownload(version, Paths.get(NhctlUtil.binaryDir()), indicator);
                } catch (Throwable t) {
                    throw new Exception(t.getMessage());
                }
                nocalhostBin.setExecutable(true);
                return null;
            }
        });
    }

    private void startDownload(final String version, final Path binDir, final ProgressIndicator indicator) throws IOException {
        final String downloadFilename = getDownloadFilename();
        final HttpUrl url = pickDownloadUrl(version);
        if (url == null) {
            return;
        }

        indicator.setText(NocalhostI18n.format("progress.downloading", downloadFilename, version));
        indicator.setText2(NocalhostI18n.get("common.from") + " " + url.host());

        Files.createDirectories(binDir);

        Request request = new Request.Builder().url(url).build();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        Path downloadingPath = binDir.resolve(getDownloadingTempFilename());

        FileOutputStream fos = null;
        FileLock fl = null;
        Response response = null;
        try {
            fos = new FileOutputStream(downloadingPath.toFile());
            fl = fos.getChannel().tryLock();
            if (fl == null) {
                return;
            }

            response = client.newCall(request).execute();

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
        } finally {
            if (fl != null) {
                try {
                    fl.release();
                } catch (IOException ignore) {
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignore) {
                }
            }
            if (response != null) {
                response.close();
            }
        }

        var destPath = Paths.get(NhctlUtil.binaryPath());
        if (SystemInfo.isWindows) {
            if (Files.exists(destPath)) {
                var tempPath = Paths.get(NhctlUtil.binaryPath() + "." + System.currentTimeMillis());
                Files.move(destPath, tempPath, StandardCopyOption.REPLACE_EXISTING);
                tempPath.toFile().deleteOnExit();
            }
            Files.move(downloadingPath, destPath);
            return;
        }
        Files.deleteIfExists(destPath);
        Files.move(downloadingPath, destPath);
    }

    private String getDownloadingTempFilename() {
        return NhctlUtil.getName() + ".downloading";
    }

    private static String getDownloadFilename() {
        if (SystemInfo.isMac) {
           return "nhctl-darwin-" + arch();
        }
        if (SystemInfo.isLinux) {
            return "nhctl-linux-" + arch();
        }
        if (SystemInfo.isWindows) {
            return "nhctl-windows-amd64.exe";
        }
        throw new NocalhostUnsupportedOperatingSystemException(SystemInfo.OS_NAME);
    }

    private static String arch() {
        switch (SystemInfo.OS_ARCH) {
            case "aarch64":
            case "arm64":
                return "arm64";
            case "x86_64":
            case "amd64":
                return "amd64";
            default:
        }
        throw new NocalhostUnsupportedCpuArchitectureException(SystemInfo.OS_ARCH);
    }

    private static HttpUrl nhctlCodingDownloadUrl(String version) {
        return HttpUrl.parse("https://nocalhost-generic.pkg.coding.net/nocalhost/nhctl")
                .newBuilder()
                .addPathSegment(getDownloadFilename())
                .addQueryParameter("version", "v" + version)
                .build();
    }

    private static HttpUrl nhctlGithubDownloadUrl(String version) {
        return HttpUrl.parse("https://github.com/nocalhost/nocalhost/releases/download")
                .newBuilder()
                .addPathSegment("v" + version)
                .addPathSegment(getDownloadFilename())
                .build();
    }
}
