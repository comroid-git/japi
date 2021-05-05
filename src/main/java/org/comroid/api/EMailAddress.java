package org.comroid.api;

import java.net.URI;

public final class EMailAddress implements CharSequence, WrappedFormattable {
    private final String user;
    private final String domain;
    private final String string;

    public String getUser() {
        return user;
    }

    public String getDomain() {
        return domain;
    }

    public URI getDomainAsURI() {
        return Polyfill.uri("http://" + getDomain());
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

    public EMailAddress(String user, String domain) {
        this.user = user;
        this.domain = domain;
        this.string = user + '@' + domain;
    }

    public static EMailAddress parse(String parse) {
        int index = parse.indexOf('@');

        String user = parse.substring(0, index);
        String domain = parse.substring(index + 1);

        return new EMailAddress(user, domain);
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

    @Override
    public String toString() {
        return string;
    }
}
