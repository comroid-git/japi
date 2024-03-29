package org.comroid.api.net;

import lombok.experimental.UtilityClass;

import java.net.InetAddress;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

import static org.comroid.api.func.exc.ThrowingFunction.sneaky;

@UtilityClass
public class UtilApi {
    private CompletableFuture<REST.Response> req(String url) {
        return REST.request(REST.Method.GET, url)
                .execute()
                .thenApply(REST.Response::validate2xxOK);
    }

    public CompletableFuture<InetAddress> myIP() {
        return req("https://api.comroid.org/myIP")
                .thenApply(rsp -> rsp.getBody().asString())
                .thenApply(sneaky(InetAddress::getByName));
    }

    public CompletableFuture<String> md5(URL url) {
        return req("https://api.comroid.org/md5?url=" + url)
                .thenApply(rsp -> rsp.getBody().asString());
    }
}
