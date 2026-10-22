package org.example.randomcardgenerator.viewmodel;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.randomcardgenerator.card.Card;
import org.example.randomcardgenerator.service.*;

import java.util.List;
import java.util.function.Consumer;

public class CardOverviewViewModel {

    // Servizi di business
    private final DataService dataService = new DataService();
    private final RandomService randomService = new RandomService();
    private final FilterService filterService = new FilterService();

    // Stato dell'applicazione: Solo normali ObservableList!
    private final ObservableList<Card> cardData = FXCollections.observableArrayList();
    private final ObservableList<Card> currentRandomCards = FXCollections.observableArrayList();

    public CardOverviewViewModel() {
        // Inizializzazione base
    }

    // --- Getters per il Binding con la UI ---

    // Cambiato: ora restituisce direttamente l'ObservableList pulita
    public ObservableList<Card> getCardData() { return cardData; }
    public ObservableList<Card> getCurrentRandomCards() { return currentRandomCards; }

    // --- Logica Applicativa ---

    public void loadData(Runnable onSuccess, Consumer<String> onError) {
        new Thread(() -> {
            try {
                // Carica le carte (magari all'avvio ne carichi solo una parte o tutte,
                // dipende da come hai impostato loadAllCards)
                List<Card> cards = dataService.loadAllCards();

                javafx.application.Platform.runLater(() -> {
                    // Sostituito masterCardData con cardData
                    cardData.setAll(cards);
                    if (onSuccess != null) onSuccess.run();
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    if (onError != null) onError.accept(e.getMessage());
                });
            }
        }).start();
    }

    public void generateNewRandomCards(int count) {
        // Ora il random pick avviene sulle carte attualmente caricate/filtrate in cardData
        List<Card> selection = randomService.pickRandomCards(cardData, count);
        currentRandomCards.setAll(selection);
    }

    public void addRandomCards(int count) {
        // Stessa cosa qui: peschiamo da cardData
        List<Card> selection = randomService.pickRandomCards(cardData, count);
        currentRandomCards.addAll(selection);
    }

    public void updateData(Runnable onSuccess, Consumer<String> onError) {
        new Thread(() -> {
            try {
                dataService.updateCardDataFromApi();
                List<Card> updatedCards = dataService.loadAllCards();

                javafx.application.Platform.runLater(() -> {
                    // Sostituito masterCardData con cardData
                    cardData.setAll(updatedCards);
                    if (onSuccess != null) onSuccess.run();
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    if (onError != null) onError.accept(e.getMessage());
                });
            }
        }).start();
    }

    public void applyFilters(String name, String type, String attr, String level, String race, String desc, String dateFrom, String dateTo) {
        // Interroghiamo il database tramite il FilterService aggiungendo le date
        List<Card> risultati = filterService.getFilteredCards(name, type, attr, level, race, desc, dateFrom, dateTo);

        // Aggiorniamo direttamente la lista collegata alla UI
        cardData.setAll(risultati);
    }
}