package com.guanchedata.infrastructure.adapters.tokenizer;

import java.util.Set;

public interface StopWordsLoader {
    Set<String> load();
}
