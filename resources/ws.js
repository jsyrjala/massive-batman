
$(function() {
  var ws = null;

  function timestamp() {
    function zeroPad(num, places) {
      var zero = places - num.toString().length + 1;
      return Array(+(zero > 0 && zero)).join("0") + num;
    }
    var a = new Date();
    var hour = a.getUTCHours();
    var min = a.getUTCMinutes();
    var sec = a.getUTCSeconds();
    return zeroPad(hour,2)+":"+zeroPad(min,2)+":"+zeroPad(sec,2);
  }

  function send(msg) {
    console.info("Client: ", msg);
    try {
      $("#content").prepend("<li class='client'>" + timestamp() + " Client: <code>" + msg + "</code></li>");

      ws.send(msg);
    } catch(err) {
      console.error("Sending failed, reconnecting", err);
      setTimeout(function() {
        connect(msg);
      }, 100);

    }
  };
  function sendJson(msg) {
    send(JSON.stringify(msg));
  };

  function connect(msg) {
    ws = new WebSocket('ws://localhost:9090/websocket')
    $("#content").prepend("<li class='client'>" + timestamp() + " Client connecting...</li>");

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
      $("#content").prepend("<li class='server'>" + timestamp() + " Server: <code>" + e.data + "</code></li>");
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

  $('#ping').click(function(e) {
    sendJson({ping: timestamp()});
    return false;
  });

  $('#subscribe').click(function(e){
    var value = $('#ids').val();
    sendJson({subscribe: "trackers", ids: value});
    return false;
  });

  $('#unsubscribe').click(function(e){
    var value = $('#ids').val();
    sendJson({unsubscribe: "trackers", ids: value});
    return false;
  });

  $('#send_event').click(function(e) {
    var tracker_id = $('#tracker_id').val();
    var data = $('#event_data').val();
    var event_data = {event: true, tracker_id: tracker_id, data: data};
    sendJson(event_data);
    return false;
  });
  connect();


});

