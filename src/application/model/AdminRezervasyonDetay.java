// application/model/AdminRezervasyonDetay.java
package application.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AdminRezervasyonDetay {
    private final IntegerProperty rezervasyonID;
    private final StringProperty kullaniciAdi; 
    private final StringProperty filmAdi;     
    private final StringProperty salonAdi;     
    private final StringProperty sehirAdi;     
    private final ObjectProperty<LocalDateTime> seansZamani;
    private final StringProperty formatliSeansZamani;
    private final StringProperty koltukNumarasi;
    private final StringProperty biletMusteriAdi;
    private final StringProperty biletMusteriSoyadi;
    private final ObjectProperty<LocalDateTime> rezervasyonZamani;
    private final StringProperty formatliRezervasyonZamani;
    private final BooleanProperty iptalEdildi;
    private final ObjectProperty<LocalDateTime> iptalZamani; 
    private final StringProperty formatliIptalZamani; 

    public static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public AdminRezervasyonDetay(int rezervasyonID, String kullaniciAdi, String filmAdi, String salonAdi, String sehirAdi,
                                 LocalDateTime seansZamani, String koltukNumarasi, String biletMusteriAdi,
                                 String biletMusteriSoyadi, LocalDateTime rezervasyonZamani, boolean iptalEdildi,
                                 LocalDateTime iptalZamani) {
        this.rezervasyonID = new SimpleIntegerProperty(rezervasyonID);
        this.kullaniciAdi = new SimpleStringProperty(kullaniciAdi);
        this.filmAdi = new SimpleStringProperty(filmAdi);
        this.salonAdi = new SimpleStringProperty(salonAdi);
        this.sehirAdi = new SimpleStringProperty(sehirAdi);
        this.seansZamani = new SimpleObjectProperty<>(seansZamani);
        this.formatliSeansZamani = new SimpleStringProperty(seansZamani != null ? seansZamani.format(DT_FORMATTER) : "-");
        this.koltukNumarasi = new SimpleStringProperty(koltukNumarasi);
        this.biletMusteriAdi = new SimpleStringProperty(biletMusteriAdi);
        this.biletMusteriSoyadi = new SimpleStringProperty(biletMusteriSoyadi);
        this.rezervasyonZamani = new SimpleObjectProperty<>(rezervasyonZamani);
        this.formatliRezervasyonZamani = new SimpleStringProperty(rezervasyonZamani != null ? rezervasyonZamani.format(DT_FORMATTER) : "-");
        this.iptalEdildi = new SimpleBooleanProperty(iptalEdildi);
        this.iptalZamani = new SimpleObjectProperty<>(iptalZamani);
        this.formatliIptalZamani = new SimpleStringProperty(iptalZamani != null ? iptalZamani.format(DT_FORMATTER) : "-");
    }

    // Tüm alanlar için Getter ve JavaFX Property metotları
    public int getRezervasyonID() { return rezervasyonID.get(); }
    public IntegerProperty rezervasyonIDProperty() { return rezervasyonID; }

    public String getKullaniciAdi() { return kullaniciAdi.get(); }
    public StringProperty kullaniciAdiProperty() { return kullaniciAdi; }

    public String getFilmAdi() { return filmAdi.get(); }
    public StringProperty filmAdiProperty() { return filmAdi; }

    public String getSalonAdi() { return salonAdi.get(); }
    public StringProperty salonAdiProperty() { return salonAdi; }

    public String getSehirAdi() { return sehirAdi.get(); }
    public StringProperty sehirAdiProperty() { return sehirAdi; }

    public LocalDateTime getSeansZamani() { return seansZamani.get(); }
    public ObjectProperty<LocalDateTime> seansZamaniProperty() { return seansZamani; }
    public String getFormatliSeansZamani() { return formatliSeansZamani.get(); }
    public StringProperty formatliSeansZamaniProperty() { return formatliSeansZamani; }


    public String getKoltukNumarasi() { return koltukNumarasi.get(); }
    public StringProperty koltukNumarasiProperty() { return koltukNumarasi; }

    public String getBiletMusteriAdi() { return biletMusteriAdi.get(); }
    public StringProperty biletMusteriAdiProperty() { return biletMusteriAdi; }

    public String getBiletMusteriSoyadi() { return biletMusteriSoyadi.get(); }
    public StringProperty biletMusteriSoyadiProperty() { return biletMusteriSoyadi; }

    public LocalDateTime getRezervasyonZamani() { return rezervasyonZamani.get(); }
    public ObjectProperty<LocalDateTime> rezervasyonZamaniProperty() { return rezervasyonZamani; }
    public String getFormatliRezervasyonZamani() { return formatliRezervasyonZamani.get(); }
    public StringProperty formatliRezervasyonZamaniProperty() { return formatliRezervasyonZamani; }

    public boolean isIptalEdildi() { return iptalEdildi.get(); }
    public BooleanProperty iptalEdildiProperty() { return iptalEdildi; }
    public void setIptalEdildi(boolean iptalEdildi) { this.iptalEdildi.set(iptalEdildi); }


    public LocalDateTime getIptalZamani() { return iptalZamani.get(); }
    public ObjectProperty<LocalDateTime> iptalZamaniProperty() { return iptalZamani; }
    public String getFormatliIptalZamani() { return formatliIptalZamani.get(); }
    public StringProperty formatliIptalZamaniProperty() { return formatliIptalZamani; }
    public void setIptalZamani(LocalDateTime iptalZamani) { // İptal zamanını güncellemek için
        this.iptalZamani.set(iptalZamani);
        this.formatliIptalZamani.set(iptalZamani != null ? iptalZamani.format(DT_FORMATTER) : "-");
    }
}