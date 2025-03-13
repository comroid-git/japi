package org.comroid.api.model;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public enum Type {
        Anonymous, UsernamePassword, UsernameToken, OnlyUsername, Token
    }
}
