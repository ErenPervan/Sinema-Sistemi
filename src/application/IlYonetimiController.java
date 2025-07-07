// application/IlYonetimiController.java
package application;

import application.model.Il; 
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class IlYonetimiController {

    @FXML
    private TableView<Il> tableViewIller;
    @FXML
    private TableColumn<Il, Integer> colIlID;
    @FXML
    private TableColumn<Il, String> colIlAdi;
    @FXML
    private TextField txtIlAdi;
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

    private ObservableList<Il> ilListesi = FXCollections.observableArrayList();
    private Il seciliIl = null;

    public void initialize() {
        colIlID.setCellValueFactory(new PropertyValueFactory<>("sehirID"));
        colIlAdi.setCellValueFactory(new PropertyValueFactory<>("sehirAdi"));

        illeriYukle();

        // TableView'da bir satır seçildiğinde formu doldur
        tableViewIller.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                seciliIl = newSelection;
                txtIlAdi.setText(seciliIl.getSehirAdi());
                btnGuncelle.setDisable(false);
                btnSil.setDisable(false);
            } else {
            	handleTemizleForm();
            }
        });
        handleTemizleForm(); // Başlangıçta butonları ayarla
    }

    private void illeriYukle() {
        ilListesi.clear();
        String query = "SELECT SehirID, SehirAdi FROM Sehirler ORDER BY SehirAdi";

        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                ilListesi.add(new Il(rs.getInt("SehirID"), rs.getString("SehirAdi")));
            }
            tableViewIller.setItems(ilListesi);

        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "İller yüklenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEkleIl() {
        String yeniIlAdi = txtIlAdi.getText().trim();
        if (yeniIlAdi.isEmpty()) {
            showAlert("Giriş Hatası", "İl adı boş bırakılamaz.", Alert.AlertType.WARNING);
            return;
        }

        for (Il il : ilListesi) {
            if (il.getSehirAdi().equalsIgnoreCase(yeniIlAdi)) {
                showAlert("Hata", "Bu isimde bir il zaten mevcut.", Alert.AlertType.ERROR);
                return;
            }
        }

        String query = "INSERT INTO Sehirler (SehirAdi, EkleyenKullaniciID) VALUES (?, ?)";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, yeniIlAdi);
            // EkleyenKullaniciID UserSession'dan alınabilir
            if (UserSession.getInstance().isLoggedIn()) {
                pstmt.setInt(2, UserSession.getInstance().getKullaniciID());
            } else {
                pstmt.setNull(2, java.sql.Types.INTEGER); 
            }


            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                // Eklenen ilin ID'sini almak için 
                // ResultSet generatedKeys = pstmt.getGeneratedKeys();
                // if (generatedKeys.next()) {
                // int yeniId = generatedKeys.getInt(1);
                // }
                showAlert("Başarılı", "İl başarıyla eklendi.", Alert.AlertType.INFORMATION);
                illeriYukle(); // Listeyi yenile
                handleTemizleForm();
            }
        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "İl eklenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGuncelleIl() {
        if (seciliIl == null) {
            showAlert("Hata", "Güncellenecek bir il seçilmedi.", Alert.AlertType.WARNING);
            return;
        }
        String guncelIlAdi = txtIlAdi.getText().trim();
        if (guncelIlAdi.isEmpty()) {
            showAlert("Giriş Hatası", "İl adı boş bırakılamaz.", Alert.AlertType.WARNING);
            return;
        }

        if (seciliIl.getSehirAdi().equalsIgnoreCase(guncelIlAdi)) {
             showAlert("Bilgi", "İl adında bir değişiklik yapılmadı.", Alert.AlertType.INFORMATION);
             return;
        }
        
        // Yeni isimle başka bir il var mı kontrolü
        for (Il il : ilListesi) {
            if (il.getSehirAdi().equalsIgnoreCase(guncelIlAdi) && il.getSehirID() != seciliIl.getSehirID()) {
                showAlert("Hata", "Bu isimde başka bir il zaten mevcut.", Alert.AlertType.ERROR);
                return;
            }
        }


        String query = "UPDATE Sehirler SET SehirAdi = ? WHERE SehirID = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, guncelIlAdi);
            pstmt.setInt(2, seciliIl.getSehirID());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                showAlert("Başarılı", "İl başarıyla güncellendi.", Alert.AlertType.INFORMATION);
                illeriYukle(); // Listeyi yenile
                handleTemizleForm();
            }
        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "İl güncellenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSilIl() {
        if (seciliIl == null) {
            showAlert("Hata", "Silinecek bir il seçilmedi.", Alert.AlertType.WARNING);
            return;
        }

        // Silme onayı
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Silme Onayı");
        alert.setHeaderText(null);
        alert.setContentText(seciliIl.getSehirAdi() + " adlı ili silmek istediğinizden emin misiniz?\nBu ile bağlı salonlar da silinecektir (ON DELETE CASCADE nedeniyle).");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {

            String query = "DELETE FROM Sehirler WHERE SehirID = ?";
            try (Connection conn = DbHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setInt(1, seciliIl.getSehirID());
                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    showAlert("Başarılı", "İl ve bağlı salonlar başarıyla silindi.", Alert.AlertType.INFORMATION);
                    illeriYukle(); // Listeyi yenile
                    handleTemizleForm();
                }
            } catch (SQLException e) {
         
                showAlert("Veritabanı Hatası", "İl silinirken bir hata oluştu: " + e.getMessage() + "\nBu ile bağlı kayıtlar (salonlar vb.) olabilir.", Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleTemizleForm() {
        tableViewIller.getSelectionModel().clearSelection();
        txtIlAdi.clear();
        seciliIl = null;
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