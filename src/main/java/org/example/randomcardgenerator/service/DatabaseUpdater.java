package org.example.randomcardgenerator.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.example.randomcardgenerator.database.DatabaseManager;
import org.example.randomcardgenerator.utils.AppConstants;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.stream.Collectors;

public class DatabaseUpdater {

    private static final String VERSION_API_URL = "https://db.ygoprodeck.com/api/v7/checkDBVer.php";
    private static final String SETS_API_URL = "https://db.ygoprodeck.com/api/v7/cardsets.php";
    private static final String VERSION_FILE = AppConstants.CARDS_JSON_FILE + ".version";

    /**
     * Metodo mantenuto per compatibilità con il codice che lo chiama all'avvio.
     *
     * Non controlla internet e non scarica il database:
     * esegue soltanto l'inizializzazione locale e, se necessaria,
     * la migrazione una tantum della tabella ponte.
     */
    public void checkForUpdates() {
        initializeAtStartup();
    }

    /**
     * Metodo da chiamare all'avvio dell'app.
     * Non effettua download e non interroga le API.
     */
    public void initializeAtStartup() {
        DatabaseManager.initializeDatabase();

        try {
            migrateBridgeTableLocallyIfNeeded();
        } catch (Exception e) {
            System.err.println("Errore durante la migrazione locale della tabella card_set_cards.");
            e.printStackTrace();
        }

        /*
         * La stampa è solo diagnostica: anche se fallisce,
         * non deve impedire l'avvio dell'app.
         */
        printDatabaseStatusSafely();
    }

    /**
     * Metodo da collegare al pulsante/comando "Aggiorna database".
     *
     * Solo su richiesta esplicita:
     * - controlla la versione online;
     * - scarica il JSON aggiornato delle carte;
     * - aggiorna cards e card_set_cards;
     * - scarica e aggiorna card_sets.
     */
    public void updateDatabaseOnRequest() throws IOException {
        System.out.println("Aggiornamento database richiesto dall'utente...");

        DatabaseManager.initializeDatabase();

        String onlineVersion = fetchOnlineVersion();

        downloadFullDatabase();
        DatabaseManager.updateDatabaseFromJson();
        downloadAndSyncSets();

        saveLocalVersion(onlineVersion);

        /*
         * Non propaga eccezioni: l'aggiornamento non deve risultare fallito
         * soltanto perché non è stato possibile stampare un conteggio.
         */
        printDatabaseStatusSafely();

        System.out.println("Aggiornamento database completato.");
    }

    /**
     * Migrazione locale una tantum:
     * se le carte esistono già, ma card_set_cards è vuota,
     * popola la tabella ponte usando il JSON locale già presente.
     *
     * Non scarica nulla da internet.
     */
    private void migrateBridgeTableLocallyIfNeeded() throws Exception {
        boolean cardsAlreadyPresent = tableHasRows("cards");
        boolean bridgeAlreadyPresent = tableHasRows("card_set_cards");

        if (!cardsAlreadyPresent || bridgeAlreadyPresent) {
            return;
        }

        File localCardsJson = new File(AppConstants.CARDS_JSON_FILE);

        if (!localCardsJson.exists()) {
            System.out.println(
                    ">>> DATABASE: card_set_cards è vuota e il JSON locale non esiste. "
                            + "Usa il comando 'Aggiorna database' per completare la migrazione."
            );
            return;
        }

        System.out.println(
                ">>> DATABASE: migrazione locale una tantum in corso; "
                        + "popolamento di card_set_cards dal JSON locale..."
        );

        DatabaseManager.updateDatabaseFromJson();
    }

    private String fetchOnlineVersion() throws IOException {
        HttpURLConnection conn = openGetConnection(VERSION_API_URL);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining());
        } finally {
            conn.disconnect();
        }
    }

    private void downloadFullDatabase() throws IOException {
        HttpURLConnection conn = openGetConnection(AppConstants.API_URL);
        File destFile = new File(AppConstants.CARDS_JSON_FILE);

        if (destFile.getParentFile() != null) {
            destFile.getParentFile().mkdirs();
        }

        try (InputStream is = conn.getInputStream();
             BufferedInputStream bis = new BufferedInputStream(is);
             FileOutputStream fos = new FileOutputStream(destFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = bis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        } finally {
            conn.disconnect();
        }
    }

    private void downloadAndSyncSets() throws IOException {
        System.out.println("Sincronizzazione espansioni in corso...");

        HttpURLConnection conn = openGetConnection(SETS_API_URL);

        try (InputStreamReader reader = new InputStreamReader(
                conn.getInputStream(), StandardCharsets.UTF_8)) {

            JsonArray setsArray = JsonParser.parseReader(reader).getAsJsonArray();
            DatabaseManager.updateSetsInDatabase(setsArray);

            System.out.println("Tabella 'card_sets' popolata con successo.");
        } finally {
            conn.disconnect();
        }
    }

    private HttpURLConnection openGetConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(15000);

        return conn;
    }

    private boolean tableHasRows(String tableName) throws Exception {
        validateTableName(tableName);

        String sql = "SELECT EXISTS (SELECT 1 FROM " + tableName + " LIMIT 1)";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            return rs.next() && rs.getInt(1) == 1;
        }
    }

    private int countRows(String tableName) throws Exception {
        validateTableName(tableName);

        String sql = "SELECT COUNT(*) FROM " + tableName;

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void validateTableName(String tableName) {
        if (!tableName.equals("cards")
                && !tableName.equals("card_sets")
                && !tableName.equals("card_set_cards")) {
            throw new IllegalArgumentException("Tabella non consentita: " + tableName);
        }
    }

    /**
     * Stampa di debug non bloccante.
     * A differenza della versione precedente, non dichiara throws Exception
     * e non può impedire la compilazione o interrompere un aggiornamento riuscito.
     */
    private void printDatabaseStatusSafely() {
        try {
            System.out.println(">>> DATABASE STATUS:");
            System.out.println("    cards: " + countRows("cards"));
            System.out.println("    card_sets: " + countRows("card_sets"));
            System.out.println("    card_set_cards: " + countRows("card_set_cards"));
        } catch (Exception e) {
            System.err.println("Impossibile stampare lo stato del database: " + e.getMessage());
        }
    }

    private String readLocalVersion() {
        File file = new File(VERSION_FILE);

        if (!file.exists()) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(
                new FileReader(file, StandardCharsets.UTF_8))) {
            String version = reader.readLine();
            return version != null ? version : "";
        } catch (IOException e) {
            return "";
        }
    }

    private void saveLocalVersion(String version) throws IOException {
        File file = new File(VERSION_FILE);

        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            writer.write(version);
        }
    }
}