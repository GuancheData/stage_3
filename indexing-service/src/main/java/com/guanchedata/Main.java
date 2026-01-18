package com.guanchedata;

import com.guanchedata.application.usecases.indexingservice.IndexingController;
import com.guanchedata.infrastructure.adapters.web.IndexingService;
import com.guanchedata.infrastructure.adapters.bookstore.HazelcastBookStore;
import com.guanchedata.infrastructure.adapters.broker.RebuildMessageListener;
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
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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

        IndexingService indexingService = new IndexingService(indexStore, tokenizer, bookStore, hazelcastMetadataStore, hazelcastInstance);

        InvertedIndexRecovery invertedIndexRecovery = new InvertedIndexRecovery(args[0], indexingService);
        IngestionQueueManager ingestionQueueManager = new IngestionQueueManager(hazelcastInstance);
        ReindexingExecutor reindexingExecutor = new ReindexingExecutor(invertedIndexRecovery, hazelcastInstance, ingestionQueueManager);

        reindexingExecutor.executeRecovery();

        RebuildMessageListener rebuildListener = new RebuildMessageListener(
                hazelcastInstance,
                reindexingExecutor,
                config.getBrokerUrl()
        );
        rebuildListener.startListening();

        MessageBrokerConfig brokerConfig = new MessageBrokerConfig();
        MessageConsumer messageConsumer = brokerConfig.createConsumer(
                config.getBrokerUrl(),
                indexingService,
                rebuildListener
        );

        IndexingController controller = new IndexingController(indexingService, reindexingExecutor, config.getBrokerUrl(), hazelcastInstance);

        Javalin app = Javalin.create(config2 -> {
            config2.http.defaultContentType = "application/json";
        }).start(7002);

        app.post("/index/document/{documentId}", controller::indexDocument);
        app.post("/index/rebuild", controller::rebuild);
        app.get("/health", controller::health);

        log.info("Indexing Service running on port {}\n", config.getPort());
    }
}
