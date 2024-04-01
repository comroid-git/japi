package org.comroid.api.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
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

import java.net.URI;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static java.util.Collections.unmodifiableMap;
import static org.comroid.api.Polyfill.uncheckedCast;

@Log
@Value
public class Rabbit {
    private static final ConnectionFactory factory = new ConnectionFactory();
    private static final Map<URI, Rabbit> $cache = new ConcurrentHashMap<>();
    public static final Map<URI, Rabbit> CACHE = unmodifiableMap($cache);

    public static Wrap<Rabbit> of(@Nullable String uri) {
        if (uri == null) return Wrap.empty();
        final var _uri = Polyfill.uri(uri);
        return SoftDepend.type("com.rabbitmq.client.Connection")
                .map($ -> $cache.computeIfAbsent(_uri, Rabbit::new));
    }

    URI uri;
    @NonFinal Connection connection;
    Map<String, Exchange> exchanges = new ConcurrentHashMap<>();

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
        @NonFinal Channel channel;

        @SneakyThrows
        private Exchange(String exchange) {
            this.exchange = exchange;
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
            channel.exchangeDeclare(exchange, "topic");
            return channel;
        }

        public <T extends DataNode> Route<T> route(String routingKey, Class<? extends T> type) {
            return uncheckedCast(routes.computeIfAbsent(routingKey, (k) -> new Route<>(routingKey, type)));
        }

        public Rabbit rabbit() {
            return Rabbit.this;
        }

        @Value
        public class Route<T extends DataNode> extends Event.Bus<T> {
            String routingKey;
            Activator<T> ctor;
            @NonFinal String tag;

            private Route(String routingKey, Class<T> type) {
                this.routingKey = routingKey;
                this.ctor = Activator.get(type);

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

            @SneakyThrows
            public void send(T data) {
                try {
                    var body = SoftDepend.type("com.fasterxml.jackson.databind.ObjectMapper")
                            .map(ThrowingFunction.logging(log, $$ -> new ObjectMapper().writeValueAsString(data)))
                            .or(data::toSerializedString)
                            .assertion();
                    touch().basicPublish(exchange, routingKey, null, body.getBytes());
                } catch (Throwable t) {
                    log.log(Level.FINE, "Could not send data to rabbit", t);
                }
            }

            private void handleRabbitData(String $, Delivery content) {
                final var body = new String(content.getBody());
                var data = SoftDepend.type("com.fasterxml.jackson.databind.ObjectMapper")
                        .map(ThrowingFunction.logging(log, $$ -> new ObjectMapper().readValue(body, ctor.getTarget())))
                        .or(() -> ctor.createInstance(JSON.Parser.parse(body)))
                        .assertion();
                publish(data);
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
