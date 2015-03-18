package com.github.dirkraft.httpecho;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class TextXml {

    static final XmlMapper XML = new XmlMapper();

    public static String serialize(Summary s) {
        return Ø.call(() -> XML.writeValueAsString(s));
    }
}
