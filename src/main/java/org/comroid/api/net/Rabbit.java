package org.comroid.api.net;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.java.Log;
import org.comroid.api.ByteConverter;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.func.util.Debug;
import org.comroid.api.func.util.Event;
import org.comroid.api.java.SoftDepend;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.*;
import static org.comroid.api.Polyfill.*;

@Log
@Value
public class Rabbit {
    private static final ConnectionFactory factory = new ConnectionFactory();
    private static final Map<URI, Rabbit> $cache = new ConcurrentHashMap<>();
    public static final  Map<URI, Rabbit> CACHE  = unmodifiableMap($cache);

    public static Wrap<Rabbit> of(@Nullable String uri) {
        if (uri == null) return Wrap.empty();
        final var uri0 = uri(uri);
        return SoftDepend.type("com.rabbitmq.client.Connection")
                .map($ -> $cache.computeIfAbsent(uri0, Rabbit::new));
    }

    URI                   uri;
    Map<String, Exchange> exchanges = new ConcurrentHashMap<>();
    @NonFinal Connection connection;

    private Rabbit(URI uri) {
        this.uri = uri;
        this.connection = touch();
    }

    @SneakyThrows
    private synchronized Connection touch() {
        if (connection != null && connection.isOpen())
            return connection;
        if (connection != null) try {
            connection.close();
        } catch (Throwable ignored) {
        }
        factory.setUri(uri);
        return factory.newConnection();
    }

    @Builder(builderMethodName = "bind", buildMethodName = "create", builderClassName = "Binder")
    public <T> Exchange.Route<T> bind(String exchange, @Nullable String exchangeType, String routingKey, ByteConverter<T> converter) {
        return exchange(exchange, exchangeType).route(routingKey, converter);
    }

    public Exchange exchange(String exchange) {
        return exchange(exchange, null);
    }

    public Exchange exchange(String exchange, String exchangeType) {
        return exchanges.computeIfAbsent(exchange, exc -> new Exchange(exc, exchangeType));
    }

    @Value
    public class Exchange {
        Map<String, Route<?>> routes = new ConcurrentHashMap<>();
        String exchange;
        String exchangeType;
        @NonFinal Channel channel;

        private Exchange(String exchange, @Nullable String exchangeType) {
            this.exchange     = exchange;
            this.exchangeType = exchangeType == null ? "topic" : exchangeType;
        }

        @SneakyThrows
        private synchronized Channel touch() {
            if (channel != null) {
                if (channel.isOpen())
                    return channel;
                try {
                    channel.close();
                } catch (Throwable ignored) {
                }
            }
            connection = Rabbit.this.touch();
            channel = connection.createChannel();
            channel.exchangeDeclare(exchange, exchangeType);
            return channel;
        }

        public <T> Route<T> route(String routingKey, ByteConverter<T> converter) {
            return uncheckedCast(routes.computeIfAbsent(routingKey, (k) -> new Route<>(routingKey, converter)));
        }

        public Rabbit rabbit() {
            return Rabbit.this;
        }

        @Value
        public class Route<T> extends Event.Bus<T> {
            @Nullable String routingKey;
            ByteConverter<T> converter;
            @NonFinal String tag;

            private Route(String routingKey, ByteConverter<T> converter) {
                this.routingKey = routingKey;
                this.converter  = converter;

                new Timer("Route Watchdog").schedule(new TimerTask() {
                    @Override
                    public void run() {
                        channel = touch();
                    }
                }, 0, TimeUnit.MINUTES.toMillis(15));
            }

            @SneakyThrows
            public synchronized Channel touch() {
                var channel = Exchange.this.touch();
                if (tag == null) {
                    var queue = Exchange.this.touch().queueDeclare().getQueue();
                    channel.queueBind(queue, exchange, routingKey);
                    tag = channel.basicConsume(queue, this::handleRabbitData, tag -> {
                    });
                }
                return channel;
            }

            private void handleRabbitData(String $, Delivery content) {
                var data = SoftDepend.type("com.fasterxml.jackson.databind.ObjectMapper")
                        .map($$ -> converter.fromBytes(content.getBody()))
                        .assertion();
                Debug.log(log, "Data received: " + data);
                publish(data);
            }

            public void send(T data) {
                send(data, routingKey);
            }

            @SneakyThrows
            public void send(T data, @Nullable String routingKey) {
                try {
                    var body = converter.toBytes(data);
                    touch().basicPublish(exchange, Objects.requireNonNullElse(routingKey, this.routingKey), null, body);
                    Debug.log(log, "Data sent: " + data);
                } catch (Throwable t) {
                    Debug.log(log, "Could not send data to rabbit", t);
                }
            }

            public Rabbit rabbit() {
                return Rabbit.this;
            }

            public Exchange exchange() {
                return Exchange.this;
            }
        }
    }
}
