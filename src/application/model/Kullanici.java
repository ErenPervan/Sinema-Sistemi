// application/model/Kullanici.java
package application.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Kullanici {
    private final IntegerProperty kullaniciID;
    private final StringProperty kullaniciAdi;
    private final StringProperty rol; 
    private final StringProperty ad;
    private final StringProperty soyad;
    private final StringProperty eposta;


    public Kullanici(int kullaniciID, String kullaniciAdi, String rol, String ad, String soyad, String eposta) {
        this.kullaniciID = new SimpleIntegerProperty(kullaniciID);
        this.kullaniciAdi = new SimpleStringProperty(kullaniciAdi);
        this.rol = new SimpleStringProperty(rol);
        this.ad = new SimpleStringProperty(ad);
        this.soyad = new SimpleStringProperty(soyad);
        this.eposta = new SimpleStringProperty(eposta);
    }

    // KullaniciID
    public int getKullaniciID() { return kullaniciID.get(); }
    public IntegerProperty kullaniciIDProperty() { return kullaniciID; }

    // KullaniciAdi
    public String getKullaniciAdi() { return kullaniciAdi.get(); }
    public StringProperty kullaniciAdiProperty() { return kullaniciAdi; }
    public void setKullaniciAdi(String kullaniciAdi) { this.kullaniciAdi.set(kullaniciAdi); }

    // Rol
    public String getRol() { return rol.get(); }
    public StringProperty rolProperty() { return rol; }
    public void setRol(String rol) { this.rol.set(rol); }

    // Ad
    public String getAd() { return ad.get(); }
    public StringProperty adProperty() { return ad; }
    public void setAd(String ad) { this.ad.set(ad); }

    // Soyad
    public String getSoyad() { return soyad.get(); }
    public StringProperty soyadProperty() { return soyad; }
    public void setSoyad(String soyad) { this.soyad.set(soyad); }

    // Eposta
    public String getEposta() { return eposta.get(); }
    public StringProperty epostaProperty() { return eposta; }
    public void setEposta(String eposta) { this.eposta.set(eposta); }

    @Override
    public String toString() {
        return getKullaniciAdi() + " (" + getAd() + " " + getSoyad() + ")";
    }
}