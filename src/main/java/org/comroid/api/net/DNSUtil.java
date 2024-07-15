package org.comroid.api.net;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.Optional;

public final class DNSUtil {
    public static Optional<String> getTxtContent(String domain) {
        // based on https://www.inprose.com/content/how-get-dns-txt-record-java/
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");

        try {
            DirContext dirContext = new InitialDirContext(env);
            Attributes attrs = dirContext.getAttributes(domain, new String[]{ "TXT" });
            Attribute  attr  = attrs.get("TXT");

            if (attr != null) {
                return Optional.ofNullable(attr.get().toString());
            }
        } catch (NamingException ignored) {
        }

        return Optional.empty();
    }
}
