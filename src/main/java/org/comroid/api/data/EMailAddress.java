package org.comroid.api.data;

import org.comroid.api.Polyfill;
import org.comroid.api.func.WrappedFormattable;

import java.net.URI;

public final class EMailAddress implements CharSequence, WrappedFormattable {
    public static EMailAddress parse(String parse) {
        parse = parse.replace("%40", "@");
        int index = parse.indexOf('@');

        String user   = parse.substring(0, index);
        String domain = parse.substring(index + 1);

        return new EMailAddress(user, domain);
    }

    private final String user;
    private final String domain;
    private final String string;

    public EMailAddress(String user, String domain) {
        this.user   = user;
        this.domain = domain;
        this.string = user + '@' + domain;
    }

    public String getUser() {
        return user;
    }

    public URI getDomainAsURI() {
        return Polyfill.uri("http://" + getDomain());
    }

    public String getDomain() {
        return domain;
    }

    public URI getMailtoURI() {
        return Polyfill.uri("mailto:" + this);
    }

    @Override
    public String getPrimaryName() {
        return toString();
    }

    @Override
    public String getAlternateName() {
        return String.format("EMailAddress{user=%s, domain=%s}", user, domain);
    }

    public int hashCode() {
        return string.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof EMailAddress) {
            EMailAddress other = (EMailAddress) o;
            return other.string.equals(string);
        } else return false;
    }

    @Override
    public String toString() {
        return string;
    }

    @Override
    public int length() {
        return string.length();
    }

    @Override
    public char charAt(int index) {
        return string.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return string.subSequence(start, end);
    }
}
