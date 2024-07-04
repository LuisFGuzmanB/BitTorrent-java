package serversocket;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public abstract class SocketHandler implements Runnable, Closeable {
        protected Socket socket;
        protected InputStream inputStream;
        protected OutputStream outputStream;
        protected ServerSocketThread<? extends SocketHandler> serverSocketThread;

        // No se inicializan los flujos de objetos porque no se sabe si se van a
        // utilizar, depende de la implementación de cada clase hija, además, si se
        // inicializan causan un error al crear el objeto

        protected ObjectInputStream objectInputStream;
        protected ObjectOutputStream objectOutputStream;

        protected SocketHandler(Socket socket, ServerSocketThread<? extends SocketHandler> serverSocketThread) {
                this.socket = socket;
                this.serverSocketThread = serverSocketThread;

                logSocketAction("Conectado");

                try {
                        this.inputStream = socket.getInputStream();
                        this.outputStream = socket.getOutputStream();
                } catch (IOException e) {
                        System.err.println("Error al obtener los flujos de entrada y salida");
                        e.printStackTrace();

                        close();
                }
        }

        protected void logSocketAction(String action) {
                System.out.println(getFullRemoteIp() + " - " + action);
        }

        protected String getFullRemoteIp() {
                return socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        }

        /**
         * Cierra los flujos de entrada, salida y el socket de forma segura
         */
        public void close() {
                closeObject(objectInputStream);
                closeObject(objectOutputStream);
                closeObject(inputStream);
                closeObject(outputStream);
                closeObject(socket);
        }

        private void closeObject(Closeable object) {
                if (object == null) {
                        return;
                }

                try {
                        object.close();
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }
}
