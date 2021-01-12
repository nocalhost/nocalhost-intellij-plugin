package dev.nocalhost.plugin.intellij.commands.data;

import com.google.gson.GsonBuilder;

import java.util.List;

public class KubeResourceList {
    private List<KubeResource> items;

    public List<KubeResource> getItems() {
        return items;
    }

    public void setItems(List<KubeResource> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}
