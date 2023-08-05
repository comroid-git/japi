package org.comroid.util;

import lombok.*;
import lombok.extern.java.Log;
import org.comroid.abstr.DataNode;
import org.comroid.api.Context;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.logging.Level;

@Log
@Value
public class Pushover {
    private static final URI URI = java.net.URI.create("https://api.pushover.net/1/messages.json");
    private static final HttpClient http = Context.wrap(HttpClient.class)
            .orElseGet(()->HttpClient.newBuilder().build());
    Config config;

    public CompletableFuture<Void> send(@NotNull String message) {
        return send(null, message);
    }

    @SneakyThrows
    public CompletableFuture<Void> send(@Nullable String title, @NotNull String message) {
        FormData.Object form = config.msg(title, message).form();
        var req = HttpRequest.newBuilder(URI)
                .method("POST", form.toInputStream().toBodyPublisher())
                .build();
        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenApply(r -> {
            if (r.statusCode() == 200)
                return null;
            throw new RuntimeException("Could not POST message to Pushover; form data: " + form);
        });
    }

    @Data
    public static class Config {
        private String token;
        private String user;
        private @Nullable String device;

        public Message msg(@Nullable String title, String message) {
            return new Message(this, title, message);
        }
    }

    @Data
    @NoArgsConstructor
    public static class Message extends Config implements DataNode {
        private @Nullable String title;
        private String message;

        private Message(Config config, @Nullable String title, String message) {
            super.token = config.token;
            super.user = config.user;
            super.device = config.device;
            this.title = title;
            this.message = message;
        }
    }
}
