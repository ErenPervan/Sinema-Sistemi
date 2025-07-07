// application/model/Film.java
package application.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;

public class Film {
    private final IntegerProperty filmID;
    private final StringProperty baslik;
    private final ObjectProperty<Tur> tur;        
    private final StringProperty turAdi;           
    private final IntegerProperty sureDakika;
    private final StringProperty aciklama;
    private final ObjectProperty<LocalDate> yayinTarihi;
    private final StringProperty afisURL;
    private final ObjectProperty<LocalDate> sistemeEklenmeTarihi; 
    private final StringProperty fragmanURL;
   
    public Film(int filmID, String baslik, Tur tur, int sureDakika, String aciklama, LocalDate yayinTarihi, String afisURL, LocalDate sistemeEklenmeTarihi, String fragmanURL) { // fragmanURL parametresi eklendi
        this.filmID = new SimpleIntegerProperty(filmID);
        this.baslik = new SimpleStringProperty(baslik);
        this.tur = new SimpleObjectProperty<>(tur);
        this.turAdi = new SimpleStringProperty(tur != null ? tur.getTurAdi() : "");
        this.sureDakika = new SimpleIntegerProperty(sureDakika);
        this.aciklama = new SimpleStringProperty(aciklama);
        this.yayinTarihi = new SimpleObjectProperty<>(yayinTarihi);
        this.afisURL = new SimpleStringProperty(afisURL);
        this.sistemeEklenmeTarihi = new SimpleObjectProperty<>(sistemeEklenmeTarihi);
        this.fragmanURL = new SimpleStringProperty(fragmanURL); 
    }

    // FilmID
    public int getFilmID() { return filmID.get(); }
    public IntegerProperty filmIDProperty() { return filmID; }
    public void setFilmID(int filmID) { this.filmID.set(filmID); }

    // Baslik
    public String getBaslik() { return baslik.get(); }
    public StringProperty baslikProperty() { return baslik; }
    public void setBaslik(String baslik) { this.baslik.set(baslik); }

    // Tur (Tur nesnesi olarak)
    public Tur getTur() { return tur.get(); }
    public ObjectProperty<Tur> turProperty() { return tur; }
    public void setTur(Tur tur) {
        this.tur.set(tur);
        this.turAdi.set(tur != null ? tur.getTurAdi() : ""); // turAdi'ni da güncelle
    }

    // TurAdi (String olarak, TableView için)
    public String getTurAdi() { return turAdi.get(); }
    public StringProperty turAdiProperty() { return turAdi; }

    
    // SureDakika
    public int getSureDakika() { return sureDakika.get(); }
    public IntegerProperty sureDakikaProperty() { return sureDakika; }
    public void setSureDakika(int sureDakika) { this.sureDakika.set(sureDakika); }

    // Aciklama
    public String getAciklama() { return aciklama.get(); }
    public StringProperty aciklamaProperty() { return aciklama; }
    public void setAciklama(String aciklama) { this.aciklama.set(aciklama); }

    // YayinTarihi
    public LocalDate getYayinTarihi() { return yayinTarihi.get(); }
    public ObjectProperty<LocalDate> yayinTarihiProperty() { return yayinTarihi; }
    public void setYayinTarihi(LocalDate yayinTarihi) { this.yayinTarihi.set(yayinTarihi); }

    // AfisURL
    public String getAfisURL() { return afisURL.get(); }
    public StringProperty afisURLProperty() { return afisURL; }
    public void setAfisURL(String afisURL) { this.afisURL.set(afisURL); }

    // SistemeEklenmeTarihi
    public LocalDate getSistemeEklenmeTarihi() { return sistemeEklenmeTarihi.get(); }
    public ObjectProperty<LocalDate> sistemeEklenmeTarihiProperty() { return sistemeEklenmeTarihi; }
    public void setSistemeEklenmeTarihi(LocalDate sistemeEklenmeTarihi) { this.sistemeEklenmeTarihi.set(sistemeEklenmeTarihi); }


 // FragmanURL
 public String getFragmanURL() { return fragmanURL.get(); }
 public StringProperty fragmanURLProperty() { return fragmanURL; }
 public void setFragmanURL(String fragmanURL) { this.fragmanURL.set(fragmanURL); }

    @Override
    public String toString() {
        return getBaslik();
    }
}