package com.github.dirkraft.httpecho;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpVersion;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
@JacksonXmlRootElement(localName = "summary")
public class Summary {

    public HttpVersion version;

    public String method;

    public String uri;

    public URI absoluteUri;

    public String path;

    public String query;

    public Map<String, List<String>> params;

    public Map<String, String> headers;

    public InetSocketAddress remoteAddress;

    public Summary(HttpServerRequest event, Predicate<Entry<String, String>> headersPredicate) {

        this.version = event.version();
        this.method = event.method();
        this.uri = event.uri();
        this.absoluteUri = event.absoluteURI();
        this.path = event.path();
        this.query = event.query();
        MultiMap params = event.params();
        this.params = params.names().stream()
                .map((n) -> new SimpleEntry<>(n, params.getAll(n)))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, Λ.throwingMerger(), LinkedHashMap::new));
        this.headers = event.headers().entries().stream()
                .filter(headersPredicate)
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, Λ.throwingMerger(), LinkedHashMap::new));
        this.remoteAddress = event.remoteAddress();
    }

    public Map<String, Object> asMap() {
        Map<String, Object> m = new HashMap<>(9);
        m.put("version", version);
        m.put("method", method);
        m.put("uri", uri);
        m.put("absoluteUri", absoluteUri);
        m.put("path", path);
        m.put("query", query);
        m.put("params", params);
        m.put("headers", headers);
        m.put("remoteAddress", remoteAddress);
        return m;
    }
}
