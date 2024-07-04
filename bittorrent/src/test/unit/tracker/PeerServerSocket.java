package test.unit.tracker;

import serversocket.ServerSocketThread;
import serversocket.SocketHandlerPeer;

public class PeerServerSocket {
        public static void main(String[] args) throws SecurityException, IllegalArgumentException {
                ServerSocketThread<SocketHandlerPeer> tracker = ServerSocketThread.getPeerServerSocket(5000);

                tracker.run();
        }
}
