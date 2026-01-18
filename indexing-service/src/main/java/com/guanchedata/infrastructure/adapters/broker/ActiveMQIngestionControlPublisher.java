package com.guanchedata.infrastructure.adapters.broker;

import com.google.gson.Gson;
import com.guanchedata.infrastructure.ports.IngestionControlPublisher;
import com.guanchedata.model.IngestionControlEvent;
import jakarta.jms.*;

public class ActiveMQIngestionControlPublisher implements IngestionControlPublisher {

    private final ConnectionFactory factory;
    private final Gson gson = new Gson();
    private static final String TOPIC_NAME = "ingestion.control";

    public ActiveMQIngestionControlPublisher(ConnectionFactory factory) {
        this.factory = factory;
    }

    public void publishPause() {
        publish(IngestionControlEvent.Type.INGESTION_PAUSE);
    }

    public void publishResume() {
        publish(IngestionControlEvent.Type.INGESTION_RESUME);
    }

    private void publish(IngestionControlEvent.Type type) {
        try (Connection connection = factory.createConnection()) {
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(TOPIC_NAME);

            MessageProducer producer = session.createProducer(topic);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            IngestionControlEvent event = new IngestionControlEvent(type);

            String json = gson.toJson(event);
            TextMessage message = session.createTextMessage(json);

            producer.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
