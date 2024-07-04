# Protocolo de comunicación

1. Para unirse a la red el peer envía su estado, incluyendo los archivos que
comparte: `ANNOUNCE_STATUS`

2. El tracker responde con la lista de peers que están en la red: `PEER_LIST`

3. A partir de aquí el peer puede pedir trozos de archivos a los peers que los
tienen: `REQUEST_PIECE`

4. Los peers responden con los trozos de archivos: `REPLY_PIECE`

## ESTADOS

Cada cierto tiempo el tracker solicita el estado al peer

- `UPDATE_STATUS`

Cuando un peer se va de la red lo notifica al tracker

- `NOTIFY_PEER_OFFLINE`

# SOLICITUDES

## REQUEST_PEER_STATUS:

Se usa para obtener el estado de un peer en el tracker

## ANNOUNCE_STATUS:

Para anunciar el estado de un peer en el tracker

## UPDATE_STATUS:
Actualizar el estado de un peer en el tracker

## PEER_LIST:

Se usa para enviar la lista de peers

## REQUEST_PIECE:

Se usa para obtener un trozo de un archivo

## REPLY_PIECE:

Se usa para enviar un trozo de un archivo

# IDEAS DE MEJORA

Notificar a los peers cuando un peer se va o se une a la red, sin necesidad de
reenviar la lista de peers, reduciendo el tráfico de la red.

## NOTIFY_PEER_ONLINE:

Se usa para anunciar a los peers que un peer se ha unido a la red

## NOTIFY_PEER_OFFLINE:

Se usa para anunciar a los peers que un peer se ha ido de la red
