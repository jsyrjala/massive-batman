
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

  function display(msg, css_class) {
      $("#content").prepend("<li class='" + css_class +"'>" + timestamp() + " " + msg +"</li>");
  }

  function send(msg) {
    console.info("Client: ", msg);
    try {
      display("Client: <code>" + msg + "</code>", 'client');
      ws.send(msg);
    } catch(err) {
      console.error("Sending failed, reconnecting", err);
      setTimeout(function() {
        connect(msg);
      }, 500);

    }
  };

  function sendJson(msg) {
    send(JSON.stringify(msg));
  };

  function connect(msg) {
    ws = new WebSocket('ws://laatikko.net:9090/websocket')
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
      display("Server:  <code>" + e.data + "</code>", "server");
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

  function new_event_data() {
    var tracker_id = $('#tracker_id').val();
    var tracker_code = $('#tracker_code').val();

    var data = $('#event_data').val();
    return {event: true,
            version: 1,
            tracker_id: tracker_id,
            tracker_code: tracker_code,
            data: data};
  }

  $('#send_event').click(function(e) {
    sendJson(new_event_data());
    return false;
  });


  function sendRestPost(url, data)  {
    display("Client: " + url + " <code>" + JSON.stringify(data) + "</code>", 'client');

    $.ajax({url: 'http://laatikko.net:9090' + url,
            type: 'POST',
            data: JSON.stringify(data),
            dataType: "json",
            contentType: "application/json",
            success: function(e) {
              console.log("REST success", e);
            }}).fail(function(e) {
      display("Server: " + e.status + " "+ e.statusText + ": <code>" + e.responseText + "</code>", "server error")
      console.log("REST fail", e);
    });
  };

  $('#send_event_rest').click(function(e) {
    var data = new_event_data();
    sendRestPost('/events', data);
    return false;
  });

  $('#clear_console').click(function(e){
      $("#content").html("<li>cleared</li>")
  });
  connect();

});

