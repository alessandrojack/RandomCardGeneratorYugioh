package org.example.randomcardgenerator.card;

import javafx.beans.property.SimpleStringProperty;

public class Expansion {
    private final SimpleStringProperty setName;
    private final SimpleStringProperty tcgDate;

    public Expansion(String setName, String tcgDate) {
        this.setName = new SimpleStringProperty(setName);
        this.tcgDate = new SimpleStringProperty(tcgDate);
    }

    public String getSetName() { return setName.get(); }
    public String getTcgDate() { return tcgDate.get(); }

    @Override
    public String toString() { return getSetName(); } // used by ComboBox
}