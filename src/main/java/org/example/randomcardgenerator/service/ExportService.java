package org.example.randomcardgenerator.service;

import org.example.randomcardgenerator.card.Card;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ExportService {

    public void exportToYdk(List<Card> cards, File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            // Header (opzionale ma carino)
            writer.write("#created by RandomCardGenerator V5.0.0");
            writer.newLine();

            // === SEZIONE MAIN DECK ===
            writer.write("#main");
            writer.newLine();

            // Prima passata: solo le carte che vanno nel Main (tutto tranne Extra Deck)
            for (Card card : cards) {
                String cardType = (card.getType() != null) ? card.getType().toLowerCase() : "";

                // Se NON contiene Fusion/Synchro/Xyz/Link → Main Deck
                if (!cardType.contains("fusion") &&
                        !cardType.contains("synchro") &&
                        !cardType.contains("xyz") &&
                        !cardType.contains("link")) {

                    writer.write(String.valueOf(card.getImageId()));
                    writer.newLine();
                }
            }

            // === SEZIONE EXTRA DECK ===
            writer.write("#extra");
            writer.newLine();

            // Seconda passata: solo le carte Extra Deck
            for (Card card : cards) {
                String cardType = (card.getType() != null) ? card.getType().toLowerCase() : "";

                // Fusion, Synchro, Xyz, Link (anche le versioni Pendulum)
                if (cardType.contains("fusion") ||
                        cardType.contains("synchro") ||
                        cardType.contains("xyz") ||
                        cardType.contains("link")) {

                    writer.write(String.valueOf(card.getImageId()));
                    writer.newLine();
                }
            }

            // Sezioni vuote obbligatorie per compatibilità con simulatori vecchi
            writer.write("!side");
            writer.newLine();
        }
    }
}