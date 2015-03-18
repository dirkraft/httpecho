package com.github.dirkraft.httpecho;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.message.BasicHeader;
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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpEcho implements Runnable, Handler<HttpServerRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(HttpEcho.class);

    public static final String HEADER_ALL_HEADERS = "echoAllHeaders";
    public static final String HEADER_FORMAT = "echoFormat";

    static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Heroku does not seem to expose any way to disable forwarding headers. So we'll just have to filter them out
     * and hope the original caller didn't include any such colliding headers.
     */
    static final Set<String> HEROKU_HEADERS = Stream.of(
            "X-Request-Id",
            "X-Forwarded-For",
            "X-Forwarded-Proto",
            "X-Forwarded-Port",
            "Via",
            "Connect-Time",
            "X-Request-Start",
            "Total-Route-Time"
    ).collect(Collectors.toSet());

    static final Map<String, Function<Map<String, Object>, String>> SERIALIZERS = Collections.unmodifiableMap(Stream.of(
            new Entry("text/plain", Object::toString),
            new Entry("text/html", TextHtml::serialize),
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

            Map<String, Object> summary = summarize(event);
            Function<Map<String, Object>, String> serializer = serializer(event);
            String serialized = serializer.apply(summary);
            event.response().end(serialized);

        } finally {
            event.response().close();
        }
    }

    Map<String, Object> summarize(HttpServerRequest event) {
        boolean allHeaders = "true".equalsIgnoreCase(event.params().get(HEADER_ALL_HEADERS));
        final Predicate<Map.Entry<String, String>> headersPredicate;
        if (allHeaders) {
            headersPredicate = (e) -> true;
        } else {
            headersPredicate = (e) -> !HEROKU_HEADERS.contains(e.getKey());
        }

        Map<String, Object> summary = new LinkedHashMap<>();

        summary.put("version", event.version());
        summary.put("method", event.method());
        summary.put("uri", event.uri());
        summary.put("absoluteURI", event.absoluteURI());
        summary.put("path", event.path());
        summary.put("query", event.query());
        summary.put("params", event.params());
        summary.put("headers", Iterables.filter(event.headers().entries(), headersPredicate));
        summary.put("remoteAddress", event.remoteAddress());

        return summary;
    }

    Function<Map<String, Object>, String> serializer(HttpServerRequest event) {
        String rawFormat = event.params().get(HEADER_FORMAT);
        if (null == rawFormat) {
            rawFormat = event.headers().get(HttpHeaders.ACCEPT);
        }

        if (null != rawFormat) {
            // for value parsing
            Header acceptHeader = new BasicHeader(HttpHeaders.ACCEPT.toString(), rawFormat);
            for (int i = 0; i < acceptHeader.getElements().length; i++) {
                HeaderElement headerElement = acceptHeader.getElements()[i];
                String accept = headerElement.getName();
                Function<Map<String, Object>, String> serializer = SERIALIZERS.get(accept);
                if (serializer != null) {
                    return serializer;
                }
            }
        }

        return SERIALIZERS.get("text/plain");
    }

    static class Entry {
        final String name;
        final Function<Map<String, Object>, String> serializer;

        public Entry(String name, Function<Map<String, Object>, String> serializer) {
            this.name = name;
            this.serializer = serializer;
        }
    }

    public static void main(String[] args) {
        new HttpEcho(args.length > 0 ? Integer.parseInt(args[0]) : 8080).run();
    }
}
