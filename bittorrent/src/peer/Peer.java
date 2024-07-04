package peer;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;

import serversocket.ServerSocketThread;
import serversocket.SocketHandlerPeer;
import v2.Message;
import v2.MessageType;
import v2.PeerInfo;
import v2.TorrentDownload;

public class Peer {
        public static ServerSocketThread<SocketHandlerPeer> serverSocketThread;
        public static PeerInfo peerInfo;

        public static void main(String[] args) {
                try {
                        // Set the console output encoding to UTF-8
                        System.setOut(new PrintStream(System.out, true, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                }

                attachShutDownHook();

                System.out.println("--------Peer--------");
                // CARGAR CONFIGURACION
                // CARGAR ARCHIVO DE CONFIGURACION

                if (args.length > 0 && args[0].equals("-n")) {
                        Config.reconfig("bin/.env.peer" + args[1]);
                }

                Config.load();
                Config.save();

                peerInfo = PeerInfo.fromLocal();
                System.out.println(peerInfo.toString());

                // INICIAR SERVIDOR SOCKET
                serverSocketThread = ServerSocketThread.getPeerServerSocket(Config.PEER_PORT());
                new Thread(serverSocketThread).start();

                // CONECTAR AL TRACKER
                Socket trackerSocket = null;
                try {
                        trackerSocket = new Socket(Config.TRACKER_IP(), Config.TRACKER_PORT());
                } catch (IOException e) {
                        System.out.println("No se pudo conectar al tracker");
                        e.printStackTrace();
                        System.exit(1);
                }

                // ENVIAR MENSAJE ANNOUNCE_STATUS
                Message announceStatus = new Message(MessageType.ANNOUNCE_STATUS, peerInfo);
                new Thread(new SocketHandlerPeer(trackerSocket, serverSocketThread,
                                peerInfo, announceStatus)).start();

                // Empezar descargas
                peerInfo.getArchivosDescargando().forEach(torrrentInfo -> {
                        TorrentDownload torrentDownload = new TorrentDownload(torrrentInfo);
                        new Thread(torrentDownload).start();
                        peerInfo.getDescargas().add(torrentDownload);
                });

                Menu.loop();
        }

        private static void attachShutDownHook() {
                Runtime.getRuntime().addShutdownHook(new Thread() {
                        @Override
                        public void run() {
                                System.out.println("Cerrando peer");
                                serverSocketThread.close();

                                try (Socket socket = new Socket(Config.TRACKER_IP(), Config.TRACKER_PORT())) {
                                        Message announceStatus = new Message(MessageType.NOTIFY_PEER_OFFLINE, peerInfo);
                                        Message.sendMessage(socket.getOutputStream(), announceStatus);

                                } catch (Exception e) {
                                        System.out.println("Desconexion del tracker fallida");
                                }

                                peerInfo.getDescargas().forEach(TorrentDownload::close);

                                System.out.println("Peer cerrado");
                        }
                });
        }
}
