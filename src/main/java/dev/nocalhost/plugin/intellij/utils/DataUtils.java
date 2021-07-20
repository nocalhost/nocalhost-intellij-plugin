package dev.nocalhost.plugin.intellij.utils;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.util.EnvironmentUtil;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.Set;

import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;

public class DataUtils {
    public static final Gson GSON = new Gson();

    public static final Yaml YAML;

    static {
        Representer representer = new Representer() {
            //ignore null properties
            @Override
            protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
                // if value of property is null, ignore it.
                if (propertyValue == null) {
                    return null;
                } else {
                    return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
                }
            }

            //Don't print the class definition
            @Override
            protected MappingNode representJavaBean(Set<Property> properties, Object javaBean) {
                if (!classTags.containsKey(javaBean.getClass())) {
                    addClassTag(javaBean.getClass(), Tag.MAP);
                }

                return super.representJavaBean(properties, javaBean);
            }
        };
        representer.getPropertyUtils().setSkipMissingProperties(true);

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        YAML = new Yaml(representer, dumperOptions);
    }

    public static <T> T fromYaml(String yaml, Type typeOfT) throws Exception {
        GeneralCommandLine commandLine = new GeneralCommandLine(
                Lists.newArrayList(NhctlUtil.binaryPath(), "yaml", "to-json")
        ).withEnvironment(EnvironmentUtil.getEnvironmentMap()).withRedirectErrorStream(true);
        Process process = commandLine.createProcess();

        PrintWriter out = new PrintWriter(process.getOutputStream(), false, Charsets.UTF_8);
        out.write(yaml);
        out.flush();
        out.close();

        String output = CharStreams.toString(new InputStreamReader(process.getInputStream(),
                Charsets.UTF_8));
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new NocalhostExecuteCmdException(commandLine.getCommandLineString(), exitCode,
                    output);
        }
        return GSON.fromJson(output, typeOfT);
    }

    public static String toYaml(Object src) throws Exception {
        GeneralCommandLine commandLine = new GeneralCommandLine(
                Lists.newArrayList(NhctlUtil.binaryPath(), "yaml", "from-json")
        ).withEnvironment(EnvironmentUtil.getEnvironmentMap()).withRedirectErrorStream(true);
        Process process = commandLine.createProcess();

        PrintWriter out = new PrintWriter(process.getOutputStream(), false, Charsets.UTF_8);
        out.write(GSON.toJson(src));
        out.flush();
        out.close();

        String output = CharStreams.toString(new InputStreamReader(process.getInputStream(),
                Charsets.UTF_8));
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new NocalhostExecuteCmdException(commandLine.getCommandLineString(), exitCode,
                    output);
        }
        return output;
    }
}
