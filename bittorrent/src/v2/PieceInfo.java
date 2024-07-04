package v2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import peer.Config;
import util.lib.Torrent;

public class PieceInfo implements Serializable {

        /**
         * Información del torrent
         */
        private TorrentInfo torrentInfo;

        /**
         * Indice de la pieza
         */
        private int index;

        /**
         * Desplazamiento dentro de una pieza de un bloque solicitado
         */
        private int begin;

        private transient boolean isDownloaded = false;

        // ----------------------------------------
        // Constructores y factory
        // ----------------------------------------

        /**
         * Constructor para un mensaje REQUEST_PIECE,
         * inicializa el offset y el bloque a cero
         */
        public PieceInfo(TorrentInfo torrentInfo, int index) {
                this(torrentInfo, index, 0);
        }

        /**
         * Constructor para un mensaje REPLY_PIECE o REQUEST_PIECE (en caso de que se
         * haya interrumpido la transferencia)
         */
        public PieceInfo(TorrentInfo torrentInfo, int index, int begin) {
                this.torrentInfo = torrentInfo;
                this.index = index;
                this.begin = begin;
        }

        public static PieceInfo[] fromLocalTorrentInfo(TorrentInfo torrentInfo) {
                int numberPieces = torrentInfo.getLength() / torrentInfo.getPieceLength();
                if (torrentInfo.getLength() % torrentInfo.getPieceLength() > 0) {
                        ++numberPieces;
                }

                PieceInfo[] pieces = new PieceInfo[numberPieces];
                for (int i = 0; i < numberPieces; ++i) {
                        pieces[i] = new PieceInfo(torrentInfo, i);
                }

                // Verificar que las piezas descargadas sean válidas, si no, se marcan como no
                // descargadas y se borra el .part local. También se marca el offset
                for (PieceInfo pieceInfo : pieces) {
                        byte[] pieceData = new byte[pieceInfo.getPieceLength()];
                        int bytesLeidos = PieceInfo.getPieceData(pieceInfo, pieceData);
                        if (bytesLeidos == -1) {
                                pieceInfo.setDownloaded(false);
                                pieceInfo.setBegin(0);
                                deletePieceData(pieceInfo);
                        } else if (bytesLeidos == pieceData.length) {
                                // Validar hash
                                String pieceHash = new String(Torrent.hash(pieceData), StandardCharsets.US_ASCII);
                                String receivedHash = pieceInfo.getTorrentInfo().getPieces()[pieceInfo.getIndex()];
                                if (pieceHash.equals(receivedHash)) {
                                        pieceInfo.setDownloaded(true);
                                } else {
                                        pieceInfo.setDownloaded(false);
                                        deletePieceData(pieceInfo);
                                }
                        } else {
                                pieceInfo.setDownloaded(false);
                                pieceInfo.setBegin(bytesLeidos);
                        }
                }

                return pieces;
        }

        // ----------------------------------------
        // Métodos
        // ----------------------------------------

        /**
         * Devuelve el tamaño de la pieza, tomando en cuenta si es el último bloque
         */
        public int getPieceLength() {
                if (index == torrentInfo.getPieces().length - 1) {
                        return torrentInfo.getLength() % torrentInfo.getPieceLength();
                }
                return torrentInfo.getPieceLength();
        }

        /**
         * Devuelve los bytes de la pieza desde un archivo .part tomando en cuenta el
         * desplazamiento
         * 
         * @param piece     pieza a obtener
         * @param pieceData arreglo de bytes donde se guardará la pieza (forzosamente
         *                  debe ser igual o mayor al tamaño de la pieza), no se guarda
         *                  con offset
         * @return número de bytes leídos
         */
        public static int getPieceData(PieceInfo piece, byte[] pieceData) {
                int bytesLeidos = 0;
                if (piece == null || pieceData == null || pieceData.length < piece.getPieceLength()) {
                        return bytesLeidos;
                }

                int pieceBegin = piece.getBegin();

                File pieceFile = getPieceLocalFile(piece);
                if (!pieceFile.exists()) {
                        return bytesLeidos;
                }

                try (FileInputStream fileInputStream = new FileInputStream(pieceFile)) {
                        fileInputStream.skip(pieceBegin);
                        bytesLeidos = fileInputStream.read(pieceData);
                } catch (IOException e) {
                        return -1;
                }

                return bytesLeidos;
        }

        public static void deletePieceData(PieceInfo piece) {
                File pieceFile = getPieceLocalFile(piece);
                if (pieceFile.exists()) {
                        pieceFile.delete();
                }
        }

        public static File getPieceLocalFile(PieceInfo piece) {
                if (piece == null || piece.getTorrentInfo() == null || piece.getTorrentInfo().getName() == null
                                || piece.getIndex() < 0) {
                        return null;
                }

                return new File(Config.DIR_TEMP() + "torrent" + piece.getTorrentInfo().hashCode() + "/part"
                                + piece.getIndex()
                                + ".part");
        }

        // ----------------------------------------
        // Getters y setters
        // ----------------------------------------

        public String getHash() {
                return torrentInfo.getPieces()[index];
        }

        public TorrentInfo getTorrentInfo() {
                return torrentInfo;
        }

        public void setTorrentInfo(TorrentInfo torrentInfo) {
                this.torrentInfo = torrentInfo;
        }

        public int getIndex() {
                return index;
        }

        public void setIndex(int index) {
                this.index = index;
        }

        public int getBegin() {
                return begin;
        }

        public void setBegin(int begin) {
                this.begin = begin;
        }

        public boolean isDownloaded() {
                return isDownloaded;
        }

        public void setDownloaded(boolean isDownloaded) {
                this.isDownloaded = isDownloaded;
        }

        // ----------------------------------------
        // toString
        // ----------------------------------------

        @Override
        public String toString() {
                return "PieceInfo [torrentInfo=" + torrentInfo + ", index=" + index + ", begin=" + begin
                                + ", isDownloaded=" + isDownloaded + "]";
        }
}
