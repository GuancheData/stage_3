package com.guanchedata;

import com.guanchedata.application.usecases.indexingservice.IndexingController;
import com.guanchedata.infrastructure.adapters.apiservices.IndexingService;
import com.guanchedata.infrastructure.adapters.bookstore.HazelcastBookStore;
import com.guanchedata.infrastructure.adapters.indexstore.HazelcastIndexStore;
import com.guanchedata.infrastructure.adapters.metadata.HazelcastMetadataStore;
import com.guanchedata.infrastructure.adapters.metadata.MetadataParser;
import com.guanchedata.infrastructure.adapters.recovery.IngestionQueueManager;
import com.guanchedata.infrastructure.adapters.recovery.InvertedIndexRecovery;
import com.guanchedata.infrastructure.adapters.recovery.ReindexingExecutor;
import com.guanchedata.infrastructure.adapters.tokenizer.TextTokenizer;
import com.guanchedata.infrastructure.config.HazelcastConfig;
import com.guanchedata.infrastructure.config.MessageBrokerConfig;
import com.guanchedata.infrastructure.config.ServiceConfig;
import com.guanchedata.infrastructure.ports.MessageConsumer;
import com.hazelcast.core.HazelcastInstance;
import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        ServiceConfig config = new ServiceConfig();

        HazelcastConfig hazelcastConfig = new HazelcastConfig();
        HazelcastInstance hazelcastInstance = hazelcastConfig.initHazelcast(config.getClusterName());

        HazelcastIndexStore indexStore = new HazelcastIndexStore(hazelcastInstance);
        HazelcastBookStore bookStore = new HazelcastBookStore(hazelcastInstance);
        TextTokenizer tokenizer = new TextTokenizer();
        HazelcastMetadataStore hazelcastMetadataStore = new HazelcastMetadataStore(hazelcastInstance, new MetadataParser());

        IndexingService indexingService = new IndexingService(indexStore, tokenizer, bookStore, hazelcastMetadataStore);

        MessageBrokerConfig brokerConfig = new MessageBrokerConfig();
        MessageConsumer messageConsumer = brokerConfig.createConsumer(config.getBrokerUrl(), indexingService);

        // ------------------------

        InvertedIndexRecovery invertedIndexRecovery = new InvertedIndexRecovery(args[0], hazelcastInstance, indexingService);
        IngestionQueueManager ingestionQueueManager = new IngestionQueueManager(hazelcastInstance);
        ReindexingExecutor reindexingExecutor = new ReindexingExecutor(invertedIndexRecovery, hazelcastInstance, ingestionQueueManager);

        IndexingController controller = new IndexingController(indexingService, reindexingExecutor);

        Gson gson = new Gson();

        Javalin app = Javalin.create(cfg -> {
            cfg.jsonMapper(new JsonMapper() {
                @Override
                public String toJsonString(Object obj, Type type) {
                    return gson.toJson(obj);
                }

                @Override
                public <T> T fromJsonString(String json, Type targetType) {
                    return gson.fromJson(json, targetType);
                }
            });
            cfg.http.defaultContentType = "application/json";
        }).start(config.getPort());

        reindexingExecutor.executeRecovery();

        app.post("/index/document/{documentId}", controller::indexDocument);
        app.post("/index/rebuild", controller::rebuild);

        log.info("Indexing Service running on port {}\n", config.getPort());
    }
}
