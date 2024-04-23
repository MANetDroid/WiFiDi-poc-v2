package com.example.wifipocv2;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class GroupPacket implements Serializable {
    private boolean isPing;
    private String ip;
    private String mac;
    private int port;
    private int originPort;

    private byte[] message;
    private String textMessage;
    private int type; // 0 - config, 1 - String message Host-Peer, 2 - Group device ports, 3 - String message Peer - Peer 4 - ping packet
    private int[] GroupDevicePortArray;
    private Date sentTime;

    public GroupPacket(String ip, String mac) {
        this.type = 0;
        this.ip = ip;
        this.mac = mac;
    }

    public GroupPacket(String textMessage) {
        this.type = 1;
        this.textMessage = textMessage;
    }

    public GroupPacket(byte[] message) {
        this.type = 1;
        this.message = message;
    }

    public GroupPacket(List<MainActivity.MultiServerThread> threadArray, int myPort) {
        this.type = 2;
        int[] portArray = new int[threadArray.size() - 1];
        int i = 0;
        for (MainActivity.MultiServerThread m : threadArray) {
            if (m.getSocket().getPort() == myPort) {
                continue;
            }
            portArray[i] = m.getSocket().getPort();
            i++;
        }
        this.GroupDevicePortArray = portArray;
    }

    public GroupPacket(String textMessage, int groupPort, int originPort, Date sentTime) {
        this.type = 3;
        this.port = groupPort;
        this.originPort = originPort;
        this.textMessage = textMessage;
        this.sentTime = sentTime;
    }
    public GroupPacket( int groupPort, int originPort, Date sentTime) {
        this.type = 4;
        this.port = groupPort;
        this.originPort = originPort;
        this.textMessage = "PING";
        this.sentTime = sentTime;
    }

    public Date getSentTime() {
        return sentTime;
    }

    public void setSentTime(Date sentTime) {
        this.sentTime = sentTime;
    }

    public int getOriginPort() {
        return originPort;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int[] getGroupDevicePortArray() {
        return GroupDevicePortArray;
    }

    public byte[] getMessage() {
        return message;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getTextMessage() {
        return textMessage;
    }

    public boolean isPing() {
        return isPing;
    }

    public void setPing(boolean ping) {
        isPing = ping;
    }
}
