// application/model/Tur.java
package application.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Tur {
    private final IntegerProperty turID;
    private final StringProperty turAdi;

    public Tur(int turID, String turAdi) {
        this.turID = new SimpleIntegerProperty(turID);
        this.turAdi = new SimpleStringProperty(turAdi);
    }

    // TurID
    public int getTurID() { return turID.get(); }
    public IntegerProperty turIDProperty() { return turID; }
    public void setTurID(int turID) { this.turID.set(turID); }

    // TurAdi
    public String getTurAdi() { return turAdi.get(); }
    public StringProperty turAdiProperty() { return turAdi; }
    public void setTurAdi(String turAdi) { this.turAdi.set(turAdi); }

    @Override
    public String toString() {
        return getTurAdi(); 
    }
}