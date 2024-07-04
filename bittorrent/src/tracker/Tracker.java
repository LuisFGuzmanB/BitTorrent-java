package tracker;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.Hashtable;

import serversocket.ServerSocketThread;
import serversocket.SocketHandlerTracker;
import v2.PeerInfo;

public class Tracker {
        public static ServerSocketThread<SocketHandlerTracker> serverSocketThread;
        static PeerInfo peerInfo;
        public static final Hashtable<String, Thread> peersTrackThreads = new Hashtable<>();

        public static void main(String[] args) throws UnknownHostException {
                try {
                        // Set the console output encoding to UTF-8
                        System.setOut(new PrintStream(System.out, true, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                }

                System.out.println("--------Tracker--------");
                // CARGAR CONFIGURACION
                // CARGAR ARCHIVO DE CONFIGURACION

                Config.load();
                Config.save();

                // INICIAR SERVIDOR SOCKET
                serverSocketThread = ServerSocketThread.getTrackerServerSocket(Config.TRACKER_PORT());
                new Thread(serverSocketThread).start();

        }

}