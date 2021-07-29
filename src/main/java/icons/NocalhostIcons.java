package icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public interface NocalhostIcons {
    Icon Logo = IconLoader.getIcon("/icons/logo-light.svg", NocalhostIcons.class);
    Icon ConfigurationLogo = IconLoader.getIcon("/icons/configuration-logo.svg", NocalhostIcons.class);

    interface App {
        Icon Connected = IconLoader.getIcon("/icons/app-connected.svg", NocalhostIcons.class);
        Icon Inactive = IconLoader.getIcon("/icons/app-inactive.svg", NocalhostIcons.class);
    }

    interface Status {
        Icon Normal = IconLoader.getIcon("/icons/status-normal.svg", NocalhostIcons.class);
        Icon Running = IconLoader.getIcon("/icons/status-running.svg", NocalhostIcons.class);
        Icon Unknown = IconLoader.getIcon("/icons/status-unknown.svg", NocalhostIcons.class);
        Icon Failed = IconLoader.getIcon("/icons/status-failed.svg", NocalhostIcons.class);
        Icon Loading = IconLoader.getIcon("/icons/loading.svg", NocalhostIcons.class);
        Icon DevStart = IconLoader.getIcon("/icons/dev-start.svg", NocalhostIcons.class);
        Icon DevEnd = IconLoader.getIcon("/icons/dev-end.svg", NocalhostIcons.class);
        Icon DevOther = IconLoader.getIcon("/icons/dev-other.svg", NocalhostIcons.class);
        Icon DevPortForwarding = IconLoader.getIcon("/icons/dev-port-forwarding.svg", NocalhostIcons.class);
        Icon DevPortForwardingOther = IconLoader.getIcon("/icons/dev-port-forwarding-other.svg", NocalhostIcons.class);
        Icon NormalPortForwarding = IconLoader.getIcon("/icons/normal-port-forwarding.svg", NocalhostIcons.class);
    }

    Icon Kubernetes = IconLoader.getIcon("/icons/kubernetes.svg", NocalhostIcons.class);
}
