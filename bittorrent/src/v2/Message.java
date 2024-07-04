package v2;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Hashtable;

public class Message implements java.io.Serializable {
        public static final long serialVersionUID = 1L;
        public static final int MAX_TRIES = 3;

        private MessageType type;
        private Hashtable<String, PeerInfo> peers = null;
        private PieceInfo onePiece = null;

        // ----------------------------------------
        // Constructores y defaults
        // ----------------------------------------
        public static final Message END_MESSAGE = new Message(MessageType.END_MSG);
        public static final Message REQUEST_PEER_STATUS = new Message(MessageType.REQUEST_PEER_STATUS);

        private Message(MessageType type) {
                this.type = type;
        }

        /**
         * Constructor para un mensaje ANNOUNCE_STATUS
         * 
         * @param type tipo de mensaje (ANNOUNCE_STATUS)
         */
        public Message(MessageType type, PeerInfo peer) {
                this.type = type;
                this.peers = new Hashtable<>();
                this.peers.put(peer.getFullIp(), peer);
        }

        /**
         * Constructor para un mensaje PEER_LIST
         * 
         * @param type  tipo de mensaje (PEER_LIST)
         * @param peers lista de peers
         */
        public Message(MessageType type, Hashtable<String, PeerInfo> peers) {
                this.type = type;
                this.peers = peers;
        }

        /**
         * Constructor para un mensaje REQUEST_PIECE o REPLY_PIECE (tanto para empezar
         * como para terminar)
         * 
         * @param type  tipo de mensaje (REQUEST_PIECE o REPLY_PIECE)
         * @param piece pieza
         */
        public Message(MessageType type, PieceInfo piece) {
                this.type = type;
                this.onePiece = piece;
        }

        // ----------------------------------------
        // Métodos estáticos
        // ----------------------------------------

        /**
         * Lee un mensaje de un InputStream
         * 
         * @implNote Implementa reintentos para leer el byte de inicio de mensaje
         * 
         * @param inputStream InputStream del cual se leerá el mensaje
         * @return mensaje leído
         * @throws ConnectionEndException si la conexión terminó
         */
        public static Message readMessage(InputStream inputStream) throws ConnectionEndException {
                int startByte;
                int tries = 0;
                do {
                        try {
                                startByte = inputStream.read();
                                if (startByte == -1) {
                                        throw new ConnectionEndException();
                                }
                        } catch (IOException e) {
                                System.out.println("Error al leer byte de inicio de mensaje");
                                e.printStackTrace();
                                startByte = -2;
                        }
                } while (startByte != MessageType.START_MSG.value && ++tries < MAX_TRIES);

                /*
                 * Si se intentó leer el byte de inicio de mensaje MAX_TRIES veces y no se
                 * logró, se asume que la conexión terminó, ya que afuerzas i será diferente de
                 * MessageType.START_MSG.value
                 */
                if (tries >= MAX_TRIES) {
                        throw new ConnectionEndException();
                }

                Object o = null;

                try {
                        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                        o = objectInputStream.readObject();
                } catch (ClassNotFoundException | IOException e) {
                        System.out.println("Error al leer mensaje");
                        e.printStackTrace();
                        return null;
                }
                if (o instanceof Message) {
                        return (Message) o;
                }

                System.out.println("Error al leer mensaje");
                return null;
        }

        /**
         * Envia un mensaje a través de un OutputStream
         * 
         * @param outputStream OutputStream a través del cual se enviará el mensaje
         * @param message      mensaje a enviar
         */
        public static void sendMessage(OutputStream outputStream, Message message) {
                // * Nota: nunca cerrar el ObjectOutputStream, ya que cierra el OutputStream,
                // * esto incluye no ponerlo en un try-with-resources
                int tries = 0;
                do {
                        try {
                                outputStream.write(MessageType.START_MSG.value);

                                ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
                                objectOutputStream.writeObject(message);
                                outputStream.flush();
                                return;
                        } catch (IOException e) {
                                System.out.println("Error al enviar mensaje");
                                e.printStackTrace();
                                tries++;
                        }
                } while (tries < MAX_TRIES);
        }

        // ----------------------------------------
        // Getters y setters
        // ----------------------------------------

        public MessageType getType() {
                return type;
        }

        public void setType(MessageType type) {
                this.type = type;
        }

        public Hashtable<String, PeerInfo> getPeers() {
                return peers;
        }

        public void setPeers(Hashtable<String, PeerInfo> peers) {
                this.peers = peers;
        }

        public PieceInfo getPiece() {
                return onePiece;
        }

        public void setPiece(PieceInfo onePiece) {
                this.onePiece = onePiece;
        }
}
