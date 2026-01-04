package com.guanchedata.infrastructure.ports;

import java.util.List;
import java.util.Set;

public interface Tokenizer {
    List<String> tokenize(String text);
}
