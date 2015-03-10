package com.github.dirkraft.httpecho;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.LineParser;
import org.apache.http.util.CharArrayBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.impl.DefaultVertx;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpEcho implements Runnable, Handler<HttpServerRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(HttpEcho.class);

    static final LineParser LINE_PARSER = new BasicLineParser();
    static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    static final Map<String, Function<Object, String>> SERIALIZERS = Collections.unmodifiableMap(Stream.of(
            new Entry("text/plain", Object::toString),
            new Entry("application/json", (o) -> Annoyed.sh(() -> MAPPER.writeValueAsString(o)).get())
    ).collect(Collectors.toMap((s) -> s.name, (s) -> s.serializer)));

    final int port;

    final Vertx vertx;
    final HttpServer server;

    public HttpEcho(int port) {
        this.port = port;
        vertx = new DefaultVertx();
        server = vertx.createHttpServer().requestHandler(this);
    }

    @Override
    public synchronized void run() {
        server.listen(port);
        LOG.info("listening on {}", port);
        try {
            wait();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handle(HttpServerRequest event) {
        try {

            Map<String, Object> summary = new LinkedHashMap<>();

            summary.put("version", event.version());
            summary.put("method", event.method());
            summary.put("uri", event.uri());
            summary.put("absoluteURI", event.absoluteURI());
            summary.put("path", event.path());
            summary.put("query", event.query());
            summary.put("params", event.params());
            summary.put("headers", event.headers().entries());
            summary.put("remoteAddress", event.remoteAddress());

            Optional<String> acceptOpt = Optional.ofNullable(event.headers().get(HttpHeaders.ACCEPT));
            Function<Object, String> serializer = null;
            if (acceptOpt.isPresent()) {
                Header acceptHeader = new BasicHeader(HttpHeaders.ACCEPT.toString(), acceptOpt.get());
                for (int i = 0; i < acceptHeader.getElements().length && serializer == null; i++) {
                    HeaderElement headerElement = acceptHeader.getElements()[i];
                    String accept = headerElement.getName();
                    serializer = SERIALIZERS.get(accept);
                }
            }
            if (serializer == null) {
                serializer = SERIALIZERS.get("text/plain");
            }

            String serialized = serializer.apply(summary);
            event.response().end(serialized);

        } finally {
            event.response().close();
        }
    }

    static class Entry {
        final String name;
        final Function<Object, String> serializer;

        public Entry(String name, Function<Object, String> serializer) {
            this.name = name;
            this.serializer = serializer;
        }
    }

    public static void main(String[] args) {
        new HttpEcho(args.length > 0 ? Integer.parseInt(args[0]) : 8080).run();
    }
}
