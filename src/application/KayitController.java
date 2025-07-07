// application/KayitController.java
package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException; 
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class KayitController {

    @FXML private TextField txtKayitKullaniciAdi;
    @FXML private PasswordField txtKayitSifre;
    @FXML private TextField txtKayitAd;
    @FXML private TextField txtKayitSoyad;
    @FXML private TextField txtKayitEposta;
    @FXML private Button btnKayitOl;
    @FXML private Button btnKayitGeri;

    @FXML
    private void handleKayitOl() {
        String kullaniciAdi = txtKayitKullaniciAdi.getText().trim();
        String sifre = txtKayitSifre.getText();
        String ad = txtKayitAd.getText().trim();
        String soyad = txtKayitSoyad.getText().trim();
        String eposta = txtKayitEposta.getText().trim();
        String rol = "Kullanici";

        if (kullaniciAdi.isEmpty() || sifre.isEmpty() || eposta.isEmpty()) {
            showAlert("Giriş Hatası", "Kullanıcı adı, şifre ve e-posta alanları boş bırakılamaz.", Alert.AlertType.WARNING);
            return;
        }

        if (!eposta.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showAlert("Giriş Hatası", "Lütfen geçerli bir e-posta adresi girin.", Alert.AlertType.WARNING);
            return;
        }
        
        if (sifre.length() < 6) {
            showAlert("Giriş Hatası", "Şifre en az 6 karakter olmalıdır.", Alert.AlertType.WARNING);
            return;
        }

        String checkUserQuery = "SELECT COUNT(*) FROM Kullanicilar WHERE KullaniciAdi = ? OR Eposta = ?";
        try(Connection conn = DbHelper.getConnection(); PreparedStatement pstmt = conn.prepareStatement(checkUserQuery)){
            pstmt.setString(1, kullaniciAdi);
            pstmt.setString(2, eposta);
            ResultSet rs = pstmt.executeQuery();
            if(rs.next() && rs.getInt(1) > 0){
                showAlert("Kayıt Hatası", "Bu kullanıcı adı veya e-posta adresi zaten kayıtlı.", Alert.AlertType.ERROR);
                if(rs!=null) rs.close();
                return;
            }
            if(rs!=null) rs.close();
        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "Kullanıcı kontrolü sırasında hata: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
            return;
        }

        String sifreHash;
        try {
            sifreHash = AuthHelper.hashPasswordSHA256(sifre);
        } catch (IllegalArgumentException e) {
            showAlert("Şifre Hatası", e.getMessage(), Alert.AlertType.ERROR);
            return;
        } catch (RuntimeException e) { 
            showAlert("Güvenlik Yapılandırma Hatası", "Şifreleme algoritmasıyla ilgili bir sorun oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
            return;
        }

        String insertQuery = "INSERT INTO Kullanicilar (KullaniciAdi, SifreHash, Rol, Ad, Soyad, Eposta, KayitTarihi) " +
                             "VALUES (?, ?, ?, ?, ?, ?, CURDATE())";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {

            pstmt.setString(1, kullaniciAdi);
            pstmt.setString(2, sifreHash);
            pstmt.setString(3, rol);
            pstmt.setString(4, ad.isEmpty() ? null : ad);
            pstmt.setString(5, soyad.isEmpty() ? null : soyad);
            pstmt.setString(6, eposta);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                showAlert("Kayıt Başarılı", "Yeni kullanıcı başarıyla oluşturuldu!\nŞimdi giriş yapabilirsiniz.", Alert.AlertType.INFORMATION);
                // yonlendirGirisEkrani() IOException fırlatabilir, bu yüzden try-catch içine alıyoruz.
                try {
                    yonlendirGirisEkrani(); 
                } catch (IOException e) {
                    e.printStackTrace();
                    showAlert("Yönlendirme Hatası", "Giriş ekranına dönülürken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "Kullanıcı kaydedilirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleKayitGeri() {
        try {
            yonlendirGirisEkrani();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Yönlendirme Hatası", "Giriş ekranına dönülürken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void yonlendirGirisEkrani() throws IOException {
        String fxmlPath = "/application/Sample.fxml"; 
        URL fxmlUrl = getClass().getResource(fxmlPath);
        
        if (fxmlUrl == null) {

            showAlert("Dosya Hatası", "'" + fxmlPath + "' bulunamadı.", Alert.AlertType.ERROR);
            throw new IOException("FXML dosyası bulunamadı: " + fxmlPath); 
        }
        
        Stage stage = (Stage) btnKayitGeri.getScene().getWindow(); 
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load(); 
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Giriş Ekranı");
        stage.centerOnScreen();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}