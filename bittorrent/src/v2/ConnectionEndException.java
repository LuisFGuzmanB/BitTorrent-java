package v2;

public class ConnectionEndException extends Exception {
        private static final long serialVersionUID = 1L;

        public ConnectionEndException() {
                super();
        }

        public ConnectionEndException(String message) {
                super(message);
        }

        public ConnectionEndException(String message, Throwable cause) {
                super(message, cause);
        }

        public ConnectionEndException(Throwable cause) {
                super(cause);
        }
}
