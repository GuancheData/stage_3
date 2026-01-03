package com.guanchedata;

import com.google.gson.Gson;
import com.guanchedata.application.usecases.searchservice.SearchController;
import com.guanchedata.infrastructure.adapters.apiservices.SearchService;
import com.guanchedata.infrastructure.adapters.indexstore.HazelcastIndexStore;
import com.guanchedata.infrastructure.adapters.metadata.HazelcastMetadataStore;
import com.guanchedata.infrastructure.config.HazelcastConfig;
import com.guanchedata.infrastructure.config.ServiceConfig;
import com.hazelcast.core.HazelcastInstance;
import io.javalin.Javalin;
import io.javalin.json.JsonMapper;

import java.lang.reflect.Type;
import java.util.logging.Logger;

public class Main {
    private static final Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        log.info("Starting Search Service");

        ServiceConfig config = new ServiceConfig();
        HazelcastConfig hzConfig = new HazelcastConfig();

        HazelcastInstance hazelcastInstance = hzConfig.initHazelcast(config.getClusterName());

        HazelcastIndexStore indexStore = new HazelcastIndexStore(hazelcastInstance);
        HazelcastMetadataStore metadataStore = new HazelcastMetadataStore(hazelcastInstance);

        String sortingCriteria = System.getenv("SORTING_CRITERIA");
        if (sortingCriteria == null || sortingCriteria.isEmpty()) {
            sortingCriteria = "frequency";
        }

        SearchService searchService = new SearchService(indexStore, metadataStore, sortingCriteria);
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
        }).start(config.getServicePort());

        app.get("/search", searchController::search);
        app.get("/health", searchController::health);

        log.info("Search Service running on port " + config.getServicePort() + " with sorting: " + sortingCriteria);
    }
}
