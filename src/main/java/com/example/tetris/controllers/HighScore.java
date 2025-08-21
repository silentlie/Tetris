package com.example.tetris.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class HighScore {

    @FXML
    private VBox root;   // matches fx:id="root" in FXML

    @FXML
    private void initialize() {
        // --- Demo data for Milestone 1
        List<Score> demo = List.of(
                new Score("Tom", 865_013),
                new Score("Anna", 754_859),
                new Score("Jerry", 642_371),
                new Score("Wia", 548_200),
                new Score("Tom", 537_780),
                new Score("Tom", 482_740),
                new Score("Larry", 460_120),
                new Score("Alex", 305_451),
                new Score("Anna", 301_548),
                new Score("Tom", 290_598)
        );

        // Insert rows ABOVE the Back button (which is currently the last child)
        int insertIndex = Math.max(root.getChildren().size() - 1, 0);
        for (int i = 0; i < demo.size(); i++) {
            Score s = demo.get(i);
            root.getChildren().add(insertIndex + i, makeRow(i + 1, s));
        }
    }

    /**
     * Builds a single row like: "1)  Name ....... Score"
     */
    private Node makeRow(int rank, Score s) {
        Label left = new Label(rank + ")  " + s.name());
        Label right = new Label(String.valueOf(s.score()));
        left.setStyle("-fx-font-size: 14px;");
        right.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");


        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(12, left, spacer, right);
        row.setMaxWidth(360);
        return row;
    }

    @FXML
    private void onBackButtonClick(ActionEvent e) throws IOException {
        Parent rootNode = FXMLLoader.load(
                Objects.requireNonNull(HighScore.class.getResource("/com/example/tetris/hello-view.fxml"))
        );
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        stage.setScene(new Scene(rootNode));
        stage.show();
    }


    private record Score(String name, int score) {
    }
}

