package serversocket;

import java.io.IOException;
import java.net.Socket;
import v2.ConnectionEndException;
import v2.Message;
import v2.MessageType;
import v2.PeerInfo;
import v2.PieceInfo;
import v2.TorrentInfo;

// Clase interna que maneja las conexiones de clientes
public class SocketHandlerPeer extends SocketHandler {
        private PeerInfo peer;

        // ----------------------------------------
        // Variables exclusivas de SocketHandlerPeer para mensajes iniciadores de
        // conexión
        // ----------------------------------------
        /**
         * Indica el mensaje iniciador de conexión que debe enviar, si es null, el
         * socket
         * no envía ningún mensaje iniciador de conexión
         */
        private Message starterMessage;

        // Constructor que recibe el socket del cliente
        /**
         * Constructor para sockets que no envían mensajes iniciadores de conexión
         * 
         * @param socket             socket del cliente
         * @param serverSocketThread hilo principal del servidor
         * @param peer               información del peer
         */
        public SocketHandlerPeer(Socket socket, ServerSocketThread<SocketHandlerPeer> serverSocketThread,
                        PeerInfo peer) {
                this(socket, serverSocketThread, peer, null);
        }

        /**
         ** Constructor para sockets que envían mensajes iniciadores de conexión
         * 
         * @param socket             socket del cliente
         * @param serverSocketThread hilo principal del servidor
         * @param peer               información del peer
         * @param starterMessage     mensaje iniciador de conexión (EXCEPTO
         *                           REQUEST_PIECE)
         */
        public SocketHandlerPeer(Socket socket, ServerSocketThread<SocketHandlerPeer> serverSocketThread,
                        PeerInfo peer, Message starterMessage) {
                super(socket, serverSocketThread);
                this.peer = peer;
                this.starterMessage = starterMessage;
        }

        // Método principal del hilo que procesa las solicitudes del cliente
        @Override
        public void run() {
                // Si incluimos aquí el procesamiento de socket para enviar mensajes iniciadores
                // de conexión, después de enviar el mensaje, el socket debe esperar la
                // respuesta
                //
                // Se pueden enviar los siguientes MIC:
                // * ANNOUNCE_STATUS: único mensaje que se envía al tracker, se recibe la lista
                // * y ejecuta nuevos sockets con tipo REQUEST_PIECE
                // * UPDATE_STATUS: se envía al tracker cuando se empieza a compartir un nuevo
                // * archivo
                // * NOTIFY_PEER_OFFLINE: se envía al tracker cuando se cierra la aplicación de
                // * forma correcta
                // *
                // * REQUEST_PIECE: se envía a un peer para solicitar una pieza, se espera
                // * respuesta, se procesa la pieza y se cierra el socket
                // * Si el socket es de tipo REQUEST_PIECE, a fuerza debe estar inicializado con
                // un TorrentDownload

                if (starterMessage != null) {
                        Message.sendMessage(outputStream, starterMessage);
                }

                try {
                        Message m = Message.readMessage(inputStream);

                        if (m.getType().equals(MessageType.REQUEST_PEER_STATUS)) {
                                logSocketAction("REQUEST_PEER_STATUS");
                                Message updateStatus = new Message(MessageType.UPDATE_STATUS, peer);
                                Message.sendMessage(outputStream, updateStatus);

                                // Recibe la lista de peers
                                m = Message.readMessage(inputStream);

                                logSocketAction("PEER_LIST");
                                serverSocketThread.setPeers(m.getPeers());

                                // Quita el peer actual de la lista de peers
                                serverSocketThread.removePeer(peer);

                        } else if (m.getType().equals(MessageType.PEER_LIST)) {
                                logSocketAction("PEER_LIST");
                                serverSocketThread.setPeers(m.getPeers());

                                // Quita el peer actual de la lista de peers
                                serverSocketThread.removePeer(peer);

                        } else if (m.getType().equals(MessageType.REQUEST_PIECE)) {
                                do { // Loop de carga de piezas
                                        logSocketAction(m.getPiece().toString());

                                        // * Validación de pieza
                                        TorrentInfo torrentInfo;
                                        byte[] piece = null;
                                        torrentInfo = findTorrentInfo(
                                                        m.getPiece().getTorrentInfo().getName());

                                        if (torrentInfo != null) {
                                                piece = TorrentInfo.getPieceData(m.getPiece());
                                        }

                                        if (torrentInfo == null || piece == null || piece.length == 0) {
                                                logSocketAction("Pieza no encontrada");
                                                Message reply = new Message(MessageType.REPLY_PIECE, (PeerInfo) null);
                                                Message.sendMessage(objectOutputStream, reply);
                                                m = Message.readMessage(inputStream); // A la espera de la siguiente
                                                                                      // petición
                                                continue;
                                        }
                                        // Solo si la validación fue exitosa se envía la pieza

                                        // * -----------------
                                        // * Envío de pieza
                                        // * -----------------

                                        // INICIO DE ENVIO DE PIEZA
                                        PieceInfo pieceInfo = new PieceInfo(torrentInfo,
                                                        m.getPiece().getIndex(), m.getPiece().getBegin());
                                        Message reply = new Message(MessageType.REPLY_PIECE, pieceInfo);
                                        Message.sendMessage(outputStream, reply);

                                        // Stream de bytes
                                        try {
                                                outputStream.write(piece);
                                        } catch (IOException e) {
                                                logSocketAction("Error al enviar la pieza");
                                                e.printStackTrace();
                                        }

                                        // FIN DE ENVIO DE PIEZA

                                        // * -----------------
                                        // * Termina envío de pieza
                                        // * -----------------

                                        m = Message.readMessage(inputStream); // A la espera de la siguiente
                                                                              // petición

                                } while (!m.getType().equals(MessageType.END_MSG)
                                                && m.getType().equals(MessageType.REQUEST_PIECE));

                        }

                        logSocketAction("Desconectado correctamente");
                } catch (ConnectionEndException e) {
                        logSocketAction("Desconectado inesperadamente");
                } finally {
                        Message.sendMessage(outputStream, Message.END_MESSAGE);
                        close();
                }
        }

        private TorrentInfo findTorrentInfo(String torrentName) {

                for (TorrentInfo torrent : peer.getArchivosCompartidos().toArray(new TorrentInfo[0])) {
                        if (torrent.getName().equals(torrentName)) {
                                return torrent;
                        }
                }
                return null;
        }
}
