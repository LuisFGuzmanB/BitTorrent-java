package v2;

public class TorrentInfoException extends Exception {
        public TorrentInfoException(String field) {
                super("Error: invalid " + field + " in .torrent file");
        }
}
