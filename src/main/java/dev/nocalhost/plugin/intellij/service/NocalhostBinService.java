package dev.nocalhost.plugin.intellij.service;

import com.github.zafarkhaja.semver.Version;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.exception.NocalhostUnsupportedCpuArchitectureException;
import dev.nocalhost.plugin.intellij.exception.NocalhostUnsupportedOperatingSystemException;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NocalhostBinService {
    private static final Pattern NHCTL_VERSION_PATTERN = Pattern.compile("Version:\\sv(.+)");

    private final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);

    private final String nhctlVersion;
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
            tryDownload(nhctlVersion, "Download Nocalhost Command Tool");
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
                tryDownload(nhctlVersion, "Upgrade Nocalhost Command Tool");
            }
        } catch (InterruptedException | NocalhostExecuteCmdException | IOException e) {
            Messages.showErrorDialog(e.getMessage(), "Get nhctl Version Error");
        }
    }

    private void tryDownload(String version, String title) {
        Exception downloadException;
        do {
            try {
                downloadException = null;
                downloadNhctl(version, title);
            } catch (Exception e) {
                downloadException = e;
            }
        } while ((!triedCoding || !triedGithub) && downloadException != null);
        if (triedCoding && triedCoding && downloadException != null) {
            final Exception finalDownloadException = downloadException;
            ApplicationManager.getApplication().invokeLater(() ->
                    Messages.showErrorDialog(finalDownloadException.getMessage(), "Download nhctl Error"));
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

    private Void downloadNhctl(String version, String title) throws Exception {
        return ProgressManager.getInstance().run(new Task.WithResult<Void, Exception>(null, title, false) {
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

    private void startDownload(final String version, final Path binDir, final ProgressIndicator indicator) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        final String downloadFilename = getDownloadFilename();
        final HttpUrl url = pickDownloadUrl(version);
        if (url == null) {
            return;
        }

        indicator.setText(String.format("downloading %s v%s", downloadFilename, version));
        indicator.setText2(String.format("from " + url.host()));

        Files.createDirectories(binDir);

        var x509 = new X509TrustManagerImpl();
        var ssl = SSLContext.getInstance("SSL");
        ssl.init(null, new TrustManager[]{x509}, new java.security.SecureRandom());

        Request request = new Request.Builder().url(url).build();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .sslSocketFactory(ssl.getSocketFactory(), x509)
                .hostnameVerifier((hostname, session) -> true)
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
        Path destPath = Paths.get(NhctlUtil.binaryPath());
        if (SystemInfo.isWindows) {
            Path tempPath = Paths.get(NhctlUtil.binaryPath() + ".temp");
            if (Files.exists(destPath)) {
                Files.move(destPath, tempPath);
            }
            Files.move(downloadingPath, destPath);
            Files.deleteIfExists(tempPath);
        } else {
            Files.deleteIfExists(destPath);
            Files.move(downloadingPath, destPath);
        }
    }

    private String getDownloadingTempFilename() {
        return NhctlUtil.getName() + ".downloading";
    }

    private static String getDownloadFilename() {
        String filename = String.format("nhctl-%s-amd64", os());
        if (SystemInfo.isWindows) {
            filename += ".exe";
        }
        return filename;
    }

    private static String os() {
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

    private class X509TrustManagerImpl implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
