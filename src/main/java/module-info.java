module org.example.randomcardgenerator {
    requires javafx.controls;
    requires javafx.fxml;

    // Aggiunto per l'uso di Gson
    requires com.google.gson;
    requires java.desktop;
    requires java.sql;
    requires org.controlsfx.controls;

    // CORREZIONE CRITICA: Apri il package corretto per l'FXMLoader e il Controller
    opens org.example.randomcardgenerator to javafx.fxml;

    exports org.example.randomcardgenerator;
    exports org.example.randomcardgenerator.card;
    opens org.example.randomcardgenerator.card to javafx.fxml;
    exports org.example.randomcardgenerator.controller;
    opens org.example.randomcardgenerator.controller to javafx.fxml;
}