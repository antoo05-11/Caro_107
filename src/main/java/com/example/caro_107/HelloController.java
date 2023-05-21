package com.example.caro_107;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class HelloController implements Initializable {
    @FXML
    private ScrollPane mainView;
    @FXML
    private Button playButton;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label myIDLabel;
    @FXML
    private Label opponentIDLabel;
    private int userID;
    private int opponentID = -1;
    GridPane caroGridPane = new GridPane();
    Rectangle[][] caroView;
    private SQLConnection sqlConnection;
    private String hashDeviceID;

    public void setHashDeviceID(String hashDeviceID) {
        this.hashDeviceID = hashDeviceID;
    }

    public void setSQLConnection(SQLConnection sqlConnection) {
        this.sqlConnection = sqlConnection;
    }

    public void setUserID(int userID) {
        this.userID = userID;
        Platform.runLater(() -> myIDLabel.setText(String.valueOf(userID)));
    }

    int row;
    int col;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        mainView.setContent(caroGridPane);
        final int tableSize = 100;
        final int cellSize = 30;
        caroView = new Rectangle[tableSize][tableSize];
        for (int row = 0; row < tableSize; row++) {
            for (int column = 0; column < tableSize; column++) {
                Rectangle cell = new Rectangle(cellSize, cellSize, Color.WHITE);
                cell.setStroke(Color.GREY);
                caroGridPane.add(cell, column, row);
                caroView[row][column] = cell;
            }
        }
        for (int i = 0; i < tableSize; i++) {
            for (int j = 0; j < tableSize; j++) {
                int finalI = i;
                int finalJ = j;
                caroView[finalI][finalJ].setOnMouseClicked(event -> {
                    playTurn.setValue(false);
                    caroView[finalI][finalJ].setStyle("-fx-background-color: red;");
                    this.row = finalI;
                    this.col = finalJ;
                });
            }
        }
        playButton.setOnMouseClicked(event -> {
            playButton.setDisable(true);
            String query = String.format("update temporary_users set status = 'find_opponent' where deviceID = '%s';", hashDeviceID);
            runTask(() -> {
                sqlConnection.updateQuery(query);
            }, () -> {
                runTask(this::waitForMatching, () -> {
                    System.out.println("finding successfully!");
                    if (turnID == userID) playTurn.setValue(true);
                }, progressIndicator, null);

            }, progressIndicator, null);
        });

        playTurn.addListener((observableValue, oldValue, newValue) -> {
            if (!newValue) {
                caroGridPane.setDisable(true);
                runTask(this::listenForTurn, null, progressIndicator, null);
            } else {
                caroGridPane.setDisable(false);
                runTask(() -> {
                    String query;
                    if (firstTurnID == userID)
                        query = "update matches set row1 = " + this.row + ", col1 = " + this.col + ", userTurnID = " + opponentID + ";";
                    else
                        query = "update matches set row2 = " + this.row + ", col2 = " + this.col + ", userTurnID = " + opponentID + ";";
                    sqlConnection.updateQuery(query);
                }, null, progressIndicator, null);
            }
        });
    }

    int turnID;
    int matchID = -1;

    void waitForMatching() {
        while (matchID == -1) {
            String query = "select * from temporary_users where userID = " + userID;
            ResultSet resultSet = sqlConnection.getDataQuery(query);
            try {
                if (resultSet.next()) {
                    matchID = resultSet.getInt("matchID");
                    if (matchID > 0) {
                        query = "select * from matches where matchID = " + matchID;
                        resultSet = sqlConnection.getDataQuery(query);
                        turnID = resultSet.getInt("userTurnID");
                        opponentID = resultSet.getInt("userID2");
                        Platform.runLater(() -> opponentIDLabel.setText(String.valueOf(opponentID)));
                        if (turnID == userID) playTurn.setValue(true);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    BooleanProperty playTurn = new SimpleBooleanProperty(false);
    int firstTurnID;

    void listenForTurn() {
        while (!playTurn.getValue()) {
            String query = "select * from matches where matchID = " + matchID;
            ResultSet resultSet = sqlConnection.getDataQuery(query);
            try {
                turnID = resultSet.getInt("userTurnID");
                if (turnID == userID) {
                    int row;
                    int col;
                    firstTurnID = resultSet.getInt("firstTurnID");
                    if (firstTurnID == userID) {
                        row = resultSet.getInt("row2");
                        col = resultSet.getInt("col2");
                    } else {
                        row = resultSet.getInt("row1");
                        col = resultSet.getInt("col1");
                    }
                    if (row != -1 && col != -1) {
                        caroView[row][col].setStyle("-fx-background-color: blue;");
                        playTurn.setValue(true);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    static public void runTask(Runnable taskFunction, Runnable finishFunction, ProgressIndicator progressIndicator, Node bannedArea) {
        Task<Void> task;
        task = new Task<>() {
            @Override
            protected Void call() {
                taskFunction.run();
                return null;
            }
        };
        if (progressIndicator != null) {
            progressIndicator.visibleProperty().bind(task.runningProperty());
        }
        if (bannedArea != null) {
            bannedArea.disableProperty().bind(task.runningProperty());
        }
        if (finishFunction != null) {
            task.setOnSucceeded(workerStateEvent -> {
                finishFunction.run();
            });
        }
        task.setOnFailed(e -> task.getException().printStackTrace());
        new Thread(task).start();
    }

    public ProgressIndicator getProgressIndicator() {
        return progressIndicator;
    }
}