# mxj-wsserver

####A Max/Msp mxj object to serve files and communicate via websockets.

#####Attributes:
 - port (80-65535): the port the server listens on. Set triggers restart if running.
 - webroot (folder): the base folder to look in when receiving http get requests.
	
#####Understands:
 - start: Start the server
 - stop: Stop the server
 - port \[port\]: Set the port
 - webroot \[path\]: Set the webroot
 - \[client_id\] \[args..\]: Send the args as one string to the client with this id
	
#####Outputs:
 - start: The server has started.
 - stop: The server has stopped.
 - start \[client_id\]: A new Websocket has been opened and assigned this client_id (number)
 - stop \[client_id\]: The websocket with this id has been closed
 - \[client_id\] \[args..\]: A message from the Websocket with this id has been received and split to args via Atom.parse

#####Webroot:
 - Can't be escaped via '..'
 - Serves html, js, css, jpg, gif, png files only. (See method wsserver.server.Request.respond)
 - If a given path leads to a directory containing a file named 'index.html', that file is sent in the response.

#####So...
 - if you route all output except start and stop back to the input, you have an echo websocket.
 - start a websocket connection from javascript:
 
```js
var ws = new WebSocket("ws://" + location.hostname + ":" + location.port)
ws.onopen = function() {
  ws.send("my message")
}
ws.onmessage = function(e) {
  console.log("received echo? " + ("my message" == e.data))
}
```