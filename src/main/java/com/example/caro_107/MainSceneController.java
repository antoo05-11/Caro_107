package com.example.caro_107;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class MainSceneController implements Initializable {
    @FXML
    private ScrollPane mainView;
    @FXML
    private Button playButton;
    @FXML
    private Label myIDLabel;
    @FXML
    private Label opponentIDLabel;
    @FXML
    private Label loadingLabel;
    @FXML
    private Label resultLabel;
    @FXML
    private Label opponentUsernameLabel;
    @FXML
    private TextField usernameTextField;
    private int userID;
    private int opponentID = -1;
    GridPane caroGridPane = new GridPane();
    StackPane[][] caroView;
    int[][] matchGrid;
    private SQLConnection sqlConnection;
    private String hashDeviceID;


    public void setHashDeviceID(String hashDeviceID) {
        this.hashDeviceID = hashDeviceID;
    }

    public void setSQLConnection(SQLConnection sqlConnection) {
        this.sqlConnection = sqlConnection;
    }

    public Label getLoadingLabel() {
        return loadingLabel;
    }

    public void setUserID(int userID) {
        this.userID = userID;
        Platform.runLater(() -> myIDLabel.setText(String.valueOf(userID)));
    }

    int row;
    int col;
    final int tableSize = 50;
    final int cellSize = 30;

    FontAwesomeIconView getIcon(int userID) {
        FontAwesomeIconView icon = new FontAwesomeIconView();
        if (this.firstTurnID == userID) icon.setGlyphName("CLOSE");
        else icon.setGlyphName("CIRCLE_ALT");
        icon.setGlyphSize(22);
        return icon;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        mainView.setContent(caroGridPane);

        caroView = new StackPane[tableSize][tableSize];
        matchGrid = new int[tableSize][tableSize];

        for (int row = 0; row < tableSize; row++) {
            for (int column = 0; column < tableSize; column++) {
                StackPane cell = new StackPane();

                cell.setPrefSize(cellSize, cellSize);
                cell.setStyle("-fx-border-width: 0.5 0.5 0.5 0.5; -fx-border-color: grey");
                caroGridPane.add(cell, column, row);
                caroView[row][column] = cell;
                matchGrid[row][column] = 0;
            }
        }
        caroGridPane.setDisable(true);

        for (int i = 0; i < tableSize; i++) {
            for (int j = 0; j < tableSize; j++) {
                int finalI = i;
                int finalJ = j;
                caroView[finalI][finalJ].setOnMouseClicked(event -> {
                    if (matchGrid[finalI][finalJ] == 0) {
                        Platform.runLater(() -> caroView[finalI][finalJ].getChildren().add(getIcon(userID)));
                        matchGrid[finalI][finalJ] = 1;
                        this.row = finalI;
                        this.col = finalJ;
                        playTurn.setValue(false);
                    }
                });
            }
        }
        playButton.setOnMouseClicked(event -> {
            playButton.setDisable(true);
            String query = String.format("update temporary_users set status = 'find_opponent', username = '%s' where deviceID = '%s';", usernameTextField.getText(), hashDeviceID);
            runTask(() -> sqlConnection.updateQuery(query), () -> runTask(this::waitForMatching, () -> {
                System.out.println("finding successfully!");
                if (turnID == userID) playTurn.setValue(true);
            }, loadingLabel, null), loadingLabel, null);
        });

        playTurn.addListener((observableValue, oldValue, newValue) -> {
            if (matchID > 0) {
                if (!newValue) {
                    caroGridPane.setDisable(true);
                    runTask(this::sendTurn,
                            () -> runTask(() -> {
                                        if (result == RESULT.NONE) waitForTurnAndUpdate();
                                    },
                                    null, loadingLabel, null),
                            loadingLabel, null);
                } else {
                    caroGridPane.setDisable(false);
                }
            }
        });
    }

    int turnID;
    int matchID = -1;


    String userColor;
    String opponentColor;

    void waitForMatching() {
        Platform.runLater(() -> loadingLabel.setText("Finding opponent..."));
        while (matchID == -1) {
            System.out.println("wait for matching...");
            String query = "select * from temporary_users where userID = " + userID;
            ResultSet resultSet = sqlConnection.getDataQuery(query);
            try {
                if (resultSet.next()) {
                    matchID = resultSet.getInt("matchID");
                    if (matchID > 0) {
                        query = "select * from matches where matchID = " + matchID;
                        resultSet = sqlConnection.getDataQuery(query);
                        if (resultSet.next()) {
                            turnID = resultSet.getInt("userTurnID");
                            firstTurnID = resultSet.getInt("firstTurnID");
                            opponentID = resultSet.getInt("userID2");
                            if (opponentID == userID) opponentID = resultSet.getInt("userID1");
                            Platform.runLater(() -> opponentIDLabel.setText(String.valueOf(opponentID)));
                            if (turnID == userID) {
                                playTurn.setValue(true);
                                userColor = "red";
                                opponentColor = "blue";
                            } else {
                                userColor = "blue";
                                opponentColor = "red";
                                runTask(this::waitForTurnAndUpdate, null, loadingLabel, null);
                            }

                            query = "select * from temporary_users where userID = " + opponentID;
                            resultSet = sqlConnection.getDataQuery(query);
                            if (resultSet.next()) {
                                if (resultSet.getString("username") != null) {
                                    String opponentName = resultSet.getString("username");
                                    Platform.runLater(() -> opponentUsernameLabel.setText(opponentName));
                                }
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    BooleanProperty playTurn = new SimpleBooleanProperty(false);
    Timeline timeline = new Timeline(new KeyFrame(new Duration(3000), actionEvent -> {
        for (int i = 0; i < tableSize; i++) {
            for (int j = 0; j < tableSize; j++) {
                if (caroView[i][j].getChildren().size() > 0)
                    caroView[i][j].getChildren().remove(0);
                matchGrid[i][j] = 0;
                firstTurnID = -1;
                turnID = -1;
                matchID = -1;
                playTurn.setValue(false);
                Platform.runLater(() -> {
                    resultLabel.setText("PLAYING");
                    opponentUsernameLabel.setText("");
                    myIDLabel.setText("");
                    opponentIDLabel.setText("");
                    loadingLabel.setText("");
                });
                playTurn.setValue(false);
            }
        }
        playButton.setDisable(false);
    }));
    int firstTurnID;
    RESULT result;

    void sendTurn() {
        result = checkResult(row, col);
        String query;
        if (firstTurnID == userID)
            query = "update matches set row1 = " + this.row + ", col1 = " + this.col + ", userTurnID = " + opponentID + ";";
        else
            query = "update matches set row2 = " + this.row + ", col2 = " + this.col + ", userTurnID = " + opponentID + ";";
        sqlConnection.updateQuery(query);
        if (result != RESULT.NONE) {
            query = String.format("update matches set result = %d where matchID = %d;", userID, matchID);
            sqlConnection.updateQuery(query);

            query = String.format("update temporary_users set status = 'in_lobby', matchID = -1 where userID = %d or userID = %d;", userID, opponentID);
            sqlConnection.updateQuery(query);

            Platform.runLater(() -> resultLabel.setText("VICTORY"));
            timeline.setCycleCount(1);
            timeline.play();
        }
    }

    void waitForTurnAndUpdate() {
        Platform.runLater(() -> loadingLabel.setText("Waiting for opponent..."));

        String query;
        while (!playTurn.getValue() && result != RESULT.DEFEAT) {
            query = "select * from matches where matchID = " + matchID;
            ResultSet resultSet = sqlConnection.getDataQuery(query);
            try {
                if (resultSet.next()) {
                    turnID = resultSet.getInt("userTurnID");
                    if (turnID == userID) {
                        int opponentRow;
                        int opponentCol;
                        firstTurnID = resultSet.getInt("firstTurnID");
                        if (firstTurnID == userID) {
                            opponentRow = resultSet.getInt("row2");
                            opponentCol = resultSet.getInt("col2");
                        } else {
                            opponentRow = resultSet.getInt("row1");
                            opponentCol = resultSet.getInt("col1");
                        }
                        if (opponentRow != -1 && col != -1) {
                            matchGrid[opponentRow][opponentCol] = 2;
                            Platform.runLater(() -> caroView[opponentRow][opponentCol].getChildren().add(getIcon(opponentID)));
                            resolveResult(opponentRow, opponentCol);
                        }
                    }
                }
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static public void runTask(Runnable taskFunction, Runnable finishFunction, Node progressIndicator, Node bannedArea) {
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
            task.setOnSucceeded(workerStateEvent -> finishFunction.run());
        }
        task.setOnFailed(e -> task.getException().printStackTrace());
        new Thread(task).start();
    }

    enum RESULT {
        VICTORY, DEFEAT, NONE
    }

    void resolveResult(int row, int col) {
        result = checkResult(row, col);

        if (result == RESULT.NONE)
            playTurn.setValue(true);
        else {
            Platform.runLater(() -> resultLabel.setText("DEFEAT"));

            timeline.setCycleCount(1);
            timeline.play();
        }
    }

    RESULT checkResult(int row, int col) {
        StringBuilder s = new StringBuilder();

        for (int i = row - 4; i < row + 4; i++) {
            if (isValidIndex(i, col)) {
                if (matchGrid[i][col] == 1) s.append("1");
                else if (matchGrid[i][col] == 2) s.append("2");
                else s.append("0");
            }
        }
        if (s.toString().contains("11111")) return RESULT.VICTORY;
        else if (s.toString().contains("22222")) return RESULT.DEFEAT;

        s.setLength(0);

        for (int j = col - 4; j < col + 4; j++) {
            if (isValidIndex(row, j)) {
                if (matchGrid[row][j] == 1) s.append("1");
                else if (matchGrid[row][j] == 2) s.append("2");
                else s.append("0");
            }
        }
        if (s.toString().contains("11111")) return RESULT.VICTORY;
        else if (s.toString().contains("22222")) return RESULT.DEFEAT;

        for (int i = -4; i <= 4; i++) {
            if (isValidIndex(row + i, col + i)) {
                if (matchGrid[row + i][col + i] == 1) s.append("1");
                else if (matchGrid[row + i][col + i] == 2) s.append("2");
                else s.append("0");
            }
        }
        if (s.toString().contains("11111")) return RESULT.VICTORY;
        else if (s.toString().contains("22222")) return RESULT.DEFEAT;

        for (int i = -4; i <= 4; i++) {
            if (isValidIndex(row + i, col - i)) {
                if (matchGrid[row + i][col - i] == 1) s.append("1");
                else if (matchGrid[row + i][col - i] == 2) s.append("2");
                else s.append("0");
            }
        }
        if (s.toString().contains("11111")) return RESULT.VICTORY;
        else if (s.toString().contains("22222")) return RESULT.DEFEAT;

        return RESULT.NONE;
    }

    boolean isValidIndex(int row, int col) {
        return row >= 0 && row < matchGrid.length && col >= 0
                && col < matchGrid[row].length;
    }

    @FXML
    Label onlineNumberLabel;
    @FXML
    Label readyNumberLabel;

    void keepConnection() {
        while (true) {
            String query = "select * from\n" +
                    "    (select count(*) from temporary_users where status = 'in_lobby') as t1\n" +
                    "cross join (select count(*) from temporary_users where status = 'find_opponent') as t2;";
            ResultSet resultSet = sqlConnection.getDataQuery(query);
            try {
                if (resultSet.next()) {
                    int onlineNumber = resultSet.getInt(1);
                    int readyNumber = resultSet.getInt(2);
                    Platform.runLater(() -> {
                        onlineNumberLabel.setText("Online: " + onlineNumber);
                        readyNumberLabel.setText("Ready to play: " + readyNumber);
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            query = "select * from temporary_users where userID = " + userID;
            resultSet = sqlConnection.getDataQuery(query);
            try {
                if (resultSet.next()) {
                    String connectionMessage = resultSet.getString("connectionMessage");
                    if (connectionMessage != null && !connectionMessage.matches("^.*OK$")){
                        connectionMessage += "OK";
                        query = String.format("update temporary_users set connectionMessage = '%s' where userID = %d;",
                                connectionMessage, userID);
                        sqlConnection.updateQuery(query);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}