package dev.nocalhost.plugin.intellij.settings.data;

import com.google.common.base.Objects;

import dev.nocalhost.plugin.intellij.api.data.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NocalhostAccount {
    private final String server;
    private final String username;
    private String jwt;
    private UserInfo userInfo;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NocalhostAccount that = (NocalhostAccount) o;
        return Objects.equal(server, that.server) && Objects.equal(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(server, username);
    }
}
