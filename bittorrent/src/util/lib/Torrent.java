package util.lib;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
/*Holaa */
/***
 * Torrent.java: generador del archivo .torrent
 * 
 * @see: https://stackoverflow.com/questions/2032876/how-can-i-generate-a-torrent-in-java
 */

public class Torrent {

        public static final int SHA1_HASH_SIZE = 20;
        public static final int PIECE_LENGTH = 512 * 1024;

        private static void encodeObject(Object o, OutputStream out) throws IOException {
                if (o instanceof String)
                        encodeString((String) o, out);
                else if (o instanceof Map<?, ?>){
                        @SuppressWarnings("unchecked")
                        Map<String, ?> map = (Map<String, ?>) o;
                        encodeMap(map,out) ; 
                }
                else if (o instanceof byte[])
                        encodeBytes((byte[]) o, out);
                else if (o instanceof Number)
                        encodeLong(((Number) o).longValue(), out);
                else
                        throw new Error("Unencodable type");
        }

        private static void encodeLong(long value, OutputStream out) throws IOException {
                out.write('i');
                out.write(Long.toString(value).getBytes("US-ASCII"));
                out.write('e');
        }

        private static void encodeBytes(byte[] bytes, OutputStream out) throws IOException {
                out.write(Integer.toString(bytes.length).getBytes("US-ASCII"));
                out.write(':');
                out.write(bytes);
        }

        private static void encodeString(String str, OutputStream out) throws IOException {
                encodeBytes(str.getBytes("UTF-8"), out);
        }

        private static void encodeMap(Map<String, ?> map, OutputStream out) throws IOException {
                // Sort the map. A generic encoder should sort by key bytes
                SortedMap<String, Object> sortedMap = new TreeMap<>(map);
                out.write('d');
                for (Map.Entry<String, Object> e : sortedMap.entrySet()) {
                        encodeString(e.getKey(), out);
                        encodeObject(e.getValue(), out);
                }
                out.write('e');
        }

        public static byte[] hash(byte[] bytes) {
                MessageDigest sha1;
                try {
                        sha1 = MessageDigest.getInstance("SHA");
                } catch (NoSuchAlgorithmException e) {
                        throw new Error("SHA1 not supported");
                }
                return sha1.digest(bytes);
        }

        private static byte[] hashPieces(File file, int pieceLength) throws IOException {
                MessageDigest sha1;
                try {
                        sha1 = MessageDigest.getInstance("SHA");
                } catch (NoSuchAlgorithmException e) {
                        throw new Error("SHA1 not supported");
                }
                InputStream in = new FileInputStream(file);
                ByteArrayOutputStream pieces = new ByteArrayOutputStream();
                byte[] bytes = new byte[pieceLength];
                int pieceByteCount = 0, readCount = in.read(bytes, 0, pieceLength);
                while (readCount != -1) {
                        pieceByteCount += readCount;
                        sha1.update(bytes, 0, readCount);
                        if (pieceByteCount == pieceLength) {
                                pieceByteCount = 0;
                                pieces.write(sha1.digest());
                        }
                        readCount = in.read(bytes, 0, pieceLength - pieceByteCount);
                }
                in.close();
                if (pieceByteCount > 0)
                        pieces.write(sha1.digest());
                return pieces.toByteArray();
        }

        public static void createTorrent(File outTorrentFile, File inputFile, String announceURL) throws IOException {
                Map<String, Object> info = new HashMap<>();
                info.put("name", inputFile.getName());
                info.put("length", inputFile.length());
                info.put("piece length", PIECE_LENGTH);
                info.put("pieces", hashPieces(inputFile, PIECE_LENGTH));
                Map<String, Object> metainfo = new HashMap<>();
                metainfo.put("announce", announceURL);
                metainfo.put("info", info);
                OutputStream out = new FileOutputStream(outTorrentFile);
                encodeMap(metainfo, out);
                out.close();
        }

        private Torrent() {
        }

        
}
