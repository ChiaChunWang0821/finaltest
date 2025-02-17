package com.example.jolin.afinal;

import java.io.*;

public class ChatMessage implements Serializable {
    static final int MUSCLE = 0, BYTELEN = 1;
    private int type;
    private double Message;
    private int intMessage;

    // constructor
    ChatMessage(int type, double Message) {
        this.type = type;
        this.Message = Message;
    }

    int getType() {
        return type;
    }

    int getIntMessage() {
        intMessage = (int) Message;
        return intMessage;
    }

    double getDoubleMessage(){
        return Message;
    }
}