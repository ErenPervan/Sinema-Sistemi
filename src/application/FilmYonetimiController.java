// application/FilmYonetimiController.java
package application;

import application.model.Film;
import application.model.Tur;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Optional;

public class FilmYonetimiController {

    @FXML
    private TableView<Film> tableViewFilmler;
    @FXML
    private TableColumn<Film, Integer> colFilmID;
    @FXML
    private TableColumn<Film, String> colFilmBaslik;
    @FXML
    private TableColumn<Film, String> colFilmTur; 
    @FXML
    private TableColumn<Film, Integer> colFilmSure;
    @FXML
    private TableColumn<Film, LocalDate> colFilmYayinTarihi;
    @FXML
    private TableColumn<Film, String> colFilmAciklama;

    @FXML
    private TextField txtFilmBaslik;
    @FXML
    private ComboBox<Tur> comboFilmTurleri;
    @FXML
    private TextField txtFilmSure;
    @FXML
    private DatePicker datePickerYayinTarihi;
    @FXML
    private TextField txtAfisURL;
    @FXML
    private TextArea txtFilmAciklama;
    @FXML 
    private TextField txtFragmanURL;

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

    private ObservableList<Film> filmListesi = FXCollections.observableArrayList();
    private ObservableList<Tur> turComboListesi = FXCollections.observableArrayList();
    private Film seciliFilm = null;

    public void initialize() {
        // --- HATA AYIKLAMA KONTROLÜ ---
        if (txtFragmanURL == null) {
            System.out.println("HATA AYIKLAMA (FilmYonetimiController - initialize): txtFragmanURL NULL!");
        } else {
            System.out.println("HATA AYIKLAMA (FilmYonetimiController - initialize): txtFragmanURL BAŞARIYLA ENJEKTE EDİLDİ.");
        }
        // --- HATA AYIKLAMA KONTROLÜ SONU ---

        colFilmID.setCellValueFactory(new PropertyValueFactory<>("filmID"));
        colFilmBaslik.setCellValueFactory(new PropertyValueFactory<>("baslik"));
        colFilmTur.setCellValueFactory(new PropertyValueFactory<>("turAdi")); 
        colFilmSure.setCellValueFactory(new PropertyValueFactory<>("sureDakika"));
        colFilmYayinTarihi.setCellValueFactory(new PropertyValueFactory<>("yayinTarihi"));
        colFilmAciklama.setCellValueFactory(new PropertyValueFactory<>("aciklama"));

        comboFilmTurleri.setConverter(new StringConverter<Tur>() {
            @Override
            public String toString(Tur tur) {
                return tur == null ? null : tur.getTurAdi();
            }
            @Override
            public Tur fromString(String string) { return null; } 
        });
        filmTurleriniYukleComboBox();

        filmleriYukle();

        tableViewFilmler.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                seciliFilm = newSelection;
                doldurForm(seciliFilm); 
                btnGuncelle.setDisable(false);
                btnSil.setDisable(false);
                btnEkle.setDisable(true);
            } else {
                handleTemizleForm();
            }
        });
        handleTemizleForm(); 
    }

    private void filmTurleriniYukleComboBox() {
        turComboListesi.clear();
        String query = "SELECT TurID, TurAdi FROM Turler ORDER BY TurAdi";
        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                turComboListesi.add(new Tur(rs.getInt("TurID"), rs.getString("TurAdi")));
            }
            comboFilmTurleri.setItems(turComboListesi);
        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "Film türleri yüklenirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void filmleriYukle() {
        filmListesi.clear();
        // FragmanURL'yi sorguya ekleyin
        String query = "SELECT f.FilmID, f.Baslik, f.TurID, t.TurAdi, f.SureDakika, f.Aciklama, f.YayinTarihi, f.AfisURL, f.SistemeEklenmeTarihi, f.FragmanURL " + 
                       "FROM Filmler f " +
                       "LEFT JOIN Turler t ON f.TurID = t.TurID " +
                       "ORDER BY f.Baslik";

        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Tur filmTuru = null;
                int turID = rs.getInt("TurID");
                if (!rs.wasNull()) {
                    filmTuru = new Tur(turID, rs.getString("TurAdi"));
                }

                LocalDate yayinTarihi = null;
                Date sqlYayinTarihi = rs.getDate("YayinTarihi");
                if (sqlYayinTarihi != null) {
                    yayinTarihi = sqlYayinTarihi.toLocalDate();
                }

                LocalDate sistemeEklenme = null;
                Date sqlSistemeEklenme = rs.getDate("SistemeEklenmeTarihi");
                if (sqlSistemeEklenme != null) {
                    sistemeEklenme = sqlSistemeEklenme.toLocalDate();
                }

                String fragmanURL = rs.getString("FragmanURL"); 

                filmListesi.add(new Film(
                        rs.getInt("FilmID"),
                        rs.getString("Baslik"),
                        filmTuru,
                        rs.getInt("SureDakika"),
                        rs.getString("Aciklama"),
                        yayinTarihi,
                        rs.getString("AfisURL"),
                        sistemeEklenme,
                        fragmanURL 
                ));
            }
            tableViewFilmler.setItems(filmListesi);

        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "Filmler yüklenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void doldurForm(Film film) {
        if (film == null) return;
        txtFilmBaslik.setText(film.getBaslik());
        comboFilmTurleri.setValue(film.getTur());
        txtFilmSure.setText(String.valueOf(film.getSureDakika()));
        datePickerYayinTarihi.setValue(film.getYayinTarihi());
        txtAfisURL.setText(film.getAfisURL());
        txtFilmAciklama.setText(film.getAciklama());
        txtFragmanURL.setText(film.getFragmanURL()); 
    }
    @FXML
    private void handleEkleFilm() {
        System.out.println("Debug: handleEkleFilm metodu başladı."); // BAŞLANGIÇ
        String baslik = txtFilmBaslik.getText().trim();
        Tur secilenTur = comboFilmTurleri.getValue();
        String sureStr = txtFilmSure.getText().trim();
        LocalDate yayinTarihi = datePickerYayinTarihi.getValue();
        String afisURL = txtAfisURL.getText().trim();
        String aciklama = txtFilmAciklama.getText().trim();
        String fragmanURL = txtFragmanURL.getText().trim();

        if (baslik.isEmpty() || secilenTur == null || sureStr.isEmpty()) {
            System.out.println("Debug: Giriş doğrulaması başarısız."); 
            showAlert("Giriş Hatası", "Film başlığı, türü ve süresi boş bırakılamaz.", Alert.AlertType.WARNING);
            return;
        }

        int sureDakika;
        try {
            sureDakika = Integer.parseInt(sureStr);
            if (sureDakika <= 0) {
                System.out.println("Debug: Süre pozitif değil."); 
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            System.out.println("Debug: Süre parse hatası.");
            showAlert("Giriş Hatası", "Süre geçerli bir pozitif sayı olmalıdır.", Alert.AlertType.WARNING);
            return;
        }

        String query = "INSERT INTO Filmler (Baslik, TurID, SureDakika, Aciklama, YayinTarihi, AfisURL, SistemeEklenmeTarihi, EkleyenKullaniciID, FragmanURL) " +
                       "VALUES (?, ?, ?, ?, ?, ?, CURDATE(), ?, ?)";
        System.out.println("Debug: SQL Query: " + query); 

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, baslik);
            pstmt.setInt(2, secilenTur.getTurID());
            pstmt.setInt(3, sureDakika);
            pstmt.setString(4, aciklama.isEmpty() ? null : aciklama);
            pstmt.setDate(5, yayinTarihi == null ? null : Date.valueOf(yayinTarihi));
            pstmt.setString(6, afisURL.isEmpty() ? null : afisURL);
            // SistemeEklenmeTarihi için CURDATE()
            if (UserSession.getInstance().isLoggedIn()) {
                pstmt.setInt(7, UserSession.getInstance().getKullaniciID());
            } else {
                pstmt.setNull(7, java.sql.Types.INTEGER);
            }
            pstmt.setString(8, fragmanURL.isEmpty() ? null : fragmanURL);

            System.out.println("Debug: executeUpdate çağrılmadan önce."); 
            int affectedRows = pstmt.executeUpdate();
            System.out.println("Debug: affectedRows = " + affectedRows); 

            if (affectedRows > 0) {
                System.out.println("Debug: Film başarıyla eklendi, showAlert çağrılacak.");
                showAlert("Başarılı", "Film başarıyla eklendi.", Alert.AlertType.INFORMATION);
                filmleriYukle();
                handleTemizleForm();
            } else {
         
                System.out.println("Debug: Film eklenemedi, affectedRows 0 veya daha az.");
                showAlert("İşlem Başarısız", "Film eklenirken bir sorun oluştu (etkilenen satır yok).", Alert.AlertType.WARNING);
            }
        } catch (SQLException e) {
            System.out.println("Debug: SQLException yakalandı: " + e.getMessage()); // SQL EXCEPTION KONTROLÜ
            showAlert("Veritabanı Hatası", "Film eklenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGuncelleFilm() {
        if (seciliFilm == null) {
            showAlert("Hata", "Güncellenecek bir film seçilmedi.", Alert.AlertType.WARNING);
            return;
        }

        String baslik = txtFilmBaslik.getText().trim();
        Tur secilenTur = comboFilmTurleri.getValue();
        String sureStr = txtFilmSure.getText().trim();
        LocalDate yayinTarihi = datePickerYayinTarihi.getValue();
        String afisURL = txtAfisURL.getText().trim();
        String aciklama = txtFilmAciklama.getText().trim();
        String fragmanURL = txtFragmanURL.getText().trim(); 

        if (baslik.isEmpty() || secilenTur == null || sureStr.isEmpty()) {
            showAlert("Giriş Hatası", "Film başlığı, türü ve süresi boş bırakılamaz.", Alert.AlertType.WARNING);
            return;
        }
        int sureDakika;
        try {
            sureDakika = Integer.parseInt(sureStr);
            if (sureDakika <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showAlert("Giriş Hatası", "Süre geçerli bir pozitif sayı olmalıdır.", Alert.AlertType.WARNING);
            return;
        }

        String query = "UPDATE Filmler SET Baslik = ?, TurID = ?, SureDakika = ?, Aciklama = ?, YayinTarihi = ?, AfisURL = ?, FragmanURL = ? " +
                       "WHERE FilmID = ?";

        System.out.println("DEBUG - Update SQL Query: " + query); 

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, baslik);                                
            pstmt.setInt(2, secilenTur.getTurID());                      
            pstmt.setInt(3, sureDakika);                                
            pstmt.setString(4, aciklama.isEmpty() ? null : aciklama);    
            pstmt.setDate(5, yayinTarihi == null ? null : Date.valueOf(yayinTarihi)); 
            pstmt.setString(6, afisURL.isEmpty() ? null : afisURL);      
            pstmt.setString(7, fragmanURL.isEmpty() ? null : fragmanURL);  
            pstmt.setInt(8, seciliFilm.getFilmID());                   

            System.out.println("DEBUG - Güncelleme için executeUpdate çağrılmadan önce. FilmID: " + seciliFilm.getFilmID());
            int affectedRows = pstmt.executeUpdate();
            System.out.println("DEBUG - Güncelleme sonrası affectedRows = " + affectedRows); 

            if (affectedRows > 0) {
                showAlert("Başarılı", "Film başarıyla güncellendi.", Alert.AlertType.INFORMATION);
                filmleriYukle();      
                handleTemizleForm();  
            } else {
                // Bu durum, WHERE koşulundaki FilmID'ye sahip bir kayıt bulunamadığında oluşabilir.
                showAlert("Uyarı", "Film güncellenemedi (kayıt bulunamadı veya değişiklik yapılmadı).", Alert.AlertType.WARNING);
            }
        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "Film güncellenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSilFilm() {
        if (seciliFilm == null) {
            showAlert("Hata", "Silinecek bir film seçilmedi.", Alert.AlertType.WARNING);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Silme Onayı");
        alert.setHeaderText(null);
        alert.setContentText(seciliFilm.getBaslik() + " adlı filmi silmek istediğinizden emin misiniz?\nBu filme bağlı seanslar ve rezervasyonlar da silinecektir (ON DELETE CASCADE).");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            String query = "DELETE FROM Filmler WHERE FilmID = ?";
            try (Connection conn = DbHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setInt(1, seciliFilm.getFilmID());
                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    showAlert("Başarılı", "Film ve bağlı tüm seans/rezervasyonlar başarıyla silindi.", Alert.AlertType.INFORMATION);
                    filmleriYukle();
                    handleTemizleForm();
                }
            } catch (SQLException e) {
                showAlert("Veritabanı Hatası", "Film silinirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleTemizleForm() {
        tableViewFilmler.getSelectionModel().clearSelection();
        txtFilmBaslik.clear();
        comboFilmTurleri.getSelectionModel().clearSelection();
        txtFilmSure.clear();
        datePickerYayinTarihi.setValue(null);
        txtAfisURL.clear();
        txtFilmAciklama.clear();
        txtFragmanURL.clear();
        seciliFilm = null;
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