package tracker;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;

import peer.Validator;
import util.dotenv.Dotenv;
import util.lib.IpChecker;

public class Config {
        public static final boolean _DEBUG = true;

        private static final String CONFIG_FILE = "bin/.env.tracker";
        private static Dotenv dotenv = Dotenv
                        .configure()
                        .filename(CONFIG_FILE)
                        .ignoreIfMissing()
                        .load();

        // --------- Llaves --------- //
        private static final String KEY_USE_LOCAL_IP = "USE_LOCAL_IP";
        private static final String KEY_TRACKER_IP = "TRACKER_IP";
        private static final String KEY_TRACKER_PORT = "TRACKER_PORT";
        private static final String KEY_TRACKER_MAX_CONNECTIONS = "TRACKER_MAX_CONNECTIONS";

        // --------- Tracker --------- //
        // Configuración del tracker

        // Nota:
        // Se considera solo un tracker para la aplicación,
        // en caso de que se quiera usar más de un tracker
        // se debe modificar el código

        private static boolean USE_LOCAL_IP = true;
        private static String TRACKER_IP = "";
        private static int TRACKER_PORT = 0;
        private static int TRACKER_MAX_CONNECTIONS = 2;

        public static void load() throws UnknownHostException {
                final String MSG_ENVVARNF = "No se encontro la configuracion de: "; // Mensaje de variable de entorno
                                                                                    // no encontrada

                // ------------ 2 Cargar IP y puerto ------------ //

                if (dotenv.get(KEY_USE_LOCAL_IP) == null) {
                        System.out.println(MSG_ENVVARNF + KEY_USE_LOCAL_IP);
                        System.out.println("Se usara el valor por defecto: " + USE_LOCAL_IP);
                } else {
                        USE_LOCAL_IP = Boolean.parseBoolean(dotenv.get(KEY_USE_LOCAL_IP));
                }

                if (USE_LOCAL_IP) {
                        TRACKER_IP = IpChecker.getLocalIp();
                } else {
                        try {
                                TRACKER_IP = IpChecker.getPublicIp();
                        } catch (IOException e) {
                                System.out.println("Error al obtener la IP publica, checar URL de la API");
                                e.printStackTrace();
                                System.exit(1);
                                return;
                        }
                }

                if (dotenv.get(KEY_TRACKER_PORT) == null) {
                        System.out.println(MSG_ENVVARNF + KEY_TRACKER_PORT);
                }

                TRACKER_PORT = Validator.validarPuerto(
                                // * Nota: la función acepta un 0 para que se pida el puerto,
                                // * en caso contrario valida el puerto
                                dotenv.get(KEY_TRACKER_PORT) != null
                                                ? Integer.parseInt(dotenv.get(KEY_TRACKER_PORT))
                                                : 0);

                if (dotenv.get(KEY_TRACKER_MAX_CONNECTIONS) == null) {
                        System.out.println(MSG_ENVVARNF + KEY_TRACKER_MAX_CONNECTIONS);
                        System.out.println("Se usara el valor por defecto: " + TRACKER_MAX_CONNECTIONS);
                }

                show();
        }

        public static void save() {

                try (FileWriter configFile = new FileWriter(CONFIG_FILE)) {
                        BufferedWriter bufferedWriter = new BufferedWriter(configFile);

                        // ------------ 2 Guardar IP y puerto ------------ //
                        writeString(bufferedWriter, KEY_USE_LOCAL_IP, USE_LOCAL_IP ? "true" : "false");
                        writeString(bufferedWriter, KEY_TRACKER_IP, TRACKER_IP);
                        writeString(bufferedWriter, KEY_TRACKER_PORT, String.valueOf(TRACKER_PORT));
                        writeString(bufferedWriter, KEY_TRACKER_MAX_CONNECTIONS,
                                        String.valueOf(TRACKER_MAX_CONNECTIONS));

                        bufferedWriter.close();
                } catch (IOException e) {
                        System.err.println("Error al guardar la configuracion");
                        e.printStackTrace();

                        System.exit(1);
                }

        }

        public static void show() {
                int maxKeyLength = KEY_TRACKER_MAX_CONNECTIONS.length();
                int separatorLength = maxKeyLength + 27; // 26 is the length of "| Configuración | Valor |"
                String separatorLine = "+" + "-".repeat(separatorLength) + "+";

                System.out.println(separatorLine);
                System.out.println("| Configuracion          | Valor" + " ".repeat(separatorLength - 26 - 1) + "|");
                System.out.println(separatorLine);
                System.out.println("| IP del tracker         | " + formatValue(TRACKER_IP, maxKeyLength) + " |");
                System.out.println("| Puerto del tracker     | "
                                + formatValue(String.valueOf(TRACKER_PORT), maxKeyLength) + " |");
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
        public static String TRACKER_IP() {
                return TRACKER_IP;
        }

        public static int TRACKER_PORT() {
                return TRACKER_PORT;
        }

        public static int TRACKER_MAX_CONNECTIONS() {
                return TRACKER_MAX_CONNECTIONS;
        }

        private Config() {
        }
}
