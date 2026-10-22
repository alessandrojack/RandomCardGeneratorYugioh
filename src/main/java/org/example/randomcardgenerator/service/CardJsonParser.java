package org.example.randomcardgenerator.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.example.randomcardgenerator.card.Card;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CardJsonParser {

    private final ImageService imageService;

    public CardJsonParser(ImageService imageService) {
        this.imageService = imageService;
    }

    public List<Card> parseFile(File file) throws IOException {
        List<Card> cards = new ArrayList<>(); // Lista finale diretta!
        Gson gson = new Gson();

        try (JsonReader reader = new JsonReader(new FileReader(file, StandardCharsets.UTF_8))) {
            JsonToken token = reader.peek();

            if (token == JsonToken.BEGIN_OBJECT) {
                reader.beginObject();
                while (reader.hasNext()) {
                    if (reader.nextName().equals("data")) {
                        reader.beginArray();
                        while (reader.hasNext()) {
                            // Leggi l'oggetto e convertilo sùbito in Carta!
                            JsonObject json = gson.fromJson(reader, JsonObject.class);
                            cards.add(convertJsonToCard(json));
                        }
                        reader.endArray();
                        break;
                    } else {
                        reader.skipValue();
                    }
                }
            } else if (token == JsonToken.BEGIN_ARRAY) {
                reader.beginArray();
                while (reader.hasNext()) {
                    JsonObject json = gson.fromJson(reader, JsonObject.class);
                    cards.add(convertJsonToCard(json));
                }
                reader.endArray();
            }
        }

        // Addio rawList e addio secondo ciclo for!
        return cards;
    }

    private Card convertJsonToCard(JsonObject json) {
        if (json == null) return null;

        String name = getSafeString(json, "name", "Unknown");
        String type = getSafeString(json, "type", "Unknown");
        String desc = getSafeString(json, "desc", "");
        String url  = getSafeString(json, "ygoprodeck_url", "");
        String attribute = getSafeString(json, "attribute", "N/A");
        String race = getSafeString(json, "race", "N/A");
        int level = getSafeInt(json, "level", 0);
        int atk = getSafeInt(json, "atk", -1);
        int def = getSafeInt(json, "def", -1);

        int imageId = 0;
        if (json.has("card_images") && !json.get("card_images").isJsonNull()) {
            var arr = json.getAsJsonArray("card_images");
            if (!arr.isEmpty()) {
                JsonObject imgObj = arr.get(0).getAsJsonObject();
                imageId = getSafeInt(imgObj, "id", 0);
                String imgUrl = getSafeString(imgObj, "image_url_small", "");
                if (!imgUrl.isEmpty()) imageService.downloadImageAsync(imgUrl, imageId);
            }
        }
        return new Card(name, type, attribute, race, level, desc, atk, def, url, imageId);
    }

    private String getSafeString(JsonObject json, String key, String def) {
        return (json.has(key) && !json.get(key).isJsonNull()) ? json.get(key).getAsString() : def;
    }

    private int getSafeInt(JsonObject json, String key, int def) {
        return (json.has(key) && !json.get(key).isJsonNull()) ? json.get(key).getAsInt() : def;
    }
}