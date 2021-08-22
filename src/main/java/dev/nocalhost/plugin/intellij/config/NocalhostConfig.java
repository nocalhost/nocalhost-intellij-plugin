package dev.nocalhost.plugin.intellij.config;

import com.google.common.collect.Sets;
import com.google.gson.reflect.TypeToken;

import com.intellij.util.Function;
import com.intellij.util.SystemProperties;

import org.apache.commons.lang3.StringUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;

import dev.nocalhost.plugin.intellij.settings.data.NocalhostAccount;
import dev.nocalhost.plugin.intellij.settings.data.StandaloneCluster;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import lombok.AllArgsConstructor;

public class NocalhostConfig {
    private static final Path CONFIG_FILE_STANDALONE_CLUSTERS = Paths.get(
            SystemProperties.getUserHome(), ".nh", "intellij-plugin", "standaloneClusters.json");
    private static final Path CONFIG_FILE_NOCALHOST_ACCOUNTS = Paths.get(
            SystemProperties.getUserHome(), ".nh", "intellij-plugin", "nocalhostAccounts.json");

    private static final Path LOCK_FILE_STANDALONE_CLUSTERS = Paths.get(
            SystemProperties.getUserHome(), ".nh", "intellij-plugin", "standaloneClusters.lock");
    private static final Path LOCK_FILE_NOCALHOST_ACCOUNTS = Paths.get(
            SystemProperties.getUserHome(), ".nh", "intellij-plugin", "nocalhostAccounts.lock");

    private static final Set<OpenOption> OPEN_OPTIONS = Set.of(
            StandardOpenOption.CREATE, StandardOpenOption.WRITE);

    public void updateStandaloneCluster(final StandaloneCluster standaloneCluster) {
        saveConfig(LOCK_FILE_STANDALONE_CLUSTERS, CONFIG_FILE_STANDALONE_CLUSTERS, StandaloneCluster.class, set -> {
            set.remove(standaloneCluster);
            set.add(standaloneCluster);
            return set;
        });
    }

    public void removeStandaloneCluster(final StandaloneCluster standaloneCluster) {
        saveConfig(LOCK_FILE_STANDALONE_CLUSTERS, CONFIG_FILE_STANDALONE_CLUSTERS, StandaloneCluster.class, set -> {
            set.remove(standaloneCluster);
            return set;
        });
    }

    public Set<StandaloneCluster> getStandaloneClusters() {
        return loadConfig(LOCK_FILE_STANDALONE_CLUSTERS, CONFIG_FILE_STANDALONE_CLUSTERS, StandaloneCluster.class);
    }

    public void updateNocalhostAccount(final NocalhostAccount nocalhostAccount) {
        saveConfig(LOCK_FILE_NOCALHOST_ACCOUNTS, CONFIG_FILE_NOCALHOST_ACCOUNTS, NocalhostAccount.class, set -> {
            set.remove(nocalhostAccount);
            set.add(nocalhostAccount);
            return set;
        });
    }

    public void removeNocalhostAccount(final NocalhostAccount nocalhostAccount) {
        saveConfig(LOCK_FILE_NOCALHOST_ACCOUNTS, CONFIG_FILE_NOCALHOST_ACCOUNTS, NocalhostAccount.class, set -> {
            set.remove(nocalhostAccount);
            return set;
        });
    }

    public Set<NocalhostAccount> getNocalhostAccounts() {
        return loadConfig(LOCK_FILE_NOCALHOST_ACCOUNTS, CONFIG_FILE_NOCALHOST_ACCOUNTS, NocalhostAccount.class);
    }

    private <T> Set<T> loadConfig(Path lockFile, Path configFile, Class<T> type) {
        ConfigFileLock configFileLock = null;

        try {
            configFileLock = lock(lockFile);

            String json = load(configFile);

            Set<T> set = Sets.newHashSet();
            if (StringUtils.isNotEmpty(json)) {
                set = DataUtils.GSON.fromJson(json, TypeToken.getParameterized(Set.class, type).getType());
            }

            return set;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (configFileLock != null) {
                try {
                    configFileLock.fileLock.release();
                    configFileLock.fileChannel.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private <T> void saveConfig(Path lockFile, Path configFile, Class<T> type, Function<Set<T>, Set<T>> func) {
        ConfigFileLock configFileLock = null;
        FileOutputStream fos = null;
        FileLock fl = null;
        PrintWriter pw = null;

        try {
            configFileLock = lock(lockFile);

            String json = load(configFile);

            Set<T> set = Sets.newHashSet();
            if (StringUtils.isNotEmpty(json)) {
                set = DataUtils.GSON.fromJson(json, TypeToken.getParameterized(Set.class, type).getType());
            }
            set = func.fun(set);

            fos = new FileOutputStream(configFile.toFile());
            fl = fos.getChannel().lock();
            pw = new PrintWriter(fos, false, StandardCharsets.UTF_8);
            pw.print(DataUtils.GSON.toJson(set));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (pw != null) {
                pw.flush();
                pw.close();
            }
            if (fl != null) {
                try {
                    fl.release();
                } catch (IOException ignored) {
                }
            }

            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }

            if (configFileLock != null) {
                try {
                    configFileLock.fileLock.release();
                    configFileLock.fileChannel.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private String load(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private ConfigFileLock lock(Path path) throws IOException {
        FileChannel fileChannel = FileChannel.open(path, OPEN_OPTIONS);
        FileLock fileLock = fileChannel.lock();
        return new ConfigFileLock(fileChannel, fileLock);
    }

    @AllArgsConstructor
    private static class ConfigFileLock {
        FileChannel fileChannel;
        FileLock fileLock;
    }
}
