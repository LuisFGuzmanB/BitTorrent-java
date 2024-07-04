package test.unit.tracker;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class TestServerSocket {

        public static void main(String[] args) throws IOException, InterruptedException {

                try (Socket socket = new Socket("localhost", Integer.parseInt(args[0]))) {
                        System.out.println("Conectado");
                        TimeUnit.SECONDS.sleep(20);
                        socket.getOutputStream().write(1);
                }
        }
}
