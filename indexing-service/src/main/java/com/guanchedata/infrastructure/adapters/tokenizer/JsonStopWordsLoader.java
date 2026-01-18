package com.guanchedata.infrastructure.adapters.tokenizer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.*;

public class JsonStopWordsLoader implements StopWordsLoader {

    private static final Logger log = LoggerFactory.getLogger(JsonStopWordsLoader.class);
    private static final String FILE_NAME = "stopwords-iso.json";

    @Override
    public Set<String> load() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(FILE_NAME)) {
            if (is == null) {
                log.warn("{} not found, using empty stopwords set", FILE_NAME);
                return new HashSet<>();
            }

            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
            Map<String, List<String>> allStopwords = gson.fromJson(new InputStreamReader(is), type);

            Set<String> combined = new HashSet<>();
            combined.addAll(allStopwords.getOrDefault("en", Collections.emptyList()));
            combined.addAll(allStopwords.getOrDefault("es", Collections.emptyList()));

            log.info("Loaded {} stopwords", combined.size());
            return combined;
        } catch (Exception e) {
            log.error("Error loading stopwords: {}", e.getMessage());
            return new HashSet<>();
        }
    }
}