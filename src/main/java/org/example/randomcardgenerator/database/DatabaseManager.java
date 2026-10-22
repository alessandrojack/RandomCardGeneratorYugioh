package org.example.randomcardgenerator.database;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.example.randomcardgenerator.utils.AppConstants;

import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DatabaseManager {

    /**
     * Stabilisce la connessione con SQLite.
     * Se il file .db non esiste, SQLite lo crea automaticamente.
     * Le foreign key in SQLite vanno abilitate per ogni connessione.
     */
    public static Connection getConnection() throws Exception {
        Connection conn = DriverManager.getConnection(AppConstants.JDBC_URL);

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }

        return conn;
    }

    /**
     * Crea le tabelle necessarie se non esistono già:
     * - cards: dati delle singole carte
     * - card_sets: set principali selezionabili nel filtro per data
     * - card_set_cards: relazione molti-a-molti tra carte e set
     */
    public static void initializeDatabase() {
        String createCardsTable = """
            CREATE TABLE IF NOT EXISTS cards (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                type TEXT,
                desc TEXT,
                atk INTEGER,
                def INTEGER,
                level INTEGER,
                race TEXT,
                attribute TEXT,
                tcg_date TEXT,
                card_sets TEXT
            );
            """;

        String createSetsTable = """
            CREATE TABLE IF NOT EXISTS card_sets (
                set_name TEXT PRIMARY KEY,
                tcg_date TEXT NOT NULL
            );
            """;

        /*
         * Non c'è una foreign key su set_name verso card_sets:
         * card_set_cards contiene TUTTE le appartenenze reali delle carte,
         * mentre card_sets contiene solo i set principali selezionabili.
         */
        String createCardSetCardsTable = """
            CREATE TABLE IF NOT EXISTS card_set_cards (
                card_id INTEGER NOT NULL,
                set_name TEXT NOT NULL,
                PRIMARY KEY (card_id, set_name),
                FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE
            );
            """;

        String createSetDateIndex = """
            CREATE INDEX IF NOT EXISTS idx_card_sets_tcg_date
            ON card_sets(tcg_date);
            """;

        String createBridgeSetNameIndex = """
            CREATE INDEX IF NOT EXISTS idx_card_set_cards_set_name
            ON card_set_cards(set_name);
            """;

        String createBridgeCardIdIndex = """
            CREATE INDEX IF NOT EXISTS idx_card_set_cards_card_id
            ON card_set_cards(card_id);
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createCardsTable);
            stmt.execute(createSetsTable);
            stmt.execute(createCardSetCardsTable);

            stmt.execute(createSetDateIndex);
            stmt.execute(createBridgeSetNameIndex);
            stmt.execute(createBridgeCardIdIndex);

            System.out.println("Database pronto (Tabelle: cards, card_sets, card_set_cards).");

        } catch (Exception e) {
            System.err.println("Errore durante l'inizializzazione del database SQLite.");
            e.printStackTrace();
        }
    }

    /**
     * Legge il JSON delle carte e aggiorna:
     * - la tabella cards;
     * - la tabella ponte card_set_cards con ogni appartenenza carta/set.
     *
     * La colonna cards.card_sets viene mantenuta per compatibilità o visualizzazione,
     * ma il filtering per set deve usare card_set_cards.
     */
    public static void updateDatabaseFromJson() {
        System.out.println("Inizio trasferimento dati da JSON a SQLite...");

        String insertCardSQL = """
            INSERT OR REPLACE INTO cards
            (id, name, type, desc, atk, def, level, race, attribute, tcg_date, card_sets)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        String insertCardSetLinkSQL = """
            INSERT OR IGNORE INTO card_set_cards (card_id, set_name)
            VALUES (?, ?)
            """;

        try (Connection conn = getConnection();
             FileReader reader = new FileReader(AppConstants.CARDS_JSON_FILE);
             PreparedStatement cardStmt = conn.prepareStatement(insertCardSQL);
             PreparedStatement linkStmt = conn.prepareStatement(insertCardSetLinkSQL);
             Statement clearStmt = conn.createStatement()) {

            conn.setAutoCommit(false);

            try {
                /*
                 * Ricostruiamo da zero le associazioni per non lasciare collegamenti
                 * non più presenti in caso di aggiornamento del JSON.
                 */
                clearStmt.executeUpdate("DELETE FROM card_set_cards");

                JsonObject rootObject = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray dataArray = rootObject.getAsJsonArray("data");

                int cardCount = 0;
                int linkCount = 0;

                for (JsonElement element : dataArray) {
                    JsonObject cardJson = element.getAsJsonObject();
                    int cardId = cardJson.get("id").getAsInt();

                    cardStmt.setInt(1, cardId);
                    cardStmt.setString(2, getStringOrNull(cardJson, "name"));
                    cardStmt.setString(3, getStringOrNull(cardJson, "type"));
                    cardStmt.setString(4, getStringOrNull(cardJson, "desc"));

                    setIntegerOrNull(cardStmt, 5, cardJson, "atk");
                    setIntegerOrNull(cardStmt, 6, cardJson, "def");
                    setIntegerOrNull(cardStmt, 7, cardJson, "level");

                    cardStmt.setString(8, getStringOrNull(cardJson, "race"));
                    cardStmt.setString(9, getStringOrNull(cardJson, "attribute"));

                    String tcgDate = null;
                    if (cardJson.has("misc_info") && !cardJson.get("misc_info").isJsonNull()) {
                        JsonArray miscInfoArray = cardJson.getAsJsonArray("misc_info");

                        if (!miscInfoArray.isEmpty()) {
                            JsonObject miscInfo = miscInfoArray.get(0).getAsJsonObject();

                            if (miscInfo.has("tcg_date") && !miscInfo.get("tcg_date").isJsonNull()) {
                                tcgDate = miscInfo.get("tcg_date").getAsString();
                            }
                        }
                    }
                    cardStmt.setString(10, tcgDate);

                    List<String> setNames = new ArrayList<>();

                    if (cardJson.has("card_sets") && !cardJson.get("card_sets").isJsonNull()) {
                        JsonArray setsArray = cardJson.getAsJsonArray("card_sets");

                        for (JsonElement setElement : setsArray) {
                            JsonObject setObj = setElement.getAsJsonObject();

                            if (setObj.has("set_name") && !setObj.get("set_name").isJsonNull()) {
                                String setName = setObj.get("set_name").getAsString();

                                setNames.add(setName);

                                linkStmt.setInt(1, cardId);
                                linkStmt.setString(2, setName);
                                linkStmt.addBatch();
                                linkCount++;
                            }
                        }
                    }

                    String cardSetsText = setNames.isEmpty()
                            ? null
                            : String.join(", ", setNames);

                    cardStmt.setString(11, cardSetsText);
                    cardStmt.addBatch();
                    cardCount++;
                }

                /*
                 * Le carte devono essere inserite prima delle righe ponte,
                 * perché card_set_cards.card_id fa riferimento a cards.id.
                 */
                cardStmt.executeBatch();
                linkStmt.executeBatch();

                conn.commit();

                System.out.println(
                        "Trasferimento completato con successo: "
                                + cardCount + " carte e "
                                + linkCount + " associazioni carta/set elaborate."
                );

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }

        } catch (Exception e) {
            System.err.println("Errore durante l'aggiornamento del database SQLite.");
            e.printStackTrace();
        }
    }

    /**
     * Aggiorna i set selezionabili nel filtro temporale.
     * In questa tabella rimangono soltanto le espansioni principali valide.
     */
    public static void updateSetsInDatabase(JsonArray setsArray) {
        String insertSetSQL = """
            INSERT OR REPLACE INTO card_sets (set_name, tcg_date)
            VALUES (?, ?)
            """;

        List<String> blacklist = Arrays.asList(
                "Structure Deck",
                "Starter Deck",
                "Promo",
                "Tournament",
                "Duel Terminal",
                "Sneak Peek",
                "Video Game",
                "Prize Card",
                "Official Tournament Store",
                "League",
                "Speed Duel",
                "Skill Card"
        );

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSetSQL);
             Statement clearStmt = conn.createStatement()) {

            conn.setAutoCommit(false);

            try {
                /*
                 * card_sets deve rappresentare solo l'elenco attuale dei set
                 * selezionabili, quindi viene ricostruita a ogni aggiornamento.
                 */
                clearStmt.executeUpdate("DELETE FROM card_sets");

                int count = 0;

                for (JsonElement element : setsArray) {
                    JsonObject setObj = element.getAsJsonObject();

                    if (!setObj.has("set_name") || setObj.get("set_name").isJsonNull()) {
                        continue;
                    }

                    String setName = setObj.get("set_name").getAsString();
                    int numCards = setObj.has("num_of_cards") && !setObj.get("num_of_cards").isJsonNull()
                            ? setObj.get("num_of_cards").getAsInt()
                            : 0;

                    boolean isExcluded = blacklist.stream().anyMatch(setName::contains);
                    boolean isCoreSize = numCards > 40;
                    boolean hasValidTcgDate = setObj.has("tcg_date") && !setObj.get("tcg_date").isJsonNull();

                    if (!isExcluded && isCoreSize && hasValidTcgDate) {
                        pstmt.setString(1, setName);
                        pstmt.setString(2, setObj.get("tcg_date").getAsString());
                        pstmt.addBatch();
                        count++;
                    }
                }

                pstmt.executeBatch();
                conn.commit();

                System.out.println(
                        ">>> DATABASE: Filtro applicato! Inserite "
                                + count
                                + " espansioni principali (esclusi mazzi e promo)."
                );

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }

        } catch (Exception e) {
            System.err.println("Errore durante l'aggiornamento dei set: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Metodi di supporto per gestire valori nulli ---

    private static String getStringOrNull(JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        return null;
    }

    private static void setIntegerOrNull(
            PreparedStatement pstmt,
            int index,
            JsonObject json,
            String key
    ) throws Exception {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            pstmt.setInt(index, json.get(key).getAsInt());
        } else {
            pstmt.setNull(index, java.sql.Types.INTEGER);
        }
    }
}