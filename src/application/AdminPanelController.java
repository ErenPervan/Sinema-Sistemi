// application/AdminPanelController.java
package application;

import javafx.fxml.FXML;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class AdminPanelController {

    @FXML
    private Button btnIlYonetimi;
    @FXML
    private Button btnSalonYonetimi;
    @FXML
    private Button btnFilmYonetimi;
    @FXML
    private Button btnTurYonetimi;
    @FXML
    private Button btnSeansYonetimi;
    @FXML
    private Button btnKullaniciYonetimi;
    @FXML
    private Button btnAnaMenu;
    @FXML
    private Button btnHatirlaticiGonder;
    @FXML
    private Button btnRezervasyonYonetimi;


    public void initialize(){
        // AdminPanel.fxml'e sadece admin rolündeki kullanıcılar MainDashboardController üzerinden yönlendirilmeli.
        if (UserSession.getInstance().isLoggedIn() && !UserSession.getInstance().getRol().equals("Yonetici")) {
            showAlert("Yetkisiz Erişim", "Bu alana erişim yetkiniz bulunmamaktadır.", Alert.AlertType.ERROR);
            disableAdminButtons();
          
        }
    }

    private void disableAdminButtons() {
        btnIlYonetimi.setDisable(true);
        btnSalonYonetimi.setDisable(true);
        btnFilmYonetimi.setDisable(true);
        btnTurYonetimi.setDisable(true);
        btnSeansYonetimi.setDisable(true);
        btnKullaniciYonetimi.setDisable(true);
    }

    @FXML
    private void handleIlYonetimi() {
        System.out.println("İl Yönetimi tıklandı.");
        loadAdminSubPanel("IlYonetimi.fxml", "İl Yönetimi");
    }

    @FXML
    private void handleSalonYonetimi() {
        System.out.println("Salon Yönetimi tıklandı.");
        loadAdminSubPanel("SalonYonetimi.fxml", "Salon Yönetimi"); 
    }

    @FXML
    private void handleFilmYonetimi() {
        System.out.println("Film Yönetimi tıklandı.");
        loadAdminSubPanel("FilmYonetimi.fxml", "Film Yönetimi"); 
    }

    @FXML
    private void handleTurYonetimi() {
        System.out.println("Film Türü Yönetimi tıklandı.");
        loadAdminSubPanel("TurYonetimi.fxml", "Film Türü Yönetimi");
    }

    @FXML
    private void handleSeansYonetimi() {
        System.out.println("Seans Yönetimi tıklandı.");
        loadAdminSubPanel("SeansYonetimi.fxml", "Seans Yönetimi");
    }
    
    @FXML
    private void handleKullaniciYonetimi() {
        System.out.println("Kullanıcı Yönetimi tıklandı.");
        loadAdminSubPanel("KullaniciYonetimi.fxml", "Kullanıcı Yönetimi");
    }
    @FXML
    private void handleRezervasyonYonetimi() {
        System.out.println("Rezervasyon Yönetimi tıklandı.");
        loadAdminSubPanel("RezervasyonYonetimi.fxml", "Rezervasyon Yönetimi");
    }

    /**
     * Belirtilen FXML dosyasını yeni bir modal pencerede yükler.
     * @param fxmlFile Yüklenecek FXML dosyasının adı 
     * @param title Yeni pencerenin başlığı
     */
    private void loadAdminSubPanel(String fxmlFile, String title) {
        try {
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);



            stage.showAndWait(); // Bu pencere kapanana kadar bekle

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Yönetim Paneli Hatası", "Alt panel '" + title + "' yüklenirken hata oluştu:\n" + e.getMessage(), Alert.AlertType.ERROR);
        } catch (NullPointerException e) {
            e.printStackTrace();
            showAlert("Kaynak Bulunamadı Hatası", "'" + fxmlFile + "' adlı FXML dosyası bulunamadı.\nLütfen dosya adını ve projedeki konumunu kontrol edin.", Alert.AlertType.ERROR);
        }
    }


    @FXML
    private void handleAnaMenu() {
        try {
            Stage stage = (Stage) btnAnaMenu.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("MainDashboard.fxml"));
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Ana Sayfa - Sinema Otomasyonu");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Hata", "Ana menü yüklenirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    @FXML
    private void handleHatirlaticiGonder() {
        btnHatirlaticiGonder.setDisable(true); // Butonu geçici olarak devre dışı bırak
        showAlert("E-posta Gönderimi", "Hatırlatma e-postaları gönderilmeye başlanıyor... Bu işlem biraz sürebilir. Lütfen bekleyin.", Alert.AlertType.INFORMATION);
        // arka plan thread
        new Thread(() -> {
            ReminderService reminderService = new ReminderService();
            reminderService.sendUpcomingSessionReminders();

            // işlem bitinceee butonu aktif et 
            Platform.runLater(() -> {
                showAlert("E-posta Gönderimi Tamamlandı", "Hatırlatma e-postası gönderme işlemi tamamlandı. Detaylar için konsol loglarını kontrol edebilirsiniz.", Alert.AlertType.INFORMATION);
                btnHatirlaticiGonder.setDisable(false);
            });
        }).start();
    }

    private void showAlertNotImplemented(String featureName) {
        showAlert(featureName + " - Geliştirme Aşamasında", "Bu özellik henüz tamamlanmamıştır.", Alert.AlertType.INFORMATION);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (btnHatirlaticiGonder != null && btnHatirlaticiGonder.getScene() != null && btnHatirlaticiGonder.getScene().getWindow() != null) {
             alert.initOwner(btnHatirlaticiGonder.getScene().getWindow());
        }
        alert.showAndWait();
    }
}