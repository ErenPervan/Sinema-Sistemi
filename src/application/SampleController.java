// application/SampleController.java
package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SampleController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;

    @FXML
    private void handleLogin() {
        String kullaniciAdi = usernameField.getText();
        String girilenSifre = passwordField.getText();

        if (kullaniciAdi.isEmpty() || girilenSifre.isEmpty()) {
            showAlert("Giriş Hatası", "Kullanıcı adı ve şifre boş bırakılamaz!", Alert.AlertType.WARNING);
            return;
        }

        String query = "SELECT KullaniciID, Rol, SifreHash FROM Kullanicilar WHERE KullaniciAdi = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DbHelper.getConnection();
            pstmt = conn.prepareStatement(query);
            pstmt.setString(1, kullaniciAdi);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                int kullaniciID = rs.getInt("KullaniciID");
                String rol = rs.getString("Rol");
                String veritabanindakiSifreHash = rs.getString("SifreHash");

                if (veritabanindakiSifreHash == null || veritabanindakiSifreHash.isEmpty()) {
                    showAlert("Giriş Hatası", "Kullanıcı hesabı için şifre ayarlanmamış veya hatalı.", Alert.AlertType.ERROR);
                    return;
                }

                boolean sifreDogru = false;
                try {
                    sifreDogru = AuthHelper.checkPasswordSHA256(girilenSifre, veritabanindakiSifreHash);
                } catch (IllegalArgumentException iae) {
                    System.err.println("Şifre kontrolünde geçersiz argüman: " + iae.getMessage());
                } catch (RuntimeException re) { 
                     System.err.println("Kritik Güvenlik Hatası - Şifreleme Algoritması: " + re.getMessage());
                     re.printStackTrace();
                     showAlert("Sistem Hatası", "Güvenlik altyapısında beklenmedik bir sorun oluştu. Lütfen sistem yöneticinize başvurun.", Alert.AlertType.ERROR);
                     return; 
                }
                
                if (sifreDogru) {
                    UserSession.getInstance().setKullanici(kullaniciID, kullaniciAdi, rol);
                    showAlert("Giriş Başarılı", "Giriş başarılı!", Alert.AlertType.INFORMATION);
                    yonlendirAnaSayfa();
                } else {
                    showAlert("Giriş Hatası", "Kullanıcı adı veya şifre hatalı!", Alert.AlertType.ERROR);
                }
            } else {
                showAlert("Giriş Hatası", "Kullanıcı adı veya şifre hatalı!", Alert.AlertType.ERROR);
            }
        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "Veritabanı bağlantısında veya sorgusunda bir sorun oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        } catch (IOException e) { 
            showAlert("Yönlendirme Hatası", "Ana sayfa yüklenirken bir sorun oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                DbHelper.closeConnection(conn);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void yonlendirAnaSayfa() throws IOException {
        String fxmlPath = "/application/MainDashboard.fxml";
        URL fxmlUrl = getClass().getResource(fxmlPath);
        if (fxmlUrl == null) {
            showAlert("Dosya Hatası", "'" + fxmlPath + "' bulunamadı.", Alert.AlertType.ERROR);
            return; 
        }

        Stage stage = (Stage) usernameField.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Ana Sayfa - Sinema Otomasyonu");
        stage.centerOnScreen();
        stage.show();
    }

    @FXML
    private void handleKayitOlEkraninaGit() {
        try {
            String fxmlPath = "/application/KayitEkrani.fxml"; 
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                showAlert("Dosya Hatası", "'" + fxmlPath + "' bulunamadı.", Alert.AlertType.ERROR);
                return;
            }

            Stage stage = (Stage) usernameField.getScene().getWindow(); // Mevcut pencereyi al
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Yeni Kullanıcı Kaydı");
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Yönlendirme Hatası", "Kayıt ekranı yüklenirken bir sorun oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    @FXML
    private void handleSifremiUnuttumEkraninaGit() {
        try {
            String fxmlPath = "/application/SifreSifirlamaTalebiEkrani.fxml";
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                showAlert("Dosya Hatası", "'" + fxmlPath + "' bulunamadı.", Alert.AlertType.ERROR);
                return;
            }

            Stage stage = (Stage) usernameField.getScene().getWindow(); 
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Şifre Sıfırlama Talebi");
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Yönlendirme Hatası", "Şifre sıfırlama ekranı yüklenirken bir sorun oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
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