package tracker;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import v2.ConnectionEndException;
import v2.Message;
import v2.MessageType;
import v2.PeerInfo;

public class TrackPeerThread implements Runnable {
        PeerInfo peerInfo;

        public TrackPeerThread(PeerInfo peerInfo) {
                this.peerInfo = peerInfo;
        }

        public void run() {
                boolean connected = true;
                try {
                        do {
                                TimeUnit.SECONDS.sleep(20);

                                connected = tryConnection();

                        } while (connected);
                } catch (InterruptedException e) {
                        logSocketAction("INTERRUPTED_EXCEPTION");
                }

                Tracker.serverSocketThread.removePeer(peerInfo);
                Tracker.peersTrackThreads.remove(peerInfo.getFullIp());
                logSocketAction("Desconectado de la red");
        }

        private boolean tryConnection() {
                int tries = 0;
                do {
                        try (Socket socket = new Socket(peerInfo.getIp(), peerInfo.getPort())) {
                                Message.sendMessage(socket.getOutputStream(), Message.REQUEST_PEER_STATUS);
                                Message responsePeerStatus = Message.readMessage(socket.getInputStream());

                                if (responsePeerStatus.getType().value == MessageType.UPDATE_STATUS.value) {

                                        logSocketAction("UPDATE_STATUS");

                                        Tracker.serverSocketThread.updatePeer(
                                                        responsePeerStatus.getPeers()
                                                                        .get(peerInfo.getFullIp()));

                                        Message peersList = new Message(MessageType.PEER_LIST,
                                                        Tracker.serverSocketThread.getPeers());
                                        Message.sendMessage(socket.getOutputStream(), peersList);
                                        Message.readMessage(socket.getInputStream());
                                        logSocketAction("UPDATE_STATUS - FIN");
                                        return true;
                                }
                        } catch (UnknownHostException e) {
                                logSocketAction("Intento " + tries + " - Host desconocido");
                        } catch (ConnectionEndException e) {
                                logSocketAction("Desconectado inesperadamente, intento " + tries);
                        } catch (IOException e) {
                                logSocketAction("Intento " + tries + " - IOException");
                        }

                } while (++tries < 3);

                return false;
        }

        protected void logSocketAction(String action) {
                System.out.println(peerInfo.getFullIp() + " - " + action);
        }

}
