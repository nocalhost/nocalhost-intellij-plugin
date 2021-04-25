package dev.nocalhost.plugin.intellij.utils;

import com.intellij.openapi.components.ServiceManager;

import org.apache.commons.lang3.StringUtils;

import java.util.Base64;

import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import lombok.Getter;
import lombok.Setter;

public final class TokenUtil {
    public static boolean isTokenValid() {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(
                NocalhostSettings.class);
        String jwt = nocalhostSettings.getJwt();
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

    @Getter
    @Setter
    private class Token {
        private long exp;
    }
}
