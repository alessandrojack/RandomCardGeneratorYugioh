package org.example.randomcardgenerator.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.randomcardgenerator.card.Card;
// Se Card è ancora nel package vecchio usa: import org.example.randomcardgenerator.Card;
import org.example.randomcardgenerator.service.ImageService;

import java.io.File;
import java.util.List;

public class RandomCardController {

    @FXML private Label titleLabel;
    @FXML private FlowPane cardsContainer; // Collega al FlowPane dell'FXML

    private Stage stage;
    private final ImageService imageService = new ImageService(); // O usa il tuo metodo file

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Riceve una LISTA di carte e le genera a schermo.
     */
    public void setCardList(List<Card> cards) {
        cardsContainer.getChildren().clear(); // Pulisce vecchi risultati
        titleLabel.setText("You pulled " + cards.size() + " card(s)!");

        for (Card card : cards) {
            // Creiamo un contenitore verticale per ogni carta
            VBox cardBox = new VBox(5);
            cardBox.setAlignment(Pos.CENTER);
            cardBox.setPrefWidth(160); // Larghezza fissa per ogni elemento

            // 1. Immagine
            ImageView iv = new ImageView();
            iv.setFitWidth(150);  // Larghezza immagine (più piccola per farne stare tante)
            iv.setPreserveRatio(true);

            // Caricamento Immagine (Simile a prima)
            File imgFile = new File("card_images" + File.separator + card.getImageId() + ".jpg");
            if (imgFile.exists()) {
                iv.setImage(new Image(imgFile.toURI().toString()));
            } else {
                // Placeholder o null
            }

            // 2. Testo (Nome carta)
            Label nameLbl = new Label(card.getName());
            nameLbl.setWrapText(true);
            nameLbl.setAlignment(Pos.CENTER);
            nameLbl.setStyle("-fx-font-weight: bold; -fx-text-alignment: center;");

            // 3. Aggiungiamo al box singolo
            cardBox.getChildren().addAll(iv, nameLbl);

            // 4. Aggiungiamo il box singolo alla griglia generale
            cardsContainer.getChildren().add(cardBox);
        }
    }

    @FXML
    private void handleClose() {
        if (stage != null) stage.close();
    }
}