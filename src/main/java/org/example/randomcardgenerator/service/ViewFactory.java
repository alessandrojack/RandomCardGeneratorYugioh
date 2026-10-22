package org.example.randomcardgenerator.service;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.example.randomcardgenerator.card.Card;
import org.example.randomcardgenerator.controller.CardFilterController;
import org.example.randomcardgenerator.controller.CardOverviewController;
import org.example.randomcardgenerator.controller.RandomCardController;

import java.io.IOException;
import java.util.List;

public class ViewFactory {

    public void showFilterWindow(CardOverviewController mainController) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/randomcardgenerator/controller/card-filter-view.fxml"));
            Parent root = loader.load();

            CardFilterController controller = loader.getController();
            controller.setMainController(mainController); // Passaggio dipendenza

            Stage stage = new Stage();
            stage.setTitle("Set Card Filters");
            stage.setScene(new Scene(root));
            stage.setMinWidth(660);
            stage.setMinHeight(560);

            // Setup Stage nel controller figlio se necessario
            controller.setDialogStage(stage);

            stage.show();
        } catch (IOException e) {
            e.printStackTrace(); // O usa un logger
        }
    }

    public void showRandomResults(List<Card> cards, Window owner) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/randomcardgenerator/controller/random-card-view.fxml"));
            Parent root = loader.load();

            RandomCardController controller = loader.getController();
            Stage stage = new Stage();

            controller.setStage(stage);
            controller.setCardList(cards);

            stage.setTitle("Random Cards");
            stage.setScene(new Scene(root));
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            stage.initOwner(owner); // Fix Linux

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}