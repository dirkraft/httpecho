package com.github.dirkraft.httpecho;

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
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.impl.DefaultVertx;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpEcho implements Runnable, Handler<HttpServerRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(HttpEcho.class);

    public static final String HEADER_ALL_HEADERS = "echoAllHeaders";
    public static final String HEADER_FORMAT = "echoFormat";

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

    static final Map<String, Serializer> SERIALIZERS = Collections.unmodifiableMap(Stream.of(
            new Serializer("text/plain", Object::toString),
            new Serializer("text/html", TextHtml::serialize),
            new Serializer("text/xml", TextXml::serialize),
            new Serializer("application/json", TextJson::serialize)
    ).collect(Collectors.toMap((s) -> s.type, Function.identity())));

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
        HttpServerResponse response = event.response();
        try {

            Summary summary = summarize(event);
            Serializer serializer = serializer(event);
            String serialized = serializer.fn.apply(summary);
            response.putHeader(HttpHeaders.CONTENT_TYPE, serializer.type);
            response.end(serialized);

        } finally {
            response.close();
        }
    }

    Summary summarize(HttpServerRequest event) {
        boolean allHeaders = "true".equalsIgnoreCase(event.params().get(HEADER_ALL_HEADERS));
        final Predicate<Entry<String, String>> headersPredicate;
        if (allHeaders) {
            headersPredicate = (e) -> true;
        } else {
            headersPredicate = (e) -> !HEROKU_HEADERS.contains(e.getKey());
        }

        return new Summary(event, headersPredicate);
    }

    Serializer serializer(HttpServerRequest event) {
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
                Serializer serializer = SERIALIZERS.get(accept);
                if (serializer != null) {
                    return serializer;
                }
            }
        }

        return SERIALIZERS.get("text/plain");
    }

    static class Serializer {
        final String type;
        final Function<Summary, String> fn;

        public Serializer(String type, Function<Summary, String> fn) {
            this.type = type;
            this.fn = fn;
        }
    }

    public static void main(String[] args) {
        new HttpEcho(args.length > 0 ? Integer.parseInt(args[0]) : 8080).run();
    }
}
