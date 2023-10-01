package org.comroid.util;

import lombok.Singular;
import lombok.Value;
import lombok.With;
import org.comroid.abstr.DataNode;
import org.comroid.api.Polyfill;
import org.comroid.api.Serializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Value
public class REST {
    public static final REST Default = new REST(new Function<>() {
        private final HttpClient client = HttpClient.newHttpClient();

        @Override
        public CompletableFuture<Response> apply(Request request) {
            var pub = Stream.of(Method.GET, Method.OPTIONS, Method.TRACE)
                    .noneMatch(request.method::equals)
                    || request.body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(request.body.toString());
            var req = HttpRequest.newBuilder()
                    .uri(request.uri)
                    .method(request.method.name(), pub)
                    .build();
            var res = HttpResponse.BodyHandlers.ofString();
            return client.sendAsync(req, res).thenApply(response -> {
                var body = response.body().isBlank() || response.statusCode()/100!=2
                        ? DataNode.of(null)
                        : request.serializer.parse(response.body());
                return Default.new Response(request, response.statusCode(), body, response.headers().map());
            }).thenCompose(request::handleRedirect);
        }
    });

    Function<Request, CompletableFuture<Response>> executor;

    public static CompletableFuture<Response> get(String uri) {return request(Method.GET, uri).execute(); }
    public static CompletableFuture<Response> post(String uri, @Nullable DataNode body) {return request(Method.POST, uri).withBody(body).execute(); }
    public static CompletableFuture<Response> put(String uri, @Nullable DataNode body) {return request(Method.PUT, uri).withBody(body).execute(); }
    public static CompletableFuture<Response> delete(String uri, @Nullable DataNode body) {return request(Method.DELETE, uri).withBody(body).execute(); }
    public static CompletableFuture<Response> head(String uri, @Nullable DataNode body) {return request(Method.HEAD, uri).withBody(body).execute(); }
    public static CompletableFuture<Response> options(String uri) {return request(Method.OPTIONS, uri).execute(); }
    public static CompletableFuture<Response> trace(String uri) {return request(Method.TRACE, uri).execute(); }
    public static CompletableFuture<Response> connect(String uri, @Nullable DataNode body) {return request(Method.CONNECT, uri).withBody(body).execute(); }
    public static CompletableFuture<Response> patch(String uri, @Nullable DataNode body) {return request(Method.PATCH, uri).withBody(body).execute(); }

    public static Request request(Method method, String uri) {
        return Default.new Request(method, Polyfill.uri(uri));
    }

    @Value
    public class Request {
        Method method;
        @With URI uri;
        @With @Nullable DataNode body;
        @With Serializer<DataNode> serializer;
        @Singular Map<String, String> headers = new ConcurrentHashMap<>();

        public Request(Method method, URI uri) {
            this(method, uri, null);
        }

        public Request(Method method, URI uri, @Nullable DataNode body) {
            this(method, uri, body, Jackson.JSON);
        }

        public Request(Method method, URI uri, @Nullable DataNode body, @NotNull Serializer<DataNode> serializer) {
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
            return withUri(Polyfill.uri(location)).execute()
                    .thenCompose(this::handleRedirect);
        }
    }

    @Value
    public class Response {
        Request request;
        int responseCode;
        DataNode body;
        Map<String, List<String>> headers;

        public void require(int... statusCodes) {
            IntStream.of(statusCodes).forEach(x->require(x,null));
        }

        public void require(int statusCode, @Nullable String message) {
            if (responseCode!=statusCode)
                throw new RuntimeException(Objects.requireNonNullElseGet(message, this::reqErrMsg));
        }

        private String reqErrMsg() {
            return "Invalid response received: " + responseCode;
        }
    }

    public enum Method {
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
