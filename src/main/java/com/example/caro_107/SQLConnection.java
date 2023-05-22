package com.example.caro_107;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.*;
import java.util.Scanner;

public class SQLConnection {
    private Connection connection;
    private String server;
    private String port;
    private String database;
    private String user;
    private String password;
    private boolean reconnectingNotification;
    private boolean reconnecting;

    int userID;

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public boolean isReconnecting() {
        return reconnectingNotification;
    }

    public void setReconnecting(boolean reconnectingNotification) {
        this.reconnectingNotification = reconnectingNotification;
    }

    public void configure(InputStream configFile, String serverID) {
            Scanner scanner = new Scanner(configFile);
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                if (line.contains(serverID)) {
                    while (scanner.hasNext()) {
                        line = scanner.nextLine();

                        if (line.contains("}")) break;

                        if (line.contains("user:")) {
                            user = line.substring(line.indexOf("user:") + 5);
                        } else if (line.contains("password:")) {
                            password = line.substring(line.indexOf("password:") + 9);
                        } else if (line.contains("server:")) {
                            server = line.substring(line.indexOf("server:") + 7);
                        } else if (line.contains("database:")) {
                            database = line.substring(line.indexOf("database:") + 9);
                        } else if (line.contains("port:")) {
                            port = line.substring(line.indexOf("port:") + 5);
                        }
                    }
                    break;
                }
            }

    }


    public String getURL() {
        return String.format("jdbc:mysql://%s:%s/%s", server, port, database);
    }

    public Connection getConnection() {
        return connection;
    }

    public SQLConnection() {

    }

    /**
     * You need url to database server, username and password to log in database server.
     *
     * @see Connection
     * @since 1.0
     */
    public void connectServer() {
        while (connection == null) {
            try {
                connection = DriverManager.getConnection(getURL(), user, password);
            } catch (SQLException e) {
                e.printStackTrace();
                reconnectingNotification = true;
                reconnecting = true;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Get result set of a SQL query.
     *
     * @since 1.0
     */
    public ResultSet getDataQuery(String query) {
        Statement statement;
        ResultSet resultSet = null;
        try {
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);
        } catch (SQLException e) {
            System.out.println(query);
            e.printStackTrace();
        }
        return resultSet;
    }


    public void updateQuery(String query) {
        try {
            Statement statement;
            statement = connection.createStatement();
            statement.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}