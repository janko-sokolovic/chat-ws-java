package com.jasofalcon.chat;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jasofalcon.message.Message;
import com.jasofalcon.message.MessageType;
import com.jasofalcon.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

public class ChatServer extends WebSocketServer {

    private static int TCP_PORT;

    private final static Logger logger = LogManager.getLogger(ChatServer.class);

    private Set<User> users;

    private Set<WebSocket> conns;

    private ChatServer(int port) {
        super(new InetSocketAddress(port));
        conns = new HashSet<>();
        users = new HashSet<>();
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

            switch (msg.getType()) {
                case USER_JOINED:
                    addUser(new User(msg.getUser().getName()), conn);
                    break;
                case USER_LEFT:
                    removeUser(msg.getUser());
                    break;
                case TEXT_MESSAGE:
                    broadcastMessage(msg);
            }

            System.out.println("Message from user: " + msg.getUser() + ", text: " + msg.getData() + ", type:" + msg.getType());
            logger.info("Message from user: " + msg.getUser() + ", text: " + msg.getData());
        } catch (IOException e) {
            logger.error("Wrong message format.");
            // return error message to user
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

        if (conn != null) {
            conns.remove(conn);
        }
        assert conn != null;
        System.out.println("ERROR from " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
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

    private void addUser(User user, WebSocket conn) throws JsonProcessingException {
        users.add(user);
        acknowledgeUserJoined(user, conn);
        broadcastUserActivityMessage(MessageType.USER_JOINED);
    }

    private void removeUser(User user) throws JsonProcessingException {
        users.remove(user);
        broadcastUserActivityMessage(MessageType.USER_LEFT);
    }

    private void acknowledgeUserJoined(User user, WebSocket conn) throws JsonProcessingException {
        Message message = new Message();
        message.setType(MessageType.USER_JOINED_ACK);
        message.setUser(user);
        conn.send(new ObjectMapper().writeValueAsString(message));
    }

    private void broadcastUserActivityMessage(MessageType messageType) throws JsonProcessingException {

        Message newMessage = new Message();

        ObjectMapper mapper = new ObjectMapper();
        String data = mapper.writeValueAsString(users);
        newMessage.setData(data);
        newMessage.setType(messageType);
        broadcastMessage(newMessage);
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv("PORT"));
        if (port <= 0) port = 9000;
        new ChatServer(port).start();
    }

}
