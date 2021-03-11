package dev.nocalhost.plugin.intellij.commands.data;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KubeResource {
    private String kind;
    private Metadata metadata;
    private Spec spec;
    private Status status;

    @Getter
    @Setter
    public static class Metadata {
        private String name;
        private Map<String, String> labels;
        private String deletionTimestamp;
    }

    @Getter
    @Setter
    public static class Spec {
        private List<Container> containers;
        private Selector selector;

        @Getter
        @Setter
        public static class Container {
            private String name;
        }

        @Getter
        @Setter
        public static class Selector {
            private Map<String, String> matchLabels;
        }
    }

    @Getter
    @Setter
    public static class Status {
        private List<Condition> conditions;
        // statefulset
        private int readyReplicas;
        private int replicas;
        private String phase;

        public List<Condition> getConditions() {
            return conditions;
        }

        public void setConditions(List<Condition> conditions) {
            this.conditions = conditions;
        }

        @Getter
        @Setter
        public static class Condition {
            private String status;
            private String type;
        }
    }

    public boolean canSelector() {
        return getStatus() != null && getMetadata() != null
                && StringUtils.equalsIgnoreCase(getStatus().getPhase(), "running") && StringUtils.isBlank(getMetadata().getDeletionTimestamp());
    }
}
