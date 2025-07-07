// application/SifreSifirlamaTalebiController.java
package application;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
import java.util.UUID;

public class SifreSifirlamaTalebiController {

    @FXML private TextField txtEpostaAdresi;
    @FXML private Button btnKodGonder;
    @FXML private Button btnGirisEkraniDon;

    @FXML
    private void handleKodGonder() {
        String eposta = txtEpostaAdresi.getText().trim();

        if (eposta.isEmpty()) {
            showAlert("Eksik Bilgi", "Lütfen e-posta adresinizi girin.", Alert.AlertType.WARNING);
            return;
        }

        if (!eposta.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showAlert("Geçersiz E-posta", "Lütfen geçerli bir e-posta adresi girin.", Alert.AlertType.WARNING);
            return;
        }

        //  E-posta veritabanında var mı kontrol et
        String checkEmailQuery = "SELECT KullaniciID FROM Kullanicilar WHERE Eposta = ?";
        int kullaniciID = -1;

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(checkEmailQuery)) {
            pstmt.setString(1, eposta);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                kullaniciID = rs.getInt("KullaniciID");
            }
            if (rs != null) rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "E-posta kontrolü sırasında bir hata oluştu.", Alert.AlertType.ERROR);
            return;
        }

        if (kullaniciID == -1) {
            showAlert("Kullanıcı Bulunamadı", "Bu e-posta adresiyle kayıtlı bir kullanıcı bulunamadı.", Alert.AlertType.ERROR);
            return;
        }

        // Benzersiz bir sıfırlama kodu üret ve geçerlilik tarihi belirle
        String sifirlamaKodu = UUID.randomUUID().toString().substring(0, 8).toUpperCase(); 
        LocalDateTime gecerlilikTarihi = LocalDateTime.now().plusMinutes(15); 

        // Kodu ve geçerlilik tarihini veritabanına kaydet
        String updateCodeQuery = "UPDATE Kullanicilar SET SifirlamaKodu = ?, SifirlamaKoduGecerlilikTarihi = ? WHERE KullaniciID = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateCodeQuery)) {
            pstmt.setString(1, sifirlamaKodu);
            pstmt.setTimestamp(2, Timestamp.valueOf(gecerlilikTarihi));
            pstmt.setInt(3, kullaniciID);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                // 4. Kullanıcıya e-posta ile kodu gönder 
                final String finalEposta = eposta;
                final String finalSifirlamaKodu = sifirlamaKodu;
                new Thread(() -> {
                    EmailService emailService = new EmailService();
                    String konu = "Sinema Bilet Sistemi - Şifre Sıfırlama Kodu";
                    String icerik = "Merhaba,\n\n" +
                                    "Şifrenizi sıfırlamak için aşağıdaki kodu kullanabilirsiniz:\n\n" +
                                    "Sıfırlama Kodu: " + finalSifirlamaKodu + "\n\n" +
                                    "Bu kod 15 dakika boyunca geçerlidir.\n\n" +
                                    "Eğer bu talebi siz yapmadıysanız, bu e-postayı görmezden gelin.\n\n" +
                                    "İyi günler,\nSinema Bilet Otomasyonu";
                    boolean emailGonderildi = emailService.sendEmail(finalEposta, konu, icerik);
                    Platform.runLater(() -> {
                        if (emailGonderildi) {
                            showAlert("Kod Gönderildi", "Şifre sıfırlama kodunuz e-posta adresinize gönderildi.\nLütfen e-postanızı kontrol edin.", Alert.AlertType.INFORMATION);
                            // Kullanıcıyı Şifre Yenileme Ekranına Yönlendir
                            try {
                                yonlendirSifreYenilemeEkrani(finalEposta); 
                            } catch (IOException e) {
                                e.printStackTrace();
                                showAlert("Yönlendirme Hatası", "Şifre yenileme ekranına yönlendirilirken bir hata oluştu.", Alert.AlertType.ERROR);
                            }
                        } else {
                            showAlert("E-posta Gönderim Hatası", "Sıfırlama kodu e-postayla gönderilemedi. Lütfen daha sonra tekrar deneyin veya sistem yöneticinize başvurun.", Alert.AlertType.ERROR);
                        }
                    });
                }).start();

            } else {
                showAlert("Veritabanı Hatası", "Sıfırlama kodu kaydedilemedi.", Alert.AlertType.ERROR);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Sıfırlama kodu güncellenirken bir hata oluştu.", Alert.AlertType.ERROR);
        }
    }

    private void yonlendirSifreYenilemeEkrani(String eposta) throws IOException {
        String fxmlPath = "/application/SifreYenilemeEkrani.fxml";
        URL fxmlUrl = getClass().getResource(fxmlPath);
        if (fxmlUrl == null) {
            showAlert("Dosya Hatası", "'" + fxmlPath + "' bulunamadı.", Alert.AlertType.ERROR);
            throw new IOException("FXML dosyası bulunamadı: " + fxmlPath);
        }

        Stage stage = (Stage) btnKodGonder.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();

        // SifreYenilemeController'a e-posta adresini aktar
        SifreYenilemeController controller = loader.getController();
        if (controller != null) {
            controller.initData(eposta);
        } else {
            System.err.println("HATA: SifreYenilemeController yüklenemedi!");
        }

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Yeni Şifre Belirleme");
        stage.centerOnScreen();
    }

    @FXML
    private void handleGirisEkraniDon() {
        try {
            Stage stage = (Stage) btnGirisEkraniDon.getScene().getWindow();
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