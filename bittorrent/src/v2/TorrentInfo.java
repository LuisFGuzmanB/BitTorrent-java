package v2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

import peer.Config;
import util.bencode.BDecoder;
import util.bencode.BDict;
import util.bencode.BObject;
import util.lib.Torrent;

public class TorrentInfo implements Serializable {
        private String tracker;
        /**
         * Name of the file
         */
        private String name;

        /**
         * Size of the file in bytes
         */
        private int length;
        private int pieceLength;
        private String[] pieces;

        // ----------------------------------------
        // Constructores y factory
        // ----------------------------------------
        public TorrentInfo(String tracker, String name, int length, int pieceLength, String[] pieces) {
                this.tracker = tracker;
                this.name = name;
                this.length = length;
                this.pieceLength = pieceLength;
                this.pieces = pieces;
        }

        public static TorrentInfo[] deserializeDownloadTorrents() {
                File dir = new File(Config.DIR_TORRENTS());
                File[] files = dir.listFiles();
                if (files == null) {
                        return new TorrentInfo[0];
                }

                files = Arrays.stream(files).filter(f -> f.getName().endsWith(".ser")).toArray(File[]::new);

                TorrentInfo[] torrents = new TorrentInfo[files.length];
                for (int i = 0; i < files.length; ++i) {
                        try (FileInputStream fileIn = new FileInputStream(files[i])) {
                                torrents[i] = (TorrentInfo) new ObjectInputStream(fileIn).readObject();
                        } catch (IOException | ClassNotFoundException e) {
                                System.err.println("Error al leer el archivo: " + e.getMessage());
                        }
                }

                return torrents;
        }

        /**
         * Creates a TorrentInfo object from a .torrent file.
         * 
         * @param path path to the .torrent file
         * @return a TorrentInfo object or null if the file is invalid
         */
        public static TorrentInfo fromFile(String path) {
                BObject[] metainfo;
                TorrentInfo torrentInfo = new TorrentInfo("", "", 0, 0, null);

                /* Leer y parsear a BObject informacion del .torrent */
                try {
                        String encoded = null;
                        byte[] torrentData = Files.readAllBytes(Paths.get(path));

                        encoded = new String(torrentData, StandardCharsets.US_ASCII);
                        encoded = encoded.trim();

                        metainfo = BDecoder.read(encoded);
                } catch (IOException ex) {
                        System.err.print("Error: cannot open " + path);
                        return null;
                }

                if (!validateMetainfo(metainfo)) {
                        return null;
                }

                BDict metaDict = (BDict) metainfo[0];

                if (!validateInfoDict(metaDict)) {
                        return null;
                }

                BDict infoDict = (BDict) metaDict.get("info");

                try {
                        parseTracker(metaDict, torrentInfo);
                        parseInfoDict(infoDict, torrentInfo);
                } catch (TorrentInfoException e) {
                        System.err.print(e.getMessage());
                        return null;
                }

                return torrentInfo;
        }

        public static void toFile(TorrentInfo torrentInfo) {
                // ? checar si es necesario implementar este método

                // ? esta es una opción para guardar el objeto TorrentInfo en un archivo
                // ? pero puede no ser la mejor opción, ya que el archivo no queda como un
                // ? .torrent y se tendría que implementar un método para leer el archivo
                // ? y crear un objeto TorrentInfo a partir de él, además de considerarlo
                // ? para los archivos en descarga

                // Implementar el código para guardar el objeto TorrentInfo en un archivo

                // Aquí puedes usar la clase ObjectOutputStream para escribir el objeto en un
                // archivo
                // y la clase FileOutputStream para crear el flujo de salida hacia el archivo.
                // Por ejemplo:
                try (FileOutputStream fileOut = new FileOutputStream(
                                Config.DIR_TORRENTS() + torrentInfo.getName() + ".ser")) {

                        ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
                        objectOut.writeObject(torrentInfo);
                        objectOut.close();

                } catch (IOException e) {
                        System.err.println("Error al guardar el objeto TorrentInfo en el archivo: " + e.getMessage());
                }
        }

        // ----------------------------------------
        // Métodos
        // ----------------------------------------
        public static File getLocalTorrentFile(String name) {
                if (name == null || name.isEmpty()) {
                        return null;
                }

                return new File(Config.DIR_TORRENTS() + name + ".torrent");
        }

        public static File getLocalFile(TorrentInfo torrentInfo) {
                if (torrentInfo == null || torrentInfo.getName() == null) {
                        return null;
                }

                return new File(Config.DIR_PRINCIPAL() + torrentInfo.getName());
        }

        public static boolean validateLocalExistence(TorrentInfo torrentInfo) {
                File file = getLocalFile(torrentInfo);
                return file.exists();
        }

        /**
         * Devuelve los bytes de la pieza desde el archivo principal tomando en cuenta
         * el desplazamiento
         * 
         * @param piece
         * @return
         */
        public static byte[] getPieceData(PieceInfo piece) {
                if (piece == null) {
                        return new byte[0];
                }

                TorrentInfo torrentInfo = piece.getTorrentInfo();
                int pieceIndex = piece.getIndex();
                int pieceBegin = piece.getBegin();
                int offset = pieceIndex * torrentInfo.getPieceLength() + pieceBegin;

                File file = new File(
                                Config.DIR_PRINCIPAL() + torrentInfo.getName());
                if (!file.exists()) {
                        return new byte[0];
                }

                byte[] pieceData = new byte[piece.getPieceLength()];
                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                        fileInputStream.skip(offset);
                        fileInputStream.read(pieceData);
                } catch (IOException e) {
                        e.printStackTrace();
                }

                return pieceData;
        }

        // ----------------------------------------
        // Métodos privados para parsear el .torrent
        // ----------------------------------------
        /**
         * Validates the metainfo of a .torrent file.
         * 
         * @param metainfo the metainfo to be validated
         * @return true if the metainfo is valid, false otherwise
         */
        private static boolean validateMetainfo(BObject[] metainfo) {
                if (metainfo.length != 1 || !(metainfo[0] instanceof BDict)) {
                        System.err.print("error: invalid .torrent file");
                        return false;
                }
                return true;
        }

        private static boolean validateInfoDict(BDict infoDict) {
                if (!infoDict.containsKey("info")) {
                        System.err.print("error: no info field specified in .torrent file");
                        return false;
                }
                return true;
        }

        private static void parseInfoDict(BDict infoDict, TorrentInfo torrentInfo) throws TorrentInfoException {
                parseLength(infoDict, torrentInfo);
                parsePieceLength(infoDict, torrentInfo);
                parseName(infoDict, torrentInfo);
                parsePieces(infoDict, torrentInfo);
        }

        private static void parseTracker(BDict infoDict, TorrentInfo torrentInfo) throws TorrentInfoException {
                BObject tracker = infoDict.get("announce");
                if (tracker == null) {
                        throw new TorrentInfoException("tracker");
                }
                torrentInfo.setTracker(tracker.print());
                logDebug("got tracker " + torrentInfo.getTracker());
        }

        private static void parseLength(BDict infoDict, TorrentInfo torrentInfo) throws TorrentInfoException {
                BObject length = infoDict.get("length");
                if (length == null) {
                        throw new TorrentInfoException("length");
                }
                torrentInfo.setLength(Integer.parseInt(length.print()));
                logDebug("got pieces " + torrentInfo.getLength());
        }

        private static void parsePieceLength(BDict infoDict, TorrentInfo torrentInfo) throws TorrentInfoException {
                BObject plen = infoDict.get("piece length");
                if (plen == null) {
                        throw new TorrentInfoException("piece length");
                }

                torrentInfo.setPieceLength(Integer.parseInt(plen.print()));
                logDebug("got pieceLength " + torrentInfo.getPieceLength());

                int numberPieces = torrentInfo.getLength() / torrentInfo.getPieceLength();
                if (torrentInfo.getLength() % torrentInfo.getPieceLength() > 0) {
                        ++numberPieces;
                }
                logDebug("Got numPieces " + numberPieces);
        }

        private static void parseName(BDict infoDict, TorrentInfo torrentInfo) throws TorrentInfoException {
                BObject sname = infoDict.get("name");
                if (sname == null) {
                        throw new TorrentInfoException("name");
                }
                torrentInfo.setName(sname.print());
                logDebug("got name " + torrentInfo.getName());
        }

        private static void parsePieces(BDict infoDict, TorrentInfo torrentInfo) throws TorrentInfoException {
                BObject sha = infoDict.get("pieces");
                if (sha == null) {
                        throw new TorrentInfoException("SHA1");
                }

                String piecesSHA1 = sha.print();
                if (piecesSHA1.length() % Torrent.SHA1_HASH_SIZE != 0) {
                        throw new TorrentInfoException("SHA1");
                }

                torrentInfo.setPieces(new String[piecesSHA1.length() / Torrent.SHA1_HASH_SIZE]);
                for (int i = 0; i < torrentInfo.getPieces().length; ++i) {
                        String s = piecesSHA1.substring(Torrent.SHA1_HASH_SIZE * i, Torrent.SHA1_HASH_SIZE * (i + 1));
                        torrentInfo.getPieces()[i] = s;
                }

                if (Config._DEBUG) {
                        logDebug("Got the following SHA1 pieces:");
                        for (int i = 0; i < torrentInfo.getPieces().length; ++i) {
                                logDebug(torrentInfo.getPieces()[i]);
                        }
                }
        }

        // ----------------------------------------
        // Getters y Setters
        // ----------------------------------------

        public String getTracker() {
                return tracker;
        }

        public void setTracker(String tracker) {
                this.tracker = tracker;
        }

        public String getName() {
                return name;
        }

        public void setName(String name) {
                this.name = name;
        }

        public int getLength() {
                return length;
        }

        public void setLength(int length) {
                this.length = length;
        }

        public int getPieceLength() {
                return pieceLength;
        }

        public void setPieceLength(int pieceLength) {
                this.pieceLength = pieceLength;
        }

        public String[] getPieces() {
                return pieces;
        }

        public void setPieces(String[] pieces) {
                this.pieces = pieces;
        }

        public static void logDebug(String str) {
                if (Config._DEBUG) {
                        System.err.println(str);
                }
        }

        // ----------------------------------------
        // Métodos sobreescritos
        // ----------------------------------------

        @Override
        public String toString() {
                return "Name: " + name;
        }

        public String toFullString() {
                return "Tracker: " + tracker + "\n" + "Name: " + name + "\n" + "Length: " + length + "\n"
                                + "PieceLength: " + pieceLength + "\n" + "Pieces: " + Arrays.toString(pieces) + "\n";
        }

        @Override
        public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + length;
                result = prime * result + Objects.hashCode(name);
                result = prime * result + pieceLength;
                result = prime * result + Arrays.hashCode(pieces);
                result = prime * result + Objects.hashCode(tracker);
                return result;
        }

        @Override
        public boolean equals(Object obj) {
                if (this == obj)
                        return true;
                if (!(obj instanceof TorrentInfo))
                        return false;
                TorrentInfo other = (TorrentInfo) obj;
                if (length != other.length)
                        return false;
                if (!Objects.equals(name, other.name))
                        return false;
                if (pieceLength != other.pieceLength)
                        return false;
                if (!Arrays.equals(pieces, other.pieces))
                        return false;
                if (!Objects.equals(tracker, other.tracker))
                        return false;
                return true;
        }
}
