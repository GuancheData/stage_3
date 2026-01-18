package com.guanchedata.infrastructure.adapters.tokenizer;

import com.guanchedata.infrastructure.ports.Tokenizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TextTokenizer implements Tokenizer {
    private static final Pattern CLEANUP_PATTERN = Pattern.compile("[^a-z0-9\\s]");
    private static final Pattern SPLIT_PATTERN = Pattern.compile("\\s+");

    private final Set<String> stopwords;

    public TextTokenizer(Set<String> stopwords) {
        this.stopwords = stopwords;
    }

    @Override
    public List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        String cleaned = CLEANUP_PATTERN.matcher(text.toLowerCase()).replaceAll(" ");
        String[] tokens = SPLIT_PATTERN.split(cleaned);

        return Arrays.stream(tokens)
                .parallel()
                .filter(token -> !token.isEmpty())
                .filter(token -> token.length() > 2)
                .filter(token -> !stopwords.contains(token))
                .collect(Collectors.toList());
    }
}