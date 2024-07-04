package peer;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;

import util.lib.Torrent;
import v2.PeerInfo;
import v2.TorrentDownload;
import v2.TorrentInfo;

public class Menu {
        private static final String[] OPTIONS = {
                        "MOSTRAR ESTADO DE LA RED",
                        "COMPARTIR ARCHIVO",
                        "DESCARGAR ARCHIVO"
        };

        private static final String[] STATIC_OPTIONS = {
                        "CONFIGURACION",
                        "SALIR"
        };

        public static void loop() {
                int opcion = 0;

                do {
                        try {
                                System.out.println(" |");
                                System.out.println(" |--------MENU--------");
                                System.out.println(" |");
                                for (int i = 0; i < OPTIONS.length; i++) {
                                        System.out.println(" " + (i + 1) + ". " + OPTIONS[i]);
                                }

                                System.out.println(" |");
                                System.out.println(" 8. " + STATIC_OPTIONS[0]);
                                System.out.println(" 9. " + STATIC_OPTIONS[1]);

                                System.out.print("Ingrese una opcion: ");

                                opcion = Integer.parseInt(System.console().readLine());

                                System.out.println();

                                switch (opcion) {
                                        case 1:
                                                System.out.println("MOSTRAR ESTADO DE LA RED");

                                                PeerInfo peerInfo = Peer.peerInfo;
                                                System.out.println(peerInfo);

                                                Enumeration<PeerInfo> peers = Peer.serverSocketThread.getPeers()
                                                                .elements();
                                                while (peers.hasMoreElements()) {
                                                        PeerInfo peer = peers.nextElement();
                                                        System.out.println(peer);
                                                }

                                                break;
                                        case 2:
                                                System.out.println("COMPARTIR ARCHIVO");

                                                uploadFileMenu();

                                                break;
                                        case 3:
                                                System.out.println();
                                                System.out.println("DESCARGAR ARCHIVO");
                                                TorrentInfo torrentToDownload = downloadFileMenu();

                                                if (torrentToDownload == null) {
                                                        break;
                                                }

                                                // Serializarlo para saber que se está descargando y reanudar
                                                // la descarga en caso de que se interrumpa
                                                TorrentInfo.toFile(torrentToDownload);

                                                // Iniciar descarga
                                                TorrentDownload torrentDownload = new TorrentDownload(
                                                                torrentToDownload);
                                                new Thread(torrentDownload).start();
                                                Peer.peerInfo.addDescarga(torrentDownload);

                                                break;
                                        case 8:
                                                Config.show();
                                                break;
                                        case 9:
                                                break;
                                        default:
                                                System.out.println("Opcion invalida");
                                                break;
                                }

                                System.out.println("\n");
                        } catch (NumberFormatException e) {
                                System.out.println("Opcion invalida");
                        }
                } while (opcion != 9);

                System.out.println("Saliendo...");
                System.out.println("Gracias por usar el programa");
                System.exit(0);
        }

        private static TorrentInfo uploadFileMenu() {
                File carpetaPrincipal = new File(Config.DIR_PRINCIPAL());
                File[] archivos = carpetaPrincipal.listFiles();

                if (archivos == null) {
                        System.out.println("No hay archivos en la carpeta principal (" + Config.DIR_PRINCIPAL()
                                        + ")");
                        return null;
                }

                System.out.println(" |");
                System.out.println(" |--------ARCHIVOS EN LA CARPETA PRINCIPAL--------");
                System.out.println(" |");
                int i = 1;
                for (File file : archivos) {
                        if (file.isFile()) {
                                System.out.println(" " + i + ". " + file.getName());
                                i++;
                        }
                }

                System.out.println(" |");
                System.out.println(" " + i + ". " + "VOLVER");

                System.out.print("Ingrese una opcion: ");

                int opcion = Integer.parseInt(System.console().readLine());

                if (opcion == i) {
                        return null;
                }

                File torrentFile = TorrentInfo.getLocalTorrentFile(archivos[opcion - 1].getName());

                if (!torrentFile.exists()) {
                        try {
                                Torrent.createTorrent(
                                                TorrentInfo.getLocalTorrentFile(archivos[opcion - 1].getName()),
                                                archivos[opcion - 1],
                                                Config.TRACKER_IP() + ":" + Config.TRACKER_PORT());
                        } catch (IOException e) {
                                System.out.println("Error al crear el torrent");
                                e.printStackTrace();
                                System.exit(1);
                        }
                }

                TorrentInfo torrentToShare = TorrentInfo.fromFile(torrentFile.getPath());

                if (Peer.peerInfo.getArchivosCompartidos().contains(torrentToShare)) {
                        System.out.println("Ya se está compartiendo este archivo");
                        return null;
                }

                Peer.peerInfo.addArchivoCompartido(torrentToShare);
                return torrentToShare;
        }

        /**
         * Muestra la lista de archivos en la red y permite seleccionar uno
         * 
         * @return TorrentInfo del archivo seleccionado
         */
        private static TorrentInfo downloadFileMenu() {
                // MOSTRAR LISTA DE ARCHIVOS EN LA RED (peer list)
                Enumeration<PeerInfo> peers = Peer.serverSocketThread.getPeers().elements();
                HashMap<String, TorrentInfo> archivos = new HashMap<>();

                while (peers.hasMoreElements()) {
                        PeerInfo peerInfo = peers.nextElement();
                        peerInfo.getArchivosCompartidos().forEach(torrentInfo -> {
                                archivos.put(torrentInfo.getName(), torrentInfo);
                        });
                }

                System.out.println(" |");
                System.out.println(" |--------ARCHIVOS EN LA RED--------");
                System.out.println(" |");

                int i = 1;
                for (TorrentInfo torrentInfo : archivos.values()) {
                        System.out.println(" " + i + ". " + torrentInfo.getName());
                        i++;
                }

                System.out.println(" |");
                System.out.println(" " + i + ". " + "VOLVER");

                System.out.print("Ingrese una opcion: ");

                int opcion = Integer.parseInt(System.console().readLine());

                if (opcion == i) {
                        return null;
                }

                return (TorrentInfo) archivos.values().toArray()[opcion - 1];
        }

        private Menu() {
        }
}
