package com.github.dirkraft.httpecho;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class TextJson {

    static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public static String serialize(Summary s) {
        return Ø.call(() -> JSON.writeValueAsString(s));
    }
}
