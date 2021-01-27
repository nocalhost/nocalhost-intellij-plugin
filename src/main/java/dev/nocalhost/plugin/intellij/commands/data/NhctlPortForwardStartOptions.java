package dev.nocalhost.plugin.intellij.commands.data;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NhctlPortForwardStartOptions extends NhctlGlobalOptions {
    private boolean daemon;
    private String deployment;
    private Way way;
    private List<String> devPorts;
    private String pod;

    public enum Way {
        MANUAL("manual"),
        DEV_PORTS("devPorts");

        private final String val;

        Way(String val) {
            this.val = val;
        }

        public String getVal() {
            return val;
        }
    }
}
