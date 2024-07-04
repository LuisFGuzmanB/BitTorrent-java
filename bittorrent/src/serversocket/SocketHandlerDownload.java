package serversocket;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import peer.Config;
import util.lib.Torrent;
import v2.ConnectionEndException;
import v2.Message;
import v2.MessageType;
import v2.PeerInfo;
import v2.PieceInfo;
import v2.TorrentDownload;

public class SocketHandlerDownload extends SocketHandler {
        public static final int OUTPUT_BUFFER_SIZE = 256 * 1024;

        private TorrentDownload torrentDownload;
        private PeerInfo peerInfo;

        public SocketHandlerDownload(TorrentDownload torrentDownload, PeerInfo peerInfo) throws IOException {
                super(PeerInfo.connect(peerInfo), null);
                this.torrentDownload = torrentDownload;
                this.peerInfo = peerInfo;
        }

        @Override
        public void run() {
                try {
                        // Loop de descarga
                        while (torrentDownload.getDownloadedPieces() < torrentDownload.getTotalPieces()) {
                                Message replyPiece = null;

                                replyPiece = loopValidPieceReply();
                                if (replyPiece == null || replyPiece.getPiece() == null) {
                                        logSocketAction("No se pudo descargar la pieza");
                                        break;
                                }

                                // Guardar pieza
                                logSocketAction("Guardando pieza " + replyPiece.getPiece().getIndex());
                                if (savePiece(replyPiece)) {
                                        logSocketAction("Pieza guardada correctamente");
                                        torrentDownload.addDownloadedPiece();
                                } else {
                                        logSocketAction("Pieza guardada incorrectamente");
                                        PieceInfo.deletePieceData(replyPiece.getPiece());
                                        torrentDownload.addPiece(replyPiece.getPiece());
                                }
                        }

                        Message.sendMessage(outputStream, Message.END_MESSAGE);
                } catch (ConnectionEndException e) {
                        logSocketAction("Desconexion inesperada");
                } finally {
                        logSocketAction("Desconectado");
                        torrentDownload.removeDownloadSocket(peerInfo);
                        close();
                }
        }

        /**
         * Guarda la pieza recibida en un archivo temporal
         * 
         * @param m
         * @return <code>true</code> si se guardó correctamente toda la pieza,
         *         <code>false</code> si no
         */
        private boolean savePiece(Message m) {
                File piece = PieceInfo.getPieceLocalFile(m.getPiece());

                if (piece == null) {
                        logSocketAction("No se pudo obtener el archivo de la pieza");
                        return false;
                }

                if (!piece.exists()) {
                        try {
                                piece.createNewFile();
                        } catch (IOException e) {
                                System.out.println(piece.getPath() + piece.getName());
                                logSocketAction("No se pudo crear el archivo de la pieza");
                                e.printStackTrace();
                                System.exit(1);
                                return false;
                        }
                }

                 // Recibir el stream de bytes
                int bytesRecibidos = 0;
                try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(piece),
                                OUTPUT_BUFFER_SIZE)) {
                        while (inputStream.available() > 0) {
                                bos.write(inputStream.read());
                                bytesRecibidos++;
                        }

                        System.out.println("Bytes recibidos: " + bytesRecibidos + " de "
                                        + m.getPiece().getPieceLength());

                } catch (IOException e) {
                        return false;
                }

                // Validar pieza con el hash
                m.getPiece().setBegin(0);
                byte[] pieceData = new byte[m.getPiece().getPieceLength()];
                int bytesLeidos = PieceInfo.getPieceData(m.getPiece(), pieceData);
                if (bytesLeidos != pieceData.length) {
                        return false;
                }

                String pieceHash = new String(Torrent.hash(pieceData), StandardCharsets.US_ASCII);
                String receivedHash = m.getPiece().getTorrentInfo().getPieces()[m.getPiece().getIndex()];

                if (!receivedHash.equals(pieceHash)) {
                        return false;
                }

                return true;
        }

        /**
         * Regresa una pieza válida del peer, si no hay piezas disponibles, regresa
         * <code>null</code>
         * 
         * @implNote Se implementan reintentos, por lo que se tiene la certeza de que
         *           se regresará una pieza válida o mejor cerrar la conexión
         * 
         * @param m
         * @return
         * @throws ConnectionEndException
         */
        private Message loopValidPieceReply() throws ConnectionEndException {

                Message replyPiece = null;
                // Loop de piezas hasta que se reciba una pieza válida
                PieceInfo randomPiece;
                Message requestPiece;

                int intentosPeer = 0; // Intentos de descarga con el peer
                int intentosPieza = 2;// Multiplicador de intentos de descarga, se
                                      // incrementa cada vez que no hay piezas disponibles
                                      // para descargar, para evitar que se quede en un
                                      // loop demasiado largo

                do {
                        randomPiece = torrentDownload.getRandomPiece();

                        if (randomPiece == null) {
                                intentosPeer++;

                                // Si no hay piezas disponibles, se reduce el número de intentos
                                // por el multiplicador para evitar que se quede en un loop
                                // demasiado largo, pero también para evitar que otro hilo la
                                // haya reservado y que no la pueda descargar
                                intentosPeer *= intentosPieza++;
                                try {
                                        TimeUnit.SECONDS.sleep(2);
                                } catch (InterruptedException e) {
                                        e.printStackTrace();
                                }
                                continue;
                        }

                        requestPiece = new Message(MessageType.REQUEST_PIECE, randomPiece);
                        Message.sendMessage(outputStream, requestPiece);

                        replyPiece = Message.readMessage(inputStream);

                        if (replyPiece == null || replyPiece.getPiece() == null) {
                                intentosPeer++;
                                torrentDownload.addPiece(randomPiece);
                        }
                } while (replyPiece == null && intentosPeer < torrentDownload.getTotalPieces());

                return replyPiece;
        }

        // ----------------------------------------
        // Getters y setters
        // ----------------------------------------

        public PeerInfo getPeerInfo() {
                return peerInfo;
        }
}
