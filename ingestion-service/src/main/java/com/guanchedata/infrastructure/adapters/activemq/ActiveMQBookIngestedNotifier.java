package com.guanchedata.infrastructure.adapters.activemq;

import com.google.gson.Gson;
import com.guanchedata.infrastructure.ports.BookIngestedNotifier;
import com.guanchedata.model.DocumentIngestedEvent;
import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

public class ActiveMQBookIngestedNotifier implements BookIngestedNotifier {

    private final ConnectionFactory factory;

    public ActiveMQBookIngestedNotifier(String brokerUrl) {
        this.factory = new ActiveMQConnectionFactory(brokerUrl);
    }

    @Override
    public void notify(int bookId) {
        try (Connection connection = factory.createConnection()) {
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination queue = session.createTopic("documents.ingested");
            MessageProducer producer = session.createProducer(queue);

            Gson gson = new Gson();
            DocumentIngestedEvent event = new DocumentIngestedEvent(bookId);
            String json = gson.toJson(event);
            //System.out.println(json);

            TextMessage message = session.createTextMessage(json);
            producer.send(message);

            System.out.println("[documents.ingested] Message sent: " + json);

            session.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
