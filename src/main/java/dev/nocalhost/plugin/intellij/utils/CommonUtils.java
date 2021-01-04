package dev.nocalhost.plugin.intellij.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.intellij.openapi.diagnostic.Logger;
import dev.nocalhost.plugin.intellij.api.data.UserInfo;

import java.io.IOException;

public class CommonUtils {

    public static final Logger LOG = Logger.getInstance("noclahost");

    public static UserInfo decodedJWT(String token) {
        UserInfo userInfo = null;
        try {
            Algorithm algorithm = Algorithm.HMAC256("secret");
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("auth0")
                    .build();
            DecodedJWT jwt = verifier.verify(token);
            String payload = jwt.getPayload();
            userInfo = JSON.fromJson(payload, UserInfo.class);
        } catch (JWTVerificationException e){
            LOG.error("Decode jwt error, ", e);
        } catch (IOException e) {
            LOG.error("deserializable json error, ", e);
        }
        return userInfo;
    }
}
