package dev.nocalhost.plugin.intellij.utils;

import com.google.common.collect.ImmutableSet;

public class Constants {

    public static final String DEFAULT_APPLICATION_NAME = "default.application";

    public static final String MANIFEST_TYPE_RAW_MANIFEST = "rawManifest";
    public static final String MANIFEST_TYPE_RAW_MANIFEST_GIT = "rawManifestGit";
    public static final String MANIFEST_TYPE_RAW_MANIFEST_LOCAL = "rawManifestLocal";
    public static final String MANIFEST_TYPE_HELM_REPO = "helmRepo";
    public static final String MANIFEST_TYPE_HELM_GIT = "helmGit";
    public static final String MANIFEST_TYPE_HELM_LOCAL = "helmLocal";
    public static final String MANIFEST_TYPE_KUSTOMIZE_GIT = "kustomizeGit";
    public static final String MANIFEST_TYPE_KUSTOMIZE_LOCAL = "kustomizeLocal";

    public static final String WORKLOAD_TYPE_DEPLOYMENT = "deployment";
    public static final String WORKLOAD_TYPE_STATEFULSET = "statefulset";
    public static final String WORKLOAD_TYPE_DAEMONSET = "daemonset";
    public static final String WORKLOAD_TYPE_JOB = "job";
    public static final String WORKLOAD_TYPE_CRONJOB = "cronjob";
    public static final String WORKLOAD_TYPE_POD = "pod";

    public static final ImmutableSet<String> ALL_WORKLOAD_TYPES = ImmutableSet.of(
            WORKLOAD_TYPE_DEPLOYMENT,
            WORKLOAD_TYPE_STATEFULSET,
            WORKLOAD_TYPE_DAEMONSET,
            WORKLOAD_TYPE_JOB,
            WORKLOAD_TYPE_CRONJOB,
            WORKLOAD_TYPE_POD
    );

    public static final String DEMO_NAME = "bookinfo";

    public static final String SPACE_OWN_TYPE_VIEWER = "Viewer";

    public static final String PRIVILEGE_TYPE_CLUSTER_ADMIN = "CLUSTER_ADMIN";
    public static final String PRIVILEGE_TYPE_CLUSTER_VIEWER = "CLUSTER_VIEWER";

    public static final String DEVELOP_STATUS_STARTED = "STARTED";
    public static final String DEVELOP_STATUS_STARTING = "STARTING";
    public static final String DEVELOP_STATUS_NONE = "NONE";

    public static final String DEV_MODE_DUPLICATE = "duplicate";
}
