package application;

// Mevcut importlarınız...
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.application.Platform; 
import java.util.Optional;         
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class MainDashboardController {

    @FXML
    private Label lblHosgeldin;
    @FXML
    private ComboBox<String> comboIller;
    @FXML
    private ComboBox<String> comboSalonlar;
    @FXML
    private ComboBox<String> comboFilmler;
    @FXML
    private ComboBox<String> comboSeanslar;

    private Integer seciliIlID;
    private Integer seciliSalonID;
    private Integer seciliFilmID;
    private Integer seciliSeansID;

    @FXML
    private Button btnKoltukSec;
    @FXML
    private Label lblBosKoltukSayisi;

    @FXML
    private MenuItem menuAdminPaneli;
    @FXML
    private MenuItem menuFilmAra;
    @FXML
    private MenuItem menuBiletlerim;
   

    public void initialize() {
        UserSession session = UserSession.getInstance();
        if (session.isLoggedIn()) {
            lblHosgeldin.setText("Hoşgeldin, " + session.getKullaniciAdi() + " (" + session.getRol() + ")");
            if (menuAdminPaneli != null) { 
                menuAdminPaneli.setVisible(session.getRol().equals("Yonetici"));
            }
        } else {
            lblHosgeldin.setText("Lütfen giriş yapın.");
            if (menuAdminPaneli != null) {
                menuAdminPaneli.setVisible(false);
            }
        }

        illeriYukle();

        comboIller.setOnAction(event -> ilSecildi());
        comboSalonlar.setOnAction(event -> salonSecildi());
        comboFilmler.setOnAction(event -> filmSecildi());
        comboSeanslar.setOnAction(event -> seansSecildi());

        comboSalonlar.setDisable(true);
        comboFilmler.setDisable(true);
        comboSeanslar.setDisable(true);
        btnKoltukSec.setDisable(true);
        lblBosKoltukSayisi.setText("Boş Koltuk Sayısı: -");
        lblBosKoltukSayisi.getStyleClass().add("info-label-dashboard"); 
    }

    private void illeriYukle() {
        ObservableList<String> ilListesi = FXCollections.observableArrayList();
        String query = "SELECT SehirAdi FROM Sehirler ORDER BY SehirAdi";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                ilListesi.add(rs.getString("SehirAdi"));
            }
            comboIller.setItems(ilListesi);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "İller yüklenirken hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void ilSecildi() {
        String secilenIlAdi = comboIller.getValue();
        lblBosKoltukSayisi.setText("Boş Koltuk Sayısı: -");
        lblBosKoltukSayisi.getStyleClass().clear();
        lblBosKoltukSayisi.getStyleClass().add("info-label-dashboard");
        btnKoltukSec.setDisable(true);
        comboSalonlar.getItems().clear();
        comboSalonlar.setValue(null);
        comboSalonlar.setDisable(true);
        comboFilmler.getItems().clear();
        comboFilmler.setValue(null);
        comboFilmler.setDisable(true);
        comboSeanslar.getItems().clear();
        comboSeanslar.setValue(null);
        comboSeanslar.setDisable(true);
        seciliIlID = null;
        seciliSalonID = null;

        if (secilenIlAdi == null) return;

        String idQuery = "SELECT SehirID FROM Sehirler WHERE SehirAdi = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(idQuery)) {
            pstmt.setString(1, secilenIlAdi);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                seciliIlID = rs.getInt("SehirID");
                salonlariYukle(seciliIlID);
                comboSalonlar.setDisable(false);
            }
             if (rs != null) rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "İl ID alınırken hata: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void salonlariYukle(int sehirID) {
        ObservableList<String> salonListesi = FXCollections.observableArrayList();
        String query = "SELECT SalonAdi FROM SinemaSalonlari WHERE SehirID = ? ORDER BY SalonAdi";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, sehirID);
            ResultSet rs = pstmt.executeQuery();
            salonListesi.clear();
            while (rs.next()) {
                salonListesi.add(rs.getString("SalonAdi"));
            }
            comboSalonlar.setItems(salonListesi);
            if (rs != null) rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Salonlar yüklenirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void salonSecildi() {
        String secilenSalonAdi = comboSalonlar.getValue();
        lblBosKoltukSayisi.setText("Boş Koltuk Sayısı: -");
        lblBosKoltukSayisi.getStyleClass().clear();
        lblBosKoltukSayisi.getStyleClass().add("info-label-dashboard");
        btnKoltukSec.setDisable(true);
        comboFilmler.getItems().clear();
        comboFilmler.setValue(null);
        comboFilmler.setDisable(true);
        comboSeanslar.getItems().clear();
        comboSeanslar.setValue(null);
        comboSeanslar.setDisable(true);
        seciliSalonID = null;
        seciliFilmID = null; 

        if (secilenSalonAdi == null || seciliIlID == null) return;

        String idQuery = "SELECT SalonID FROM SinemaSalonlari WHERE SalonAdi = ? AND SehirID = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(idQuery)) {
            pstmt.setString(1, secilenSalonAdi);
            pstmt.setInt(2, seciliIlID);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                seciliSalonID = rs.getInt("SalonID");
                filmleriYukle(seciliSalonID);
                comboFilmler.setDisable(false);
            }
            if (rs != null) rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Salon ID alınırken hata: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void filmleriYukle(int salonID) {
        ObservableList<String> filmListesi = FXCollections.observableArrayList();
        String query = "SELECT DISTINCT f.Baslik FROM Filmler f " +
                       "JOIN Seanslar s ON f.FilmID = s.FilmID " +
                       "WHERE s.SalonID = ? AND s.SeansZamani >= NOW() " +
                       "ORDER BY f.Baslik";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, salonID);
            ResultSet rs = pstmt.executeQuery();
            filmListesi.clear();
            while (rs.next()) {
                filmListesi.add(rs.getString("Baslik"));
            }
            comboFilmler.setItems(filmListesi);
            if (filmListesi.isEmpty()) {
                 showAlert("Bilgi", "Bu salonda gösterimde olan aktif film bulunamadı.", Alert.AlertType.INFORMATION);
            }
            if (rs != null) rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Filmler yüklenirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void filmSecildi() {
        String secilenFilmAdi = comboFilmler.getValue();
        
        lblBosKoltukSayisi.setText("Boş Koltuk Sayısı: -");
        lblBosKoltukSayisi.getStyleClass().clear();
        lblBosKoltukSayisi.getStyleClass().add("info-label-dashboard");
        btnKoltukSec.setDisable(true);
        comboSeanslar.getItems().clear();
        comboSeanslar.setValue(null);
        comboSeanslar.setDisable(true); 
        comboSeanslar.setPromptText("Seans Seçiniz"); 
        seciliFilmID = null;
        seciliSeansID = null; 

        if (secilenFilmAdi == null || seciliSalonID == null) return;

        String idQuery = "SELECT FilmID FROM Filmler WHERE Baslik = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(idQuery)) {
            pstmt.setString(1, secilenFilmAdi);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                seciliFilmID = rs.getInt("FilmID");
                seanslariYukle(seciliSalonID, seciliFilmID, true); 
            }
            if (rs != null) rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Film ID alınırken hata: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void seanslariYukle(int salonID, int filmID, boolean sadeceBugunMu) {
        ObservableList<String> seansListesi = FXCollections.observableArrayList();
        comboSeanslar.getItems().clear();
        comboSeanslar.setValue(null);
        seciliSeansID = null;
        btnKoltukSec.setDisable(true);
        lblBosKoltukSayisi.setText("Boş Koltuk Sayısı: -");
        lblBosKoltukSayisi.getStyleClass().clear();
        lblBosKoltukSayisi.getStyleClass().add("info-label-dashboard");
        comboSeanslar.setPromptText("Seanslar Yükleniyor...");
        comboSeanslar.setDisable(true);

        String query;
        if (sadeceBugunMu) {
            query = "SELECT SeansID, DATE_FORMAT(SeansZamani, '%d.%m.%Y %H:%i') AS FormatliSeans " +
                    "FROM Seanslar " +
                    "WHERE SalonID = ? AND FilmID = ? AND DATE(SeansZamani) = CURDATE() AND SeansZamani >= NOW() " +
                    "ORDER BY SeansZamani";
        } else {
            query = "SELECT SeansID, DATE_FORMAT(SeansZamani, '%d.%m.%Y %H:%i') AS FormatliSeans " +
                    "FROM Seanslar " +
                    "WHERE SalonID = ? AND FilmID = ? AND SeansZamani >= NOW() " +
                    "ORDER BY SeansZamani";
        }

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, salonID);
            pstmt.setInt(2, filmID);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                seansListesi.add(rs.getString("FormatliSeans") + " (ID:" + rs.getInt("SeansID") + ")");
            }
            if (rs != null) rs.close();

            if (seansListesi.isEmpty()) {
                if (sadeceBugunMu) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Seans Bulunamadı");
                    String filmAdiPrompt = comboFilmler.getValue() != null ? comboFilmler.getValue() : "Seçili film";
                    alert.setHeaderText("'" + filmAdiPrompt + "' filmi için BUGÜN uygun seans bulunamadı.");
                    alert.setContentText("İleriki tarihlerdeki seansları görmek ister misiniz?");

                    ButtonType btnIleriTarih = new ButtonType("İleriki Tarihleri Göster");
                    ButtonType btnVazgec = new ButtonType("Vazgeç", ButtonBar.ButtonData.CANCEL_CLOSE);
                    alert.getButtonTypes().setAll(btnIleriTarih, btnVazgec);

                    Stage stage = (Stage) comboSeanslar.getScene().getWindow();
                    alert.initOwner(stage);

                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == btnIleriTarih) {
                        seanslariYukle(salonID, filmID, false);
                    } else {
                        comboSeanslar.setPromptText("Bugün seans yok");
                    }
                } else {
                    String filmAdiPrompt = comboFilmler.getValue() != null ? comboFilmler.getValue() : "Seçili film";
                    showAlert("Seans Yok", "'" + filmAdiPrompt + "' için bu salonda hiç aktif seans bulunamadı.", Alert.AlertType.WARNING);
                    comboSeanslar.setPromptText("Aktif seans yok");
                }
            } else {
                comboSeanslar.setItems(seansListesi);
                comboSeanslar.setDisable(false);
                comboSeanslar.setPromptText("Seans Seçiniz");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Seanslar yüklenirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
            comboSeanslar.setPromptText("Seans yüklenemedi");
        }
    }

    private int getSalonKapasitesi(int salonID) {
        String query = "SELECT Kapasite FROM SinemaSalonlari WHERE SalonID = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, salonID);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("Kapasite");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Salon kapasitesi alınırken hata: " + e.getMessage(), Alert.AlertType.ERROR);
        }
        return 15; //HATA OLURSA 15 DEĞERİ DÖNECEKK
    }

    private void seansSecildi() {
        String secilenSeansStr = comboSeanslar.getValue();
        lblBosKoltukSayisi.getStyleClass().clear();
        lblBosKoltukSayisi.getStyleClass().add("info-label-dashboard"); 
        lblBosKoltukSayisi.setText("Boş Koltuk Sayısı: -");
        btnKoltukSec.setDisable(true);
        seciliSeansID = null;

        if (secilenSeansStr == null) {
            return;
        }

        try {
            int idStartIndex = secilenSeansStr.indexOf("(ID:") + 4;
            int idEndIndex = secilenSeansStr.lastIndexOf(")");
            if (idStartIndex > 3 && idEndIndex > idStartIndex) {
                 seciliSeansID = Integer.parseInt(secilenSeansStr.substring(idStartIndex, idEndIndex));
            } else {
                 System.err.println("SeansID formatı bozuk: " + secilenSeansStr);
                return;
            }
        } catch (NumberFormatException e) {
            System.err.println("SeansID ayrıştırılamadı (NumberFormatException): " + secilenSeansStr + " - " + e.getMessage());
            return;
        } catch (Exception e) {
            System.err.println("SeansID ayrıştırılırken genel hata: " + secilenSeansStr + " - " + e.getMessage());
            e.printStackTrace();
            return;
        }

        if (seciliSeansID != null && seciliSalonID != null) {
            int salonKapasitesi = getSalonKapasitesi(seciliSalonID); 

            String countQuery = "SELECT COUNT(*) AS DoluKoltukSayisi FROM Rezervasyonlar WHERE SeansID = ? AND IptalEdildiMi = 0";            try (Connection conn = DbHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(countQuery)) {
                pstmt.setInt(1, seciliSeansID);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    int doluKoltuk = rs.getInt("DoluKoltukSayisi");
                    int bosKoltuk = salonKapasitesi - doluKoltuk;


                    lblBosKoltukSayisi.getStyleClass().clear();

                    if (bosKoltuk <= 0) {
                        lblBosKoltukSayisi.setText("Tüm Koltuklar Dolu!");
                        lblBosKoltukSayisi.getStyleClass().add("info-label-no-seats");
                        btnKoltukSec.setDisable(true);
                    showAlert("Bilgi", "Bu seansta boş koltuk bulunmamaktadır.", Alert.AlertType.INFORMATION);
                    } else if (bosKoltuk <= 5) { 
                        lblBosKoltukSayisi.setText("Boş Koltuk: " + bosKoltuk + " (Az Kaldı!)");
                        lblBosKoltukSayisi.getStyleClass().add("info-label-low-seats");
                        btnKoltukSec.setDisable(false);
                    } else {
                        lblBosKoltukSayisi.setText("Boş Koltuk Sayısı: " + bosKoltuk);
                        lblBosKoltukSayisi.getStyleClass().add("info-label-dashboard"); 
                        btnKoltukSec.setDisable(false);
                    }
                }
                if (rs != null) rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
                lblBosKoltukSayisi.setText("Boş Koltuk: Hata!");
                lblBosKoltukSayisi.getStyleClass().clear(); 
                lblBosKoltukSayisi.getStyleClass().add("info-label-no-seats"); 
                showAlert("Veritabanı Hatası", "Boş koltuk sayısı alınırken hata: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }


    @FXML
    private void handleKoltukSecimi() {
        if (seciliSeansID == null || seciliFilmID == null || seciliSalonID == null) {
            showAlert("Eksik Seçim", "Lütfen film, salon ve seans seçiminizi tamamlayın.", Alert.AlertType.WARNING);
            return;
        }
        if (!UserSession.getInstance().isLoggedIn()){
            showAlert("Giriş Gerekli", "Bilet alabilmek için lütfen giriş yapınız.", Alert.AlertType.WARNING);
            
            return;
        }

        String fxmlPath = "/application/KoltukSecimEkranı.fxml";
        URL fxmlUrl = getClass().getResource(fxmlPath);

        if (fxmlUrl == null) {
             showAlert("Dosya Hatası", "'" + fxmlPath + "' dosyası bulunamadı.", Alert.AlertType.ERROR);
             return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            KoltukSecimController controller = loader.getController();

            String filmAdiParam = comboFilmler.getValue() != null ? comboFilmler.getValue() : "Bilinmeyen Film";
            String salonAdiParam = comboSalonlar.getValue() != null ? comboSalonlar.getValue() : "Bilinmeyen Salon";
            String seansZamaniParam = comboSeanslar.getValue(); 

            if (seansZamaniParam != null && seansZamaniParam.contains("(ID:")) {
                seansZamaniParam = seansZamaniParam.substring(0, seansZamaniParam.indexOf("(ID:")).trim();
            } else if (seansZamaniParam == null) {
                seansZamaniParam = "Bilinmeyen Seans";
            }

            controller.initData(seciliSeansID, seciliFilmID, seciliSalonID, filmAdiParam, salonAdiParam, seansZamaniParam);

            Stage stage = new Stage();
            stage.setTitle("Koltuk Seçimi ve Bilet Alma");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            
            Stage anaPencere = (Stage) lblHosgeldin.getScene().getWindow();
            if (anaPencere != null) {
                stage.initOwner(anaPencere);
            }

            stage.setOnHidden(event -> {
                // Koltuk seçimi ekranı kapandıktan sonra boş koltuk sayısını güncelle
                if (comboSeanslar.getValue() != null) { // Hala bir seans seçiliyse
                    seansSecildi(); // Boş koltuk sayısını ve buton durumunu yeniden değerlendir
                }
            });
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Beklenmedik Hata", "Koltuk seçimi ekranı açılırken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    private void handleMenuDashboard() {
        String fxmlPath = "/application/DashboardStatsScreen.fxml";
        URL fxmlUrl = getClass().getResource(fxmlPath);
         try {
            if (fxmlUrl == null) {
                 showAlert("Dosya Hatası", "'" + fxmlPath + "' bulunamadı.", Alert.AlertType.ERROR);
                 return;
            }
            Stage currentStage = (Stage) lblHosgeldin.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            currentStage.setScene(new Scene(root));
            currentStage.setTitle("Dashboard İstatistikleri");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Yükleme Hatası","Dashboard istatistik ekranı yüklenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleMenuAdminPaneli() {
        if (UserSession.getInstance().isLoggedIn() && UserSession.getInstance().getRol().equals("Yonetici")) {
            String fxmlPath = "/application/AdminPanel.fxml";
            URL fxmlUrl = getClass().getResource(fxmlPath);
            try {
                if (fxmlUrl == null) {
                    showAlert("Dosya Hatası", "'" + fxmlPath + "' bulunamadı.", Alert.AlertType.ERROR);
                    return;
                }
                Stage stage = (Stage) lblHosgeldin.getScene().getWindow();
                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                Parent root = loader.load();
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.setTitle("Admin Paneli");
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Yükleme Hatası","Admin paneli yüklenirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            showAlert("Yetki Hatası", "Bu alana erişim yetkiniz bulunmamaktadır.", Alert.AlertType.WARNING);
        }
    }
    @FXML
    private void handleMenuVizyondakiler() {
        try {
            Stage stage = (Stage) lblHosgeldin.getScene().getWindow(); // lblHosgeldin veya başka bir node
            Parent root = FXMLLoader.load(getClass().getResource("VizyondakiFilmler.fxml"));
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Vizyondaki Filmler");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Yükleme Hatası","Vizyondaki Filmler ekranı yüklenirken bir hata oluştu.", Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    private void handleMenuFilmAra() {
        String fxmlPath = "/application/FilmAramaEkrani.fxml";
        URL fxmlUrl = getClass().getResource(fxmlPath);
        try {
            if (fxmlUrl == null) {
                showAlert("Dosya Hatası", "'" + fxmlPath + "' bulunamadı.", Alert.AlertType.ERROR);
                return;
            }
            Stage stage = (Stage) lblHosgeldin.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("İle Göre Film Arama");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Yükleme Hatası", "Film Arama ekranı yüklenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleMenuBiletlerim() {
        if (!UserSession.getInstance().isLoggedIn()) {
            showAlert("Giriş Gerekli", "Biletlerinizi görmek için lütfen giriş yapınız.", Alert.AlertType.WARNING);
            return;
        }

        try {
            Stage stage = (Stage) lblHosgeldin.getScene().getWindow(); 
            String fxmlPath = "/application/BiletlerimEkrani.fxml";
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                showAlert("Dosya Hatası", "'" + fxmlPath + "' bulunamadı.", Alert.AlertType.ERROR);
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Biletlerim");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Yükleme Hatası", "Biletlerim ekranı yüklenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleMenuCikis() {
        UserSession.getInstance().clearSession();
        String fxmlPath = "/application/Sample.fxml";
        URL fxmlUrl = getClass().getResource(fxmlPath);
        try {
             if (fxmlUrl == null) {
                 showAlert("Dosya Hatası", "'" + fxmlPath + "' (Giriş Ekranı) bulunamadı.", Alert.AlertType.ERROR);
                 return;
            }
            Stage stage = (Stage) lblHosgeldin.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Giriş Ekranı");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Yükleme Hatası","Çıkış yapılırken hata: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleGeri() { 
        comboIller.setValue(null); 
 
    }
    
    @FXML
    private void handleUygulamayiKapat() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Uygulamadan Çıkış");
        alert.setHeaderText("Uygulamayı kapatmak üzeresiniz.");
        alert.setContentText("Çıkmak istediğinizden emin misiniz?");

        Stage stage = (Stage) lblHosgeldin.getScene().getWindow();
        if (stage != null) { //SAHNE NULL MU
            alert.initOwner(stage);
        }

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Platform.exit();
            System.exit(0); 
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (lblHosgeldin != null && lblHosgeldin.getScene() != null && lblHosgeldin.getScene().getWindow() != null) {
             alert.initOwner(lblHosgeldin.getScene().getWindow());
        }
        alert.showAndWait();
    }
}