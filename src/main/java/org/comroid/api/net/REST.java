package org.comroid.api.net;

import lombok.*;
import lombok.experimental.NonFinal;
import org.comroid.api.attr.Named;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.data.seri.MimeType;
import org.comroid.api.Polyfill;
import org.comroid.api.data.seri.Serializer;
import org.comroid.api.func.util.Cache;
import org.comroid.api.info.Constraint;
import org.comroid.api.data.seri.adp.Jackson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.IntStream;

@Data
@AllArgsConstructor
public final class REST {
    public static final REST Default = new REST(
            Jackson.JSON,
            new Cache<>(Duration.ofMinutes(10)),
            new Function<>() {
                private final HttpClient client = HttpClient.newHttpClient();

                @Override
                public CompletableFuture<Response> apply(Request request) {
                    var pub = List.of(Method.GET, Method.OPTIONS, Method.TRACE).contains(request.method)
                            || request.body == null
                            ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofString(request.body.toString());
                    var req = HttpRequest.newBuilder()
                            .uri(request.uri)
                            .method(request.method.name(), pub);
                    request.headers.forEach(req::header);
                    req.header("Content-Type", "application/json");
                    var res = HttpResponse.BodyHandlers.ofString();
                    return client.sendAsync(req.build(), res).thenApply(response -> {
                        var body = response.body().isBlank() || response.statusCode() / 100 != 2
                                ? DataNode.of(null)
                                : request.serializer.parse(response.body());
                        return Default.new Response(request, response.statusCode(), body, response.headers().map());
                    }).thenCompose(request::handleRedirect);
                }
            });

    private Serializer<? extends DataNode> serializer;
    private @Nullable Cache<URI, Response> cache;
    private Function<Request, CompletableFuture<Response>> executor;

    public static CompletableFuture<Response> get(String uri) {
        return request(Method.GET, uri, null).execute();
    }

    public static CompletableFuture<Response> post(String uri, @Nullable DataNode body) {
        return request(Method.POST, uri, body).execute();
    }

    public static CompletableFuture<Response> put(String uri, @Nullable DataNode body) {
        return request(Method.PUT, uri, body).execute();
    }

    public static CompletableFuture<Response> delete(String uri, @Nullable DataNode body) {
        return request(Method.DELETE, uri, body).execute();
    }

    public static CompletableFuture<Response> head(String uri, @Nullable DataNode body) {
        return request(Method.HEAD, uri, body).execute();
    }

    public static CompletableFuture<Response> options(String uri) {
        return request(Method.OPTIONS, uri, null).execute();
    }

    public static CompletableFuture<Response> trace(String uri) {
        return request(Method.TRACE, uri, null).execute();
    }

    public static CompletableFuture<Response> connect(String uri, @Nullable DataNode body) {
        return request(Method.CONNECT, uri, body).execute();
    }

    public static CompletableFuture<Response> patch(String uri, @Nullable DataNode body) {
        return request(Method.PATCH, uri, body).execute();
    }

    public static Request request(Method method, String uri) {
        return request(method, uri, null);
    }

    public static Request request(Method method, String uri, @Nullable DataNode body) {
        return Default.new Request(method, Polyfill.uri(uri), body, Default.serializer);
    }

    @Value
    public class Request implements MimeType.Container {
        @NotNull Method method;
        @Setter @NonFinal @NotNull URI uri;
        @Setter @NonFinal @Nullable DataNode body;
        @Setter @NonFinal @NotNull Serializer<? extends DataNode> serializer;
        @Singular Map<String, String> headers = new ConcurrentHashMap<>();

        @Override
        public MimeType getMimeType() {
            return serializer.getMimeType();
        }

        public Request addHeader(String name, String value) {
            headers.put(name, value);
            return this;
        }

        private Request(@NotNull Method method, @NotNull URI uri, @Nullable DataNode body,
                        @NotNull Serializer<? extends DataNode> serializer) {
            this.method = method;
            this.uri = uri;
            this.body = body;
            this.serializer = serializer;
        }

        public CompletableFuture<Response> execute() {
            return executor.apply(this);
        }

        private CompletableFuture<Response> handleRedirect(Response response) {
            if (response.responseCode / 100 != 3)
                return CompletableFuture.completedFuture(response);
            var location = response.headers.get("Location").get(0);
            return setUri(Polyfill.uri(location))
                    .execute()
                    .thenCompose(this::handleRedirect);
        }

        @Override
        public String toString() {
            return "%s %s (%d headers)".formatted(method, uri, headers.size());
        }
    }

    @Value
    public class Response {
        Request request;
        int responseCode;
        DataNode body;
        Map<String, List<String>> headers;

        public Response validate2xxOK() {
            Constraint.equals(responseCode/100, 2, "responseCode")
                    .setMessageOverride("Invalid response code; expected code in 200-299 range ("+responseCode+")\n\t\t"+request.toString())
                    .run();
            return this;
        }

        public void require(int... statusCodes) {
            IntStream.of(statusCodes).forEach(x -> require(x, null));
        }

        public void require(int statusCode, @Nullable String message) {
            if (responseCode != statusCode)
                throw new RuntimeException(Objects.requireNonNullElseGet(message, this::reqErrMsg));
        }

        private String reqErrMsg() {
            return "Invalid response received: " + responseCode;
        }

    }

    public enum Method implements Named {
        GET,
        POST,
        PUT,
        DELETE,
        HEAD,
        OPTIONS,
        TRACE,
        CONNECT,
        PATCH
    }
}
