package com.company;

import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Base64;
import java.util.Date;
import java.sql.Timestamp;
public class clientThread extends Thread {
    Socket socket;
    Connection conn;
    public clientThread(Socket socket){
        this.socket=socket;
        conn = null;
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
    }
    public int getUserID(String email) throws SQLException {
        String query = "select * from User where email=?";
        PreparedStatement stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        stmt.setString(1, email);
        ResultSet rs = stmt.executeQuery();
        int count=0;
        while(rs.next()){
            count++;
        }
        rs.first();
        if(count==0){
            return -1;
        }else {
            return rs.getInt("userID");
        }
    }
    public String getUserEmail(int userID) throws SQLException {
        String query = "select * from User where userID=?";
        System.out.println(userID+ " USER IDD ");
        PreparedStatement stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        stmt.setInt(1, userID);
        ResultSet rs = stmt.executeQuery();
        int count=0;
        while(rs.next()){
            count++;
        }
        rs.first();
        if(count==0){
            return "null";
        }else {
            return rs.getString("email");
        }
    }
    public void run() {
        System.out.println("MyThread running");
        boolean repeat=true;
        String query;
        JSONObject returnJSON = new JSONObject();
        ResultSet rs;
        int row;
        int count;
        int userID;
        PreparedStatement stmt = null;
        while (repeat){
            try {
                PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String userInput = null;

                userInput = br.readLine();
                returnJSON=new JSONObject();
                System.out.println("The message: " + userInput);
                JSONObject jsonObject;
                if(userInput!=null){
                    jsonObject = new JSONObject(userInput);
                }else{
                    jsonObject = new JSONObject();
                    jsonObject.put("type","null");
                }


                switch (jsonObject.getString("type")){
                    case "login":
                        System.out.println(jsonObject.getString("email") + " is logging in");
                        userID = getUserID(jsonObject.getString("email"));
                        /*query = "select * from User where email=?";
                        stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                        stmt.setString(1, jsonObject.getString("email"));
                        rs = stmt.executeQuery();
                        count=0;
                        while(rs.next()){
                            count++;
                        }
                        rs.first();
                        System.out.println("THERE ARE "+count);
                            */
                        if(userID==-1){
                            returnJSON.put("isRegistered",false);
                        }else{
                            returnJSON.put("isRegistered",true);
                            query = "select path from User where userID=?";
                            stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                            stmt.setInt(1, userID);
                            rs = stmt.executeQuery();
                            rs.first();
                            returnJSON.put("filePath",rs.getString("path"));
                        }
                        break;
                    case "insert":
                        System.out.println("INSERTING INTO TABLE " + jsonObject.getString("table"));
                        query = "INSERT INTO user (email,path) VALUES (?,?)";
                        stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                        stmt.setString(1, jsonObject.getString("email"));
                        stmt.setString(2, jsonObject.getString("path"));
                        row = stmt.executeUpdate();
                        returnJSON.put("outcome",false);
                        System.out.println("EXECUTED "+row);
                        break;
                    case "addFriend":
                        System.out.println(jsonObject.get("userEmail")+ " is adding " + jsonObject.getString("friendEmail"));
                        int friendsID = getUserID(jsonObject.getString("friendEmail"));
                        if(friendsID==-1){
                            System.out.println("Friend not registerd");
                            returnJSON.put("outcome",false);
                            returnJSON.put("reason","notRegistered");
                        }else{
                            userID = getUserID(jsonObject.getString("userEmail"));

                            System.out.println("MY ID " + userID + " friends ID " + friendsID);
                            query = "SELECT * FROM friends WHERE friends.user1ID =? and friends.user2ID=? or friends.user1ID = ? and friends.user2ID=?";
                            stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                            stmt.setInt(1, userID);
                            stmt.setInt(2, friendsID);
                            stmt.setInt(3, friendsID);
                            stmt.setInt(4, userID);
                            rs = stmt.executeQuery();
                            count=0;
                            while(rs.next()){
                                count++;
                            }
                            if(count==0){
                                query = "INSERT INTO friends (user1ID,user2ID) VALUES (?,?)";
                                stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                                stmt.setInt(1, userID);
                                stmt.setInt(2, friendsID);
                                row = stmt.executeUpdate();
                                System.out.println("SUCCESSFULLY ADDED");
                            }else{
                                System.out.println("YOU ARE ALREADY FRIENDS");
                            }
                            /*ResultSetMetaData rsmd = rs.getMetaData();
                            int columnsNumber = rsmd.getColumnCount();
                            while (rs.next()) {
                                for (int i = 1; i <= columnsNumber; i++) {
                                    if (i > 1) System.out.print(",  ");
                                    String columnValue = rs.getString(i);

                                    System.out.print(columnValue + " " + rsmd.getColumnName(i));
                                }
                                System.out.println("");
                            }*/

                        }
                        break;
                    case "viewFriends":
                        System.out.println("VIEW FRIENDS OF " + jsonObject.getString("email"));
                        userID = getUserID( jsonObject.getString("email"));
                        query = "SELECT * FROM friends WHERE friends.user1ID =? or friends.user2ID=?";
                        stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                        stmt.setInt(1, userID);
                        stmt.setInt(2, userID);
                        rs = stmt.executeQuery();

                        count=0;
                        StringBuilder str = new StringBuilder();
                        str.append("SELECT * FROM user WHERE userID = ? ");
                        while (rs.next()) {
                            if(count!=0){
                                str.append("or userID=? ");
                            }
                            count++;

                            if(rs.getInt("user1ID")==userID){
                                System.out.println("FRIENDS WITH " + rs.getInt("user2ID"));
                            }else{
                                System.out.println("FRIENDS WITH " + rs.getInt("user1ID"));
                            }

                        }
                        System.out.println(count + " FRIEND COUNT");
                        System.out.println("QUERY IS " + str );
                        if(count>0){


                        query = str.toString();
                        stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                        rs.first();
                        for(int x=0;x<count;x++){
                            if(rs.getInt("user1ID")==userID){
                                stmt.setInt(x+1, rs.getInt("user2ID"));
                                //System.out.println("ADDING "+rs.getInt("user2ID"));
                            }else{
                                stmt.setInt(x+1, rs.getInt("user1ID"));
                               // System.out.println("ADDING "+rs.getInt("user1ID"));
                            }
                            rs.next();
                        }
                        rs = stmt.executeQuery();

                        count=1;
                        while (rs.next()) {
                            returnJSON.put("email"+String.valueOf(count),rs.getString("email"));
                            count++;

                        }
                        }
                        System.out.println("json is "+returnJSON.toString());
                        break;
                    case "sendImage":
                        String image = jsonObject.get("image").toString();
                        Date date= new Date();

                        long time = date.getTime();
                        Timestamp ts = new Timestamp(time);
                        System.out.println("Current Time Stamp: " + ts);
                        //convert from base64 to byte array
                        userID = getUserID(jsonObject.getString("fromEmail"));
                        int toUserID = getUserID(jsonObject.getString("toEmail"));
                        query = "INSERT INTO sentimages (fromUserID,toUserID,timeSent) VALUES (?,?,?)";
                        stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                        stmt.setInt(1, userID);
                        stmt.setInt(2, toUserID);
                        stmt.setTimestamp(3,ts);
                        row = stmt.executeUpdate();
                        rs = stmt.getGeneratedKeys();
                        int generatedKey=-1;
                        if (rs.next()) {
                            generatedKey = rs.getInt(1);
                            returnJSON.put("isSuccess",true);
                        }else{
                            returnJSON.put("isSuccess",false);
                        }
                        System.out.println("GENERATED KEY IS "+ generatedKey);
                        byte[] imageByteArray = decodeImage(image);
                        FileOutputStream imageOutFile = new FileOutputStream(String.valueOf(generatedKey) +".png");
                        imageOutFile.write(imageByteArray);
                        imageOutFile.close();
                        System.out.println("IMAGE IS " + image.getBytes(StandardCharsets.UTF_8).length);

                        /*ResultSetMetaData rsmd = rs.getMetaData();
                        int columnsNumber = rsmd.getColumnCount();
                        while (rs.next()) {
                            for (int i = 1; i <= columnsNumber; i++) {
                                if (i > 1) System.out.print(",  ");
                                String columnValue = rs.getString(i);

                                System.out.print(columnValue + " " + rsmd.getColumnName(i));
                            }
                            System.out.println("");
                        }*/
                        break;
                    case "viewMsgs":
                        userID = getUserID(jsonObject.getString("email"));

                        if(jsonObject.getString("fromEmail")==""){
                            System.out.println("FROM ANYONE");
                            query = "SELECT * FROM sentImages WHERE toUserID=?";
                            stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                            stmt.setInt(1, userID);

                        }else{
                            System.out.println("FROM " + jsonObject.getString("fromEmail"));
                            int fromUserID = getUserID(jsonObject.getString("fromEmail"));
                            query = "SELECT * FROM sentImages WHERE (toUserID=? and fromUserID=?) or (toUserID=? and fromUserID=?)";
                            stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                            stmt.setInt(1, userID);
                            stmt.setInt(2, fromUserID);
                            stmt.setInt(3, fromUserID);
                            stmt.setInt(4, userID);

                        }

                        rs = stmt.executeQuery();
                        count=0;
                        JSONObject imageJson = new JSONObject();
                        while (rs.next()) {
                            File file = new File(rs.getString("imageID")+".png");
                            //Image conversion to byte array
                            FileInputStream imageInFile = new FileInputStream(file);
                            byte imageData[] = new byte[(int) file.length()];
                            imageInFile.read(imageData);
                            String imageDataString = encodeImage(imageData);
                            System.out.println(imageDataString);
                            imageInFile.close();
                            imageJson.put("image",imageDataString);
                            imageJson.put("time",rs.getTimestamp("timeSent"));
                            imageJson.put("fromEmail",getUserEmail(rs.getInt("fromUserID")));
                            imageJson.put("toEmail",getUserEmail(rs.getInt("toUserID")));
                            imageJson.put("imageID",rs.getInt("imageID"));
                            returnJSON.put("image"+Integer.valueOf(count+1),imageJson.toString());
                            count++;
                        }
                        System.out.println("SUCCESSFULLY SENT IMAGE TO CLIENT");
                        break;
                    case "deleteMsg":
                        query = "delete from sentimages where imageID=?";
                        stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                        stmt.setInt(1, jsonObject.getInt("imageID"));
                        int deleted = stmt.executeUpdate();
                        if(deleted==0){
                            returnJSON.put("isSuccess",false);
                        }else{
                            returnJSON.put("isSuccess",true);
                        }
                        break;
                    case "updatePath":
                        query = "UPDATE user SET path =? WHERE email=?";
                        stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                        stmt.setString(1, jsonObject.getString("path"));
                        stmt.setString(2, jsonObject.getString("email"));
                        int success = stmt.executeUpdate();
                        if(success==0){
                            returnJSON.put("isSuccess",false);
                        }else{
                            returnJSON.put("isSuccess",true);
                        }
                        break;
                    case "deleteAccount":
                        query = "DELETE FROM user WHERE email=?";
                        stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                        stmt.setString(1, jsonObject.getString("email"));
                        if(stmt.executeUpdate()==0){
                            returnJSON.put("isSuccess",false);
                        }else{
                            returnJSON.put("isSuccess",true);
                        }
                        break;
                }


                pw.println(returnJSON.toString());
            } catch (IOException | SQLException e) {
                System.out.println("Exeception");
                return;
               // e.printStackTrace();
            }

        }
    }public static String encodeImage(byte[] imageByteArray) {
        return Base64.getEncoder().encodeToString(imageByteArray);
    }
    public static byte[] decodeImage(String imageDataString) {
        return Base64.getDecoder().decode(imageDataString);
    }
}
