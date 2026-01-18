package com.guanchedata;

import com.google.gson.Gson;
import com.guanchedata.application.usecases.searchservice.ContentSearchEngine;
import com.guanchedata.infrastructure.adapters.web.SearchController;
import com.guanchedata.application.usecases.searchservice.FindBooks;
import com.guanchedata.infrastructure.adapters.sorter.SortByFrequency;
import com.guanchedata.infrastructure.adapters.sorter.SortById;
import com.guanchedata.infrastructure.adapters.hazelcast.HazelcastIndexStore;
import com.guanchedata.infrastructure.adapters.hazelcast.HazelcastMetadataStore;
import com.guanchedata.infrastructure.config.HazelcastConfig;
import com.guanchedata.infrastructure.ports.SortingStrategy;
import com.hazelcast.core.HazelcastInstance;
import io.javalin.Javalin;
import io.javalin.json.JsonMapper;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        HazelcastConfig hzConfig = new HazelcastConfig();

        HazelcastInstance hazelcastInstance = hzConfig.initHazelcast(System.getenv().getOrDefault("CLUSTER_NAME", "SearchEngine"));

        HazelcastIndexStore indexStore = new HazelcastIndexStore(hazelcastInstance);
        HazelcastMetadataStore metadataStore = new HazelcastMetadataStore(hazelcastInstance);

        Map<String, SortingStrategy> strategies = new HashMap<>();
        strategies.put("frequency", new SortByFrequency());
        strategies.put("id", new SortById());

        ExecutorService searchExecutor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() - 3
        );

        ContentSearchEngine engine = new ContentSearchEngine(indexStore, searchExecutor);

        String sortingEnv = System.getenv("SORTING_CRITERIA");

        if (sortingEnv == null) sortingEnv = "frequency";

        SortingStrategy selectedStrategy = strategies.getOrDefault(
                sortingEnv.toLowerCase(),
                new SortByFrequency()
        );

        FindBooks search = new FindBooks(engine, metadataStore, selectedStrategy);

        SearchController controller = new SearchController(search);

        FindBooks searchService = new FindBooks(engine, metadataStore, selectedStrategy);

        SearchController searchController = new SearchController(searchService);

        Gson gson = new Gson();

        Javalin app = Javalin.create(javalinConfig -> {
            javalinConfig.jsonMapper(new JsonMapper() {
                @Override
                public String toJsonString(Object obj, Type type) {
                    return gson.toJson(obj, type);
                }

                @Override
                public <T> T fromJsonString(String json, Type targetType) {
                    return gson.fromJson(json, targetType);
                }
            });
        }).start(7003);

        app.get("/search", searchController::search);
        app.get("/health", searchController::health);
    }
}
