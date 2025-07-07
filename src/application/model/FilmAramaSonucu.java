// application/model/FilmAramaSonucu.java
package application.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class FilmAramaSonucu {
    private final StringProperty filmAdi;
    private final StringProperty salonAdi;
    private final StringProperty sehirAdi;
    private final StringProperty seansZamaniStr; 
    private final IntegerProperty bosKoltukSayisi;
    private final IntegerProperty seansID; 
    private final IntegerProperty filmID;
    private final IntegerProperty salonID;
    private final StringProperty turAdi;
    private final StringProperty afisURL;

    public FilmAramaSonucu(String filmAdi, String salonAdi, String sehirAdi, String seansZamaniStr,
    		 int bosKoltukSayisi, int seansID, int filmID, int salonID, String turAdi, String afisURL) { 
        this.filmAdi = new SimpleStringProperty(filmAdi);
        this.salonAdi = new SimpleStringProperty(salonAdi);
        this.sehirAdi = new SimpleStringProperty(sehirAdi);
        this.seansZamaniStr = new SimpleStringProperty(seansZamaniStr);
        this.bosKoltukSayisi = new SimpleIntegerProperty(bosKoltukSayisi);
        this.seansID = new SimpleIntegerProperty(seansID);
        this.filmID = new SimpleIntegerProperty(filmID);
        this.salonID = new SimpleIntegerProperty(salonID);
        this.turAdi = new SimpleStringProperty(turAdi);
        this.afisURL = new SimpleStringProperty(afisURL);
    }

    public String getFilmAdi() { return filmAdi.get(); }
    public StringProperty filmAdiProperty() { return filmAdi; }

    public String getSalonAdi() { return salonAdi.get(); }
    public StringProperty salonAdiProperty() { return salonAdi; }

    public String getSehirAdi() { return sehirAdi.get(); }
    public StringProperty sehirAdiProperty() { return sehirAdi; }

    public String getSeansZamaniStr() { return seansZamaniStr.get(); }
    public StringProperty seansZamaniStrProperty() { return seansZamaniStr; }

    public int getBosKoltukSayisi() { return bosKoltukSayisi.get(); }
    public IntegerProperty bosKoltukSayisiProperty() { return bosKoltukSayisi; }

    public int getSeansID() { return seansID.get(); }
    public IntegerProperty seansIDProperty() { return seansID; }

    public int getFilmID() { return filmID.get(); }
    public IntegerProperty filmIDProperty() { return filmID; }

    public int getSalonID() { return salonID.get(); }
    public IntegerProperty salonIDProperty() { return salonID; }

    // YENİ GETTER VE PROPERTY METOTLARI
    public String getTurAdi() { return turAdi.get(); }
    public StringProperty turAdiProperty() { return turAdi; }
 // YENİ AfisURL GETTER VE PROPERTY METOTLARI
    public String getAfisURL() { return afisURL.get(); }
    public StringProperty afisURLProperty() { return afisURL; }
    
}