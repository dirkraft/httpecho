package com.github.dirkraft.httpecho;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Objects;

public class TextHtml {

    public static final String TEMPLATE = Ø.call(() ->
            Resources.toString(Resources.getResource("template.html"), Charsets.UTF_8));
    public static final String TOKEN_TBODY = "TOKEN_TBODY";

    public static String serialize(Summary s) {
        StringWriter html = new StringWriter();
        PrintWriter p = new PrintWriter(html);

        for (Map.Entry<String, Object> e : s.asMap().entrySet()) {
            final String value;
            if (e.getValue() instanceof Iterable) {
                StringWriter inner = new StringWriter();
                PrintWriter pInner = new PrintWriter(inner);
                for (Object o : (Iterable) e.getValue()) {
                    pInner.println("<ul>");
                    pInner.format("<li>%s</li>%n", Objects.toString(o));
                    pInner.println("</ul>");
                }
                value = inner.toString();
            } else {
                value = Objects.toString(e.getValue());
            }
            p.format("      <tr><td>%s</td><td>%s</td></tr>%n", e.getKey(), value);
        }

        return TEMPLATE.replace(TOKEN_TBODY, html.toString());
    }

}
