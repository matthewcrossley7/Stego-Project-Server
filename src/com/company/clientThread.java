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
    String folderLocation="C:\\Users\\mcrossley\\IdeaProjects\\Stego Server\\stored Images\\";
    Socket socket;
    Connection conn;
    public clientThread(Socket socket){
        this.socket=socket;
        conn = null;
        try{
            //allows access to local database using JDBC
            conn = (Connection) DriverManager.getConnection("jdbc:mysql://localhost:3306/stegodb","root","root");
            if(conn==null){
                System.out.println("Error connecting to database");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    //function returns the userID from a user's email
    public int getUserID(String email) throws SQLException {
        //prepare sql statement
        String query = "select * from User where email=?";
        PreparedStatement stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        //bind parameters to statement
        stmt.setString(1, email);
        //execute SQL statement
        ResultSet rs = stmt.executeQuery();
        int count=0;
        while(rs.next()){
            count++;
        }
        rs.first();
        //if no user with email found then return -1
        if(count==0){
            return -1;
        }else {
            return rs.getInt("userID");
        }
    }
    //returns a user's email given a userID
    public String getUserEmail(int userID) throws SQLException {
        //prepares sql statement and binds userID paramater
        String query = "select * from User where userID=?";
        PreparedStatement stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        stmt.setInt(1, userID);
        ResultSet rs = stmt.executeQuery();
        int count=0;
        while(rs.next()){
            count++;
        }
        rs.first();
        //returns null if no user found
        if(count==0){
            return "null";
        }else {
            return rs.getString("email");
        }
    }
    //entry point of new server thread
    public void run() {

        boolean repeat=true;
        String query;
        JSONObject returnJSON;
        ResultSet rs;
        int row;
        int count;
        int userID;
        PreparedStatement stmt ;
        while (repeat){
            try {
                //initiates socket and buffer reader to listen for requests
                PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String userInput = null;

                //reads in string recieved
                userInput = br.readLine();
                returnJSON=new JSONObject();

                JSONObject jsonObject;
                if(userInput!=null){
                    //create JSONObject from string recieved
                    jsonObject = new JSONObject(userInput);
                }else{
                    jsonObject = new JSONObject();
                    jsonObject.put("type","null");
                }

                //switch statement used to run required code based on type of request
                switch (jsonObject.getString("type")){
                    //checks whether user is already registered in the system
                    case "login":
                        userID = getUserID(jsonObject.getString("email"));
                        if(userID==-1){
                            //returns false if not registered
                            returnJSON.put("isRegistered",false);
                        }else{
                            returnJSON.put("isRegistered",true);
                            query = "select path from User where userID=?";
                            stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                            stmt.setInt(1, userID);
                            rs = stmt.executeQuery();
                            rs.first();
                            //returns true and user's filepath if already registered
                            returnJSON.put("filePath",rs.getString("path"));
                        }
                        break;
                    //registers user in database
                    case "register":
                        //inserts users email and filepath into database
                        query = "INSERT INTO user (email,path) VALUES (?,?)";
                        stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                        stmt.setString(1, jsonObject.getString("email"));
                        stmt.setString(2, jsonObject.getString("path"));
                        row = stmt.executeUpdate();
                        returnJSON.put("outcome",false);
                        break;
                    //adds a user as a friend
                    case "addFriend":
                       //retrieve userID of friend being added
                        int friendsID = getUserID(jsonObject.getString("friendEmail"));
                        if(friendsID==-1){
                            //if friend isn't registered then return an error
                            returnJSON.put("outcome",false);
                            returnJSON.put("reason","notRegistered");
                        }else{
                            //get userID for user adding a friend
                            userID = getUserID(jsonObject.getString("userEmail"));
                            //checks if users are already friends
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
                                //if users aren't current friends then thir userIDs are added in database
                                query = "INSERT INTO friends (user1ID,user2ID) VALUES (?,?)";
                                stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                                stmt.setInt(1, userID);
                                stmt.setInt(2, friendsID);
                                row = stmt.executeUpdate();
                                returnJSON.put("isSuccess",true);
                            }else{
                                //if users are already friends then error returned
                                returnJSON.put("isSuccess",false);
                            }
                        }
                        break;
                    //returns a user's list of friends
                    case "viewFriends":
                        //get user's userID
                        userID = getUserID( jsonObject.getString("email"));
                        //prepare and execute statement to obtain the userIDs of friends
                        query = "SELECT * FROM friends WHERE friends.user1ID =? or friends.user2ID=?";
                        stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                        stmt.setInt(1, userID);
                        stmt.setInt(2, userID);
                        rs = stmt.executeQuery();
                        count=0;
                        StringBuilder str = new StringBuilder();
                        //create a sql statement that selects information about all users the user is friends with
                        str.append("SELECT * FROM user WHERE userID = ? ");
                        while (rs.next()) {
                            if(count!=0){
                                str.append("or userID=? ");
                            }
                            count++;
                        }
                        if(count>0){
                            query = str.toString();
                            stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                            rs.first();
                            //bind userID values as parameters to statement
                            for(int x=0;x<count;x++){
                                if(rs.getInt("user1ID")==userID){
                                    stmt.setInt(x+1, rs.getInt("user2ID"));
                                }else{
                                    stmt.setInt(x+1, rs.getInt("user1ID"));
                                }
                                rs.next();
                            }
                            //execute sql statement
                            rs = stmt.executeQuery();

                            count=1;
                            //add each email of user to JSON that will be sent back to client
                            while (rs.next()) {
                                returnJSON.put("email"+String.valueOf(count),rs.getString("email"));
                                count++;
                            }
                        }
                        break;
                    //sends an image to specified user by adding record to database and saving image to file system
                    case "sendImage":
                        String image = jsonObject.get("image").toString();
                        //gets the current datetime
                        Date date= new Date();
                        long time = date.getTime();
                        Timestamp ts = new Timestamp(time);


                        userID = getUserID(jsonObject.getString("fromEmail"));
                        int toUserID = getUserID(jsonObject.getString("toEmail"));
                        //record inserted into database
                        query = "INSERT INTO sentimages (fromUserID,toUserID,timeSent) VALUES (?,?,?)";
                        stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                        stmt.setInt(1, userID);
                        stmt.setInt(2, toUserID);
                        stmt.setTimestamp(3,ts);
                        row = stmt.executeUpdate();
                        rs = stmt.getGeneratedKeys();
                        int generatedKey=-1;
                        if (rs.next()) {
                            //the primary key generated is retrieved and user as filename of image
                            generatedKey = rs.getInt(1);
                            returnJSON.put("isSuccess",true);
                        }else{
                            returnJSON.put("isSuccess",false);
                        }
                        //image string is converted from base64 to byte array and then saved in local filesystem with filename of generated key
                        byte[] imageByteArray = decodeImage(image);
                        FileOutputStream imageOutFile = new FileOutputStream(folderLocation+String.valueOf(generatedKey) +".png");
                        imageOutFile.write(imageByteArray);
                        imageOutFile.close();
                        break;
                    //views a list of messages sent between 2 users
                    case "viewMsgs":
                        //gets userID of user
                        userID = getUserID(jsonObject.getString("email"));
                        if(jsonObject.getString("fromEmail")==""){
                            //if all incoming messages required
                            //sql query prepared
                            query = "SELECT * FROM sentImages WHERE toUserID=?";
                            stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                            stmt.setInt(1, userID);

                        }else{
                            //if only messages with a specific user is required
                            //prepares sql statement and binds paramaters
                            int fromUserID = getUserID(jsonObject.getString("fromEmail"));
                            query = "SELECT * FROM sentImages WHERE (toUserID=? and fromUserID=?) or (toUserID=? and fromUserID=?)";
                            stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                            stmt.setInt(1, userID);
                            stmt.setInt(2, fromUserID);
                            stmt.setInt(3, fromUserID);
                            stmt.setInt(4, userID);

                        }
                        //query executed
                        rs = stmt.executeQuery();
                        count=0;
                        JSONObject imageJson = new JSONObject();
                        //for each message retrieved from query
                        while (rs.next()) {
                            //open relevant image file and convert to a string value
                            File file = new File(folderLocation+rs.getString("imageID")+".png");
                            FileInputStream imageInFile = new FileInputStream(file);
                            byte imageData[] = new byte[(int) file.length()];
                            imageInFile.read(imageData);
                            String imageDataString = encodeImage(imageData);
                            imageInFile.close();
                            //add information about the message to the json object
                            imageJson.put("image",imageDataString);
                            imageJson.put("time",rs.getTimestamp("timeSent"));
                            imageJson.put("fromEmail",getUserEmail(rs.getInt("fromUserID")));
                            imageJson.put("toEmail",getUserEmail(rs.getInt("toUserID")));
                            imageJson.put("imageID",rs.getInt("imageID"));
                            returnJSON.put("image"+Integer.valueOf(count+1),imageJson.toString());
                            count++;
                        }
                        break;
                    //deletes a message
                    case "deleteMsg":
                        //prepares sql delete query and binds imageID paramater
                        query = "delete from sentimages where imageID=?";
                        stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                        stmt.setInt(1, jsonObject.getInt("imageID"));
                        int deleted = stmt.executeUpdate();
                        //status of operation returned, either true/ false
                        if(deleted==0){
                            returnJSON.put("isSuccess",false);
                        }else{
                            returnJSON.put("isSuccess",true);
                        }
                        break;
                    //updates the filepath of a user
                    case "updatePath":
                        //prepares statement and binds email and new file path
                        query = "UPDATE user SET path =? WHERE email=?";
                        stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                        stmt.setString(1, jsonObject.getString("path"));
                        stmt.setString(2, jsonObject.getString("email"));
                        int success = stmt.executeUpdate();
                        //status of operation returned, either true or false
                        if(success==0){
                            returnJSON.put("isSuccess",false);
                        }else{
                            returnJSON.put("isSuccess",true);
                        }
                        break;
                    //deletes a users account, any messages they may of sent or received and the list of users they were friends with
                    case "deleteAccount":
                        userID = getUserID(jsonObject.getString("email"));

                        //deletes any friends connections of user
                        query = "DELETE FROM friends WHERE user1ID=? OR user2ID =?";
                        stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                        stmt.setInt(1, userID);
                        stmt.setInt(2, userID);
                        stmt.executeUpdate();

                        //deletes any messages that have been sent or recieved
                        query = "DELETE FROM sentimages WHERE fromUserID=? OR toUserID =?";
                        stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                        stmt.setInt(1, userID);
                        stmt.setInt(2, userID);
                        stmt.executeUpdate();

                        //deletes user from user table
                        query = "DELETE FROM user WHERE email=?";
                        stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                        stmt.setString(1, jsonObject.getString("email"));

                        //returns true or false depending on outcome of statements
                        if(stmt.executeUpdate()==0){
                            returnJSON.put("isSuccess",false);
                        }else{
                            returnJSON.put("isSuccess",true);
                        }
                        break;
                    //removes a user as a friend
                    case "deleteFriend":
                        //get userIDs of both users
                        userID = getUserID(jsonObject.getString("email1"));
                        int userID2 = getUserID(jsonObject.getString("email2"));
                        //prepare statement and bind paramaters
                        query = "DELETE FROM friends WHERE (user1ID=? AND user2ID=?) OR (user1ID=? AND user2ID=?)";
                        stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                        stmt.setInt(1, userID);
                        stmt.setInt(2, userID2);
                        stmt.setInt(3, userID2);
                        stmt.setInt(4, userID);
                        //return status of operation
                        if(stmt.executeUpdate()==0){
                            returnJSON.put("isSuccess",false);
                        }else{
                            returnJSON.put("isSuccess",true);
                        }

                        break;
                }
                //converts the JSON object to a string and returns it back to client
                pw.println(returnJSON.toString());
            } catch (IOException | SQLException e) {

                e.printStackTrace();
                return;

            }

        }
    }
    //used to covert image to string
    public static String encodeImage(byte[] imageByteArray) {
        return Base64.getEncoder().encodeToString(imageByteArray);
    }
    //used to convert string to image
    public static byte[] decodeImage(String imageDataString) {
        return Base64.getDecoder().decode(imageDataString);
    }
}
