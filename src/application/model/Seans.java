// application/model/Seans.java
package application.model;

import javafx.beans.property.IntegerProperty;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Seans {
    private final IntegerProperty seansID;
    private final ObjectProperty<Film> film; 
    private final StringProperty filmBaslik; 
    private final ObjectProperty<Salon> salon; 
    private final StringProperty salonAdi; 
    private final StringProperty sehirAdi; 
    private final ObjectProperty<LocalDateTime> seansZamani;
    private final BooleanProperty iptalEdildi;

    public static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");


    public Seans(int seansID, Film film, Salon salon, LocalDateTime seansZamani, boolean iptalEdildi) { 
        this.seansID = new SimpleIntegerProperty(seansID);
        this.film = new SimpleObjectProperty<>(film);
        this.filmBaslik = new SimpleStringProperty(film != null ? film.getBaslik() : "");
        this.salon = new SimpleObjectProperty<>(salon);
        this.salonAdi = new SimpleStringProperty(salon != null ? salon.getSalonAdi() : "");
        this.sehirAdi = new SimpleStringProperty(salon != null && salon.getSehirAdi() != null ? salon.getSehirAdi() : "");
        this.seansZamani = new SimpleObjectProperty<>(seansZamani);
        this.iptalEdildi = new SimpleBooleanProperty(iptalEdildi); 

    }

    // SeansID
    public int getSeansID() { return seansID.get(); }
    public IntegerProperty seansIDProperty() { return seansID; }

    // Film
    public Film getFilm() { return film.get(); }
    public ObjectProperty<Film> filmProperty() { return film; }
    public void setFilm(Film film) {
        this.film.set(film);
        this.filmBaslik.set(film != null ? film.getBaslik() : "");
    }
    public String getFilmBaslik() { return filmBaslik.get(); }
    public StringProperty filmBaslikProperty() { return filmBaslik; }


    // Salon
    public Salon getSalon() { return salon.get(); }
    public ObjectProperty<Salon> salonProperty() { return salon; }
    public void setSalon(Salon salon) {
        this.salon.set(salon);
        this.salonAdi.set(salon != null ? salon.getSalonAdi() : "");
        this.sehirAdi.set(salon != null && salon.getSehirAdi() != null ? salon.getSehirAdi() : "");
    }
    public String getSalonAdi() { return salonAdi.get(); }
    public StringProperty salonAdiProperty() { return salonAdi; }
    public String getSehirAdi() { return sehirAdi.get(); }
    public StringProperty sehirAdiProperty() { return sehirAdi; }
    
//    İPTALEDİLDİ Mİ
    public boolean isIptalEdildi() { return iptalEdildi.get(); }
    public BooleanProperty iptalEdildiProperty() { return iptalEdildi; }
    public void setIptalEdildi(boolean iptalEdildi) { this.iptalEdildi.set(iptalEdildi); }


    // SeansZamani
    public LocalDateTime getSeansZamani() { return seansZamani.get(); }
    public ObjectProperty<LocalDateTime> seansZamaniProperty() { return seansZamani; }
    public void setSeansZamani(LocalDateTime seansZamani) { this.seansZamani.set(seansZamani); }

    // TableView'da formatlı göstermek için
    public String getFormatliSeansZamani() {
        return getSeansZamani() != null ? getSeansZamani().format(DT_FORMATTER) : "";
    }
    public StringProperty formatliSeansZamaniProperty() { 
        return new SimpleStringProperty(getFormatliSeansZamani());
    }


    @Override
    public String toString() { // ComboBox'lar veya listeler için
        return (film.get() != null ? film.get().getBaslik() : "Bilinmeyen Film") +
               " - " + (salon.get() != null ? salon.get().getSalonAdi() : "Bilinmeyen Salon") +
               " (" + getFormatliSeansZamani() + ")";
    }
}