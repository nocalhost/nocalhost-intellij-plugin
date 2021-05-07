package dev.nocalhost.plugin.intellij.utils;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;

public final class TokenUtil {
    public static boolean isValid(String jwt) {
        if (!StringUtils.isNotEmpty(jwt)) {
            return false;
        }

        String[] parts = jwt.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT");
        }
        String json = new String(Base64.getDecoder().decode(parts[1]));
        Token token = DataUtils.GSON.fromJson(json, Token.class);
        return System.currentTimeMillis() < token.getExp() * 1000;
    }

    public static String expiredAt(String jwt) {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT");
        }
        String json = new String(Base64.getDecoder().decode(parts[1]));
        Token token = DataUtils.GSON.fromJson(json, Token.class);
        Date date = new Date(token.getExp() * 1000);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(date);
    }

    @Getter
    @Setter
    private class Token {
        private long exp;
    }
}
