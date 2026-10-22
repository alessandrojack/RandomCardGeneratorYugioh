package org.example.randomcardgenerator.utils;

public class AppConstants {
    // ABBIAMO AGGIUNTO ?misc=yes ALLA FINE!
    public static final String API_URL = "https://db.ygoprodeck.com/api/v7/cardinfo.php?misc=yes";

    public static final String DATA_DIR = "cards_data";
    public static final String IMAGE_DIR = "card_images";

    public static final String CARDS_JSON_FILE = "cards_data/cards.json";
    public static final String DATABASE_FILE = "cards_data/yugioh_cards.db";
    public static final String JDBC_URL = "jdbc:sqlite:" + DATABASE_FILE;
}