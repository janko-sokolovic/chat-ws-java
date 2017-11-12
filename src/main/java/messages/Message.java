package messages;

import java.io.Serializable;

public class Message implements Serializable{

    private String text;
    private String user;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    
}
