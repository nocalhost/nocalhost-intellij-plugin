package dev.nocalhost.plugin.intellij.helpers;

import com.google.common.collect.Lists;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;

import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.Optional;

import dev.nocalhost.plugin.intellij.commands.data.AliveDeployment;

public class UserDataKeyHelper {

    public static final Key<List<AliveDeployment>> ALIVE_DEPLOYMENTS = Key.create("ALIVE_DEPLOYMENTS");

    public static void addAliveDeployments(Project project, AliveDeployment aliveDeployment) {
        List<AliveDeployment> aliveDeployments = project.getUserData(UserDataKeyHelper.ALIVE_DEPLOYMENTS);
        if (CollectionUtils.isNotEmpty(aliveDeployments) && !aliveDeployments.contains(aliveDeployment)) {
            aliveDeployments.add(aliveDeployment);
        } else {
            aliveDeployments = Lists.newArrayList(aliveDeployment);
        }
        project.putUserData(ALIVE_DEPLOYMENTS, aliveDeployments);
    }

    public static void removeAliveDeployments(Project project, AliveDeployment aliveDeployment) {
        List<AliveDeployment> aliveDeployments = project.getUserData(UserDataKeyHelper.ALIVE_DEPLOYMENTS);
        if (CollectionUtils.isNotEmpty(aliveDeployments)) {
            aliveDeployments.remove(aliveDeployment);
        }
        project.putUserData(ALIVE_DEPLOYMENTS, aliveDeployments);
    }

    public static List<AliveDeployment> findAliveDeploymentsByProject(Project project) {
        List<AliveDeployment> aliveDeployments = project.getUserData(UserDataKeyHelper.ALIVE_DEPLOYMENTS);
        if (CollectionUtils.isEmpty(aliveDeployments)) {
            return null;
        }
        Optional<AliveDeployment> aliveDeploymentOptional = aliveDeployments.stream().filter(d -> d.getProjectPath().equals(project.getBasePath())).findAny();
        if (aliveDeploymentOptional.isEmpty()) {
            return null;
        }
        return Lists.newArrayList(aliveDeploymentOptional.stream().iterator());
    }
}
