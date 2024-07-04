package v2;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import peer.Config;
import peer.Peer;
import serversocket.SocketHandlerDownload;

public class TorrentDownload implements Runnable {
        private static final Random random = new Random();

        private TorrentInfo torrentInfo;
        Hashtable<String, PieceInfo> pieces; // ? Se podría considerar dejar las piezas descargadas, ya que se
                                             // ? implementó el campo isDownloaded
        private AtomicInteger downloadedPieces = new AtomicInteger(0);
        private int totalPieces = 0;

        private ExecutorService executorService;
        private AtomicInteger downloadThreads = new AtomicInteger(0);
        private Hashtable<String, SocketHandlerDownload> downloadSockets = new Hashtable<>();

        public TorrentDownload(TorrentInfo torrentInfo) {
                this.torrentInfo = torrentInfo;
                this.pieces = new Hashtable<>();

                // Checar si el directorio temporal del torrent existe
                File torrentPiecesDir = new File(Config.DIR_TEMP() + "torrent" + torrentInfo.hashCode());
                if (!torrentPiecesDir.exists()) {
                        torrentPiecesDir.mkdirs();
                }

                for (PieceInfo pieceInfo : PieceInfo.fromLocalTorrentInfo(torrentInfo)) {
                        pieces.put(pieceInfo.getHash(), pieceInfo);
                        if (pieceInfo.isDownloaded()) {
                                downloadedPieces.incrementAndGet();
                        }
                }
                totalPieces = pieces.size();
                this.executorService = Executors.newFixedThreadPool(Config.MAX_TORRENT_DOWNLOAD_THREADS());
        }

        @Override
        public void run() {
                // Loop de descarga
                while (downloadedPieces.get() < totalPieces || validPieces() < totalPieces) {
                        List<PeerInfo> filteredPeers = filterPeers(Peer.serverSocketThread.getPeers());

                        // Si no hay peers, esperar a que se actualice la lista de peers
                        while (filteredPeers.isEmpty()) {
                                try {
                                        synchronized (Peer.serverSocketThread) {
                                                Peer.serverSocketThread.wait(); // notifyall solo se llama cuando se
                                                                                // actualiza la lista de peers
                                                filteredPeers = filterPeers(
                                                                Peer.serverSocketThread.getPeers());
                                        }
                                } catch (InterruptedException e) {
                                        System.out.println("Error al esperar por la lista de peers");
                                }
                        }
                        // Si hay menos hilos de descarga que el máximo, se puede iniciar conexión con
                        // otro peer para descargar una pieza
                        if (downloadThreads.get() >= 3) {
                                continue;
                        }
                        downloadThreads.incrementAndGet();

                        // Conectar con el peer
                        SocketHandlerDownload socketHandlerDownload = null;
                        int tries = 0;
                        do {
                                try {
                                        socketHandlerDownload = new SocketHandlerDownload(this,
                                                        filteredPeers.get(0));
                                        executorService.execute(socketHandlerDownload);
                                        downloadSockets.put(filteredPeers.get(0).getFullIp(), socketHandlerDownload);
                                } catch (IOException e) {
                                        System.out.println("Intento " + tries + " - IOException");
                                        e.printStackTrace();
                                }
                        } while (socketHandlerDownload == null && ++tries < 3);

                }

                armarArchivo();
                borrarPiezasDescargadas();
                borrarArchivoSerializado();

                Peer.peerInfo.removeDescarga(this);

                System.out.println("Descarga completada para " + torrentInfo.getName());
        }

        // ----------------------------------------
        // Métodos
        // ----------------------------------------

        private void borrarPiezasDescargadas() {
                File file = new File(Config.DIR_TEMP() + "torrent" + torrentInfo.hashCode());
                if (file.exists()) {
                        for (File pieceFile : file.listFiles()) {
                                pieceFile.delete();
                        }
                        file.delete();
                }
        }

        private void borrarArchivoSerializado() {
                File file = new File(Config.DIR_TORRENTS() + torrentInfo.getName() + ".ser");
                if (file.exists()) {
                        file.delete();
                }
        }

        /**
         * Compara la información del gestor de descargas con la información real del
         * archivo descargado. Es un previo para ensamblar el archivo descargado
         * 
         */
        private synchronized int validPieces() {
                // ! Fue la única forma que encontré para poder modificar el valor de la
                // variable dentro del lambda
                AtomicInteger validPieces = new AtomicInteger(0);

                // Comparamos la información del gestor con la información real
                Arrays.stream(PieceInfo.fromLocalTorrentInfo(torrentInfo)).forEach(pieceInfoLocal -> {
                        PieceInfo pieceInfoFromManager = pieces
                                        .get(pieceInfoLocal.getHash());

                        if (pieceInfoFromManager.isDownloaded() == pieceInfoLocal.isDownloaded()) {
                                validPieces.incrementAndGet();
                        } else {
                                pieces.put(pieceInfoLocal.getHash(),
                                                pieceInfoLocal);
                                this.downloadedPieces.decrementAndGet();
                        }

                });

                return validPieces.get();
        }

        private void armarArchivo() {
                // Ensamblar el archivo descargado
                // Aquí se debe agregar el código para ensamblar el archivo descargado
                // Por ejemplo, se puede utilizar un FileOutputStream para escribir los datos de
                // las piezas descargadas en el archivo final
                try (FileOutputStream fileOutputStream = new FileOutputStream(
                                Config.DIR_PRINCIPAL() + torrentInfo.getName());
                                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(
                                                fileOutputStream)) {

                        Arrays.stream(PieceInfo.fromLocalTorrentInfo(torrentInfo)).forEach(pieceInfo -> {
                                System.out.println(pieceInfo.toString());
                                pieceInfo.setBegin(0);
                                byte[] pieceData = new byte[pieceInfo.getPieceLength()];
                                PieceInfo.getPieceData(pieceInfo, pieceData);
                                try {
                                        bufferedOutputStream.write(pieceData);
                                } catch (IOException e) {
                                        System.out.println("Error al ensamblar el archivo descargado");
                                }
                        });
                } catch (IOException e) {
                        System.out.println("Error al ensamblar el archivo descargado");
                        e.printStackTrace();
                }

        }

        private List<PeerInfo> filterPeers(Hashtable<String, PeerInfo> peers) {
                if (peers.isEmpty()) {
                        return new ArrayList<>();
                }

                List<PeerInfo> filteredPeers = new ArrayList<>();

                // Filtrar peers que no tengan el archivo
                peers.forEach((key, peerInfo) -> {
                        if (peerInfo.getArchivosCompartidos().contains(this.torrentInfo)) {
                                filteredPeers.add(peerInfo);
                        }
                });

                // Filtrar peers que ya se estén descargando
                filteredPeers.removeIf(peerInfo -> {
                        return downloadSockets.containsKey(peerInfo.getFullIp());
                });

                return filteredPeers;
        }

        public void addPiece(PieceInfo pieceInfo) {
                pieces.get(pieceInfo.getTorrentInfo().getPieces()[pieceInfo.getIndex()]).setDownloaded(false);
        }

        public void removePiece(PieceInfo pieceInfo) {
                pieces.get(pieceInfo.getTorrentInfo().getPieces()[pieceInfo.getIndex()]).setDownloaded(true);
        }

        public synchronized void addDownloadedPiece() {
                downloadedPieces.incrementAndGet();
        }

        /**
         * Obtiene una pieza disponible para descarga, esto incluye:
         * <ol>
         * <li>Que no se haya descargado</li>
         * <li>Que no se este descargando</li>
         * </ol>
         * 
         * @return Si no hay piezas disponibles, regresa <code>null</code>
         */
        public synchronized PieceInfo getRandomPiece() {
                PieceInfo[] piecesArray = new PieceInfo[0];
                Collection<PieceInfo> piecesCollection = pieces.values();
                piecesCollection.removeIf(PieceInfo::isDownloaded);
                piecesArray = piecesCollection.toArray(piecesArray);

                int numberPieces = piecesArray.length;
                int randomIndex = random.nextInt(numberPieces);

                removePiece(piecesArray[randomIndex]);

                return piecesArray[randomIndex];
        }

        public void removeDownloadSocket(PeerInfo peerInfo) {
                downloadSockets.remove(peerInfo.getFullIp());
                downloadThreads.decrementAndGet();
        }

        // ----------------------------------------
        // Otros
        // ----------------------------------------

        public void close() {
                executorService.shutdown();
        }

        // ------------------ Getters y Setters ------------------

        public synchronized int getDownloadedPieces() {
                return downloadedPieces.get();
        }

        public int getTotalPieces() {
                return totalPieces;
        }
}
