package org.example.randomcardgenerator.card;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class Card {

    private final SimpleStringProperty name;
    private final SimpleStringProperty type;
    private final SimpleStringProperty attribute;
    private final SimpleStringProperty race;
    private final SimpleIntegerProperty level;
    private final SimpleStringProperty desc;
    private final SimpleIntegerProperty atk;
    private final SimpleIntegerProperty def;
    private final SimpleStringProperty url;

    // NUOVO: ID dell'immagine (per il file locale)
    private final SimpleIntegerProperty imageId;

    public Card(String name, String type, String attribute, String race, int level, String desc, int atk, int def, String url, int imageId) {
        this.name = new SimpleStringProperty(name);
        this.type = new SimpleStringProperty(type);
        this.attribute = new SimpleStringProperty(attribute);
        this.race = new SimpleStringProperty(race);
        this.level = new SimpleIntegerProperty(level);
        this.desc = new SimpleStringProperty(desc);
        this.atk = new SimpleIntegerProperty(atk);
        this.def = new SimpleIntegerProperty(def);
        this.url = new SimpleStringProperty(url);
        this.imageId = new SimpleIntegerProperty(imageId);
    }

    // --- Getters e Setters esistenti ---

    public String getName() { return name.get(); }
    public SimpleStringProperty nameProperty() { return name; }
    public void setName(String name) { this.name.set(name); }

    public String getType() { return type.get(); }
    public SimpleStringProperty typeProperty() { return type; }
    public void setType(String type) { this.type.set(type); }

    public String getAttribute() { return attribute.get(); }
    public SimpleStringProperty attributeProperty() { return attribute; }
    public void setAttribute(String attribute) { this.attribute.set(attribute); }

    public String getRace() { return race.get(); }
    public SimpleStringProperty raceProperty() { return race; }
    public void setRace(String race) { this.race.set(race); }

    public int getLevel() { return level.get(); }
    public SimpleIntegerProperty levelProperty() { return level; }
    public void setLevel(int level) { this.level.set(level); }

    public String getDesc() { return desc.get(); }
    public SimpleStringProperty descProperty() { return desc; }
    public void setDesc(String desc) { this.desc.set(desc); }

    public int getAtk() { return atk.get(); }
    public SimpleIntegerProperty atkProperty() { return atk; }
    public void setAtk(int atk) { this.atk.set(atk); }

    public int getDef() { return def.get(); }
    public SimpleIntegerProperty defProperty() { return def; }
    public void setDef(int def) { this.def.set(def); }

    public String getUrl() { return url.get(); }
    public SimpleStringProperty urlProperty() { return url; }
    public void setUrl(String url) { this.url.set(url); }


    public int getImageId() { return imageId.get(); }
    public SimpleIntegerProperty imageIdProperty() { return imageId; }
    public void setImageId(int imageId) { this.imageId.set(imageId); }
}