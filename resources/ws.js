
$(function() {
  var ws = null;


  function send(msg) {
    console.info("Sending: " + msg);
    try {
      $("#content").prepend("<li>Client: " + message + "</li>");

      ws.send(msg);
    } catch(err) {
      console.error("Sending failed, reconnecting", err);
      setTimeout(function() {
        connect(msg);
      }, 100);

    }
  };

  function connect(msg) {
    ws = new WebSocket('ws://localhost:9090/websocket')
    ws.onopen = function() {
      console.debug("Connection opened");
      if(msg) {
        ws.send(msg);
      }
    };

    ws.onerror = function(error) {
      console.error("error:", error);
    }

    ws.onmessage = function(e) {
      console.info("Server: ", e.data);
      $("#content").prepend("<li>Server: " + e.data + "</li>");
    }

    ws.onclose = function(e) {
      connect();
    };
  }


  $('#send').click(function(event) {
    event.preventDefault();
    var value = $('#data').val();
    send(value);
    return false;
  });


  connect();


});

