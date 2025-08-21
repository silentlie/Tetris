module com.example.tetris {
  requires javafx.controls;
  requires javafx.fxml;

  opens com.example.tetris.controllers to javafx.fxml;
  exports com.example.tetris;
  opens com.example.tetris.helpers to javafx.fxml;
  opens com.example.tetris.models to javafx.fxml;
  exports com.example.tetris.controllers;
}