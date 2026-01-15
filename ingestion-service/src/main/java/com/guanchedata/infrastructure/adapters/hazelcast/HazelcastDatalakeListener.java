package com.guanchedata.infrastructure.adapters.hazelcast;

import com.guanchedata.infrastructure.adapters.bookprovider.BookStorageDate;
import com.guanchedata.model.NodeInfoProvider;
import com.guanchedata.model.BookReplicationCommand;
import com.guanchedata.util.GutenbergBookProvider;
import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HazelcastDatalakeListener {

    private final NodeInfoProvider nodeInfoProvider;
    private final HazelcastInstance hazelcast;
    private final GutenbergBookProvider bookProvider;
    private final BookStorageDate bookStorageDate;

    // Executor para procesar la cola en un hilo separado
    private final ExecutorService executorService;
    private volatile boolean active = true;

    public HazelcastDatalakeListener(HazelcastInstance hazelcast,
                                     NodeInfoProvider nodeInfoProvider,
                                     GutenbergBookProvider bookProvider,
                                     BookStorageDate bookStorageDate) {
        this.hazelcast = hazelcast;
        this.nodeInfoProvider = nodeInfoProvider;
        this.bookProvider = bookProvider;
        this.bookStorageDate = bookStorageDate;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void registerListener() {
        executorService.submit(this::consumeQueue);
    }

    private void consumeQueue() {
        IQueue<BookReplicationCommand> queue = hazelcast.getQueue("booksToBeReplicated");

        while (active) {
            try {
                // Bloquea hasta que hay tarea (Consumidor eficiente)
                BookReplicationCommand command = queue.take();
                processBook(command, queue);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void processBook(BookReplicationCommand command, IQueue<BookReplicationCommand> queue) {
        int bookId = command.getId();
        String myNodeId = nodeInfoProvider.getNodeId();

        // USAMOS EL MISMO MAPA QUE EL EXECUTER (IMap)
        IMap<Integer, Set<String>> replicatedNodesMap = hazelcast.getMap("replicatedNodesMap");

        // 1. VERIFICACIÓN (Lectura rápida)
        // Miramos si mi ID ya está en el Set de este libro
        Set<String> currentOwners = replicatedNodesMap.get(bookId);
        boolean iAlreadyHaveIt = currentOwners != null && currentOwners.contains(myNodeId);

        if (iAlreadyHaveIt) {
            System.out.println("Nodo " + myNodeId + " ya tiene el libro " + bookId + ". Devolviendo tarea.");
            try {
                Thread.sleep(200); // Pequeño backoff
                queue.put(command); // Devolvemos para otro
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return;
        }

        // 2. PROCESAMIENTO
        System.out.println("Nodo " + myNodeId + " descargando libro " + bookId);
        saveRetrievedBook(bookId);

        // 3. ACTUALIZACIÓN DE ESTADO (Escritura Segura con Lock)
        // Bloqueamos la clave del libro para añadirnos a la lista sin condiciones de carrera
        replicatedNodesMap.lock(bookId);
        try {
            Set<String> nodes = replicatedNodesMap.getOrDefault(bookId, new HashSet<>());
            nodes.add(myNodeId);
            replicatedNodesMap.put(bookId, nodes);
        } finally {
            replicatedNodesMap.unlock(bookId);
        }

        // 4. ACTUALIZACIÓN DEL LOG (Opcional, pero segura)
        updateReplicationLog(bookId);
    }

    private void updateReplicationLog(int bookId) {
        IMap<Integer, Integer> replicationLog = hazelcast.getMap("replicationLog");
        replicationLog.lock(bookId);
        try {
            int count = replicationLog.getOrDefault(bookId, 0);
            replicationLog.put(bookId, count + 1);
            System.out.println("Total réplicas libro " + bookId + ": " + (count + 1));
        } finally {
            replicationLog.unlock(bookId);
        }
    }

    private void saveRetrievedBook(int bookId) {
        try {
            this.bookStorageDate.save(bookId, this.bookProvider.getBook(bookId));
            // Nota: Ya no llamamos a addBookLocation aquí dentro para no duplicar lógica.
            // La actualización del mapa se hace en processBook de forma atómica.
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        this.active = false;
        this.executorService.shutdownNow();
    }
}