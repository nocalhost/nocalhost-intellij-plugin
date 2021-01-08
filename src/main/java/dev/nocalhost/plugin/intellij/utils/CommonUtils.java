package dev.nocalhost.plugin.intellij.utils;

import com.google.inject.Inject;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.AuthData;
import dev.nocalhost.plugin.intellij.api.data.UserInfo;

public class CommonUtils {

    @Inject
    private Logger log;
    @Inject
    private NocalhostApi nocalhostApi;

    public UserInfo decodedJWT(String token) {
        UserInfo userInfo = null;
        try {
            DecodedJWT jwt = JWT.decode(token);
            userInfo = new UserInfo();
            userInfo.setEmail(jwt.getClaim("email").asString());
            userInfo.setUserId(jwt.getClaim("user_id").asLong());
            userInfo.setExp(jwt.getClaim("exp").asLong());
            userInfo.setIat(jwt.getClaim("iat").asLong());
            userInfo.setNbf(jwt.getClaim("nbf").asLong());
            userInfo.setIsAdmin(jwt.getClaim("is_admin").as(Short.class));
            userInfo.setUuid(jwt.getClaim("uuid").asString());
            userInfo.setUsername(jwt.getClaim("username").asString());
        } catch (JWTVerificationException e){
            log.error("Decode jwt error, ", e);
        }
        return userInfo;
    }

    public String checkCredentials(Project project, AuthData authData, String password) {
        return nocalhostApi.login(authData, password);
    }

    public String getErrorTextFromException(Throwable t) {
        String message = t.getMessage();
        if (message == null) {
            message = "(No exception message available)";
            log.error(message, t);
        }
        return message;
    }
}
