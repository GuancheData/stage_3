package com.guanchedata;

import com.guanchedata.application.usecases.indexingservice.IndexingController;
import com.guanchedata.infrastructure.adapters.recovery.CoordinateRebuild;
import com.guanchedata.infrastructure.adapters.tokenizer.JsonStopWordsLoader;
import com.guanchedata.infrastructure.adapters.web.HazelcastBookStore;
import com.guanchedata.infrastructure.adapters.broker.ActiveMQMessageConsumer;
import com.guanchedata.infrastructure.adapters.broker.RebuildMessageListener;
import com.guanchedata.infrastructure.adapters.indexstore.HazelcastIndexStore;
import com.guanchedata.infrastructure.adapters.metadata.HazelcastMetadataStore;
import com.guanchedata.infrastructure.adapters.metadata.MetadataParser;
import com.guanchedata.infrastructure.adapters.recovery.IngestionQueueManager;
import com.guanchedata.infrastructure.adapters.recovery.InvertedIndexRecovery;
import com.guanchedata.infrastructure.adapters.recovery.ReindexingExecutor;
import com.guanchedata.infrastructure.adapters.tokenizer.TextTokenizer;
import com.guanchedata.infrastructure.adapters.web.HazelcastIndexingStatusStore;
import com.guanchedata.infrastructure.adapters.web.IndexBook;
import com.guanchedata.infrastructure.adapters.web.TermFrequencyAnalyzer;
import com.guanchedata.infrastructure.config.HazelcastConfig;
import com.guanchedata.infrastructure.ports.MessageConsumer;
import com.hazelcast.core.HazelcastInstance;
import io.javalin.Javalin;
import org.apache.activemq.ActiveMQConnectionFactory;
import jakarta.jms.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        String brokerUrl = System.getenv().getOrDefault("BROKER_URL", "tcp://activemq:61616");
        String clusterName = System.getenv().getOrDefault("HAZELCAST_CLUSTER_NAME", "SearchEngine");

        HazelcastConfig hazelcastConfig = new HazelcastConfig();
        HazelcastInstance hz = hazelcastConfig.initHazelcast(clusterName);

        HazelcastIndexStore indexStore = new HazelcastIndexStore(hz);
        HazelcastBookStore bookStore = new HazelcastBookStore(hz);
        HazelcastMetadataStore metadataStore = new HazelcastMetadataStore(hz, new MetadataParser());
        HazelcastIndexingStatusStore statusStore = new HazelcastIndexingStatusStore(hz);

        JsonStopWordsLoader stopWordsLoader = new JsonStopWordsLoader();
        TextTokenizer tokenizer = new TextTokenizer(stopWordsLoader.load());
        TermFrequencyAnalyzer analyzer = new TermFrequencyAnalyzer(tokenizer);

        IndexBook indexBook = new IndexBook(bookStore, indexStore, metadataStore, statusStore, analyzer);

        InvertedIndexRecovery recovery = new InvertedIndexRecovery(args[0], indexBook, bookStore);
        IngestionQueueManager queueManager = new IngestionQueueManager(hz);
        ReindexingExecutor reindexingExecutor = new ReindexingExecutor(recovery, hz, queueManager);

        reindexingExecutor.executeRecovery();

        RebuildMessageListener rebuildListener = new RebuildMessageListener(hz, reindexingExecutor, brokerUrl);
        rebuildListener.startListening();

        ConnectionFactory jmsFactory = new ActiveMQConnectionFactory(brokerUrl);
        MessageConsumer messageConsumer = new ActiveMQMessageConsumer(jmsFactory, "documents.ingested", rebuildListener);
        messageConsumer.startConsuming(documentId -> {
            log.info("Processing document from broker: {}", documentId);
            indexBook.execute(Integer.parseInt(documentId));
        });

        CoordinateRebuild rebuildUseCase = new CoordinateRebuild(hz, brokerUrl);

        IndexingController controller = new IndexingController(indexBook, rebuildUseCase);
        Javalin app = Javalin.create(c -> {
            c.http.defaultContentType = "application/json";
        }).start(7002);

        app.post("/index/document/{documentId}", controller::indexDocument);
        app.post("/index/rebuild", controller::rebuild);
        app.get("/health", controller::health);
    }
}