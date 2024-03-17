package org.comroid.api.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.java.Log;
import org.comroid.api.Polyfill;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.data.seri.adp.JSON;
import org.comroid.api.func.exc.ThrowingFunction;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.func.util.Event;
import org.comroid.api.java.Activator;
import org.comroid.api.java.SoftDepend;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.unmodifiableMap;
import static org.comroid.api.Polyfill.uncheckedCast;

@Log
@Value
public class Rabbit {
    private static final Map<URI, Rabbit> $cache = new ConcurrentHashMap<>();
    public static final Map<URI, Rabbit> CACHE = unmodifiableMap($cache);

    public static Wrap<Rabbit> of(@Nullable String uri) {
        if (uri == null) return Wrap.empty();
        final var _uri = Polyfill.uri(uri);
        return SoftDepend.type("com.rabbitmq.client.Connection")
                .map($ -> $cache.computeIfAbsent(_uri, Rabbit::new));
    }

    URI uri;
    Connection connection;
    Map<String, Exchange> exchanges = new ConcurrentHashMap<>();

    @SneakyThrows
    private Rabbit(URI uri) {
        this.uri = uri;
        var connFactory = new ConnectionFactory();
        connFactory.setUri(uri);
        this.connection = connFactory.newConnection();
    }

    public Exchange exchange(String exchange) {
        return exchanges.computeIfAbsent(exchange, Exchange::new);
    }

    @lombok.Builder(builderMethodName = "bind", buildMethodName = "create", builderClassName = "Binder")
    public <T extends DataNode> Exchange.Route<T> bind(String exchange, String routingKey, Class<? extends T> type) {
        return exchange(exchange).route(routingKey, type);
    }

    @Value
    public class Exchange {
        Map<String, Route<?>> routes = new ConcurrentHashMap<>();
        String exchange;
        String queue;
        @NonFinal Channel channel;

        @SneakyThrows
        private Exchange(String exchange) {
            this.exchange = exchange;
            this.queue = Exchange.this.touch().queueDeclare().getQueue();
        }

        @SneakyThrows
        private Channel touch() {
            if (channel != null) {
                if (channel.isOpen())
                    return channel;
                try {
                    channel.close();
                } catch (Throwable ignored) {
                }
            }
            channel = connection.createChannel();
            channel.exchangeDeclare(exchange, "topic");
            return channel;
        }

        public <T extends DataNode> Route<T> route(String routingKey, Class<? extends T> type) {
            return uncheckedCast(routes.computeIfAbsent(routingKey, (k) -> new Route<>(routingKey, type)));
        }

        @Value
        public class Route<T extends DataNode> extends Event.Bus<T> {
            String routingKey;
            Activator<T> ctor;
            @NonFinal String tag;

            public Route(String routingKey, Class<T> type) {
                this.routingKey = routingKey;
                this.ctor = Activator.get(type);

                var time = TimeUnit.MINUTES.toMillis(15);
                new Timer("Binding Watchdog").schedule(new TimerTask() {
                    @Override
                    public void run() {
                        touch();
                    }
                }, time, time);
            }

            @SneakyThrows
            public Channel touch() {
                var channel = Exchange.this.touch();
                if (tag == null) {
                    channel.queueBind(queue, exchange, routingKey);
                    tag = channel.basicConsume(queue, this::handleRabbitData, tag -> {
                    });
                }
                return channel;
            }

            @SneakyThrows
            public void send(T data) {
                var body = SoftDepend.type("com.fasterxml.jackson.databind.ObjectMapper")
                        .map(ThrowingFunction.logging(log, $$ -> new ObjectMapper().writeValueAsString(data)))
                        .or(data::toSerializedString)
                        .assertion();
                touch().basicPublish(exchange, routingKey, null, body.getBytes());
            }

            private void handleRabbitData(String $, Delivery content) {
                final var body = new String(content.getBody());
                var data = SoftDepend.type("com.fasterxml.jackson.databind.ObjectMapper")
                        .map(ThrowingFunction.logging(log, $$ -> new ObjectMapper().readValue(body, ctor.getTarget())))
                        .or(() -> ctor.createInstance(JSON.Parser.parse(body)))
                        .assertion();
                publish(data);
            }
        }
    }
}
