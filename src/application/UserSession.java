// application/UserSession.java
package application;

public class UserSession {
    private static UserSession instance;

    private int kullaniciID;
    private String kullaniciAdi;
    private String rol; 

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void setKullanici(int kullaniciID, String kullaniciAdi, String rol) {
        this.kullaniciID = kullaniciID;
        this.kullaniciAdi = kullaniciAdi;
        this.rol = rol;
    }

    public int getKullaniciID() {
        return kullaniciID;
    }

    public String getKullaniciAdi() {
        return kullaniciAdi;
    }

    public String getRol() {
        return rol;
    }

    public void clearSession() {
        kullaniciID = 0;
        kullaniciAdi = null;
        rol = null;
    }

    public boolean isLoggedIn() {
        return kullaniciAdi != null && !kullaniciAdi.isEmpty();
    }
}