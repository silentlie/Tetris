module com.example.tetris {
  requires javafx.controls;
  requires javafx.fxml;
  requires com.fasterxml.jackson.databind;
  requires java.logging;
  requires javafx.media;

  opens com.example.tetris.ui.screen to javafx.fxml;
  opens com.example.tetris.ui.view to javafx.fxml;
  opens com.example.tetris.ui.canvas to javafx.fxml;
  opens com.example.tetris.core.config to com.fasterxml.jackson.databind;
  opens com.example.tetris.core.state to com.fasterxml.jackson.databind;

  exports com.example.tetris;
  exports com.example.tetris.app.context;
  exports com.example.tetris.app.navigation;
  exports com.example.tetris.app.runtime;

  exports com.example.tetris.core.board;
  exports com.example.tetris.core.config;
  exports com.example.tetris.core.piece;
  exports com.example.tetris.core.random;
  exports com.example.tetris.core.rule;
  exports com.example.tetris.core.state;

  exports com.example.tetris.ui.screen;
  exports com.example.tetris.ui.view;
  exports com.example.tetris.service.audio;
  exports com.example.tetris.service.config;
}

