# BitTorrent

# Uso

El programa tiene dos ejecutables, el tracker y el peer. El tracker se encarga
de llevar el registro de los peers y de los archivos que estos tienen. El peer
se encarga de descargar y compartir archivos con otros peers.

## Ambiente

El programa fue desarrollado y probado en:

- Windows 11
- Java 17.0.1
- Localhost (aunque debería funcionar en una red normal)

## Configuración

La configuración del programa se hace a través de archivos `.env`. Estos
archivos contienen las constantes que se usan para la ejecución del programa.
Estos archivos se deben localizar en el directorio `bin/`. La primera vez que se
ejecute el programa, creará su archivo de configuración con los valores por
defecto.

El programa tiene dos archivos `.env`, uno para el tracker (`.env.tracker`) y
otro para el peer (`.env.peer`).

### Tracker

El archivo `.env.tracker` se verá muy similar a esto:

```
USE_LOCAL_IP=true               # Si es true, el tracker usará la ip local para comunicarse con los peers
TRACKER_IP=192.168.1.71         # Ip del tracker (pública o local)
TRACKER_PORT=4444               # Puerto del tracker
TRACKER_MAX_CONNECTIONS=2       # Número máximo de conexiones que puede tener el tracker
```

### Peer

El archivo `.env.peer` se verá muy similar a esto:

```
DIR_TORRENTS=bin/files/internal/torrents/         # Directorio donde se guardarán los archivos .torrent y .ser
DIR_PRINCIPAL=bin/files/archivos/                 # Directorio donde se tendrán los archivos descargados y compartidos
DIR_TEMP=bin/files/internal/temp/                 # Directorio donde se guardarán los archivos temporales (partes de archivos)
TRACKER_IP=192.168.1.71                           # Ip del tracker (pública o local)
TRACKER_PORT=4444                                 # Puerto del tracker
USE_LOCAL_IP=true                                 # Si es true, el peer usará la ip local para comunicarse con el tracker y otros peers
PEER_PORT=4448                                    # Puerto del peer
PEER_MAX_CONNECTIONS=10                           # Número máximo de conexiones que puede tener el peer
MAX_TORRENT_DOWNLOAD_THREADS=3                    # Número máximo de hilos que se usarán para descargar un archivo (conexion con otros peers)
```

> [!NOTE] Para varios peers locales, se debe cambiar el puerto del peer en el
> archivo `.env.peer` para cada peer. El puerto debe ser único para cada peer.

## Compilación

javac -sourcepath src -d bin src/peer/Peer.java

javac -sourcepath src -d bin src/tracker/Tracker.java

## Ejecución

### Tracker

`java -cp bin tracker.Tracker`

### Peers

Para un solo peer: `java -cp bin peer.Peer`

Para varios peers: `java -cp bin peer.Peer -n <Número de peer>`

> [!NOTE] El número de peer define el .env.peer que se usará para la ejecución
> del peer. Se creará un archivo `.env.peer<Número de peer>` que se usará para
> la ejecución del peer.
