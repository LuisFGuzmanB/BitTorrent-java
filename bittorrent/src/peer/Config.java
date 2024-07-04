package peer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import util.dotenv.Dotenv;

public class Config {
        public static final boolean _DEBUG = true;

        private static String CONFIG_FILE = "bin/.env.peer";
        private static Dotenv dotenv = Dotenv
                        .configure()
                        .filename(CONFIG_FILE)
                        .ignoreIfMissing()
                        .load();

        // --------- Llaves --------- //
        private static final String KEY_DIR_PRINCIPAL = "DIR_PRINCIPAL";
        private static final String KEY_DIR_TORRENTS = "DIR_TORRENTS";
        private static final String KEY_DIR_TEMP = "DIR_TEMP";

        private static final String KEY_TRACKER_IP = "TRACKER_IP";
        private static final String KEY_TRACKER_PORT = "TRACKER_PORT";

        private static final String KEY_USE_LOCAL_IP = "USE_LOCAL_IP";
        private static final String KEY_PEER_PORT = "PEER_PORT";
        private static final String KEY_PEER_MAX_CONNECTIONS = "PEER_MAX_CONNECTIONS";
        private static final String KEY_MAX_TORRENT_DOWNLOAD_THREADS = "MAX_TORRENT_DOWNLOAD_THREADS";

        // --------- Directorios --------- //
        private static String DIR_TORRENTS = "bin/files/internal/torrents/";
        private static String DIR_PRINCIPAL = "bin/files/archivos/";
        private static String DIR_TEMP = "bin/files/internal/temp/";

        // --------- Tracker --------- //
        // Configuración del tracker

        // Nota:
        // Se considera solo un tracker para la aplicación,
        // en caso de que se quiera usar más de un tracker
        // se debe modificar el código

        private static String TRACKER_IP = "";
        private static int TRACKER_PORT = 0;

        // --------- Peer --------- //
        /**
         * Determina si la red TORRENT se ejecuta en modo local
         */
        private static boolean USE_LOCAL_IP = true;
        /**
         * Puerto principal del peer
         */
        private static int PEER_PORT = 0;
        private static int PEER_MAX_CONNECTIONS = 10;
        private static int MAX_TORRENT_DOWNLOAD_THREADS = 3;

        public static void load() {
                final String MSG_DIRENVNF = "Se usara el directorio por defecto: "; // Mensaje de directorio no
                                                                                    // encontrado
                final String MSG_ENVVARNF = "No se encontro la configuracion de: "; // Mensaje de variable de entorno
                                                                                    // no encontrada

                // ------------ 1 Cargar directorios ------------ //
                // * Nota: se usa el operador ternario para que si no se encuentra la variable
                // * de entorno se use el directorio por defecto, esto a su vez porque el setter
                // * valida la existencia del directorio.

                if (dotenv.get(KEY_DIR_TORRENTS) == null) {
                        System.out.println(MSG_ENVVARNF + KEY_DIR_TORRENTS);
                        System.out.println(MSG_DIRENVNF + DIR_TORRENTS);
                }
                DIR_TORRENTS(dotenv.get(KEY_DIR_TORRENTS) != null
                                ? dotenv.get(KEY_DIR_TORRENTS)
                                : DIR_TORRENTS);

                if (dotenv.get(KEY_DIR_PRINCIPAL) == null) {
                        System.out.println(MSG_ENVVARNF + KEY_DIR_PRINCIPAL);
                        System.out.println(MSG_DIRENVNF + DIR_PRINCIPAL);
                }
                DIR_PRINCIPAL(dotenv.get(KEY_DIR_PRINCIPAL) != null
                                ? dotenv.get(KEY_DIR_PRINCIPAL)
                                : DIR_PRINCIPAL);

                if (dotenv.get(KEY_DIR_TEMP) == null) {
                        System.out.println(MSG_ENVVARNF + KEY_DIR_TEMP);
                        System.out.println(MSG_DIRENVNF + DIR_TEMP);

                }
                DIR_TEMP(dotenv.get(KEY_DIR_TEMP) != null
                                ? dotenv.get(KEY_DIR_TEMP)
                                : DIR_TEMP);

                // ------------ 2 Cargar IP y puerto ------------ //

                if (dotenv.get(KEY_TRACKER_IP) == null) {
                        System.out.println(MSG_ENVVARNF + KEY_TRACKER_IP);

                        System.out.println("Ingrese la IP del tracker: ");
                        TRACKER_IP = System.console().readLine();
                } else {
                        TRACKER_IP = dotenv.get(KEY_TRACKER_IP);
                }
                Validator.validarIP(TRACKER_IP);

                if (dotenv.get(KEY_TRACKER_PORT) == null) {
                        System.out.println(MSG_ENVVARNF + KEY_TRACKER_PORT);
                }

                TRACKER_PORT = Validator.validarPuerto(
                                // * Nota: la función acepta un 0 para que se pida el puerto,
                                // * en caso contrario valida el puerto
                                dotenv.get(KEY_TRACKER_PORT) != null
                                                ? Integer.parseInt(dotenv.get(KEY_TRACKER_PORT))
                                                : 0);

                // ------------ 3 Cargar constantes del peer ------------ //

                if (dotenv.get(KEY_USE_LOCAL_IP) == null) {
                        System.out.println(MSG_ENVVARNF + KEY_USE_LOCAL_IP);
                        System.out.println("Se usara el valor por defecto: " + USE_LOCAL_IP);
                }

                if (dotenv.get(KEY_PEER_PORT) == null) {
                        System.out.println(MSG_ENVVARNF + KEY_PEER_PORT);
                }
                PEER_PORT(Validator.validarPuerto(
                                // * Nota: la función acepta un 0 para que se pida el puerto,
                                // * en caso contrario valida el puerto
                                dotenv.get(KEY_PEER_PORT) != null
                                                ? Integer.parseInt(dotenv.get(KEY_PEER_PORT))
                                                : 0));

                if (dotenv.get(KEY_PEER_MAX_CONNECTIONS) == null) {
                        System.out.println(MSG_ENVVARNF + KEY_PEER_MAX_CONNECTIONS);
                        System.out.println("Se usara el valor por defecto: " + PEER_MAX_CONNECTIONS);
                }

                if (dotenv.get(KEY_MAX_TORRENT_DOWNLOAD_THREADS) == null) {
                        System.out.println(MSG_ENVVARNF + KEY_MAX_TORRENT_DOWNLOAD_THREADS);
                        System.out.println("Se usara el valor por defecto: " + MAX_TORRENT_DOWNLOAD_THREADS);
                }

                show();
        }

        public static void reconfig(String configFilePath) {
                CONFIG_FILE = configFilePath;
                dotenv = Dotenv
                                .configure()
                                .filename(configFilePath)
                                .ignoreIfMissing()
                                .load();
        }

        public static void save() {

                try (FileWriter configFile = new FileWriter(CONFIG_FILE)) {
                        BufferedWriter bufferedWriter = new BufferedWriter(configFile);

                        // ------------ 1 Guardar directorios ------------ //
                        writeString(bufferedWriter, KEY_DIR_TORRENTS, DIR_TORRENTS);
                        writeString(bufferedWriter, KEY_DIR_PRINCIPAL, DIR_PRINCIPAL);
                        writeString(bufferedWriter, KEY_DIR_TEMP, DIR_TEMP);

                        // ------------ 2 Guardar IP y puerto ------------ //
                        writeString(bufferedWriter, KEY_TRACKER_IP, TRACKER_IP);
                        writeString(bufferedWriter, KEY_TRACKER_PORT, String.valueOf(TRACKER_PORT));

                        // ------------ 3 Guardar constantes del peer ------------ //
                        writeString(bufferedWriter, KEY_USE_LOCAL_IP, String.valueOf(USE_LOCAL_IP));
                        writeString(bufferedWriter, KEY_PEER_PORT, String.valueOf(PEER_PORT));
                        writeString(bufferedWriter, KEY_PEER_MAX_CONNECTIONS, String.valueOf(PEER_MAX_CONNECTIONS));
                        writeString(bufferedWriter, KEY_MAX_TORRENT_DOWNLOAD_THREADS,
                                        String.valueOf(MAX_TORRENT_DOWNLOAD_THREADS));

                        bufferedWriter.close();
                } catch (IOException e) {
                        System.err.println("Error al guardar la configuración");
                        e.printStackTrace();

                        System.exit(1);
                }

        }

        public static void show() {
                int maxKeyLength = Math.max(Math.max(DIR_TORRENTS.length(), DIR_PRINCIPAL.length()),
                                DIR_TEMP.length());
                int separatorLength = maxKeyLength + 27; // 26 is the length of "| Configuración | Valor |"
                String separatorLine = "+" + "-".repeat(separatorLength) + "+";

                System.out.println(separatorLine);
                System.out.println("| Configuracion          | Valor" + " ".repeat(separatorLength - 26 - 1) + "|");
                System.out.println(separatorLine);
                System.out.println("| Directorio de torrents | " + formatValue(DIR_TORRENTS, maxKeyLength) + " |");
                System.out.println("| Directorio de descargas| " + formatValue(DIR_PRINCIPAL, maxKeyLength) + " |");
                System.out.println("| Directorio temporal    | " + formatValue(DIR_TEMP, maxKeyLength) + " |");

                System.out.println("| IP del tracker         | " + formatValue(TRACKER_IP, maxKeyLength) + " |");
                System.out.println("| Puerto del tracker     | "
                                + formatValue(String.valueOf(TRACKER_PORT), maxKeyLength) + " |");
                System.out.println("| Puerto del peer        | " + formatValue(String.valueOf(PEER_PORT), maxKeyLength)
                                + " |");
                System.out.println(separatorLine);

        }

        private static String formatValue(String value, int width) {
                StringBuilder formattedValue = new StringBuilder(value);
                while (formattedValue.length() < width) {
                        formattedValue.append(" ");
                }
                return formattedValue.toString();
        }

        /**
         * Escribe una llave y un valor en un archivo .env
         * 
         * @param writer BufferedWriter para escribir en el archivo
         * @param key    llave
         * @param value  valor
         * @throws IOException
         */
        private static void writeString(BufferedWriter writer, String key, String value) throws IOException {
                writer.write(key + "=" + value);
                writer.newLine();
        }

        // --------- Getters --------- //

        // ----- Directorios
        public static String DIR_TORRENTS() {
                return DIR_TORRENTS;
        }

        public static String DIR_PRINCIPAL() {
                return DIR_PRINCIPAL;
        }

        public static String DIR_TEMP() {
                return DIR_TEMP;
        }

        // ----- Tracker
        public static String TRACKER_IP() {
                return TRACKER_IP;
        }

        public static int TRACKER_PORT() {
                return TRACKER_PORT;
        }

        // ----- Peer

        public static boolean USE_LOCAL_IP() {
                return USE_LOCAL_IP;
        }

        public static int PEER_PORT() {
                return PEER_PORT;
        }

        public static int PEER_MAX_CONNECTIONS() {
                return PEER_MAX_CONNECTIONS;
        }

        public static int MAX_TORRENT_DOWNLOAD_THREADS() {
                return MAX_TORRENT_DOWNLOAD_THREADS;
        }

        // --------- Setters --------- //

        /**
         * Valida que el directorio exista, si no existe lo crea
         * 
         * @param dIR_TORRENTS directorio a validar
         */
        private static void DIR_TORRENTS(String dIR_TORRENTS) {
                DIR_TORRENTS = dIR_TORRENTS;
                Validator.validarDirectorio(dIR_TORRENTS);
        }

        /**
         * Valida que el directorio exista, si no existe lo crea
         * 
         * @param DIR_PRINCIPAL directorio a validar
         */
        private static void DIR_PRINCIPAL(String dIR_PRINCIPAL) {
                Validator.validarDirectorio(dIR_PRINCIPAL);
                DIR_PRINCIPAL = dIR_PRINCIPAL;
        }

        /**
         * Valida que el directorio exista, si no existe lo crea
         * 
         * @param dIR_TEMP directorio a validar
         */
        private static void DIR_TEMP(String dIR_TEMP) {
                DIR_TEMP = dIR_TEMP;
                Validator.validarDirectorio(dIR_TEMP);
        }

        private static void PEER_PORT(int puerto) {
                boolean noValido = true;
                do {

                        try (ServerSocket clientSocket = new ServerSocket(puerto)) {
                                noValido = false;
                        } catch (UnknownHostException e) {
                                System.out.println(e);
                        } catch (IOException e) {
                                System.out.println("Puerto " + puerto + " cerrado");
                                System.out.println("Ingrese un puerto valido: ");
                                puerto = Integer.parseInt(System.console().readLine());
                        }
                } while (noValido);
                PEER_PORT = puerto;
        }

        private Config() {
        }
}
