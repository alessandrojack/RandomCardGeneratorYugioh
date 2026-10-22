package org.example.randomcardgenerator.controller;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.example.randomcardgenerator.card.Card;
import org.example.randomcardgenerator.service.DialogService;
import org.example.randomcardgenerator.service.ExportService;
import org.example.randomcardgenerator.service.ImageService;
import org.example.randomcardgenerator.service.ViewFactory;
import org.example.randomcardgenerator.view.CardPresenter;
import org.example.randomcardgenerator.viewmodel.CardOverviewViewModel;

import java.io.File;
import java.util.List;

public class CardOverviewController {

    // --- UI Components ---
    @FXML private TableView<Card> cardTable;
    @FXML private VBox randomListPane;
    @FXML private FlowPane randomImagesContainer;
    @FXML private TableColumn<Card, String> nameColumn;
    @FXML private TableColumn<Card, String> typeColumn;

    // --- UI Components (Pannello di Destra) ---
    @FXML private Label nameLabel, typeLabel, attributeLabel, raceLabel, levelLabel, atkDefLabel;
    @FXML private TextArea descLabel;
    @FXML private ImageView cardImageView;


    private final DialogService dialogService = new DialogService();
    private final ViewFactory viewFactory = new ViewFactory();
    private final ImageService imageService = new ImageService();
    private final ExportService exportService = new ExportService();
    private CardPresenter cardPresenter;

    // --- Il nostro ViewModel (La Mente) ---
    private final CardOverviewViewModel viewModel = new CardOverviewViewModel();

    @FXML
    public void initialize() {
        // 1. Inizializza il Presenter per il pannello di destra
        this.cardPresenter = new CardPresenter(
                nameLabel, typeLabel, attributeLabel, raceLabel,
                levelLabel, atkDefLabel, descLabel, cardImageView, imageService
        );

        // 2. Configura le colonne della tabella
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));

        // 3. Collega il click sulla tabella al Presenter
        cardTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> cardPresenter.display(newVal));

        // 4. Configura i collegamenti (Binding) con il ViewModel
        setupBindings();

        // 5. Avvia il caricamento dati
        viewModel.loadData(
                () -> { if (cardPresenter != null) cardPresenter.display(null); },
                errorMessage -> dialogService.showError("Errore Caricamento", errorMessage)
        );
    }

    private void setupBindings() {
        // CORREZIONE: Usa getCardData() invece di getFilteredCardData()
        cardTable.setItems(viewModel.getCardData());

        // Ascolta i cambiamenti nella lista random: se arrivano nuove carte, aggiorna le immagini
        viewModel.getCurrentRandomCards().addListener((ListChangeListener<Card>) change -> {
            populateRandomImages(viewModel.getCurrentRandomCards());
        });
    }

    // --- Metodi UI (Reazioni ai click) ---

    @FXML
    private void handleRandomCard() {
        // CORREZIONE: Usa getCardData()
        if (viewModel.getCardData().isEmpty()) {
            dialogService.showError("Errore", "Nessuna carta disponibile con i filtri attuali.");
            return;
        }

        dialogService.promptForInteger("Nuovo Random", "Quante carte?", "Numero (1-100):", 1, 100)
                .ifPresent(count -> {
                    viewModel.generateNewRandomCards(count);
                    cardTable.setVisible(false);
                    randomListPane.setVisible(true);
                });
    }

    @FXML
    private void handleAddRandomCards() {
        // CORREZIONE: Usa getCardData()
        if (viewModel.getCardData().isEmpty()) {
            dialogService.showError("Errore", "Nessuna carta disponibile con i filtri attuali.");
            return;
        }

        dialogService.promptForInteger("Aggiungi Carte", "Quante carte aggiungere?", "Numero (1-100):", 1, 100)
                .ifPresent(count -> {
                    viewModel.addRandomCards(count);
                    cardTable.setVisible(false);
                    randomListPane.setVisible(true);
                });
    }

    @FXML
    private void handleResetList() {
        randomListPane.setVisible(false);
        cardTable.setVisible(true);
    }

    @FXML
    private void handleExportYdk() {
        if (viewModel.getCurrentRandomCards().isEmpty()) {
            dialogService.showError("Errore", "Nessuna carta da esportare.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Esporta Mazzo (.ydk)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("YGOPro Deck (*.ydk)", "*.ydk"));
        fileChooser.setInitialFileName("RandomDeck.ydk");

        File file = fileChooser.showSaveDialog(cardTable.getScene().getWindow());

        if (file != null) {
            try {
                // Passiamo la lista dal ViewModel al servizio di export
                exportService.exportToYdk(viewModel.getCurrentRandomCards(), file);
                System.out.println("Export riuscito: " + file.getAbsolutePath());
            } catch (Exception e) {
                dialogService.showError("Errore Export", e.getMessage());
            }
        }
    }

    @FXML
    private void handleUpdateCardData() {
        viewModel.updateData(
                () -> dialogService.showInfo("Successo", "Database aggiornato!"),
                errorMessage -> dialogService.showError("Errore Aggiornamento", errorMessage)
        );
    }

    @FXML
    private void handleOpenFilterWindow() {
        viewFactory.showFilterWindow(this);
    }

    public void applyFilters(String name, String type, String attr, String level, String race, String desc, String dateFrom, String dateTo) {
        viewModel.applyFilters(name, type, attr, level, race, desc, dateFrom, dateTo);
    }

    // --- Metodo per la generazione puramente grafica ---

    private void populateRandomImages(List<Card> cards) {
        randomImagesContainer.getChildren().clear();

        for (Card card : cards) {
            ImageView iv = new ImageView();
            iv.setFitWidth(80);
            iv.setPreserveRatio(true);
            iv.setStyle("-fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 0, 0);");

            File imgFile = imageService.getLocalImageFile(card.getImageId());
            if (imgFile.exists()) {
                iv.setImage(new Image(imgFile.toURI().toString()));
            }

            Tooltip.install(iv, new Tooltip(card.getName()));

            // Al click sull'immagine, mostra la carta nel pannello di destra
            iv.setOnMouseClicked(e -> cardPresenter.display(card));

            randomImagesContainer.getChildren().add(iv);
        }
    }
}