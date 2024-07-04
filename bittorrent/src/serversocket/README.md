El servidor de sockets es el encargado de recibir las conexiones entrantes. Para
esto se hizo una abstracción del ServerSocket de Java (`ServerSocketThread`).
Esta abstracción permite que el servidor de sockets pueda recibir conexiones
entrantes y crear un hilo de procesamiento para cada una de ellas a partir de
una **_ThreadPool_**.

Con esto se delega la responsabilidad de manejar las conexiones entrantes a los
hilos que se crean, en un objeto de tipo `SocketHandler`. Aprovechando esta
abstracción, se definen dos manejadores de conexiones, uno para los trackers
(`SocketHandlerTracker`) y otro para los peers (`SocketHandlerPeer`).

## SocketHandlerDownload

El `SocketHandlerDownload` es el encargado de manejar las descargas de archivos.
A diferencia de los otros, este no se lanza por el ServerSocket, sino que se
crea por un `TorrentDownload` el cual genera un hilo con un socket para cada
peer del cual se pueda descargar un archivo (con un máximo de
`MAX_TORRENT_DOWNLOAD_THREADS`).
