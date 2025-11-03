package org.comroid.api.net.nextcloud.component;

import lombok.Value;
import org.comroid.annotations.Default;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.net.REST;
import org.comroid.api.net.nextcloud.model.OcsApiComponent;
import org.comroid.api.net.nextcloud.model.OcsApiCore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Value
public class FilesApi extends OcsApiComponent {
    public FilesApi(OcsApiCore ocsApi) {
        super(ocsApi);
    }

    public CompletableFuture<?> mkdirs(String path) {
        var split = path.split("/");
        IntStream.range(0, split.length)
                .mapToObj(i -> split[0] + '/' + IntStream.rangeClosed(1, i)
                        .mapToObj(off -> split[off])
                        .collect(Collectors.joining("/")))
                .forEach(this::mkdir);

        return CompletableFuture.allOf(Arrays.stream(path.split("/"))
                .map(this::mkdir)
                .toArray(CompletableFuture[]::new));
    }

    public CompletableFuture<?> mkdir(String path) {
        return getOcsApi().request(REST.Method.MKCOL,
                        "/remote.php/dav/files/" + getOcsApi().getCredentials().getUsername() + '/' + path)
                .execute()
                .thenApply(REST.Response::validate2xxOK);
    }

    public CompletableFuture<?> upload(String path, InputStream data) throws IOException {
        return getOcsApi().request(REST.Method.PUT,
                        "/remote.php/dav/files/" + getOcsApi().getCredentials().getUsername() + '/' + path)
                .setBody(new DataNode.Value<>(new String(data.readAllBytes(), StandardCharsets.UTF_8)))
                .execute()
                .thenApply(REST.Response::validate2xxOK);
    }

    public CompletableFuture<?> share(
            String path, String group, @Default("31") long permissions,
            @Default("true") boolean download
    ) {
        var queryParams = "?path=%s&shareType=1&shareWith=%s&permissions=%s".formatted(URLEncoder.encode(path,
                        StandardCharsets.UTF_8),
                URLEncoder.encode(group, StandardCharsets.UTF_8),
                URLEncoder.encode(String.valueOf(permissions), StandardCharsets.UTF_8));
        if (download) queryParams += "&=attributes=%5B%7B%22scope%22%3A%22permissions%22%2C%22key%22%3A%22download%22%2C%22value%22%3Atrue%7D%5D";
        return getOcsApi().request(REST.Method.POST, "/ocs/v2.php/apps/files_sharing/api/v1/shares" + queryParams)
                .execute()
                .thenApply(REST.Response::validate2xxOK);
    }
}
