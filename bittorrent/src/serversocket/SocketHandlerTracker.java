package serversocket;

import java.net.Socket;
import java.util.Hashtable;

import tracker.TrackPeerThread;
import tracker.Tracker;
import v2.ConnectionEndException;
import v2.Message;
import v2.MessageType;
import v2.PeerInfo;

public class SocketHandlerTracker extends SocketHandler {
        public SocketHandlerTracker(Socket socket, ServerSocketThread<SocketHandlerTracker> tracker) {
                super(socket, tracker);
        }

        @Override
        public void run() {
                try {
                        Message m = Message.readMessage(inputStream);

                        if (m.getType().value == MessageType.ANNOUNCE_STATUS.value) {
                                logSocketAction("ANNOUNCE_STATUS");
                                processAnnounceStatus(m);

                                // Iniciar thread para trackear al peer
                                PeerInfo peerInfo = m.getPeers().elements().nextElement();
                                Thread trackPeerThread = new Thread(new TrackPeerThread(peerInfo));
                                trackPeerThread.start();
                                Tracker.peersTrackThreads.put(peerInfo.getFullIp(), trackPeerThread);

                        } else if (m.getType().value == MessageType.UPDATE_STATUS.value) {
                                logSocketAction("UPDATE_STATUS");
                                serverSocketThread.addPeer(m.getPeers().elements().nextElement());

                        } else if (m.getType().value == MessageType.NOTIFY_PEER_OFFLINE.value) {
                                logSocketAction("NOTIFY_PEER_OFFLINE");
                                PeerInfo peerInfo = m.getPeers().elements().nextElement();
                                serverSocketThread.removePeer(peerInfo);
                                // * Acá se podría notificar a los peers que se desconectó un peer

                                // Detener thread de trackeo
                                Thread t = Tracker.peersTrackThreads.remove(peerInfo.getFullIp());
                                t.interrupt();
                        } else {
                                logSocketAction("Mensaje desconocido recibido: " + m.getType().value);
                        }

                        if (Message.readMessage(inputStream).getType().value == MessageType.END_MSG.value) {
                                logSocketAction("Desconectado correctamente");
                        }
                } catch (ConnectionEndException e) {
                        logSocketAction("Desconectado inesperadamente");
                } finally {
                        close();
                }
        }

        private void processAnnounceStatus(Message m) {

                Hashtable<String, PeerInfo> listaPeers = serverSocketThread.getPeers();
                Message mensajeListaPeers = new Message(MessageType.PEER_LIST, listaPeers);

                Message.sendMessage(outputStream, mensajeListaPeers);

                serverSocketThread.addPeer(m.getPeers().elements().nextElement());
        }
}
