package util.lib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

public class IpChecker {
        /**
         * Obtiene la dirección IP pública del host
         * 
         * @return String con la dirección IP pública del host o null en caso de error
         * @throws IOException si ocurre un error con la página web que se utiliza para
         *                     obtener la dirección IP
         */
        public static String getPublicIp() throws IOException {
                URL whatismyip = new URL("http://checkip.amazonaws.com");
                URLConnection connection = whatismyip.openConnection();

                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        return br.readLine();
                } catch (IOException e) {
                        System.err.println("Error inesperado al obtener la direccion IP");
                        return null;
                }
        }

        public static String getLocalIp() throws UnknownHostException {
                String localIp;
                localIp = InetAddress.getLocalHost().getHostAddress();
                return localIp;
        }

        private IpChecker() {
        }
}
