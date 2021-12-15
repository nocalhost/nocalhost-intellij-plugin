package icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public interface NocalhostIcons {
    Icon Logo = IconLoader.getIcon("/icons/logo.svg", NocalhostIcons.class);
    Icon ConfigurationLogo = IconLoader.getIcon("/icons/configuration-logo.svg", NocalhostIcons.class);

    interface App {
        Icon Connected = IconLoader.getIcon("/icons/app_connected.svg", NocalhostIcons.class);
        Icon Inactive = IconLoader.getIcon("/icons/app_inactive.svg", NocalhostIcons.class);
    }

    interface Status {
        Icon Running = IconLoader.getIcon("/icons/status_running.svg", NocalhostIcons.class);
        Icon Unknown = IconLoader.getIcon("/icons/status_unknown.svg", NocalhostIcons.class);
        Icon Failed = IconLoader.getIcon("/icons/status-failed.svg", NocalhostIcons.class);
        Icon Loading = IconLoader.getIcon("/icons/loading.svg", NocalhostIcons.class);
        Icon DevCopy = IconLoader.getIcon("/icons/dev_copy.svg", NocalhostIcons.class);
        Icon DevCopyWithPortForwarding = IconLoader.getIcon("/icons/dev_copy_port_forwarding.svg", NocalhostIcons.class);
        Icon DevStart = IconLoader.getIcon("/icons/dev_start.svg", NocalhostIcons.class);
        Icon DevEnd = IconLoader.getIcon("/icons/dev_end.svg", NocalhostIcons.class);
        Icon DevOther = IconLoader.getIcon("/icons/dev_other.svg", NocalhostIcons.class);
        Icon DevPortForwarding = IconLoader.getIcon("/icons/dev_port_forwarding.svg", NocalhostIcons.class);
        Icon DevPortForwardingOther = IconLoader.getIcon("/icons/dev_port_forwarding_other.svg", NocalhostIcons.class);
        Icon NormalPortForwarding = IconLoader.getIcon("/icons/normal_port_forwarding.svg", NocalhostIcons.class);
    }

    Icon CloudUpload = IconLoader.getIcon("/icons/cloud_upload.svg", NocalhostIcons.class);

    Icon ClusterActive = IconLoader.getIcon("/icons/cluster_active.svg", NocalhostIcons.class);
    Icon ClusterWarning = IconLoader.getIcon("/icons/cluster_warning.svg", NocalhostIcons.class);

    Icon DevSpace = IconLoader.getIcon("/icons/devspace.svg", NocalhostIcons.class);
    Icon DevSpaceViewer = IconLoader.getIcon("/icons/devspace_viewer.svg", NocalhostIcons.class);

    interface VPN {
        Icon Others = IconLoader.getIcon("/icons/vpn_others.svg", NocalhostIcons.class);
        Icon Healthy = IconLoader.getIcon("/icons/vpn_healthy.svg", NocalhostIcons.class);
        Icon Unhealthy = IconLoader.getIcon("/icons/vpn_unhealthy.svg", NocalhostIcons.class);
        Icon Disconnect = IconLoader.getIcon("/icons/vpn_disconnect.svg", NocalhostIcons.class);
    }
}
