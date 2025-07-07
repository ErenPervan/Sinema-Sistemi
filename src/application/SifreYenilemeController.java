// application/SifreYenilemeController.java
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
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class SifreYenilemeController {

    @FXML private TextField txtYenilemeEposta;
    @FXML private TextField txtSifirlamaKodu;
    @FXML private PasswordField txtYeniSifre;
    @FXML private PasswordField txtYeniSifreTekrar;
    @FXML private Button btnSifreyiGuncelle;
    @FXML private Button btnYenilemeGirisEkraniDon;

    private String epostaAdresi; // Bir önceki ekrandan gelen e-posta

    public void initData(String eposta) {
        this.epostaAdresi = eposta;
        txtYenilemeEposta.setText(this.epostaAdresi);
        txtYenilemeEposta.setEditable(false);
    }

    @FXML
    private void handleSifreyiGuncelle() {
        String eposta = txtYenilemeEposta.getText().trim();
        String girilenKod = txtSifirlamaKodu.getText().trim();
        String yeniSifre = txtYeniSifre.getText();
        String yeniSifreTekrar = txtYeniSifreTekrar.getText();

        if (eposta.isEmpty() || girilenKod.isEmpty() || yeniSifre.isEmpty() || yeniSifreTekrar.isEmpty()) {
            showAlert("Eksik Bilgi", "Lütfen tüm alanları doldurun.", Alert.AlertType.WARNING);
            return;
        }

        if (!yeniSifre.equals(yeniSifreTekrar)) {
            showAlert("Şifre Uyuşmazlığı", "Girilen yeni şifreler eşleşmiyor.", Alert.AlertType.WARNING);
            return;
        }

        if (yeniSifre.length() < 6) {
            showAlert("Şifre Zayıf", "Yeni şifre en az 6 karakter olmalıdır.", Alert.AlertType.WARNING);
            return;
        }

        // 1. Kodu, e-postayı, geçerlilik tarihini VE MEVCUT ŞİFRE HASH'İNİ veritabanından kontrol et
        String query = "SELECT KullaniciID, SifreHash, SifirlamaKodu, SifirlamaKoduGecerlilikTarihi " +
                       "FROM Kullanicilar WHERE Eposta = ? AND SifirlamaKodu IS NOT NULL";
        int kullaniciID = -1;
        LocalDateTime gecerlilikTarihi = null;
        String dbKod = null;
        String mevcutSifreHash = null; 

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, eposta);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                kullaniciID = rs.getInt("KullaniciID");
                mevcutSifreHash = rs.getString("SifreHash"); 
                dbKod = rs.getString("SifirlamaKodu");
                Timestamp ts = rs.getTimestamp("SifirlamaKoduGecerlilikTarihi");
                if (ts != null) {
                    gecerlilikTarihi = ts.toLocalDateTime();
                }
            }
            if (rs != null) rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Sıfırlama bilgileri kontrol edilirken bir hata oluştu.", Alert.AlertType.ERROR);
            return;
        }

        if (kullaniciID == -1 || dbKod == null) {
            showAlert("Hata", "Geçersiz sıfırlama talebi veya e-posta bulunamadı.", Alert.AlertType.ERROR);
            return;
        }

        if (!dbKod.equals(girilenKod)) {
            showAlert("Hatalı Kod", "Girilen sıfırlama kodu yanlış.", Alert.AlertType.ERROR);
            return;
        }

        if (gecerlilikTarihi == null || LocalDateTime.now().isAfter(gecerlilikTarihi)) {
            showAlert("Kod Geçersiz", "Sıfırlama kodunun süresi dolmuş. Lütfen yeni bir kod talep edin.", Alert.AlertType.ERROR);
            temizleSifirlamaKodu(kullaniciID);
            return;
        }

        //  YENİ ŞİFRENİN ESKİ ŞİFREYLE AYNI OLMADIĞINI KONTROL ET
        String yeniGirilenSifreHash;
        try {
            // yeniSifre değişkeninin dolu olduğu yukarıdaki kontrollerle sağlanmış olmalı
            yeniGirilenSifreHash = AuthHelper.hashPasswordSHA256(yeniSifre);
        } catch (IllegalArgumentException e) { // ÖNCE DAHA ÖZEL OLAN YAKALANIR

        	showAlert("Şifre Hatası", "Şifreleme sırasında geçersiz argüman: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
            return;
        } catch (RuntimeException e) { 
            showAlert("Güvenlik Hatası", "Şifreleme algoritmasıyla ilgili bir sorun oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace(); 
            return;
        }

        if (mevcutSifreHash != null && mevcutSifreHash.equals(yeniGirilenSifreHash)) {
            showAlert("Şifre Tekrarı", "Yeni şifreniz mevcut şifrenizle aynı olamaz. Lütfen farklı bir şifre girin.", Alert.AlertType.WARNING);
            return;
        }

        //  Her şey yolundaysa, yeni şifreyi hash'le (zaten yukarıda yaptık) ve güncelle
        String updateQuery = "UPDATE Kullanicilar SET SifreHash = ?, SifirlamaKodu = NULL, SifirlamaKoduGecerlilikTarihi = NULL WHERE KullaniciID = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
            pstmt.setString(1, yeniGirilenSifreHash); // Yeni hash'lenmiş şifre
            pstmt.setInt(2, kullaniciID); 

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                showAlert("Şifre Güncellendi", "Şifreniz başarıyla güncellendi. Lütfen yeni şifrenizle giriş yapın.", Alert.AlertType.INFORMATION);
                handleYenilemeGirisEkraniDon(); 
            } else {
                showAlert("Hata", "Şifre güncellenirken bir sorun oluştu (veritabanı kaydı etkilenmedi).", Alert.AlertType.ERROR);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Şifre güncellenirken bir veritabanı hatası oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void temizleSifirlamaKodu(int kullaniciID) {
        String clearCodeQuery = "UPDATE Kullanicilar SET SifirlamaKodu = NULL, SifirlamaKoduGecerlilikTarihi = NULL WHERE KullaniciID = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(clearCodeQuery)) {
            pstmt.setInt(1, kullaniciID);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Süresi dolmuş sıfırlama kodu temizlenirken hata: " + e.getMessage());
        }
    }

    @FXML
    private void handleYenilemeGirisEkraniDon() {
        try {
            Stage stage = (Stage) btnYenilemeGirisEkraniDon.getScene().getWindow();
            String fxmlPath = "/application/Sample.fxml";
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                 showAlert("Dosya Hatası", "'" + fxmlPath + "' bulunamadı.", Alert.AlertType.ERROR);
                 return;
            }
            Parent root = FXMLLoader.load(fxmlUrl);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Giriş Ekranı");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Yönlendirme Hatası", "Giriş ekranına dönülürken bir hata oluştu.", Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}