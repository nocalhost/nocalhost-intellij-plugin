package dev.nocalhost.plugin.intellij.utils;

import com.google.common.collect.Maps;
import org.apache.commons.codec.binary.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

public class KubeConfigUtil {
    private static final Path KUBE_CONFIGS_DIR = Paths.get(
            System.getProperty("user.home"),
            ".nh/intellij-plugin/kubeConfigs");

    private static final FileSystem fileSystem = FileSystems.getDefault();
    private static final Set<String> fileAttributeViewsSet = fileSystem.supportedFileAttributeViews();

    private static final FileAttribute<Set<PosixFilePermission>> FILE_MODE = PosixFilePermissions
            .asFileAttribute(PosixFilePermissions.fromString("rw-------"));

    private static final Map<String, Path> kubeConfigPathMap = Maps.newHashMap();

    public static Path kubeConfigPath(String kubeConfig) {
        synchronized (kubeConfigPathMap) {
            try {
                if (!kubeConfigPathMap.containsKey(kubeConfig)) {
                    Path path;
                    while (true) {
                        path = KUBE_CONFIGS_DIR.resolve(UUID.randomUUID().toString() + "_config");
                        if (!Files.exists(path)) {
                            kubeConfigPathMap.put(kubeConfig, path);
                            break;
                        }
                    }
                }
                Path path = kubeConfigPathMap.get(kubeConfig);
                if (!Files.exists(path)) {
                    Files.createDirectories(path.getParent(),getSystemSupportFileAttribute());
                    Files.write(path, kubeConfig.getBytes(StandardCharsets.UTF_8));
                    path.toFile().deleteOnExit();
                } else {
                    String content = new String(Files.readAllBytes(path));
                    if (!StringUtils.equals(content, kubeConfig)) {
                        Files.write(path, kubeConfig.getBytes(StandardCharsets.UTF_8));
                    }
                }
                return path.toAbsolutePath();
            } catch (Exception e) {
                throw new RuntimeException("Preparing kubeconfig file error", e);
            }
        }
    }

    private static FileAttribute<? extends Collection> getSystemSupportFileAttribute() {
        if (fileAttributeViewsSet.contains("acl")) {
            return new FileAttribute<>() {
                @Override
                public String name() {
                    return "acl:acl";
                }

                @Override
                public List<AclEntry> value() {
                    UserPrincipalLookupService userPrincipalLookupService = fileSystem.getUserPrincipalLookupService();
                    UserPrincipal userPrincipal = null;
                    try {
                        userPrincipal = userPrincipalLookupService.lookupPrincipalByName(System.getProperty("user.name"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Set<AclEntryFlag> flags = EnumSet.of(AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT);
                    Set<AclEntryPermission> permissions = EnumSet.of(AclEntryPermission.READ_DATA, AclEntryPermission.WRITE_DATA, AclEntryPermission.EXECUTE);

                    AclEntry.Builder builder = AclEntry.newBuilder();
                    builder.setFlags(flags);
                    builder.setPrincipal(userPrincipal);
                    builder.setPermissions(permissions);
                    builder.setType(AclEntryType.DENY);

                    AclEntry entry = builder.build();
                    List<AclEntry> aclEntryList = new ArrayList<>();
                    aclEntryList.add(entry);

                    return aclEntryList;
                }
            };
        } else if (fileAttributeViewsSet.contains("posix")) {
            return FILE_MODE;
        }
        throw new UnsupportedOperationException("不支持的文件权限操作");
    }
}
