package org.example.randomcardgenerator.service;

import org.example.randomcardgenerator.card.Card;
import org.example.randomcardgenerator.card.Expansion;
import org.example.randomcardgenerator.database.DatabaseManager;
import org.example.randomcardgenerator.utils.AppConstants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DataService {

    private final DatabaseUpdater updater;

    public DataService() {
        // ImageService e CardJsonParser non servono più qui in fase di caricamento dati base
        this.updater = new DatabaseUpdater();
    }

    // --- API Pubblica per il Controller (Da usare all'avvio) ---
    public List<Card> loadAllCards() throws Exception {
        File jsonFile = new File(AppConstants.CARDS_JSON_FILE);

        // 1. Controlliamo se è il primo avvio in assoluto (file JSON mancante).
        // Solo in questo caso forziamo il download automatico.
        if (!jsonFile.exists() || jsonFile.length() == 0) {
            System.out.println("Primo avvio: Database JSON non trovato. Scaricamento in corso...");
            try {
                updateCardDataFromApi();
            } catch (IOException e) {
                System.err.println("Errore durante il download iniziale: " + e.getMessage());
            }
        }

        // 2. Controllo di sicurezza finale: se manca ancora, siamo offline o c'è un errore grave
        if (!jsonFile.exists() || jsonFile.length() == 0) {
            throw new FileNotFoundException("Errore critico: Nessun database carte locale e connessione assente.");
        }

        // === NUOVA LOGICA SQLITE ===

        // 3. Ci assicuriamo che la tabella 'cards' esista nel database SQLite
        DatabaseManager.initializeDatabase();

        // 4. Controlliamo se la tabella SQLite è vuota
        boolean isDbEmpty = true;
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM cards")) {

            if (rs.next() && rs.getInt(1) > 0) {
                isDbEmpty = false;
            }
        }

        // 5. Se la tabella è vuota, forziamo il travaso dal JSON al DB SQLite
        if (isDbEmpty) {
            System.out.println("Database SQLite vuoto o appena creato. Popolamento in corso...");
            DatabaseManager.updateDatabaseFromJson();
        }

        // 6. Ora che il database SQL è sicuramente pronto, peschiamo le carte da lì!
        List<Card> cards = new ArrayList<>();
        String query = "SELECT * FROM cards";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                cards.add(mapResultSetToCard(rs));
            }
        }

        return cards;
    }

    // --- API Pubblica per il Controller (Da collegare al tuo TASTO AGGIORNA) ---
    public void updateCardDataFromApi() throws IOException {
        updater.checkForUpdates();
    }

    // --- Metodo Helper per convertire la riga SQL in un oggetto Card ---
    private Card mapResultSetToCard(ResultSet rs) throws Exception {
        String name = rs.getString("name");
        String type = rs.getString("type");
        String attribute = rs.getString("attribute");
        String race = rs.getString("race");
        int level = rs.getInt("level");
        String desc = rs.getString("desc");
        int atk = rs.getInt("atk");
        int def = rs.getInt("def");
        int imageId = rs.getInt("id");

        String url = "";

        return new Card(name, type, attribute, race, level, desc, atk, def, url, imageId);
    }

    public List<Expansion> loadAllExpansions() {
        List<Expansion> expansions = new ArrayList<>();
        String sql = "SELECT * FROM card_sets ORDER BY tcg_date ASC"; // Ordine cronologico!

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                expansions.add(new Expansion(
                        rs.getString("set_name"),
                        rs.getString("tcg_date")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return expansions;
    }
}