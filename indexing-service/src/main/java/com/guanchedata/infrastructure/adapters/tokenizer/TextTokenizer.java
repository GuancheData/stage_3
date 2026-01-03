package com.guanchedata.infrastructure.adapters.tokenizer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.guanchedata.infrastructure.ports.Tokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class TextTokenizer implements Tokenizer {
    private static final Logger log = LoggerFactory.getLogger(TextTokenizer.class);
    private final Set<String> stopwords;

    public TextTokenizer() {
        this.stopwords = loadStopwords();
        log.info("Loaded {} stopwords", stopwords.size());
    }

    private Set<String> loadStopwords() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("stopwords-iso.json")) {
            if (is == null) {
                log.warn("stopwords-iso.json not found, using empty stopwords set");
                return new HashSet<>();
            }

            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
            Map<String, List<String>> allStopwords = gson.fromJson(new InputStreamReader(is), type);

            Set<String> combined = new HashSet<>();
            combined.addAll(allStopwords.getOrDefault("en", Collections.emptyList()));
            combined.addAll(allStopwords.getOrDefault("es", Collections.emptyList()));

            return combined;
        } catch (Exception e) {
            log.error("Error loading stopwords: {}", e.getMessage());
            return new HashSet<>();
        }
    }

    @Override
    public List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.stream(text.toLowerCase()
                        .replaceAll("[^a-z0-9\\s]", " ")
                        .split("\\s+"))
                .parallel()
                .filter(token -> !token.isEmpty())
                .filter(token -> token.length() > 2)
                .filter(token -> !stopwords.contains(token))
                .collect(Collectors.toList());
    }
}
