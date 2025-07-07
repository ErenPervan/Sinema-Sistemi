// application/KullaniciYonetimiController.java
package application;

import application.model.Kullanici;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class KullaniciYonetimiController {

    @FXML
    private TableView<Kullanici> tableViewKullanicilar;
    @FXML
    private TableColumn<Kullanici, Integer> colKullaniciID;
    @FXML
    private TableColumn<Kullanici, String> colKullaniciAdi;
    @FXML
    private TableColumn<Kullanici, String> colAd;
    @FXML
    private TableColumn<Kullanici, String> colSoyad;
    @FXML
    private TableColumn<Kullanici, String> colEposta;
    @FXML
    private TableColumn<Kullanici, String> colRol;

    @FXML
    private TextField txtKullaniciAdi;
    @FXML
    private PasswordField txtSifre;
    @FXML
    private TextField txtAd;
    @FXML
    private TextField txtSoyad;
    @FXML
    private TextField txtEposta;
    @FXML
    private ComboBox<String> comboRol;

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
    @FXML
    private Label lblSifreNotu;


    private ObservableList<Kullanici> kullaniciListesi = FXCollections.observableArrayList();
    private Kullanici seciliKullanici = null;

    public void initialize() {
        colKullaniciID.setCellValueFactory(new PropertyValueFactory<>("kullaniciID"));
        colKullaniciAdi.setCellValueFactory(new PropertyValueFactory<>("kullaniciAdi"));
        colAd.setCellValueFactory(new PropertyValueFactory<>("ad"));
        colSoyad.setCellValueFactory(new PropertyValueFactory<>("soyad"));
        colEposta.setCellValueFactory(new PropertyValueFactory<>("eposta"));
        colRol.setCellValueFactory(new PropertyValueFactory<>("rol"));

        comboRol.setItems(FXCollections.observableArrayList("Yonetici", "Kullanici"));

        kullanicilariYukle();

        tableViewKullanicilar.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                seciliKullanici = newSelection;
                doldurForm(seciliKullanici);
                btnGuncelle.setDisable(false);
                btnSil.setDisable(false);
                btnEkle.setDisable(true);
                lblSifreNotu.setText("(Şifreyi değiştirmek istemiyorsanız bu alanı boş bırakın)");
            } else {
                handleTemizleForm();
            }
        });
        handleTemizleForm();
    }

    private void kullanicilariYukle() {
        kullaniciListesi.clear();
        String query = "SELECT KullaniciID, KullaniciAdi, Rol, Ad, Soyad, Eposta FROM Kullanicilar ORDER BY KullaniciAdi";

        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                kullaniciListesi.add(new Kullanici(
                        rs.getInt("KullaniciID"),
                        rs.getString("KullaniciAdi"),
                        rs.getString("Rol"),
                        rs.getString("Ad"),
                        rs.getString("Soyad"),
                        rs.getString("Eposta")
                ));
            }
            tableViewKullanicilar.setItems(kullaniciListesi);

        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "Kullanıcılar yüklenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void doldurForm(Kullanici kullanici) {
        if (kullanici == null) return;
        txtKullaniciAdi.setText(kullanici.getKullaniciAdi());
        txtSifre.clear();
        txtAd.setText(kullanici.getAd());
        txtSoyad.setText(kullanici.getSoyad());
        txtEposta.setText(kullanici.getEposta());
        comboRol.setValue(kullanici.getRol());
    }

    @FXML
    private void handleEkleKullanici() {
        String kullaniciAdi = txtKullaniciAdi.getText().trim();
        String sifre = txtSifre.getText();
        String ad = txtAd.getText().trim();
        String soyad = txtSoyad.getText().trim();
        String eposta = txtEposta.getText().trim();
        String rol = comboRol.getValue();

        if (kullaniciAdi.isEmpty() || sifre.isEmpty() || rol == null) {
            showAlert("Giriş Hatası", "Kullanıcı adı, şifre ve rol boş bırakılamaz.", Alert.AlertType.WARNING);
            return;
        }

        String checkUserQuery = "SELECT COUNT(*) FROM Kullanicilar WHERE KullaniciAdi = ? OR (Eposta IS NOT NULL AND Eposta = ? AND Eposta != '')";
        try(Connection conn = DbHelper.getConnection(); PreparedStatement pstmt = conn.prepareStatement(checkUserQuery)){
            pstmt.setString(1, kullaniciAdi);
            pstmt.setString(2, eposta.isEmpty() ? null : eposta); 
            ResultSet rs = pstmt.executeQuery();
            if(rs.next() && rs.getInt(1) > 0){
                showAlert("Hata", "Bu kullanıcı adı veya e-posta adresi zaten kullanılıyor.", Alert.AlertType.ERROR);
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

        String query = "INSERT INTO Kullanicilar (KullaniciAdi, SifreHash, Rol, Ad, Soyad, Eposta, KayitTarihi) " +
                       "VALUES (?, ?, ?, ?, ?, ?, CURDATE())";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, kullaniciAdi);
            pstmt.setString(2, sifreHash);
            pstmt.setString(3, rol);
            pstmt.setString(4, ad.isEmpty() ? null : ad);
            pstmt.setString(5, soyad.isEmpty() ? null : soyad);
            pstmt.setString(6, eposta.isEmpty() ? null : eposta);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                showAlert("Başarılı", "Kullanıcı başarıyla eklendi.", Alert.AlertType.INFORMATION);
                kullanicilariYukle();
                handleTemizleForm();
            }
        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "Kullanıcı eklenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGuncelleKullanici() {
        if (seciliKullanici == null) {
            showAlert("Hata", "Güncellenecek bir kullanıcı seçilmedi.", Alert.AlertType.WARNING);
            return;
        }

        String kullaniciAdi = txtKullaniciAdi.getText().trim();
        String yeniSifre = txtSifre.getText();
        String ad = txtAd.getText().trim();
        String soyad = txtSoyad.getText().trim();
        String eposta = txtEposta.getText().trim();
        String rol = comboRol.getValue();

        if (kullaniciAdi.isEmpty() || rol == null) {
            showAlert("Giriş Hatası", "Kullanıcı adı ve rol boş bırakılamaz.", Alert.AlertType.WARNING);
            return;
        }
        
        String checkUserQuery = "SELECT COUNT(*) FROM Kullanicilar WHERE (KullaniciAdi = ? OR (Eposta IS NOT NULL AND Eposta = ? AND Eposta != '')) AND KullaniciID != ?";
        try(Connection conn = DbHelper.getConnection(); PreparedStatement pstmt = conn.prepareStatement(checkUserQuery)){
            pstmt.setString(1, kullaniciAdi);
            pstmt.setString(2, eposta.isEmpty() ? null : eposta);
            pstmt.setInt(3, seciliKullanici.getKullaniciID());
            ResultSet rs = pstmt.executeQuery();
            if(rs.next() && rs.getInt(1) > 0){
                showAlert("Hata", "Güncellemeye çalıştığınız kullanıcı adı veya e-posta adresi zaten başka bir kullanıcı tarafından kullanılıyor.", Alert.AlertType.ERROR);
                 if(rs!=null) rs.close();
                return;
            }
            if(rs!=null) rs.close();
        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "Kullanıcı kontrolü sırasında hata: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
            return;
        }

        StringBuilder queryBuilder = new StringBuilder("UPDATE Kullanicilar SET KullaniciAdi = ?, Rol = ?, Ad = ?, Soyad = ?, Eposta = ?");
        boolean sifreGuncellenecek = !yeniSifre.isEmpty();
        String sifreHashToUpdate = null;

        if (sifreGuncellenecek) {
            try {
                sifreHashToUpdate = AuthHelper.hashPasswordSHA256(yeniSifre);
                queryBuilder.append(", SifreHash = ?");
            } catch (IllegalArgumentException e) {
                showAlert("Şifre Hatası", e.getMessage(), Alert.AlertType.ERROR);
                return;
            } catch (RuntimeException e) {
                showAlert("Güvenlik Yapılandırma Hatası", "Şifreleme algoritmasıyla ilgili bir sorun oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
                return;
            }
        }
        queryBuilder.append(" WHERE KullaniciID = ?");

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(queryBuilder.toString())) {

            int paramIndex = 1;
            pstmt.setString(paramIndex++, kullaniciAdi);
            pstmt.setString(paramIndex++, rol);
            pstmt.setString(paramIndex++, ad.isEmpty() ? null : ad);
            pstmt.setString(paramIndex++, soyad.isEmpty() ? null : soyad);
            pstmt.setString(paramIndex++, eposta.isEmpty() ? null : eposta);

            if (sifreGuncellenecek && sifreHashToUpdate != null) {
                pstmt.setString(paramIndex++, sifreHashToUpdate);
            }
            pstmt.setInt(paramIndex++, seciliKullanici.getKullaniciID());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                showAlert("Başarılı", "Kullanıcı başarıyla güncellendi.", Alert.AlertType.INFORMATION);
                kullanicilariYukle();
                handleTemizleForm();
            }
        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "Kullanıcı güncellenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleSilKullanici() {
        if (seciliKullanici == null) {
            showAlert("Hata", "Silinecek bir kullanıcı seçilmedi.", Alert.AlertType.WARNING);
            return;
        }
        if (UserSession.getInstance().isLoggedIn() && UserSession.getInstance().getKullaniciID() == seciliKullanici.getKullaniciID()){
            showAlert("Hata", "Kendi kendinizi silemezsiniz.", Alert.AlertType.ERROR);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Kullanıcı Silme Onayı");
        alert.setHeaderText(null);
        // Güncellenmiş Onay Mesajı
        alert.setContentText(seciliKullanici.getKullaniciAdi() + " adlı kullanıcıyı silmek istediğinizden emin misiniz?\n\n" +
                             "BU İŞLEM KULLANICIYA AİT TÜM REZERVASYONLARI DA OTOMATİK OLARAK SİLECEKTİR!\n" +
                             "(Kullanıcının eklediği filmler, seanslar vb. diğer bilgiler silinmeyecek, sadece 'ekleyen kullanıcı' bağlantısı kaldırılacaktır.)\n\n" +
                             "Devam etmek istiyor musunuz?");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            String query = "DELETE FROM Kullanicilar WHERE KullaniciID = ?";
            try (Connection conn = DbHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setInt(1, seciliKullanici.getKullaniciID());
                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    // Güncellenmiş Başarı Mesajı
                    showAlert("Başarılı", "Kullanıcı ve ilişkili tüm rezervasyonları başarıyla silindi.", Alert.AlertType.INFORMATION);
                    kullanicilariYukle(); // Kullanıcı listesini yenile
                    handleTemizleForm();  // Formu temizle
                } else {
                    // Bu durum normalde kullanıcı listede varsa ve seçilebiliyorsa yaşanmamalı.
                    showAlert("Hata", "Kullanıcı silinemedi (kayıt bulunamadı).", Alert.AlertType.WARNING);
                }
            } catch (SQLException e) {
                // Veritabanı ON DELETE CASCADE yaptığı için rezervasyonlardan kaynaklı 1451 hatası artık gelmemeli.
                // Ancak başka bir tablo Kullanicilar'a ON DELETE RESTRICT ile bağlıysa o zaman hata alınabilir.
                showAlert("Veritabanı Hatası", "Kullanıcı silinirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        }
    }

    
    @FXML
    private void handleTemizleForm() {
        tableViewKullanicilar.getSelectionModel().clearSelection();
        txtKullaniciAdi.clear();
        txtSifre.clear();
        txtAd.clear();
        txtSoyad.clear();
        txtEposta.clear();
        comboRol.getSelectionModel().clearSelection();
        seciliKullanici = null;
        btnGuncelle.setDisable(true);
        btnSil.setDisable(true);
        btnEkle.setDisable(false);
        lblSifreNotu.setText("(Şifre alanı sadece yeni kullanıcı eklerken veya şifre güncellenmek istendiğinde doldurulur)");
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