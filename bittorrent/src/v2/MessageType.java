package v2;

/**
 * Tipos de mensajes
 * <p>
 * Tenemos dos tipos de mensajes:
 * <p>
 * 
 * <ul>
 * <li>Mensajes Iniciadores de Conexión (MIC): Son mensajes que se pueden usar
 * para iniciar una conexión, estos mensajes son:
 * 
 * <ul>
 * <li><code>ANNOUNCE_STATUS</code>: Anuncia el estado de un peer al
 * tracker</li>
 * <li><code>UPDATE_STATUS</code>: Actualiza el estado de un peer al
 * tracker</li>
 * <li><code>NOTIFY_PEER_OFFLINE</code>: Notifica al tracker que un peer se ha
 * desconectado
 * </li>
 * <li><code>REQUEST_PIECE</code>: Solicita una pieza a un peer</li>
 * </ul>
 * </li>
 * 
 * <li>Mensajes de Respuesta: Son mensajes que se pueden usar para responder a
 * una conexión iniciada</li>
 * </ul>
 * 
 */
public enum MessageType {
        // -----------------------------//
        // GENERAL
        // -----------------------------//

        START_MSG(0),
        END_MSG(255),

        // -----------------------------//
        // TRACKER
        // -----------------------------//
        PEER_LIST(4),
        REQUEST_PEER_STATUS(5),

        // -----------------------------//
        // PEER
        // -----------------------------//
        /**
         * MICP
         * Anuncia el estado de un peer al tracker
         */
        ANNOUNCE_STATUS(1),
        UPDATE_STATUS(2),
        REQUEST_PIECE(6),
        REPLY_PIECE(7),
        NOTIFY_PEER_OFFLINE(8)

        ;

        public final int value;

        private MessageType(int value) {
                this.value = value;
        }
}
