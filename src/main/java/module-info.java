module com.example.tetris {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson; // ✅ allows using Gson library

    opens com.example.tetris to javafx.fxml;
    opens com.example.tetris.controllers to javafx.fxml;
    opens com.example.tetris.models to com.google.gson; // ✅ allows Gson to serialize/deserialize
    exports com.example.tetris;
    exports com.example.tetris.controllers;
    exports com.example.tetris.models;
}
