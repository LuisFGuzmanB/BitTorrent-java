package v2;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import util.lib.IpChecker;

public class PeerInfo implements Serializable {
        private String ip;
        private int port;
        private List<TorrentInfo> archivosCompartidos;
        private List<TorrentInfo> archivosDescargando;
        private transient List<TorrentDownload> descargas;

        // ----------------------------------------
        // Constructores y factory
        // ----------------------------------------

        public PeerInfo() {
        }

        public PeerInfo(String ip, int port, List<TorrentInfo> archivosCompartidos) {
                this.ip = ip;
                this.port = port;
                this.archivosCompartidos = archivosCompartidos;
                this.archivosDescargando = new ArrayList<>();
                this.descargas = new ArrayList<>();
        }

        /**
         * Crea un PeerInfo con la información del peer local
         * 
         * @implNote Las descargas no se inician, solo se cargan los TorrentInfo
         * 
         * @return
         */
        public static PeerInfo fromLocal() {
                PeerInfo peerInfo = new PeerInfo();

                // Obtener IP
                if (peer.Config.USE_LOCAL_IP()) {
                        String localIp;
                        try {
                                localIp = IpChecker.getLocalIp();
                        } catch (UnknownHostException e) {
                                System.out.println("Error al obtener la IP local, checar conexion de red");
                                e.printStackTrace();
                                System.exit(1);
                                return null;
                        }

                        peerInfo.setIp(localIp);
                } else {
                        String publicIp;
                        try {
                                publicIp = IpChecker.getPublicIp();
                        } catch (IOException e) {
                                System.out.println("Error al obtener la IP publica, checar URL de la API");
                                e.printStackTrace();
                                System.exit(1);
                                return null;
                        }

                        peerInfo.setIp(publicIp);
                }

                // Obtener puerto
                peerInfo.setPort(peer.Config.PEER_PORT());

                peerInfo.setArchivosCompartidos(findSharedFiles());
                peerInfo.setArchivosDescargando(new ArrayList<>());

                TorrentInfo[] archivosDescargando = TorrentInfo.deserializeDownloadTorrents();
                if (archivosDescargando != null) {
                        Arrays.stream(archivosDescargando).forEach(torrentInfo -> {
                                peerInfo.getArchivosDescargando().add(torrentInfo);
                        });
                }

                peerInfo.setDescargas(new ArrayList<>());

                return peerInfo;
        }

        private static List<TorrentInfo> findSharedFiles() {
                File torrentsDir = new File(peer.Config.DIR_TORRENTS());
                File[] files = torrentsDir.listFiles();
                List<TorrentInfo> archivosCompartidos = new ArrayList<>();

                for (File file : files) {
                        // Filtrar archivos .torrent solamente
                        if (file.isFile() && file.getName().endsWith(".torrent")) {

                                TorrentInfo torrentInfo = TorrentInfo.fromFile(file.getPath());

                                if (TorrentInfo.validateLocalExistence(torrentInfo)) {
                                        archivosCompartidos.add(torrentInfo);
                                }
                        }
                }
                return archivosCompartidos;
        }

        // ----------------------------------------
        // Métodos
        // ----------------------------------------

        public static Socket connect(PeerInfo peerInfo) throws IOException {
                Socket socket = null;
                int tries = 0;
                do {
                        try {
                                socket = new Socket(peerInfo.getIp(), peerInfo.getPort());
                        } catch (UnknownHostException e) {
                                System.out.println("Intento " + tries + " - Host desconocido");
                        } catch (IOException e) {
                                System.out.println("Intento " + tries + " - IOException");
                                e.printStackTrace();
                        }
                } while (socket == null && ++tries < 3);
                return socket;
        }

        // ----------------------------------------
        // Getters y setters
        // ----------------------------------------

        public List<TorrentDownload> getDescargas() {
                return descargas;
        }

        public void setDescargas(List<TorrentDownload> descargas) {
                this.descargas = descargas;
        }

        /**
         * Agrega una descarga a la lista de descargas del peer para mantener un
         * registro
         * de las descargas en curso. No se serializa.
         * 
         * @implNote Se
         * 
         * @param descarga
         */
        public void addDescarga(TorrentDownload descarga) {
                this.descargas.add(descarga);
        }

        public void removeDescarga(TorrentDownload descarga) {
                this.descargas.remove(descarga);
        }

        public List<TorrentInfo> getArchivosDescargando() {
                return archivosDescargando;
        }

        public void setArchivosDescargando(List<TorrentInfo> archivosDescargando) {
                this.archivosDescargando = archivosDescargando;
        }

        public TorrentInfo getTorrentInfo(String nombre) {
                for (TorrentInfo torrentInfo : archivosCompartidos) {
                        if (torrentInfo.getName().equals(nombre)) {
                                return torrentInfo;
                        }
                }
                return null;
        }

        public String getFullIp() {
                return ip + ":" + port;
        }

        public String getIp() {
                return ip;
        }

        public void setIp(String ip) {
                this.ip = ip;
        }

        public int getPort() {
                return port;
        }

        public void setPort(int port) {
                this.port = port;
        }

        public List<TorrentInfo> getArchivosCompartidos() {
                return archivosCompartidos;
        }

        public void setArchivosCompartidos(List<TorrentInfo> archivosCompartidos) {
                this.archivosCompartidos = archivosCompartidos;
        }

        // ----------------------------------------
        // toString
        // ----------------------------------------

        @Override
        public String toString() {
                return "PeerInfo [ip=" + ip + ", port=" + port
                                + ", archivosCompartidos="
                                + Arrays.toString(archivosCompartidos.toArray(new TorrentInfo[0]))
                                + ", archivosDescargando="
                                + Arrays.toString(archivosDescargando.toArray(new TorrentInfo[0]))
                                + "]";
        }

        public String toStringShort() {
                return "PeerInfo [ip=" + ip + ", port=" + port + ", archivosCompartidos="
                                + Arrays.toString(archivosCompartidos.toArray(new TorrentInfo[0])) + "]";
        }

        public void addArchivoCompartido(TorrentInfo torrentToShare) {
                this.archivosCompartidos.add(torrentToShare);
        }
}
