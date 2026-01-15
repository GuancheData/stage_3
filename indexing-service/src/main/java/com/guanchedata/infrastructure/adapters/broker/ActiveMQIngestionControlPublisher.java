package com.guanchedata.infrastructure.adapters.broker;

import com.google.gson.Gson;
import com.guanchedata.infrastructure.ports.IngestionControlPublisher;
import com.guanchedata.model.IngestionControlEvent;
import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

public class ActiveMQIngestionControlPublisher implements IngestionControlPublisher {

    private final ConnectionFactory factory;
    private final Gson gson = new Gson();

    public ActiveMQIngestionControlPublisher(String brokerUrl) {
        this.factory = new ActiveMQConnectionFactory(brokerUrl);
    }

    public void publishPause() {
        publish("INGESTION_PAUSE");
    }

    public void publishResume() {
        publish("INGESTION_RESUME");
    }

    private void publish(String type) {
        try (Connection connection = factory.createConnection()) {
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic("ingestion.control");

            MessageProducer producer = session.createProducer(topic);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT); // If one node is not on at the time, it still gets the message

            IngestionControlEvent event = new IngestionControlEvent(type);

            String json = gson.toJson(event);
            TextMessage message = session.createTextMessage(json);

            producer.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
