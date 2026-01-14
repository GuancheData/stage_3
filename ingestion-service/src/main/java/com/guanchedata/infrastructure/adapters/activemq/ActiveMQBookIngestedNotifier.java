package com.guanchedata.infrastructure.adapters.activemq;

import com.google.gson.Gson;
import com.guanchedata.infrastructure.ports.BookIngestedNotifier;
import com.guanchedata.model.DocumentIngestedEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.IAtomicReference;
import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.transport.TransportListener;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class ActiveMQBookIngestedNotifier implements BookIngestedNotifier {

    private final ConnectionFactory factory;
    private final HazelcastInstance hz;

    private Connection connection;
    private final IAtomicReference<Boolean> lock;

    private final AtomicReference<String> lastConnectedBrokerUrl = new AtomicReference<>();

    public ActiveMQBookIngestedNotifier(String brokerUrl, HazelcastInstance hazelcastInstance) {
        this.factory = new ActiveMQConnectionFactory(brokerUrl);
        this.hz = hazelcastInstance;
        lock = hz.getCPSubsystem().getAtomicReference("activemq-recovery-lock");
        lock.set(false);

        initConnection();
    }

    private void initConnection() {
        try {
            this.connection = factory.createConnection();
            if (this.connection instanceof ActiveMQConnection) {
                ActiveMQConnection amqConnection = (ActiveMQConnection) this.connection;
                if (amqConnection.getTransport() != null) {
                    this.lastConnectedBrokerUrl.set(amqConnection.getTransport().getRemoteAddress());
                }

                amqConnection.addTransportListener(new TransportListener() {
                    @Override
                    public void onCommand(Object o) {}

                    @Override
                    public void onException(IOException e) {
                        System.err.println("[ActiveMQ] Error de transporte: " + e.getMessage());
                    }

                    @Override
                    public void transportInterupted() {
                        System.out.println("[ActiveMQ] La conexión con el Broker se ha interrumpido.");
                    }

                    @Override
                    public void transportResumed() {
                        String currentBrokerUrl = amqConnection.getTransport().getRemoteAddress();
                        String previousUrl = lastConnectedBrokerUrl.get();
                        if (previousUrl != null && !previousUrl.equals(currentBrokerUrl)) {
                            if (lock.compareAndSet(false, true)) {
                                System.out.println("[ActiveMQ] CAMBIO DE NODO: " + previousUrl + " -> " + currentBrokerUrl);
                                triggerMessageRecovery();
                            }
                        }

                        lastConnectedBrokerUrl.set(currentBrokerUrl);
                    }
                });
            }
            this.connection.start();
            System.out.println("[ActiveMQ] Servicio iniciado correctamente.");

        } catch (Exception e) {
            System.err.println("Error fatal iniciando conexión ActiveMQ: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void notify(int bookId) {
        try (Session session = this.connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {

            Destination queue = session.createQueue("documents.ingested");
            MessageProducer producer = session.createProducer(queue);

            Gson gson = new Gson();
            DocumentIngestedEvent event = new DocumentIngestedEvent(bookId);
            String json = gson.toJson(event);

            TextMessage message = session.createTextMessage(json);
            producer.send(message);

            System.out.println("[documents.ingested] Message sent: " + json);

        } catch (Exception e) {
            System.err.println("Error al enviar notificación a ActiveMQ: " + e.getMessage());
        }
    }

    private void triggerMessageRecovery() {
        new Thread(() -> {
            System.out.println("INICIANDO RECUPERACIÓN...");
            try {
                Set<Integer> logs = this.hz.getSet("log");
                Set<Integer> registry = this.hz.getSet("indexingRegistry");

                Set<Integer> difference = new HashSet<>(logs);
                difference.removeAll(registry);

                System.out.println("Mensajes a recuperar: " + difference.size());

                for (Integer bookId : difference) {
                    try {
                        notify(bookId);
                        Thread.sleep(10);
                    } catch (Exception e) {
                        System.err.println("Error recuperando ID: " + bookId);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error en proceso de recuperación: " + e);
            } finally {
                lock.set(false);
            }
        }).start();
    }
}