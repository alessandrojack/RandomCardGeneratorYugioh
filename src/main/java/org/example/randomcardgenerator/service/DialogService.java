package org.example.randomcardgenerator.service;

import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import java.util.Optional;

public class DialogService {

    public void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void showInfo(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public Optional<Integer> promptForInteger(String title, String header, String content, int min, int max) {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(min));
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(content);

        return dialog.showAndWait().flatMap(result -> {
            try {
                int val = Integer.parseInt(result);
                // Clamp value between min and max
                return Optional.of(Math.max(min, Math.min(val, max)));
            } catch (NumberFormatException e) {
                showError("Invalid Input", "Please enter a valid number.");
                return Optional.empty();
            }
        });
    }
}