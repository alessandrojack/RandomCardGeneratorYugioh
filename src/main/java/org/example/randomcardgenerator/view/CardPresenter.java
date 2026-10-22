package org.example.randomcardgenerator.view;

import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import org.example.randomcardgenerator.card.Card;
import org.example.randomcardgenerator.service.ImageService;

import javafx.scene.control.Tooltip;

public class CardPresenter {
    private final Label nameLabel;
    private final Label typeLabel;
    private final Label attributeLabel;
    private final Label raceLabel;
    private final Label levelLabel;
    private final Label atkDefLabel;
    private final TextArea descLabel;
    private final ImageView cardImageView;
    private final ImageService imageService;
    private final Tooltip imageStatus = new Tooltip();

    // Dependency Injection dei componenti UI
    public CardPresenter(Label nameLabel, Label typeLabel, Label attributeLabel,
                         Label raceLabel, Label levelLabel, Label atkDefLabel,
                         TextArea descLabel, ImageView cardImageView, ImageService imageService) {
        this.nameLabel = nameLabel;
        this.typeLabel = typeLabel;
        this.attributeLabel = attributeLabel;
        this.raceLabel = raceLabel;
        this.levelLabel = levelLabel;
        this.atkDefLabel = atkDefLabel;
        this.descLabel = descLabel;
        this.cardImageView = cardImageView;
        this.imageService = imageService;
        Tooltip.install(cardImageView, imageStatus);
    }

    public void clear() {
        nameLabel.setText("");
        typeLabel.setText("");
        attributeLabel.setText("");
        raceLabel.setText("");
        levelLabel.setText("");
        atkDefLabel.setText("");
        descLabel.setText("");
        imageService.clearImage(cardImageView);
        imageStatus.setText("");
    }

    public void display(Card card) {
        if (card == null) {
            clear();
            return;
        }

        // Logica di formattazione SPOSTATA QUI (SRP rispettato)
        nameLabel.setText(card.getName());
        typeLabel.setText(card.getType());
        attributeLabel.setText(card.getAttribute());
        raceLabel.setText(card.getRace());

        // Logica condizionale di presentazione
        levelLabel.setText(card.getLevel() > 0 ? String.valueOf(card.getLevel()) : "N/A");
        atkDefLabel.setText((card.getAtk() >= 0) ? "ATK: " + card.getAtk() + " / DEF: " + card.getDef() : "N/A");
        descLabel.setText(card.getDesc());

        imageService.loadInto(cardImageView, card.getImageId(), message -> {
            imageStatus.setText(message.isEmpty() ? card.getName() : message);
        });
    }
}
