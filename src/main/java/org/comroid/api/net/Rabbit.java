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
import org.comroid.api.attr.Named;
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
import java.util.logging.Level;

import static java.util.Collections.*;
import static org.comroid.api.Polyfill.*;

@Log
@Value
public class Rabbit implements Named {
    private static final ConnectionFactory factory = new ConnectionFactory() {{
        setAutomaticRecoveryEnabled(true);
    }};
    private static final Map<URI, Rabbit>  $cache  = new ConcurrentHashMap<>();
    public static final  Map<URI, Rabbit>  CACHE   = unmodifiableMap($cache);

    public static Wrap<Rabbit> of(@Nullable String uri) {
        return of(null, uri);
    }

    public static Wrap<Rabbit> of(@Nullable String name, @Nullable String uri) {
        if (uri == null) return Wrap.empty();
        final var uri0 = uri(uri);
        return SoftDepend.type("com.rabbitmq.client.Connection").map($ -> $cache.computeIfAbsent(uri0, uri1 -> new Rabbit(name, uri1)));
    }

    @Nullable String name;
    URI                   uri;
    Map<String, Exchange> exchanges = new ConcurrentHashMap<>();
    @NonFinal Connection connection;

    private Rabbit(@Nullable String name, URI uri) {
        this.name = name;
        this.uri  = uri;
        this.connection = touch();
    }

    @SneakyThrows
    private synchronized Connection touch() {
        if (connection != null && connection.isOpen()) return connection;
        if (connection != null) try {
            connection.close();
        } catch (Throwable t) {
            Debug.log("Unable to close old connection", t);
        }
        factory.setUri(uri);
        return name == null ? factory.newConnection() : factory.newConnection(name);
    }

    @Builder(builderMethodName = "bind", buildMethodName = "create", builderClassName = "Binder")
    public <T> Exchange.Route<T> bind(
            @Nullable String queueName, String exchange, @Nullable String exchangeType, String routingKey,
            ByteConverter<T> converter
    ) {
        return exchange(exchange, exchangeType).route(queueName, routingKey, converter);
    }

    public Exchange exchange(String exchange) {
        return exchange(exchange, null);
    }

    public Exchange exchange(String exchange, String exchangeType) {
        return exchanges.computeIfAbsent(exchange, exc -> new Exchange(exc, exchangeType));
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Rabbit rabbit && Objects.equals(uri, rabbit.uri);
    }

    @Value
    public class Exchange implements Named {
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
                if (channel.isOpen()) return channel;
                try {
                    channel.close();
                } catch (Throwable t) {
                    Debug.log("Unable to close old channel", t);
                }
            }
            connection = Rabbit.this.touch();
            channel = connection.createChannel();
            channel.exchangeDeclare(exchange, exchangeType);
            return channel;
        }

        public <T> Route<T> route(String routingKey, ByteConverter<T> converter) {
            return route(null, routingKey, converter);
        }

        public <T> Route<T> route(String name, String routingKey, ByteConverter<T> converter) {
            return uncheckedCast(routes.computeIfAbsent(routingKey, (k) -> new Route<>(name, routingKey, converter)));
        }

        public Rabbit rabbit() {
            return Rabbit.this;
        }

        @Override
        public int hashCode() {
            return Objects.hash(exchange);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Exchange exchange && Objects.equals(this.exchange, exchange.exchange) && Objects.equals(this.rabbit(), exchange.rabbit());
        }

        @Value
        public class Route<T> extends Event.Bus<T> {
            @Nullable String name;
            @Nullable String routingKey;
            ByteConverter<T> converter;
            @NonFinal String tag;

            private Route(@Nullable String name, @Nullable String routingKey, ByteConverter<T> converter) {
                this.name = name;
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
                    var queue = (name == null ? channel.queueDeclare() : channel.queueDeclare(name, true, true, true, Map.of())).getQueue();
                    channel.queueBind(queue, exchange, routingKey);
                    tag = channel.basicConsume(queue, this::handleRabbitData, tag -> {
                    });
                }
                return channel;
            }

            private void handleRabbitData(String $, Delivery content) {
                try {
                    Debug.log(log, "Data receiving: " + new String(content.getBody()));
                    var data = SoftDepend.type("com.fasterxml.jackson.databind.ObjectMapper").map($$ -> converter.fromBytes(content.getBody())).assertion();
                    publish(data);
                } catch (Throwable t) {
                    org.comroid.api.info.Log.at(Level.WARNING, "Could not receive data from rabbit: " + new String(content.getBody()), t);
                }
            }

            public void send(T data) {
                send(data, routingKey);
            }

            @SneakyThrows
            public void send(T data, @Nullable String routingKey) {
                try {
                    Debug.log(log, "Data sending: " + data);
                    var body = converter.toBytes(data);
                    touch().basicPublish(exchange, Objects.requireNonNullElse(routingKey, this.routingKey), null, body);
                } catch (Throwable t) {
                    org.comroid.api.info.Log.at(Level.WARNING, "Could not send data to rabbit: " + data, t);
                }
            }

            public Rabbit rabbit() {
                return Rabbit.this;
            }

            public Exchange exchange() {
                return Exchange.this;
            }

            @Override
            public boolean equals(Object other) {
                return other instanceof Route<?> route && Objects.equals(routingKey, route.routingKey) && Objects.equals(exchange(), route.exchange());
            }

            @Override
            public int hashCode() {
                return Objects.hash(routingKey);
            }
        }
    }
}
