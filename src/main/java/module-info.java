module com.example.tetris {
  requires javafx.controls;
  requires javafx.fxml;
  requires com.fasterxml.jackson.databind;
  requires java.logging;

  opens com.example.tetris.controllers to javafx.fxml;
  exports com.example.tetris;
  exports com.example.tetris.helpers;
  opens com.example.tetris.helpers to javafx.fxml;
  opens com.example.tetris.models to com.fasterxml.jackson.databind, javafx.fxml;
  exports com.example.tetris.controllers;

  exports com.example.tetris.models;
  exports com.example.tetris.services;
}