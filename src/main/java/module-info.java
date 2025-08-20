module com.example.tetris {
  requires javafx.controls;
  requires javafx.fxml;

  opens com.example.tetris.controllers to javafx.fxml;
  exports com.example.tetris;
}