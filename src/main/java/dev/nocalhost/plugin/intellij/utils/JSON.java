package dev.nocalhost.plugin.intellij.utils;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import dev.nocalhost.plugin.intellij.exception.NocalhostJsonException;

public class JSON {


    @NotNull
    private static Gson getGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
//        builder.registerTypeAdapterFactory(NullCheckingFactory.INSTANCE);
        return builder.create();
    }


    public static Map fromJson(String json) {
        return getGson().fromJson(json, Map.class);
    }

    @NotNull
    public static <T> T fromJson(@Nullable JsonElement json, @NotNull Class<T> classT) throws IOException {
        if (json == null) {
            throw new NocalhostJsonException("Unexpected empty response");
        }
        T res;
        try {
            //cast as workaround for early java 1.6 bug
            //noinspection RedundantCast
            res = (T) getGson().fromJson(json, classT);
        } catch (ClassCastException | JsonParseException e) {
            throw new NocalhostJsonException("Parse exception while converting JSON to object " + classT.toString(), e);
        }
        if (res == null) {
            throw new NocalhostJsonException("Empty Json response");
        }
        return res;
    }

    public static <T> T fromJson(@Nullable String json, @NotNull Class<T> classT) throws IOException {
        if (StringUtils.isBlank(json)) {
            throw new NocalhostJsonException("Unexpected empty response");
        }
        T res;
        try {
            //cast as workaround for early java 1.6 bug
            //noinspection RedundantCast
            res = (T) getGson().fromJson(json, classT);
        } catch (ClassCastException | JsonParseException e) {
            throw new NocalhostJsonException("Parse exception while converting JSON to object " + classT.toString(), e);
        }
        if (res == null) {
            throw new NocalhostJsonException("Empty Json response");
        }
        return res;
    }

    public static String toJson(@Nullable Object obj) {
        return getGson().toJson(obj);
    }

    public static <T> T fromJson(JsonElement json, Type listType) throws NocalhostJsonException {
        if (json == null) {
            throw new NocalhostJsonException("Unexpected empty response");
        }
        T res;
        try {
            //cast as workaround for early java 1.6 bug
            //noinspection RedundantCast
            res = getGson().fromJson(json, listType);
        } catch (ClassCastException | JsonParseException e) {
            throw new NocalhostJsonException("Parse exception while converting JSON to object " + Type.class.getName(), e);
        }
        if (res == null) {
            throw new NocalhostJsonException("Empty Json response");
        }
        return res;
    }
}
