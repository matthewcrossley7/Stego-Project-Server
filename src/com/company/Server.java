package com.company;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

public class Server extends Thread {
    private boolean exit = false;
    private ServerSocket in;
    //private static List<nClient> chatClients = new ArrayList<nClient>();
    protected static String server_IP ;
    public static void main(String[] args) throws IOException {



        int cTosPortNumber = 1777;
        String str;
        InetAddress iAddress = InetAddress.getLocalHost();
        server_IP = iAddress.getHostAddress();
        System.out.println("Server IP address : " +server_IP);

        ServerSocket servSocket = new ServerSocket(cTosPortNumber);
        System.out.println("Waiting for a connection on " + cTosPortNumber);
        while(true){
            Socket fromClientSocket = servSocket.accept();
            System.out.println("ACCEPTED Conn");
            clientThread newClient = new clientThread(fromClientSocket);
            newClient.start();
            System.out.println("thread succesffully started");
        }



       /* pw.close();
        br.close();

        fromClientSocket.close();*/
    }

       /* int port = 14001;
        Server chatServer = new Server();
        chatServer.setPort(port);
        chatServer.checkForExit.start();
       // chatServer.start();						//Thread checks each client for incoming message
        chatServer.go();*/


    Thread connection = new Thread() {
        public void run(Socket socket) {

        }
    };
}
