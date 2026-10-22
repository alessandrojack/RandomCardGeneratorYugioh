package org.example.randomcardgenerator.service;

import org.example.randomcardgenerator.card.Card;
import org.example.randomcardgenerator.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterService {

    /**
     * Interroga il database SQLite e restituisce una lista di carte in base ai filtri.
     *
     * IMPORTANTE:
     * dateFrom e dateTo devono continuare ad arrivare nel formato yyyy-MM-dd.
     * Il filtro temporale non usa più cards.tcg_date, perché quella data non
     * coincide sempre con la data del set in cui la carta è contenuta.
     * Ora recupera le carte appartenenti ai set presenti nell'intervallo scelto.
     */
    public List<Card> getFilteredCards(String name, String type, String attr,
                                       String levelStr, String race, String desc,
                                       String dateFrom, String dateTo) {

        List<Card> results = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT c.*
                FROM cards c
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        // --- FILTRO NOME ---
        if (isNotEmpty(name)) {
            sql.append(" AND LOWER(c.name) LIKE ?");
            params.add("%" + name.trim().toLowerCase() + "%");
        }

        // --- FILTRO DESCRIZIONE ---
        if (isNotEmpty(desc)) {
            sql.append(" AND LOWER(c.desc) LIKE ?");
            params.add("%" + desc.trim().toLowerCase() + "%");
        }

        // --- FILTRO ATTRIBUTO (Gestione Selezione Multipla) ---
        if (isNotEmpty(attr)) {
            applyExactMultiFilter("c.attribute", attr, sql, params);
        }

        // --- FILTRO RAZZA (Gestione Selezione Multipla) ---
        if (isNotEmpty(race)) {
            applyExactMultiFilter("c.race", race, sql, params);
        }

        // --- FILTRO TIPO (Gestione Selezione Multipla + Logica Effect Monster) ---
        if (isNotEmpty(type)) {
            applyTypeFilter(type, sql, params);
        }

        // --- FILTRO LIVELLO ---
        if (isNotEmpty(levelStr)) {
            applyLevelFilter(levelStr, sql, params);
        }

        /*
         * FILTRO SET / DATA:
         * dateFrom e dateTo rappresentano le date dei set selezionati nella UI.
         * Cerchiamo almeno un set selezionabile, compreso nel range,
         * a cui la carta appartiene realmente tramite la tabella ponte.
         */
        if (isNotEmpty(dateFrom) || isNotEmpty(dateTo)) {
            sql.append("""
                     AND EXISTS (
                         SELECT 1
                         FROM card_set_cards csc
                         JOIN card_sets cs ON cs.set_name = csc.set_name
                         WHERE csc.card_id = c.id
                    """);

            if (isNotEmpty(dateFrom)) {
                sql.append(" AND cs.tcg_date >= ?");
                params.add(dateFrom.trim());
            }

            if (isNotEmpty(dateTo)) {
                sql.append(" AND cs.tcg_date <= ?");
                params.add(dateTo.trim());
            }

            sql.append(")");
        }

        sql.append(" ORDER BY c.name");

        // --- ESECUZIONE DELLA QUERY ---
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapResultSetToCard(rs));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Errore durante l'esecuzione della query di filtro.");
        }

        return results;
    }

    // =========================================================================
    // METODI DI SUPPORTO PER COSTRUIRE LA QUERY
    // =========================================================================

    private void applyExactMultiFilter(String columnName, String filterValues,
                                       StringBuilder sql, List<Object> params) {
        String[] values = filterValues.split(",");

        sql.append(" AND (");

        for (int i = 0; i < values.length; i++) {
            sql.append("LOWER(").append(columnName).append(") = ?");
            params.add(values[i].trim().toLowerCase());

            if (i < values.length - 1) {
                sql.append(" OR ");
            }
        }

        sql.append(")");
    }

    private void applyTypeFilter(String filterTypes, StringBuilder sql, List<Object> params) {
        String[] types = filterTypes.split(",");

        sql.append(" AND (");

        for (int i = 0; i < types.length; i++) {
            String t = types[i].trim().toLowerCase();

            if (t.equals("effect monster") || t.equals("effect")) {
                sql.append("""
                        (LOWER(c.type) LIKE '%monster%'
                         AND LOWER(c.type) NOT LIKE '%normal%'
                         AND LOWER(c.type) NOT LIKE '%fusion%'
                         AND LOWER(c.type) NOT LIKE '%synchro%'
                         AND LOWER(c.type) NOT LIKE '%xyz%'
                         AND LOWER(c.type) NOT LIKE '%link%')
                        """);
            } else {
                String keyword = t.replace(" monster", "")
                        .replace(" card", "")
                        .replace(" effect", "")
                        .trim();

                sql.append("LOWER(c.type) LIKE ?");
                params.add("%" + keyword + "%");
            }

            if (i < types.length - 1) {
                sql.append(" OR ");
            }
        }

        sql.append(")");
    }

    private void applyLevelFilter(String filterInput, StringBuilder sql, List<Object> params) {
        String normalizedInput = filterInput.replaceAll("\\s+", "");

        Pattern pattern = Pattern.compile(
                "^(?:(lt|gt|lte|gte)|([<>=!]+))(\\d+)$",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(normalizedInput);

        if (matcher.find()) {
            String op = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            int target = Integer.parseInt(matcher.group(3));

            if (op.equals("<")) op = "lt";
            if (op.equals(">")) op = "gt";
            if (op.equals("<=")) op = "lte";
            if (op.equals(">=")) op = "gte";

            switch (op.toLowerCase()) {
                case "gt"  -> sql.append(" AND c.level > ?");
                case "lt"  -> sql.append(" AND c.level < ?");
                case "gte" -> sql.append(" AND c.level >= ?");
                case "lte" -> sql.append(" AND c.level <= ?");
                default -> {
                    return;
                }
            }

            params.add(target);

        } else if (normalizedInput.matches("^\\d+$")) {
            sql.append(" AND c.level = ?");
            params.add(Integer.parseInt(normalizedInput));
        }
    }

    private boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * Mappa una riga del database in un oggetto Card.
     */
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
}