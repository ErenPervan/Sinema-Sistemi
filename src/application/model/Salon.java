// application/model/Salon.java
package application.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Salon {
    private final IntegerProperty salonID;
    private final StringProperty salonAdi;
    private final IntegerProperty sehirID; // Hangi şehre ait olduğu
    private final StringProperty sehirAdi; 
    private final IntegerProperty kapasite; // Sabit 15 olacak

    public Salon(int salonID, String salonAdi, int sehirID, String sehirAdi, int kapasite) {
        this.salonID = new SimpleIntegerProperty(salonID);
        this.salonAdi = new SimpleStringProperty(salonAdi);
        this.sehirID = new SimpleIntegerProperty(sehirID);
        this.sehirAdi = new SimpleStringProperty(sehirAdi); 
        this.kapasite = new SimpleIntegerProperty(kapasite);
    }

    // SalonID
    public int getSalonID() { return salonID.get(); }
    public IntegerProperty salonIDProperty() { return salonID; }
    public void setSalonID(int salonID) { this.salonID.set(salonID); }

    // SalonAdi
    public String getSalonAdi() { return salonAdi.get(); }
    public StringProperty salonAdiProperty() { return salonAdi; }
    public void setSalonAdi(String salonAdi) { this.salonAdi.set(salonAdi); }

    // SehirID
    public int getSehirID() { return sehirID.get(); }
    public IntegerProperty sehirIDProperty() { return sehirID; }
    public void setSehirID(int sehirID) { this.sehirID.set(sehirID); }

    // SehirAdi (TableView için)
    public String getSehirAdi() { return sehirAdi.get(); }
    public StringProperty sehirAdiProperty() { return sehirAdi; }
    public void setSehirAdi(String sehirAdi) { this.sehirAdi.set(sehirAdi); }


    // Kapasite
    public int getKapasite() { return kapasite.get(); }
    public IntegerProperty kapasiteProperty() { return kapasite; }
    public void setKapasite(int kapasite) { this.kapasite.set(kapasite); }

    @Override
    public String toString() {
        return getSalonAdi(); // ComboBox'ta veya listelerde görünecek değer
    }
}