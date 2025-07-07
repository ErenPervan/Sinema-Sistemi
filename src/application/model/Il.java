// application/model/Il.java (model adında yeni bir paket oluşturabilirsiniz)
package application.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Il {
    private final IntegerProperty sehirID;
    private final StringProperty sehirAdi;


    public Il(int sehirID, String sehirAdi) {
        this.sehirID = new SimpleIntegerProperty(sehirID);
        this.sehirAdi = new SimpleStringProperty(sehirAdi);
    }

    // SehirID
    public int getSehirID() {
        return sehirID.get();
    }

    public IntegerProperty sehirIDProperty() {
        return sehirID;
    }

    public void setSehirID(int sehirID) {
        this.sehirID.set(sehirID);
    }

    // SehirAdi
    public String getSehirAdi() {
        return sehirAdi.get();
    }

    public StringProperty sehirAdiProperty() {
        return sehirAdi;
    }

    public void setSehirAdi(String sehirAdi) {
        this.sehirAdi.set(sehirAdi);
    }

    @Override
    public String toString() {
        return getSehirAdi();
    }
}