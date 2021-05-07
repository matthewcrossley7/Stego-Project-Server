package com.company;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends Thread {
    private boolean exit = false;
    private ServerSocket in;
    protected static String server_IP ;
    public static void main(String[] args) throws IOException {

        int cTosPortNumber = 1777;
        InetAddress iAddress = InetAddress.getLocalHost();
        server_IP = iAddress.getHostAddress();
        System.out.println("Server IP address : " +server_IP);

        //create a socket listening on specified port
        ServerSocket servSocket = new ServerSocket(cTosPortNumber);
        System.out.println("Waiting for a connection on port " + cTosPortNumber);
        while(true){
            //accepts connections
            Socket fromClientSocket = servSocket.accept();
            System.out.println("ACCEPTED Conn");
            //create new thread to listen for requests from client
            clientThread newClient = new clientThread(fromClientSocket);
            newClient.start();
        }
    }

}
