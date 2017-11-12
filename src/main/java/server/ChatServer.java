package server;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import messages.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

public class ChatServer extends WebSocketServer {

    private static int TCP_PORT = 9000;

    private final static Logger logger = LogManager.getLogger(ChatServer.class);

    private Set<WebSocket> conns;

    private ChatServer() {
        super(new InetSocketAddress(TCP_PORT));
        conns = new HashSet<>();
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        conns.add(webSocket);

        logger.info("Connection established from: " + webSocket.getRemoteSocketAddress().getHostString());
        System.out.println("New connection from " + webSocket.getRemoteSocketAddress().getAddress().getHostAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        conns.remove(conn);

        logger.info("Connection closed to: " + conn.getRemoteSocketAddress().getHostString());
        System.out.println("Closed connection to " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Message msg = mapper.readValue(message, Message.class);

            broadcastMessage(msg);

            System.out.println("Message from user: " + msg.getUser() + ", text: " + msg.getText());
            logger.info("Message from user: " + msg.getUser() + ", text: " + msg.getText());
        } catch (IOException e) {
            logger.error("Wrong message format.");
            // return error message to user
        }

    }

    private void broadcastMessage(Message msg) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String messageJson = mapper.writeValueAsString(msg);
            for (WebSocket sock : conns) {
                sock.send(messageJson);
            }
        } catch (JsonProcessingException e) {
            logger.error("Cannot convert message to json.");
        }

    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

        if (conn != null) {
            conns.remove(conn);
            // do some thing if required
        }
        assert conn != null;
        System.out.println("ERROR from " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
    }

    public static void main(String[] args) {
        new ChatServer().start();
    }

}
