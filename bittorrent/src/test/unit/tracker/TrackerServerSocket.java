package test.unit.tracker;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;

import serversocket.ServerSocketThread;
import serversocket.SocketHandlerTracker;

public class TrackerServerSocket {
        
        public static void main(String[] args) throws NoSuchMethodException, SecurityException, IOException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                ServerSocketThread<SocketHandlerTracker> tracker = ServerSocketThread.getTrackerServerSocket(5000);

                tracker.run();
        }
}
