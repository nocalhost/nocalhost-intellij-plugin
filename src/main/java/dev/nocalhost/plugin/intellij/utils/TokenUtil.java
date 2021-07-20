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

        Token token = decodeToken(jwt);
        return System.currentTimeMillis() < token.getExp() * 1000;
    }

    public static boolean needRefresh(String jwt) {
        if (!StringUtils.isNotEmpty(jwt)) {
            return true;
        }

        Token token = decodeToken(jwt);
        return (token.getExp() - 2 * 3600) * 1000 < System.currentTimeMillis(); // less than 2 hours
    }

    public static String expiredAt(String jwt) {
        Token token = decodeToken(jwt);
        Date date = new Date(token.getExp() * 1000);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(date);
    }

    private static Token decodeToken(String jwt) {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT");
        }
        String json = new String(Base64.getDecoder().decode(parts[1]));
        return DataUtils.GSON.fromJson(json, Token.class);
    }

    @Getter
    @Setter
    private class Token {
        private long exp;
    }
}
