package com.company;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.sql.*;

import org.json.JSONArray;
import org.json.JSONObject;
public class nClient extends Thread {
    private Socket s;
    private String userInput = "";
    private String inputBuffer = "";
    private boolean sendMsgToServer = false;
    /*
     * Constructor specifies the socket the client is connected to
     */
    public nClient(Socket socket) {

        s = socket;

    }
    /*
     *A thread that checks whether a client has sent a message
     *variables called inputBuffer and sendMsgToServer used to make sure message only sent to ChatServer once
     */
    public void run() {
        System.out.println("STARTING CONN");
        Connection conn = null;
        try{
            conn = (Connection) DriverManager.getConnection("jdbc:mysql://localhost:3306/stegodb","root","root");
            if(conn!=null){
                System.out.println("CONENCTED TO DB");
            }else{
                System.out.println("NOT CONNECTED");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        InputStreamReader r;
        try {

            r = new InputStreamReader(s.getInputStream(),"UTF-8");
            BufferedReader clientIn = new BufferedReader(r);
            OutputStreamWriter writer = new OutputStreamWriter(s.getOutputStream(),"UTF-8");
            while(true) {
                userInput = clientIn.readLine(); //Waits for client to send message and stores in userInput
                JSONObject jsonObject = new JSONObject(userInput);

                switch (jsonObject.getString("type")){
                    case "login":
                        System.out.println(jsonObject.getString("email") + " is logging in");
                        PreparedStatement stmt = null;
                        String query = "select * from User where email=?";
                        stmt = conn.prepareStatement(query,ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                        stmt.setString(1, jsonObject.getString("email"));
                        ResultSet rs = stmt.executeQuery();
                        int count=0;
                        while(rs.next()){
                            count++;
                        }
                        rs.first();
                        System.out.println("THERE ARE "+count);
                        JSONObject returnJSON = new JSONObject();
                        if(count==0){
                            returnJSON.put("isRegistered",false);
                        }else{
                            returnJSON.put("isRegistered",true);
                        }
                        writer.write("take this");
                        System.out.println("SENT A MSG BACK TO CLIENBT");
                         /*while(rs.next()){
                            String email = rs.getString("email");
                            System.out.println("YOU ARE ALREADY REGISTERED"+email);
                        }*/
                        break;

                }

                System.out.println("RETURNING A MSG");
                //writer.write("return msg");
                // writer.flush();
                System.out.println("SENT RETURN MSG");
            }
        } catch (IOException e) {

            e.printStackTrace();
        }catch (SQLException throwables) {
            throwables.printStackTrace();
        }

    }
    /*
     * Function returns message received to ChatServer
     * If no message received then empty string returned
     */
   /* public String getClientMsg() {

        if(!inputBuffer.equals("")  && sendMsgToServer == false) {
            sendMsgToServer = true;
            return inputBuffer;
        }
        return "";
    }
    /*
     * Function used to send a message from the server to client
     */
    /*public void sendMsg(String msg) {
        try {
            PrintWriter clientOut = new PrintWriter(s.getOutputStream(), true);
            clientOut.println(msg);
        } catch (IOException e) {

        }
    }
    */
}
