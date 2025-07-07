// application/model/KullaniciBileti.java
package application.model;

import javafx.beans.property.BooleanProperty; 
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty; 
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class KullaniciBileti {
    private final IntegerProperty rezervasyonID;
    private final StringProperty filmAdi;
    private final StringProperty salonAdi;
    private final StringProperty sehirAdi;
    private final StringProperty seansZamaniStr;
    private final StringProperty koltukNumarasi;
    private final StringProperty biletAlinmaTarihiStr;
    private final LocalDateTime seansZamaniGercek;
    private final StringProperty turAdi;
    private final StringProperty musteriAdi;
    private final StringProperty musteriSoyadi;
    private final BooleanProperty iptalEdildi; 

    public static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public KullaniciBileti(int rezervasyonID, String filmAdi, String salonAdi, String sehirAdi,
                           LocalDateTime seansZamani, String koltukNumarasi, LocalDateTime biletAlinmaTarihi, String turAdi,
                           String musteriAdi, String musteriSoyadi, boolean iptalEdildi) { // iptalEdildi eklendi
        this.rezervasyonID = new SimpleIntegerProperty(rezervasyonID);
        this.filmAdi = new SimpleStringProperty(filmAdi);
        this.salonAdi = new SimpleStringProperty(salonAdi);
        this.sehirAdi = new SimpleStringProperty(sehirAdi);
        this.seansZamaniGercek = seansZamani;
        this.seansZamaniStr = new SimpleStringProperty(seansZamani != null ? seansZamani.format(DT_FORMATTER) : "N/A");
        this.koltukNumarasi = new SimpleStringProperty(koltukNumarasi);
        this.biletAlinmaTarihiStr = new SimpleStringProperty(biletAlinmaTarihi != null ? biletAlinmaTarihi.format(DT_FORMATTER) : "N/A");
        this.turAdi = new SimpleStringProperty(turAdi);
        this.musteriAdi = new SimpleStringProperty(musteriAdi);
        this.musteriSoyadi = new SimpleStringProperty(musteriSoyadi);
        this.iptalEdildi = new SimpleBooleanProperty(iptalEdildi); 
    }

    // ... (Mevcut getter ve property metotları) ...

    public int getRezervasyonID() { return rezervasyonID.get(); }
    public IntegerProperty rezervasyonIDProperty() { return rezervasyonID; }

    public String getFilmAdi() { return filmAdi.get(); }
    public StringProperty filmAdiProperty() { return filmAdi; }

    public String getSalonAdi() { return salonAdi.get(); }
    public StringProperty salonAdiProperty() { return salonAdi; }

    public String getSehirAdi() { return sehirAdi.get(); }
    public StringProperty sehirAdiProperty() { return sehirAdi; }

    public String getSeansZamaniStr() { return seansZamaniStr.get(); }
    public StringProperty seansZamaniStrProperty() { return seansZamaniStr; }

    public String getKoltukNumarasi() { return koltukNumarasi.get(); }
    public StringProperty koltukNumarasiProperty() { return koltukNumarasi; }

    public String getBiletAlinmaTarihiStr() { return biletAlinmaTarihiStr.get(); }
    public StringProperty biletAlinmaTarihiStrProperty() { return biletAlinmaTarihiStr; }
    
    public LocalDateTime getSeansZamaniGercek() { return seansZamaniGercek; }

    public String getTurAdi() { return turAdi.get(); }
    public StringProperty turAdiProperty() { return turAdi; }

    public String getMusteriAdi() { return musteriAdi.get(); }
    public StringProperty musteriAdiProperty() { return musteriAdi; }

    public String getMusteriSoyadi() { return musteriSoyadi.get(); }
    public StringProperty musteriSoyadiProperty() { return musteriSoyadi; }

    // YENİ GETTER VE PROPERTY METOTLARI (Iptal Durumu için)
    public boolean isIptalEdildi() { return iptalEdildi.get(); }
    public BooleanProperty iptalEdildiProperty() { return iptalEdildi; }
    public void setIptalEdildi(boolean iptalEdildi) { this.iptalEdildi.set(iptalEdildi); }
}