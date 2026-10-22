package org.example.randomcardgenerator.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.controlsfx.control.SearchableComboBox;
import org.example.randomcardgenerator.card.Expansion;
import org.example.randomcardgenerator.service.DataService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CardFilterController {

    // --- Componenti UI ---
    @FXML private SearchableComboBox<Expansion> setFromField;
    @FXML private SearchableComboBox<Expansion> setToField;
    @FXML private TextField nameFilterField;
    @FXML private MenuButton typeFilterField;
    @FXML private MenuButton attributeFilterField;
    @FXML private MenuButton raceFilterField;
    @FXML private TextField levelFilterField;
    @FXML private TextField descFilterField;

    private CardOverviewController mainController;
    private Stage dialogStage;
    private final DataService dataService = new DataService();

    @FXML
    public void initialize() {
        // 1. Setup Menu a selezione multipla (Rimane invariato)
        setupMultiSelectMenu(typeFilterField, Arrays.asList(
                "Effect Monster", "Normal Monster", "Fusion Monster", "Synchro Monster",
                "XYZ Monster", "Link Monster", "Ritual Monster", "Spell Card", "Trap Card"
        ));

        setupMultiSelectMenu(attributeFilterField, Arrays.asList("DARK", "EARTH", "FIRE", "LIGHT", "WATER", "WIND", "DIVINE"));

        setupMultiSelectMenu(raceFilterField, Arrays.asList(
                "Aqua", "Beast", "Dragon", "Fairy", "Fiend", "Machine",
                "Plant", "Psychic", "Spellcaster", "Warrior", "Zombie",
                "Normal", "Continuous", "Field", "Equip", "Ritual", "Counter"
        ));

        // 2. Caricamento dati e attivazione ricerca dinamica (Molto più pulito!)
        try {
            List<Expansion> sets = dataService.loadAllExpansions();

            if (sets != null && !sets.isEmpty()) {
                // Aggiungiamo un'opzione nulla in cima per poter "pulire" il filtro
                List<Expansion> options = new ArrayList<>();
                options.add(null);
                options.addAll(sets);

                // Deleghiamo tutto il lavoro al nostro metodo di utilità
                setupSearchableComboBox(setFromField, options);
                setupSearchableComboBox(setToField, options);
            } else {
                System.out.println("--- DEBUG ---: Nessun set trovato nel DB.");
            }
        } catch (Exception e) {
            System.err.println("Errore nel caricamento espansioni: " + e.getMessage());
        }
    }

    /**
     * Trasforma un ComboBox standard in uno ricercabile dinamicamente.
     */
    private void setupSearchableComboBox(SearchableComboBox<Expansion> comboBox, List<Expansion> items) {
        // 1. Inseriamo la lista
        comboBox.setItems(FXCollections.observableArrayList(items));

        // 2. Diciamo a ControlsFX come estrarre il nome da mostrare a schermo
        comboBox.setConverter(new StringConverter<Expansion>() {
            @Override
            public String toString(Expansion expansion) {
                return (expansion == null || expansion.getSetName() == null) ? "" : expansion.getSetName();
            }

            @Override
            public Expansion fromString(String string) {
                // ControlsFX gestisce la selezione in automatico, questo serve solo
                // per evitare errori se l'utente cancella tutto il testo.
                return null;
            }
        });
    }

    @FXML
    private void handleApplyFilter() {
        if (mainController != null) {
            String name = nameFilterField.getText();
            String type = getSelectedValues(typeFilterField);
            String attribute = getSelectedValues(attributeFilterField);
            String race = getSelectedValues(raceFilterField);
            String level = levelFilterField.getText();
            String desc = descFilterField.getText();

            // Nota: Con ComboBox editabili, getValue() restituisce l'oggetto Expansion se selezionato correttamente
            String dateFrom = (setFromField.getValue() != null) ? setFromField.getValue().getTcgDate() : "";
            String dateTo = (setToField.getValue() != null) ? setToField.getValue().getTcgDate() : "";

            mainController.applyFilters(name, type, attribute, level, race, desc, dateFrom, dateTo);
        }

        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    // --- Metodi Helper Invariati ---

    private void setupMultiSelectMenu(MenuButton menuButton, List<String> items) {
        menuButton.getItems().clear();
        for (String item : items) {
            CheckBox checkBox = new CheckBox(item);
            CustomMenuItem customMenuItem = new CustomMenuItem(checkBox);
            customMenuItem.setHideOnClick(false);
            menuButton.getItems().add(customMenuItem);
        }
    }

    private String getSelectedValues(MenuButton menuButton) {
        List<String> selected = new ArrayList<>();
        for (var menuItem : menuButton.getItems()) {
            if (menuItem instanceof CustomMenuItem customItem) {
                if (customItem.getContent() instanceof CheckBox checkBox && checkBox.isSelected()) {
                    selected.add(checkBox.getText());
                }
            }
        }
        return String.join(",", selected);
    }

    public void setMainController(CardOverviewController mainController) {
        this.mainController = mainController;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
}