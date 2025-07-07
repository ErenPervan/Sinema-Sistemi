// application/TurYonetimiController.java
package application;

import application.model.Tur;
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

public class TurYonetimiController {

    @FXML
    private TableView<Tur> tableViewTurler;
    @FXML
    private TableColumn<Tur, Integer> colTurID;
    @FXML
    private TableColumn<Tur, String> colTurAdi;
    @FXML
    private TextField txtTurAdi;
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

    private ObservableList<Tur> turListesi = FXCollections.observableArrayList();
    private Tur seciliTur = null;

    public void initialize() {
        colTurID.setCellValueFactory(new PropertyValueFactory<>("turID"));
        colTurAdi.setCellValueFactory(new PropertyValueFactory<>("turAdi"));

        turleriYukle();

        tableViewTurler.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                seciliTur = newSelection;
                txtTurAdi.setText(seciliTur.getTurAdi());
                btnGuncelle.setDisable(false);
                btnSil.setDisable(false);
                btnEkle.setDisable(true);
            } else {
                handleTemizleForm();
            }
        });
        handleTemizleForm();
    }

    private void turleriYukle() {
        turListesi.clear();
        String query = "SELECT TurID, TurAdi FROM Turler ORDER BY TurAdi"; 

        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                turListesi.add(new Tur(rs.getInt("TurID"), rs.getString("TurAdi")));
            }
            tableViewTurler.setItems(turListesi);

        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "Film türleri yüklenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEkleTur() {
        String yeniTurAdi = txtTurAdi.getText().trim();
        if (yeniTurAdi.isEmpty()) {
            showAlert("Giriş Hatası", "Tür adı boş bırakılamaz.", Alert.AlertType.WARNING);
            return;
        }

        for (Tur tur : turListesi) {
            if (tur.getTurAdi().equalsIgnoreCase(yeniTurAdi)) {
                showAlert("Hata", "Bu isimde bir film türü zaten mevcut.", Alert.AlertType.ERROR);
                return;
            }
        }

        String query = "INSERT INTO Turler (TurAdi, EkleyenKullaniciID) VALUES (?, ?)";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, yeniTurAdi);
            if (UserSession.getInstance().isLoggedIn()) {
                pstmt.setInt(2, UserSession.getInstance().getKullaniciID());
            } else {
                pstmt.setNull(2, java.sql.Types.INTEGER);
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                showAlert("Başarılı", "Film türü başarıyla eklendi.", Alert.AlertType.INFORMATION);
                turleriYukle();
                handleTemizleForm();
            }
        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "Film türü eklenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGuncelleTur() {
        if (seciliTur == null) {
            showAlert("Hata", "Güncellenecek bir film türü seçilmedi.", Alert.AlertType.WARNING);
            return;
        }
        String guncelTurAdi = txtTurAdi.getText().trim();
        if (guncelTurAdi.isEmpty()) {
            showAlert("Giriş Hatası", "Tür adı boş bırakılamaz.", Alert.AlertType.WARNING);
            return;
        }
        if (seciliTur.getTurAdi().equalsIgnoreCase(guncelTurAdi)) {
             showAlert("Bilgi", "Tür adında bir değişiklik yapılmadı.", Alert.AlertType.INFORMATION);
             return;
        }
        for (Tur tur : turListesi) {
            if (tur.getTurAdi().equalsIgnoreCase(guncelTurAdi) && tur.getTurID() != seciliTur.getTurID()) {
                showAlert("Hata", "Bu isimde başka bir film türü zaten mevcut.", Alert.AlertType.ERROR);
                return;
            }
        }

        String query = "UPDATE Turler SET TurAdi = ? WHERE TurID = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, guncelTurAdi);
            pstmt.setInt(2, seciliTur.getTurID());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                showAlert("Başarılı", "Film türü başarıyla güncellendi.", Alert.AlertType.INFORMATION);
                turleriYukle();
                handleTemizleForm();
            }
        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "Film türü güncellenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }


@FXML
private void handleSilTur() {
    if (seciliTur == null) {
        showAlert("Hata", "Silinecek bir film türü seçilmedi.", Alert.AlertType.WARNING);
        return;
    }

    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Film Türü Silme Onayı");
    alert.setHeaderText(null);
    // Güncellenmiş onay mesajı
    alert.setContentText(seciliTur.getTurAdi() + " adlı film türünü silmek istediğinizden emin misiniz?\n\n" +
                         "Bu türe atanmış filmler silinmeyecek, ancak bu filmlerin tür bilgisi kaldırılacak (türleri 'Belirtilmemiş' veya 'Boş' olarak görünecektir).\n\n" +
                         "Devam etmek istiyor musunuz?");
    Optional<ButtonType> result = alert.showAndWait();

    if (result.isPresent() && result.get() == ButtonType.OK) {
        String query = "DELETE FROM Turler WHERE TurID = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, seciliTur.getTurID());
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                // Güncellenmiş başarı mesajı
                showAlert("Başarılı", "Film türü başarıyla silindi. İlişkili filmlerin tür bilgisi 'BELİRTİLMEMİŞ' olarak ayarlandı.", Alert.AlertType.INFORMATION);
                turleriYukle(); // Tür listesini yenile
                handleTemizleForm(); // Formu temizle

            } else {
                showAlert("Hata", "Film türü silinemedi (kayıt bulunamadı).", Alert.AlertType.WARNING);
            }
        } catch (SQLException e) {
            // ON DELETE SET NULL sonrası, 1451 hatası (filmlerden dolayı kısıtlama) artık bu senaryoda gelmemeli.
            showAlert("Veritabanı Hatası", "Film türü silinirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }
}

    @FXML
    private void handleTemizleForm() {
        tableViewTurler.getSelectionModel().clearSelection();
        txtTurAdi.clear();
        seciliTur = null;
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