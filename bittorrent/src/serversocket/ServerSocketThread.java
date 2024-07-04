package serversocket;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import peer.Peer;
import v2.PeerInfo;

public class ServerSocketThread<T extends SocketHandler> implements Runnable, Closeable {
        // Servidor Socket para escuchar conexiones entrantes
        private ServerSocket serverSocket;

        /**
         * Tabla hash para almacenar los peers
         * String: ip del peer
         * PeerInfo: información del peer
         */
        private Hashtable<String, PeerInfo> peers;

        // Constructor del handler para las conexiones entrantes
        private Constructor<T> handlerConstructor;

        // Executor para manejar los hilos de forma concurrente
        private ExecutorService executor;

        private AtomicBoolean isClosed = new AtomicBoolean(false);

        // ----------------------------------------
        // Constructor y factory
        // ----------------------------------------

        // Constructor del tracker que inicializa el servidor socket en el puerto
        // especificado
        private ServerSocketThread(int port, Constructor<T> handlerConstructor) throws IOException {
                System.out.println("Iniciando tracker en puerto " + port);
                do {
                        try {
                                this.serverSocket = new ServerSocket(port);
                        } catch (BindException e) {
                                System.out.println("Puerto ocupado, intentando con el siguiente");
                                port++;
                        }
                } while (this.serverSocket == null);
                System.out.println("Timeout: " + this.serverSocket.getSoTimeout());

                this.peers = new Hashtable<>();

                this.handlerConstructor = handlerConstructor;
                this.executor = Executors.newFixedThreadPool(tracker.Config.TRACKER_MAX_CONNECTIONS());
        }

        public static ServerSocketThread<SocketHandlerPeer> getPeerServerSocket(int port) {
                try {
                        return new ServerSocketThread<>(
                                        port,
                                        SocketHandlerPeer.class.getDeclaredConstructor(Socket.class,
                                                        ServerSocketThread.class, PeerInfo.class));
                } catch (NoSuchMethodException | SecurityException | IOException e) {
                        e.printStackTrace();
                }
                return null;
        }

        public static ServerSocketThread<SocketHandlerTracker> getTrackerServerSocket(int port) {
                try {
                        return new ServerSocketThread<>(
                                        port,
                                        SocketHandlerTracker.class.getDeclaredConstructor(Socket.class,
                                                        ServerSocketThread.class));
                } catch (NoSuchMethodException | SecurityException | IOException e) {
                        e.printStackTrace();
                        System.exit(1);
                }
                return null;
        }

        // ----------------------------------------
        // Implementación de Runnable
        // ----------------------------------------

        /**
         * Inicia el bucle de escucha de conexiones entrantes
         */
        public void run() {
                System.out.println("Escuchando conexiones entrantes");

                while (!isClosed.get()) {
                        // Acepta una nueva conexión entrante
                        Socket socket;
                        try {
                                socket = serverSocket.accept();
                        } catch (IOException e) {
                                e.printStackTrace();
                                continue;
                        }

                        // Crea un nuevo hilo para manejar la conexión de forma independiente
                        SocketHandler handlerInstance;
                        try {
                                if (handlerConstructor.getParameterCount() == 2) {
                                        handlerInstance = this.handlerConstructor.newInstance(socket, this);

                                } else {
                                        handlerInstance = this.handlerConstructor.newInstance(socket, this,
                                                        Peer.peerInfo);
                                }
                        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                                        | InvocationTargetException e) {
                                System.err.println("Error al crear instancia de handler");
                                e.printStackTrace();
                                System.exit(1);
                                return;
                        }

                        executor.execute(handlerInstance);
                }
        }

        // ----------------------------------------
        // Métodos para manejar la lista de peers
        // ----------------------------------------

        // Método para agregar un peer a la lista de peers
        public void addPeer(PeerInfo peer) {
                peers.put(peer.getFullIp(), peer);
        }

        public void updatePeer(PeerInfo peer) {
                peers.replace(peer.getFullIp(), peer);
        }

        // Método para eliminar un peer de la lista de peers
        public void removePeer(PeerInfo peer) {
                peers.remove(peer.getFullIp());
        }

        // ----------------------------------------
        // Getters y setters
        // ----------------------------------------

        // Método para obtener la lista de peers
        public Hashtable<String, PeerInfo> getPeers() {
                return peers;
        }

        // Método para establecer la lista de peers
        public void setPeers(Hashtable<String, PeerInfo> peers) {
                synchronized (Peer.serverSocketThread) {
                        Peer.serverSocketThread.notifyAll();
                        this.peers = peers;
                }
        }

        // ----------------------------------------
        // Implementación de Closeable
        // ----------------------------------------

        // Método para cerrar el servidor socket
        @Override
        public void close() {
                try {
                        isClosed.set(true);
                        executor.shutdown();
                        serverSocket.close();
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }
}
