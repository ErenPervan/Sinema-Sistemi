// application/SeansYonetimiController.java
package application;

import javafx.application.Platform;
import application.model.Tur;
import application.model.Film;
import application.model.Il;
import application.model.Salon;
import application.model.Seans;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SeansYonetimiController {

    @FXML
    private TableView<Seans> tableViewSeanslar;
    @FXML
    private TableColumn<Seans, Integer> colSeansID;
    @FXML
    private TableColumn<Seans, String> colFilmAdi;
    @FXML
    private TableColumn<Seans, String> colSehirAdi;
    @FXML
    private TableColumn<Seans, String> colSalonAdi;
    @FXML
    private TableColumn<Seans, String> colSeansZamani;

    @FXML
    private ComboBox<Film> comboFilmler;
    @FXML
    private ComboBox<Il> comboIller;
    @FXML
    private ComboBox<Salon> comboSalonlar;
    @FXML
    private DatePicker datePickerSeansTarihi;
    @FXML
    private ComboBox<Integer> comboSaat;
    @FXML
    private ComboBox<Integer> comboDakika;

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

    private ObservableList<Seans> seansListesi = FXCollections.observableArrayList();
    private ObservableList<Film> filmComboListesi = FXCollections.observableArrayList();
    private ObservableList<Il> ilComboListesi = FXCollections.observableArrayList();
    private ObservableList<Salon> salonComboListesi = FXCollections.observableArrayList();

    private Seans seciliSeans = null;

    public void initialize() {
        colSeansID.setCellValueFactory(new PropertyValueFactory<>("seansID"));
        colFilmAdi.setCellValueFactory(new PropertyValueFactory<>("filmBaslik"));
        colSehirAdi.setCellValueFactory(new PropertyValueFactory<>("sehirAdi"));
        colSalonAdi.setCellValueFactory(new PropertyValueFactory<>("salonAdi"));
        colSeansZamani.setCellValueFactory(new PropertyValueFactory<>("formatliSeansZamani"));

        setupComboBoxConverters();
        saatDakikaComboBoxlariniDoldur();
        filmleriYukleComboBox();
        illeriYukleComboBox();

        comboIller.setOnAction(event -> ilSecildiComboSalonlarIcin());
        comboSalonlar.setDisable(true);

        seanslariYukle();

        tableViewSeanslar.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                seciliSeans = newSelection;
                doldurForm(seciliSeans);
                btnGuncelle.setDisable(false);
                btnSil.setDisable(false);
                btnEkle.setDisable(true);
            } else {
                handleTemizleForm();
            }
        });
        handleTemizleForm();
    }

    private void setupComboBoxConverters() {
        comboFilmler.setConverter(new StringConverter<Film>() {
            @Override public String toString(Film film) { return film == null ? null : film.getBaslik(); }
            @Override public Film fromString(String string) { return null; }
        });
        comboIller.setConverter(new StringConverter<Il>() {
            @Override public String toString(Il il) { return il == null ? null : il.getSehirAdi(); }
            @Override public Il fromString(String string) { return null; }
        });
        comboSalonlar.setConverter(new StringConverter<Salon>() {
            @Override public String toString(Salon salon) { return salon == null ? null : salon.getSalonAdi(); }
            @Override public Salon fromString(String string) { return null; }
        });
    }

    private void saatDakikaComboBoxlariniDoldur() {
        comboSaat.setItems(FXCollections.observableList(
                IntStream.rangeClosed(0, 23).boxed().collect(Collectors.toList())));
        comboDakika.setItems(FXCollections.observableList(
                IntStream.iterate(0, n -> n + 15).limit(4).boxed().collect(Collectors.toList())));
    }

    private void filmleriYukleComboBox() {
        filmComboListesi.clear();
        // Film süresini de alıyoruz (SureDakika)
        String query = "SELECT FilmID, Baslik, SureDakika, Aciklama, YayinTarihi, AfisURL, SistemeEklenmeTarihi, FragmanURL, TurID FROM Filmler ORDER BY Baslik";
        try (Connection conn = DbHelper.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Tur placeholderTur = null; 
                
                int turID = rs.getInt("TurID");
                if (!rs.wasNull()) {
                     
                	
                     placeholderTur = new Tur(turID, null); 
                }

                 LocalDate yayinTarihi = rs.getDate("YayinTarihi") != null ? rs.getDate("YayinTarihi").toLocalDate() : null;
                 LocalDate sistemeEklenme = rs.getDate("SistemeEklenmeTarihi") != null ? rs.getDate("SistemeEklenmeTarihi").toLocalDate() : null;

                 filmComboListesi.add(new Film(
                            rs.getInt("FilmID"), rs.getString("Baslik"),
                            placeholderTur, // Tur nesnesi
                            rs.getInt("SureDakika"), rs.getString("Aciklama"),
                            yayinTarihi, rs.getString("AfisURL"), sistemeEklenme,
                            rs.getString("FragmanURL")
                 ));
            }
            comboFilmler.setItems(filmComboListesi);
        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "Filmler (ComboBox) yüklenirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void illeriYukleComboBox() {
        ilComboListesi.clear();
        String query = "SELECT SehirID, SehirAdi FROM Sehirler ORDER BY SehirAdi";
        try (Connection conn = DbHelper.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                ilComboListesi.add(new Il(rs.getInt("SehirID"), rs.getString("SehirAdi")));
            }
            comboIller.setItems(ilComboListesi);
        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "İller (ComboBox) yüklenirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void ilSecildiComboSalonlarIcin() {
        Il secilenIl = comboIller.getValue();
        salonComboListesi.clear();
        comboSalonlar.setValue(null);
        comboSalonlar.setPromptText("Salon Seçiniz");
        if (secilenIl == null) {
            comboSalonlar.setDisable(true);
            return;
        }
        comboSalonlar.setDisable(false);
        String query = "SELECT SalonID, SalonAdi, SehirID, Kapasite FROM SinemaSalonlari WHERE SehirID = ? ORDER BY SalonAdi";
        try (Connection conn = DbHelper.getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, secilenIl.getSehirID());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                salonComboListesi.add(new Salon(rs.getInt("SalonID"), rs.getString("SalonAdi"),
                                            rs.getInt("SehirID"), secilenIl.getSehirAdi(), rs.getInt("Kapasite")));
            }
            comboSalonlar.setItems(salonComboListesi);
            if (salonComboListesi.isEmpty()) {
                comboSalonlar.setPromptText("Bu ilde salon yok");
            }
        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "Salonlar (ComboBox) yüklenirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void seanslariYukle() {
        seansListesi.clear();
        String query = "SELECT s.SeansID, s.SeansZamani, s.IptalEdildiMi, " + // IptalEdildiMi eklendi
                       "f.FilmID, f.Baslik AS FilmBaslik, f.SureDakika AS FilmSure, " +
                       "sl.SalonID, sl.SalonAdi, sl.Kapasite AS SalonKapasite, " +
                       "c.SehirID, c.SehirAdi " +
                       "FROM Seanslar s " +
                       "JOIN Filmler f ON s.FilmID = f.FilmID " +
                       "JOIN SinemaSalonlari sl ON s.SalonID = sl.SalonID " +
                       "JOIN Sehirler c ON sl.SehirID = c.SehirID " +
                       "ORDER BY s.SeansZamani DESC";

        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

        	while (rs.next()) {
        	    Film film = new Film(rs.getInt("FilmID"), rs.getString("FilmBaslik"), null, rs.getInt("FilmSure"), null, null, null, null,null);
        	    Salon salon = new Salon(rs.getInt("SalonID"), rs.getString("SalonAdi"), rs.getInt("SehirID"), rs.getString("SehirAdi"), rs.getInt("SalonKapasite"));
        	    LocalDateTime seansZamani = rs.getTimestamp("SeansZamani").toLocalDateTime();
        	    boolean seansIptalDurumu = rs.getBoolean("IptalEdildiMi"); // Veritabanından oku
        	    seansListesi.add(new Seans(rs.getInt("SeansID"), film, salon, seansZamani, seansIptalDurumu)); // Yeni constructor'a göre
        	}
            tableViewSeanslar.setItems(seansListesi);
        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "Seanslar yüklenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void doldurForm(Seans seans) {
        if (seans == null) return;

        Film filmToSelect = filmComboListesi.stream()
                                .filter(f -> f.getFilmID() == seans.getFilm().getFilmID())
                                .findFirst().orElse(null);
        comboFilmler.setValue(filmToSelect);
        
        Il ilToSelect = ilComboListesi.stream()
                                .filter(i -> i.getSehirID() == seans.getSalon().getSehirID())
                                .findFirst().orElse(null);
        comboIller.setValue(ilToSelect); // Bu, ilSecildiComboSalonlarIcin'i tetikleyip salonları dolduracak

        Platform.runLater(() -> {
            Salon salonToSelect = salonComboListesi.stream()
                                    .filter(s -> s.getSalonID() == seans.getSalon().getSalonID())
                                    .findFirst().orElse(null);
            comboSalonlar.setValue(salonToSelect);
        });

        datePickerSeansTarihi.setValue(seans.getSeansZamani().toLocalDate());
        comboSaat.setValue(seans.getSeansZamani().getHour());
        comboDakika.setValue(seans.getSeansZamani().getMinute());
    }

    private boolean checkSeansCakismasi(Connection conn, int salonID, LocalDateTime yeniSeansBaslangic, LocalDateTime yeniSeansBitis, Integer guncellenenSeansID) throws SQLException {
        StringBuilder queryBuilder = new StringBuilder(
            "SELECT COUNT(*) AS cakismaSayisi " +
            "FROM Seanslar s " +
            "JOIN Filmler f ON s.FilmID = f.FilmID " +
            "WHERE s.SalonID = ? AND IFNULL(s.IptalEdildiMi, 0) = 0 " + // Sadece aktif seanslarlardaki çakışam
            "AND (s.SeansZamani < ? AND TIMESTAMPADD(MINUTE, f.SureDakika, s.SeansZamani) > ?)"
        );

        if (guncellenenSeansID != null) {
            queryBuilder.append(" AND s.SeansID != ?");
        }

        try (PreparedStatement pstmt = conn.prepareStatement(queryBuilder.toString())) {
            int paramIndex = 1;
            pstmt.setInt(paramIndex++, salonID);
            pstmt.setTimestamp(paramIndex++, Timestamp.valueOf(yeniSeansBitis));
            pstmt.setTimestamp(paramIndex++, Timestamp.valueOf(yeniSeansBaslangic));
            if (guncellenenSeansID != null) {
                pstmt.setInt(paramIndex++, guncellenenSeansID);
            }

            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getInt("cakismaSayisi") > 0) {
                return true; 
            }
        }
        return false; 
    }

    @FXML
    private void handleEkleSeans() {
        Film secilenFilm = comboFilmler.getValue();
        Salon secilenSalon = comboSalonlar.getValue();
        LocalDate secilenTarih = datePickerSeansTarihi.getValue();
        Integer secilenSaat = comboSaat.getValue();
        Integer secilenDakika = comboDakika.getValue();

        if (secilenFilm == null || secilenSalon == null || secilenTarih == null || secilenSaat == null || secilenDakika == null) {
            showAlert("Giriş Hatası", "Lütfen tüm alanları (Film, Salon, Tarih, Saat, Dakika) doldurun.", Alert.AlertType.WARNING);
            return;
        }
        if (secilenFilm.getSureDakika() <= 0) {
             showAlert("Film Süresi Hatası", "Seçilen filmin süresi (0 veya negatif) geçerli değil. Lütfen film yönetiminden kontrol edin.", Alert.AlertType.ERROR);
            return;
        }

        LocalDateTime yeniSeansBaslangicZamani = LocalDateTime.of(secilenTarih, LocalTime.of(secilenSaat, secilenDakika));
        LocalDateTime yeniSeansBitisZamani = yeniSeansBaslangicZamani.plusMinutes(secilenFilm.getSureDakika());

        if (yeniSeansBaslangicZamani.isBefore(LocalDateTime.now())) {
            showAlert("Geçersiz Tarih/Saat", "Geçmiş bir tarih veya saate seans ekleyemezsiniz.", Alert.AlertType.WARNING);
            return;
        }

        try (Connection conn = DbHelper.getConnection()) {
            if (checkSeansCakismasi(conn, secilenSalon.getSalonID(), yeniSeansBaslangicZamani, yeniSeansBitisZamani, null)) {
                showAlert("Çakışma Hatası", "Bu salonda belirtilen zaman aralığında başka bir aktif seans bulunmaktadır.\nLütfen farklı bir zaman veya salon seçin.", Alert.AlertType.ERROR);
                return;
            }

            String query = "INSERT INTO Seanslar (FilmID, SalonID, SeansZamani, EkleyenKullaniciID, IptalEdildiMi) VALUES (?, ?, ?, ?, 0)";

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, secilenFilm.getFilmID());
                pstmt.setInt(2, secilenSalon.getSalonID());
                pstmt.setTimestamp(3, Timestamp.valueOf(yeniSeansBaslangicZamani));
                if (UserSession.getInstance().isLoggedIn()) {
                    pstmt.setInt(4, UserSession.getInstance().getKullaniciID());
                } else {
                    pstmt.setNull(4, java.sql.Types.INTEGER);
                }

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    showAlert("Başarılı", "Seans başarıyla eklendi.", Alert.AlertType.INFORMATION);
                    seanslariYukle();
                    handleTemizleForm();
                }
            }
        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "Seans eklenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleGuncelleSeans() {
        if (seciliSeans == null) {
            showAlert("Hata", "Güncellenecek bir seans seçilmedi.", Alert.AlertType.WARNING);
            return;
        }

        Film secilenFilmForm = comboFilmler.getValue();
        Salon secilenSalonForm = comboSalonlar.getValue();
        LocalDate secilenTarihForm = datePickerSeansTarihi.getValue();
        Integer secilenSaatForm = comboSaat.getValue();
        Integer secilenDakikaForm = comboDakika.getValue();

        if (secilenFilmForm == null || secilenSalonForm == null || secilenTarihForm == null || secilenSaatForm == null || secilenDakikaForm == null) {
            showAlert("Giriş Hatası", "Lütfen tüm alanları doldurun.", Alert.AlertType.WARNING);
            return;
        }
        if (secilenFilmForm.getSureDakika() <= 0) {
             showAlert("Film Süresi Hatası", "Seçilen filmin süresi (0 veya negatif) geçerli değil.", Alert.AlertType.ERROR);
            return;
        }

        LocalDateTime yeniSeansBaslangicZamani = LocalDateTime.of(secilenTarihForm, LocalTime.of(secilenSaatForm, secilenDakikaForm));
        LocalDateTime yeniSeansBitisZamani = yeniSeansBaslangicZamani.plusMinutes(secilenFilmForm.getSureDakika());

     

        try (Connection conn = DbHelper.getConnection()) {
            if (checkSeansCakismasi(conn, secilenSalonForm.getSalonID(), yeniSeansBaslangicZamani, yeniSeansBitisZamani, seciliSeans.getSeansID())) {
                showAlert("Çakışma Hatası", "Bu salonda belirtilen zaman aralığında başka bir aktif seans bulunmaktadır.\nLütfen farklı bir zaman veya salon seçin.", Alert.AlertType.ERROR);
                return;
            }

            String query = "UPDATE Seanslar SET FilmID = ?, SalonID = ?, SeansZamani = ? WHERE SeansID = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, secilenFilmForm.getFilmID());
                pstmt.setInt(2, secilenSalonForm.getSalonID());
                pstmt.setTimestamp(3, Timestamp.valueOf(yeniSeansBaslangicZamani));
                pstmt.setInt(4, seciliSeans.getSeansID());

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    showAlert("Başarılı", "Seans başarıyla güncellendi.", Alert.AlertType.INFORMATION);
                    seanslariYukle();
                    handleTemizleForm();
                } else {
                    showAlert("Uyarı", "Seans güncellenemedi (kayıt bulunamadı veya değişiklik yapılmadı).", Alert.AlertType.WARNING);
                }
            }
        } catch (SQLException e) {
            showAlert("Veritabanı Hatası", "Seans güncellenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSilSeans() {
        if (seciliSeans == null) {
            showAlert("Hata", "Silinecek bir seans seçilmedi.", Alert.AlertType.WARNING);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Seans Silme Onayı");
        alert.setHeaderText(seciliSeans.getFilmBaslik() + " - " + seciliSeans.getFormatliSeansZamani() + " seansını silmek üzeresiniz.");
        alert.setContentText("Bu seansa ait tüm aktif rezervasyonlar önce 'iptal edildi' olarak işaretlenecek ve ardından seans silinecektir.\nDevam etmek istediğinizden emin misiniz?");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            Connection conn = null;
            PreparedStatement pstmtUpdateReservations = null;
            PreparedStatement pstmtDeleteSeans = null;
            int cancelledReservationsCount = 0;

            String updateReservationsQuery = "UPDATE Rezervasyonlar SET IptalEdildiMi = 1, IptalZamani = CURRENT_TIMESTAMP " +
                                             "WHERE SeansID = ? AND IptalEdildiMi = 0";
            String deleteSeansQuery = "DELETE FROM Seanslar WHERE SeansID = ?";

            try {
                conn = DbHelper.getConnection();
                conn.setAutoCommit(false); // Transaction'ı başlat

              //Bu seansa ait aktif rezervasyonları iptal et
                pstmtUpdateReservations = conn.prepareStatement(updateReservationsQuery);
                pstmtUpdateReservations.setInt(1, seciliSeans.getSeansID());
                cancelledReservationsCount = pstmtUpdateReservations.executeUpdate(); // Kaç rezervasyonun iptal edildiğini say

                 //Seansın kendisini sil
                pstmtDeleteSeans = conn.prepareStatement(deleteSeansQuery);
                pstmtDeleteSeans.setInt(1, seciliSeans.getSeansID());
                int seansAffectedRows = pstmtDeleteSeans.executeUpdate();

                if (seansAffectedRows > 0) {
                    conn.commit(); // Her iki işlem de başarılıysa değişiklikleri onayla
                    
                    String successMessage = "Seans başarıyla silindi.";
                    if (cancelledReservationsCount > 0) {
                        successMessage += "\nBu seansa ait " + cancelledReservationsCount + " aktif rezervasyon da iptal edildi.";
                    } else {
                        successMessage += "\nBu seansa ait iptal edilecek aktif rezervasyon bulunmuyordu.";
                    }
                    showAlert("Başarılı", successMessage, Alert.AlertType.INFORMATION);
                    
                    seanslariYukle();
                    handleTemizleForm(); 
                } else {
//                	seans silinmediyse
                    conn.rollback(); // Değişiklikleri geri al
                    showAlert("Hata", "Seans silinemedi (veritabanı kaydı etkilenmedi). Lütfen tekrar deneyin.", Alert.AlertType.WARNING);
                }

            } catch (SQLException e) {
                if (conn != null) {
                    try {
                        conn.rollback(); // Herhangi bir SQL hatasında değişiklikleri geri al
                    } catch (SQLException ex) {
                        ex.printStackTrace(); // Rollback hatasını logla
                    }
                }
                showAlert("Veritabanı Hatası", "Seans silinirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
            } finally {
                // Kaynakları kapat
                if (pstmtUpdateReservations != null) try { pstmtUpdateReservations.close(); } catch (SQLException e) { /* ignored */ }
                if (pstmtDeleteSeans != null) try { pstmtDeleteSeans.close(); } catch (SQLException e) { /* ignored */ }
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true); // Otomatik commit modunu geri al
                        DbHelper.closeConnection(conn);
                    } catch (SQLException e) { /* ignored */ }
                }
            }
        }
    }

    @FXML
    private void handleTemizleForm() {
        tableViewSeanslar.getSelectionModel().clearSelection();
        comboFilmler.getSelectionModel().clearSelection();
        comboIller.getSelectionModel().clearSelection(); 
        comboSalonlar.getItems().clear(); 
        comboSalonlar.setDisable(true);
        datePickerSeansTarihi.setValue(null);
        comboSaat.getSelectionModel().clearSelection();
        comboDakika.getSelectionModel().clearSelection();
        seciliSeans = null;
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
        if (btnKapat != null && btnKapat.getScene() != null && btnKapat.getScene().getWindow() != null) {
             alert.initOwner(btnKapat.getScene().getWindow());
        }
        alert.showAndWait();
    }
}