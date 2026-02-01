package org.comroid.api.model;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.comroid.api.comp.Base64;
import org.comroid.api.info.Constraint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap;
import java.util.Map;

@Value
@Builder
@NonFinal
public class Authentication {
    public static Authentication ofAnonymous() {
        return Authentication.builder().build();
    }

    public static Authentication ofAnonymous(final @NotNull String username) {
        return Authentication.builder().username(username).build();
    }

    public static Authentication ofLogin(final @NotNull String username, final @NotNull String password) {
        return new Authentication(Type.UsernamePassword, username, password);
    }

    public static Authentication ofToken(final @NotNull String token) {
        return new Authentication(Type.Token, null, token);
    }

    public static Authentication ofToken(final @NotNull String username, final @NotNull String token) {
        return new Authentication(Type.UsernameToken, username, token);
    }

    @Default           Type   type     = Type.Anonymous;
    @Default @Nullable String username = null;
    @Default @Nullable String passkey  = null;

    public Map.Entry<String, String> toHttpBasicHeader() {
        Constraint.notNull(username, "username").run();
        Constraint.notNull(passkey, "passkey").run();

        return new AbstractMap.SimpleImmutableEntry<>("Authorization",
                "Basic " + Base64.encode(username + ':' + passkey));
    }

    public Map.Entry<String, String> toBearerTokenHeader() {
        Constraint.notNull(passkey, "passkey").run();

        return new AbstractMap.SimpleImmutableEntry<>("Authorization", "Bearer " + passkey);
    }

    public enum Type {
        Anonymous, UsernamePassword, UsernameToken, OnlyUsername, Token
    }
}
