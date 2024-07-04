package peer;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Validator {
        /**
         * Valida que el directorio exista, si no existe lo crea
         * 
         * @param dir directorio a validar
         */
        public static void validarDirectorio(String dir) {
                File directorio = new File(dir);
                if (!directorio.exists()) {
                        System.out.println("El directorio " + dir + " no existe");
                        System.out.println("Creando directorio " + dir);
                        directorio.mkdirs();
                }
        }

        /**
         * Valida la IP, sino es válida pide que se ingrese una nueva IP
         * 
         * @param ip IP a validar
         * @return la IP válida
         */
        public static String validarIP(String ip) {
                do {
                        try {
                                InetAddress.getByName(ip);
                                break;
                        } catch (UnknownHostException e) {
                                System.out.println("La IP ingresada no es valida");
                                System.out.println("Ingrese la IP del tracker: ");
                                ip = System.console().readLine();
                        }
                } while (true);

                return ip;
        }

        /**
         * Si se ingresó 0 pide que se ingrese un puerto, si no está
         * entre 1024 y 65535 pide que se ingrese un puerto válido
         * 
         * @param port puerto a validar
         * @return el puerto válido
         */
        public static int validarPuerto(int port) {
                do {
                        try {
                                if (port == 0) { // Si no se ingresó el puerto
                                        System.out.println("Ingrese el puerto: ");
                                        port = Integer.parseInt(System.console().readLine());
                                } else if (port < 1024
                                                || port > 65535) {
                                        System.out.println("El puerto debe estar entre 1024 y 65535");
                                        System.out.println("Ingrese el puerto: ");
                                        port = Integer.parseInt(System.console().readLine());
                                } else {
                                        break;
                                }
                        } catch (NumberFormatException e) {
                                System.out.println("Puerto invalido");
                                System.out.println("Ingrese el puerto: ");
                                port = Integer.parseInt(System.console().readLine());
                        }
                } while (true);

                return port;
        }
}
