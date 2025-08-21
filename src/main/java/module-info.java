module com.example.tetris {
    requires javafx.controls;
    requires javafx.fxml;
    // requires java.prefs; // only if you use Preferences elsewhere

    opens com.example.tetris to javafx.fxml;
    exports com.example.tetris;
}
