// application/SalonYonetimiController.java
package application;

import application.model.Il; 
import application.model.Salon;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class SalonYonetimiController {

    @FXML
    private TableView<Salon> tableViewSalonlar;
    @FXML
    private TableColumn<Salon, Integer> colSalonID;
    @FXML
    private TableColumn<Salon, String> colSalonAdi;
    @FXML
    private TableColumn<Salon, String> colSehirAdi; 
    @FXML
    private TableColumn<Salon, Integer> colKapasite;

    @FXML
    private ComboBox<Il> comboSehirler; 
    @FXML
    private TextField txtSalonAdi;
 

    @FXML
    private Button btnEkle;
    @FXML
    private Button btnGuncelle;
    @FXML
    private Button btnSil;
    @FXML
    private Button btnTemizle;
    @FXML
    private Button btnKapat;

    private ObservableList<Salon> salonListesi = FXCollections.observableArrayList();
    private ObservableList<Il> ilComboListesi = FXCollections.observableArrayList();
    private Salon seciliSalon = null;

    public void initialize() {
        colSalonID.setCellValueFactory(new PropertyValueFactory<>("salonID"));
        colSalonAdi.setCellValueFactory(new PropertyValueFactory<>("salonAdi"));
        colSehirAdi.setCellValueFactory(new PropertyValueFactory<>("sehirAdi")); // Modeldeki sehirAdi'na bağlanır
        colKapasite.setCellValueFactory(new PropertyValueFactory<>("kapasite"));

        comboSehirler.setConverter(new StringConverter<Il>() {
            @Override
            public String toString(Il il) {
                return il == null ? null : il.getSehirAdi();
            }

            @Override
            public Il fromString(String string) {
                return comboSehirler.getItems().stream().filter(il ->
                        il.getSehirAdi().equals(string)).findFirst().orElse(null);
            }
        });

        illeriYukleComboBox();
        salonlariYukle(); 

        tableViewSalonlar.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                seciliSalon = newSelection;
                txtSalonAdi.setText(seciliSalon.getSalonAdi());
                for (Il il : comboSehirler.getItems()) {
                    if (il.getSehirID() == seciliSalon.getSehirID()) {
                        comboSehirler.setValue(il);
                        break;
                    }
                }
                btnGuncelle.setDisable(false);
                btnSil.setDisable(false);
            } else {
                handleTemizleForm();
            }
        });
        handleTemizleForm(); 
    }

    private void illeriYukleComboBox() {
        ilComboListesi.clear();
        String query = "SELECT SehirID, SehirAdi FROM Sehirler ORDER BY SehirAdi";
        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                ilComboListesi.add(new Il(rs.getInt("SehirID"), rs.getString("SehirAdi")));
            }
            comboSehirler.setItems(ilComboListesi);
        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "Şehirler yüklenirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void salonlariYukle() {
        salonListesi.clear();
        String query = "SELECT s.SalonID, s.SalonAdi, s.SehirID, c.SehirAdi, s.Kapasite " +
                       "FROM SinemaSalonlari s " +
                       "JOIN Sehirler c ON s.SehirID = c.SehirID " +
                       "ORDER BY c.SehirAdi, s.SalonAdi";

        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                salonListesi.add(new Salon(
                        rs.getInt("SalonID"),
                        rs.getString("SalonAdi"),
                        rs.getInt("SehirID"),
                        rs.getString("SehirAdi"),
                        rs.getInt("Kapasite")));
            }
            tableViewSalonlar.setItems(salonListesi);

        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "Salonlar yüklenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEkleSalon() {
        String yeniSalonAdi = txtSalonAdi.getText().trim();
        Il secilenIl = comboSehirler.getValue();

        if (yeniSalonAdi.isEmpty()) {
            showAlert("Giriş Hatası", "Salon adı boş bırakılamaz.", Alert.AlertType.WARNING);
            return;
        }
        if (secilenIl == null) {
            showAlert("Giriş Hatası", "Lütfen bir şehir seçin.", Alert.AlertType.WARNING);
            return;
        }

        String checkQuery = "SELECT COUNT(*) FROM SinemaSalonlari WHERE SalonAdi = ? AND SehirID = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement checkPstmt = conn.prepareStatement(checkQuery)) {
            checkPstmt.setString(1, yeniSalonAdi);
            checkPstmt.setInt(2, secilenIl.getSehirID());
            ResultSet rs = checkPstmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                showAlert("Hata", "'" + secilenIl.getSehirAdi() + "' şehrinde '" + yeniSalonAdi + "' adında bir salon zaten mevcut.", Alert.AlertType.ERROR);
                return;
            }
        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "Salon kontrol edilirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
            return;
        }


        String query = "INSERT INTO SinemaSalonlari (SalonAdi, SehirID, Kapasite, EkleyenKullaniciID) VALUES (?, ?, ?, ?)";
        final int KAPASITE_SABIT = 15;

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, yeniSalonAdi);
            pstmt.setInt(2, secilenIl.getSehirID());
            pstmt.setInt(3, KAPASITE_SABIT);
            if (UserSession.getInstance().isLoggedIn()) {
                pstmt.setInt(4, UserSession.getInstance().getKullaniciID());
            } else {
                pstmt.setNull(4, java.sql.Types.INTEGER);
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                showAlert("Başarılı", "Salon başarıyla eklendi.", Alert.AlertType.INFORMATION);
                salonlariYukle();
                handleTemizleForm();
            }
        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "Salon eklenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGuncelleSalon() {
        if (seciliSalon == null) {
            showAlert("Hata", "Güncellenecek bir salon seçilmedi.", Alert.AlertType.WARNING);
            return;
        }
        String guncelSalonAdi = txtSalonAdi.getText().trim();
        Il secilenIlCombo = comboSehirler.getValue();

        if (guncelSalonAdi.isEmpty()) {
            showAlert("Giriş Hatası", "Salon adı boş bırakılamaz.", Alert.AlertType.WARNING);
            return;
        }
        if (secilenIlCombo == null) {
            showAlert("Giriş Hatası", "Lütfen bir şehir seçin.", Alert.AlertType.WARNING);
            return;
        }

        // Eğer isim ve şehir değişmediyse uyarı
        if (seciliSalon.getSalonAdi().equalsIgnoreCase(guncelSalonAdi) && seciliSalon.getSehirID() == secilenIlCombo.getSehirID()) {
            showAlert("Bilgi", "Salon adı veya şehirde bir değişiklik yapılmadı.", Alert.AlertType.INFORMATION);
            return;
        }
        
        // Yeni isim ve yeni şehir kombinasyonuyla başka bir salon var mı kontrolü
        String checkQuery = "SELECT COUNT(*) FROM SinemaSalonlari WHERE SalonAdi = ? AND SehirID = ? AND SalonID != ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement checkPstmt = conn.prepareStatement(checkQuery)) {
            checkPstmt.setString(1, guncelSalonAdi);
            checkPstmt.setInt(2, secilenIlCombo.getSehirID());
            checkPstmt.setInt(3, seciliSalon.getSalonID());
            ResultSet rs = checkPstmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                showAlert("Hata", "'" + secilenIlCombo.getSehirAdi() + "' şehrinde '" + guncelSalonAdi + "' adında başka bir salon zaten mevcut.", Alert.AlertType.ERROR);
                return;
            }
        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "Salon kontrol edilirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
            return;
        }

        String query = "UPDATE SinemaSalonlari SET SalonAdi = ?, SehirID = ? WHERE SalonID = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, guncelSalonAdi);
            pstmt.setInt(2, secilenIlCombo.getSehirID());
            pstmt.setInt(3, seciliSalon.getSalonID());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                showAlert("Başarılı", "Salon başarıyla güncellendi.", Alert.AlertType.INFORMATION);
                salonlariYukle();
                handleTemizleForm();
            }
        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "Salon güncellenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSilSalon() {
        if (seciliSalon == null) {
            showAlert("Hata", "Silinecek bir salon seçilmedi.", Alert.AlertType.WARNING);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Silme Onayı");
        alert.setHeaderText(null);
        alert.setContentText(seciliSalon.getSalonAdi() + " adlı salonu silmek istediğinizden emin misiniz?\nBu salona bağlı seanslar ve rezervasyonlar da silinecektir (ON DELETE CASCADE).");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
          
            String query = "DELETE FROM SinemaSalonlari WHERE SalonID = ?";
            try (Connection conn = DbHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setInt(1, seciliSalon.getSalonID());
                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    showAlert("Başarılı", "Salon ve bağlı kayıtlar başarıyla silindi.", Alert.AlertType.INFORMATION);
                    salonlariYukle();
                    handleTemizleForm();
                }
            } catch (SQLException e) {
                showAlert("Veritabanı Hatası", "Salon silinirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleTemizleForm() {
        tableViewSalonlar.getSelectionModel().clearSelection();
        txtSalonAdi.clear();
        comboSehirler.getSelectionModel().clearSelection();
        seciliSalon = null;
        btnGuncelle.setDisable(true);
        btnSil.setDisable(true);
        btnEkle.setDisable(false);
    }

    @FXML
    private void handleKapat() {
        Stage stage = (Stage) btnKapat.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}