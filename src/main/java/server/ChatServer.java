package server;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import message.Message;
import message.MessageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import user.User;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ChatServer extends WebSocketServer {

    private static int TCP_PORT = 9000;

    private final static Logger logger = LogManager.getLogger(ChatServer.class);

    private Set<User> users;

    private Set<WebSocket> conns;

    private ChatServer() {
        super(new InetSocketAddress(TCP_PORT));
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
                    addUser(msg.getUser());
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

    private void removeUser(String name) throws JsonProcessingException {
        User userToRemove = new User(name);
        users.remove(userToRemove);
        Message newMessage = new Message();

        // when user joins send to all users list of active users
        // otherwise new users wouldn't know how many are active
        ObjectMapper mapper = new ObjectMapper();
        String data = mapper.writeValueAsString(users);
        newMessage.setData(data);
        newMessage.setUser(name);
        newMessage.setType(MessageType.USER_JOINED);
        broadcastMessage(newMessage);
    }

    private void addUser(String name) throws JsonProcessingException {
        User newUser = new User(name);
        users.add(newUser);
        Message newMessage = new Message();

        // when user joins send to all users list of active users
        // otherwise new users wouldn't know how many are active
        ObjectMapper mapper = new ObjectMapper();
        String data = mapper.writeValueAsString(users);
        newMessage.setData(data);
        newMessage.setUser(name);
        newMessage.setType(MessageType.USER_JOINED);
        broadcastMessage(newMessage);
    }

    public static void main(String[] args) {
        new ChatServer().start();
    }

}
