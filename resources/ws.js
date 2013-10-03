

var ws = null;
function connect() {
 ws = new WebSocket('ws://localhost:9090/websocket')
}
connect();

ws.onopen = function() {
    console.log("open");
};

ws.onerror = function(error) {
    console.log("error:", error);
}

ws.onmessage = function(e) {
    console.log("Server: ", e.data);
}

ws.onclose = function(e) {
    connect();
}
