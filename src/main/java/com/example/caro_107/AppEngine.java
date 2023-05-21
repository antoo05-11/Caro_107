package com.example.caro_107;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class AppEngine extends Application {
    public static SQLConnection sqlConnection;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(AppEngine.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        HelloController controller = fxmlLoader.getController();
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();

        final String user = "MRkPzdU0Jjzpq8HhLWY1";
        final String password = "MRkPzdU0Jjzpq8HhLWY1";
        final String url = "jdbc:mysql://u2n3pkuyuobnpxzf:MRkPzdU0Jjzpq8HhLWY1@b9z5evsse65g8b1l5vgs-mysql.services.clever-cloud.com:3306/b9z5evsse65g8b1l5vgs";

        sqlConnection = new SQLConnection(url, user, password);
        controller.setSQLConnection(sqlConnection);
        HelloController.runTask(() -> {
            sqlConnection.connectServer();
            System.out.println(sqlConnection.getConnection());
            String hashDeviceID = LocalDateTime.now().toString() + Math.random() * 100000000;
            System.out.println(hashDeviceID);
            String query = String.format("insert into temporary_users (deviceID) VALUES ('%s');", hashDeviceID);
            controller.setHashDeviceID(hashDeviceID);
            sqlConnection.updateQuery(query);
            query = String.format("select userID from temporary_users where deviceID = '%s';", hashDeviceID);
            ResultSet resultSet = sqlConnection.getDataQuery(query);
            try {
                if (resultSet.next()) controller.setUserID(resultSet.getInt("userID"));
                System.out.println(resultSet.getInt("userID"));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, null, controller.getProgressIndicator(), null);
    }

    public static void main(String[] args) {
        launch();
    }
}