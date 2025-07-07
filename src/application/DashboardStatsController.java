// application/DashboardStatsController.java
package application;

import application.model.Film;
import application.model.Il;
import application.model.Salon;
import application.model.Seans;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DashboardStatsController {

    @FXML private Label lblToplamIl;
    @FXML private Label lblToplamSalon;
    @FXML private ListView<String> listSonEklenenFilmler;
    @FXML private ComboBox<Il> comboIllerDetay;
    @FXML private Label lblIlSalonSayisi;
    @FXML private ComboBox<Salon> comboSalonlarDetay;
    @FXML private ComboBox<Film> comboFilmlerDetay;
    @FXML private ListView<Seans> listViewSeanslarDetay;
    @FXML private Label lblSeciliSeansBosKoltuk;
    @FXML private FlowPane flowPaneKoltuklarDetay;
    @FXML private ScrollPane scrollPaneKoltuklar;

    @FXML private ListView<String> listPopulerFilmler;
    @FXML private ListView<String> listPopulerTurler;
    @FXML private ListView<String> listYogunRezervasyonGunleri;
    @FXML private ListView<String> listYogunRezervasyonSaatleri;
    @FXML private ListView<String> listViewOrtalamaSalonDoluluk;
    @FXML private ListView<String> listViewAnlikSeansDoluluk;

    @FXML private Button btnAnaMenu;

    private ObservableList<Il> ilComboListesiDetay = FXCollections.observableArrayList();
    private ObservableList<Salon> salonComboListesiDetay = FXCollections.observableArrayList();
    private ObservableList<Film> filmComboListesiDetay = FXCollections.observableArrayList();
    private ObservableList<Seans> seansListesiDetay = FXCollections.observableArrayList();

    //private final int SALON_KAPASITESI_DETAY = 15; 
    private Label placeholderLabelKoltuklar = null;

    public void initialize() {
        genelIstatistikleriYukle();
        enPopulerFilmleriYukle();
        enPopulerTurleriYukle();
        enYogunRezervasyonGunleriYukle();
        enYogunRezervasyonSaatleriYukle();
        ortalamaSalonDoluluklariniYukle();
        anlikSeansDoluluklariniYukle();
        setupComboBoxConverters();
        illeriYukleDetayComboBox();

        comboSalonlarDetay.setDisable(true);
        comboFilmlerDetay.setDisable(true);

        listViewSeanslarDetay.setItems(seansListesiDetay);
        listViewSeanslarDetay.setCellFactory(param -> new ListCell<Seans>() {
            @Override
            protected void updateItem(Seans seans, boolean empty) {
                super.updateItem(seans, empty);
                if (empty || seans == null || seans.getFormatliSeansZamani() == null) {
                    setText(null);
                } else {
                    setText(seans.getFormatliSeansZamani());
                }
            }
        });

        flowPaneKoltuklarDetay.getChildren().clear();
        setKoltuklarPlaceholder("Lütfen bir seans seçerek koltuk durumunu görüntüleyin.");
        lblSeciliSeansBosKoltuk.setText("Seans Seçiniz...");
    }

    private void setupComboBoxConverters() {
        comboIllerDetay.setConverter(new StringConverter<Il>() {
            @Override public String toString(Il il) { return il == null ? null : il.getSehirAdi(); }
            @Override public Il fromString(String string) { return null; }
        });
        comboSalonlarDetay.setConverter(new StringConverter<Salon>() {
            @Override public String toString(Salon salon) { return salon == null ? null : salon.getSalonAdi(); }
            @Override public Salon fromString(String string) { return null; }
        });
        comboFilmlerDetay.setConverter(new StringConverter<Film>() {
            @Override public String toString(Film film) { return film == null ? null : film.getBaslik(); }
            @Override public Film fromString(String string) { return null; }
        });
    }

    private void enPopulerFilmleriYukle() {
        ObservableList<String> populerFilmListesi = FXCollections.observableArrayList();
        String query = "SELECT f.Baslik AS FilmBasligi, COUNT(r.RezervasyonID) AS ToplamBiletSayisi " +
                       "FROM Filmler f " +
                       "JOIN Seanslar s ON f.FilmID = s.FilmID " +
                       "JOIN Rezervasyonlar r ON s.SeansID = r.SeansID " +
                       "WHERE r.IptalEdildiMi = 0 " + // Aktif biletler
                       "GROUP BY f.FilmID, f.Baslik " +
                       "ORDER BY ToplamBiletSayisi DESC " +
                       "LIMIT 10";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            int siraNo = 1;
            while (rs.next()) {
                String filmAdi = rs.getString("FilmBasligi"); 
                int biletSayisi = rs.getInt("ToplamBiletSayisi");
                populerFilmListesi.add(siraNo + ". " + filmAdi + " (" + biletSayisi + " bilet)");
                siraNo++;
            }
            listPopulerFilmler.setItems(populerFilmListesi);
            if (populerFilmListesi.isEmpty()) {
                listPopulerFilmler.setPlaceholder(new Label("Popüler film bulunamadı."));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Popüler filmler yüklenirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
            listPopulerFilmler.setPlaceholder(new Label("Popüler filmler yüklenemedi."));
        }
    }

    private void enPopulerTurleriYukle() {
        ObservableList<String> populerTurListesi = FXCollections.observableArrayList();
        String query = "SELECT t.TurAdi AS FilmTuru, COUNT(r.RezervasyonID) AS ToplamBiletSayisi " +
                       "FROM Turler t " +
                       "JOIN Filmler f ON t.TurID = f.TurID " +
                       "JOIN Seanslar s ON f.FilmID = s.FilmID " +
                       "JOIN Rezervasyonlar r ON s.SeansID = r.SeansID " +
                       "WHERE r.IptalEdildiMi = 0 " + 
                       "GROUP BY t.TurID, t.TurAdi " +
                       "ORDER BY ToplamBiletSayisi DESC " +
                       "LIMIT 5";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            int siraNo = 1;
            while (rs.next()) {
                String turAdi = rs.getString("FilmTuru");
                int biletSayisi = rs.getInt("ToplamBiletSayisi");
                populerTurListesi.add(siraNo + ". " + turAdi + " (" + biletSayisi + " bilet)");
                siraNo++;
            }
            listPopulerTurler.setItems(populerTurListesi);
            if (populerTurListesi.isEmpty()) {
                listPopulerTurler.setPlaceholder(new Label("Popüler film türü bulunamadı."));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Popüler film türleri yüklenirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
            listPopulerTurler.setPlaceholder(new Label("Popüler türler yüklenemedi."));
        }
    }

    private void enYogunRezervasyonGunleriYukle() {
        ObservableList<String> yogunGunListesi = FXCollections.observableArrayList();
        String query = "SELECT " +
                       "    CASE s.dow " + 
                       "        WHEN 1 THEN 'Pazar' " +
                       "        WHEN 2 THEN 'Pazartesi' " +
                       "        WHEN 3 THEN 'Salı' " +
                       "        WHEN 4 THEN 'Çarşamba' " +
                       "        WHEN 5 THEN 'Perşembe' " +
                       "        WHEN 6 THEN 'Cuma' " +
                       "        WHEN 7 THEN 'Cumartesi' " +
                       "    END AS GunAdi, " +
                       "    s.ToplamRezervasyonSayisi " + 
                       "FROM ( " +
                       "    SELECT " +
                       "        DAYOFWEEK(r.RezervasyonZamani) as dow, " + 
                       "        COUNT(r.RezervasyonID) AS ToplamRezervasyonSayisi " +
                       "    FROM Rezervasyonlar r " +
                       "    WHERE r.IptalEdildiMi = 0 " + 
                       "    GROUP BY DAYOFWEEK(r.RezervasyonZamani) " + 
                       ") AS s " + 
                       "ORDER BY s.ToplamRezervasyonSayisi DESC";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            int siraNo = 1;
            while (rs.next()) {
                String gunAdi = rs.getString("GunAdi");
                int rezervasyonSayisi = rs.getInt("ToplamRezervasyonSayisi");
                yogunGunListesi.add(siraNo + ". " + gunAdi + " (" + rezervasyonSayisi + " rezervasyon)");
                siraNo++;
            }
            listYogunRezervasyonGunleri.setItems(yogunGunListesi);
            if (yogunGunListesi.isEmpty()) {
                listYogunRezervasyonGunleri.setPlaceholder(new Label("Rezervasyon günü verisi bulunamadı."));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Yoğun rezervasyon günleri yüklenirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
            listYogunRezervasyonGunleri.setPlaceholder(new Label("Veriler yüklenemedi."));
        }
    }

    private void enYogunRezervasyonSaatleriYukle() {
        ObservableList<String> yogunSaatListesi = FXCollections.observableArrayList();
        String query = "SELECT " +
                       "    HOUR(r.RezervasyonZamani) AS SaatDilimi, " +
                       "    COUNT(r.RezervasyonID) AS ToplamRezervasyonSayisi " +
                       "FROM Rezervasyonlar r " +
                       "WHERE r.IptalEdildiMi = 0 " + 
                       "GROUP BY SaatDilimi " +
                       "ORDER BY ToplamRezervasyonSayisi DESC";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            int siraNo = 1;
            while (rs.next()) {
                int saatDilimi = rs.getInt("SaatDilimi");
                int rezervasyonSayisi = rs.getInt("ToplamRezervasyonSayisi");
                String saatAraligi = String.format("%02d:00 - %02d:59", saatDilimi, saatDilimi);
                yogunSaatListesi.add(siraNo + ". " + saatAraligi + " (" + rezervasyonSayisi + " rezervasyon)");
                siraNo++;
            }
            listYogunRezervasyonSaatleri.setItems(yogunSaatListesi);
            if (yogunSaatListesi.isEmpty()) {
                listYogunRezervasyonSaatleri.setPlaceholder(new Label("Rezervasyon saati verisi bulunamadı."));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Yoğun rezervasyon saatleri yüklenirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
            listYogunRezervasyonSaatleri.setPlaceholder(new Label("Veriler yüklenemedi."));
        }
    }

    private void ortalamaSalonDoluluklariniYukle() {
        ObservableList<String> ortalamaDolulukListesi = FXCollections.observableArrayList();
        String query = "SELECT " +
                       "    ss.SalonAdi, ss.Kapasite AS SalonKapasitesi, " +
                       "    COUNT(r.RezervasyonID) AS ToplamSatilanBilet, " +
                       "    COUNT(DISTINCT s.SeansID) AS ToplamGerceklesenSeansSayisi, " +
                       "    (COUNT(r.RezervasyonID) * 100.0 / NULLIF(COUNT(DISTINCT s.SeansID) * ss.Kapasite, 0)) AS OrtalamaDolulukYuzdesi " +
                       "FROM " +
                       "    SinemaSalonlari ss " +
                       "JOIN " +
                       "    Seanslar s ON ss.SalonID = s.SalonID " +
                       "LEFT JOIN " +
                       "    Rezervasyonlar r ON (s.SeansID = r.SeansID AND r.IptalEdildiMi = 0) " + 
                       "WHERE " +
                       "    s.SeansZamani < NOW() " +
                       "GROUP BY " +
                       "    ss.SalonID, ss.SalonAdi, ss.Kapasite " +
                       "HAVING " +
                       "    COUNT(DISTINCT s.SeansID) > 0 " +
                       "ORDER BY " +
                       "    OrtalamaDolulukYuzdesi DESC, ss.SalonAdi ASC " +
                       "LIMIT 10";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            int siraNo = 1;
            while (rs.next()) {
                String salonAdi = rs.getString("SalonAdi");
                double ortalamaYuzde = rs.getDouble("OrtalamaDolulukYuzdesi");
                int toplamBilet = rs.getInt("ToplamSatilanBilet");
                int toplamSeans = rs.getInt("ToplamGerceklesenSeansSayisi");
                ortalamaDolulukListesi.add(String.format("%d. %s - Ort. Doluluk: %.1f%% (Bilet: %d, Seans: %d)",
                        siraNo, salonAdi, ortalamaYuzde, toplamBilet, toplamSeans));
                siraNo++;
            }
            listViewOrtalamaSalonDoluluk.setItems(ortalamaDolulukListesi);
            if (ortalamaDolulukListesi.isEmpty()) {
                listViewOrtalamaSalonDoluluk.setPlaceholder(new Label("Geçmiş seanslar için salon doluluk verisi yok."));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Ortalama salon dolulukları yüklenirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
            listViewOrtalamaSalonDoluluk.setPlaceholder(new Label("Veriler yüklenemedi."));
        }
    }

    private void anlikSeansDoluluklariniYukle() {
        ObservableList<String> anlikDolulukListesi = FXCollections.observableArrayList();
        String query = "SELECT " +
                       "    ss.SalonAdi, ss.Kapasite AS SalonKapasitesi, " +
                       "    f.Baslik AS FilmAdi, " +
                       "    DATE_FORMAT(s.SeansZamani, '%H:%i') AS SeansSaati, " +
                       "    COUNT(r.RezervasyonID) AS SatilanBiletSayisi, " +
                       "    (COUNT(r.RezervasyonID) * 100.0 / ss.Kapasite) AS DolulukYuzdesi " +
                       "FROM " +
                       "    SinemaSalonlari ss " +
                       "JOIN " +
                       "    Seanslar s ON ss.SalonID = s.SalonID " +
                       "JOIN " +
                       "    Filmler f ON s.FilmID = f.FilmID " +
                       "LEFT JOIN " +
                       "    Rezervasyonlar r ON (s.SeansID = r.SeansID AND r.IptalEdildiMi = 0) " + 
                       "WHERE " +
                       "    DATE(s.SeansZamani) = CURDATE() AND s.SeansZamani >= NOW() " +
                       "GROUP BY " +
                       "    ss.SalonID, ss.SalonAdi, s.SeansID, f.Baslik, s.SeansZamani, ss.Kapasite " +
                       "ORDER BY " +
                       "    ss.SalonAdi, s.SeansZamani " +
                       "LIMIT 15";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String salonAdi = rs.getString("SalonAdi");
                String filmAdi = rs.getString("FilmAdi");
                String seansSaati = rs.getString("SeansSaati");
                int satilanBilet = rs.getInt("SatilanBiletSayisi");
                double dolulukYuzdesi = rs.getDouble("DolulukYuzdesi");
                int salonKapasitesi = rs.getInt("SalonKapasitesi");
                anlikDolulukListesi.add(String.format("%s - %s (%s) - Doluluk: %.1f%% (%d/%d)",
                        salonAdi, filmAdi, seansSaati, dolulukYuzdesi, satilanBilet, salonKapasitesi));
            }
            listViewAnlikSeansDoluluk.setItems(anlikDolulukListesi);
            if (anlikDolulukListesi.isEmpty()) {
                listViewAnlikSeansDoluluk.setPlaceholder(new Label("Bugün için yaklaşan seans bulunamadı."));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Anlık seans dolulukları yüklenirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
            listViewAnlikSeansDoluluk.setPlaceholder(new Label("Veriler yüklenemedi."));
        }
    }

    private void genelIstatistikleriYukle() {
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) AS IlSayisi FROM Sehirler");
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) lblToplamIl.setText(String.valueOf(rs.getInt("IlSayisi")));
        } catch (SQLException e) { e.printStackTrace(); lblToplamIl.setText("Hata"); }

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) AS SalonSayisi FROM SinemaSalonlari");
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) lblToplamSalon.setText(String.valueOf(rs.getInt("SalonSayisi")));
        } catch (SQLException e) { e.printStackTrace(); lblToplamSalon.setText("Hata"); }

        ObservableList<String> filmler = FXCollections.observableArrayList();
        String filmQuery = "SELECT Baslik, SistemeEklenmeTarihi FROM Filmler WHERE SistemeEklenmeTarihi >= DATE_SUB(CURDATE(), INTERVAL 10 DAY) ORDER BY SistemeEklenmeTarihi DESC";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(filmQuery);
             ResultSet rs = pstmt.executeQuery()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            while (rs.next()) {
                String baslik = rs.getString("Baslik");
                LocalDate eklenmeTarihi = rs.getDate("SistemeEklenmeTarihi").toLocalDate();
                filmler.add(baslik + " (Eklendi: " + eklenmeTarihi.format(formatter) + ")");
            }
            listSonEklenenFilmler.setItems(filmler);
             if (filmler.isEmpty()) {
                listSonEklenenFilmler.setPlaceholder(new Label("Son 10 günde eklenen film yok."));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            listSonEklenenFilmler.setPlaceholder(new Label("Filmler yüklenirken hata oluştu."));
        }
    }

    private void illeriYukleDetayComboBox() {
        ilComboListesiDetay.clear();
        String query = "SELECT SehirID, SehirAdi FROM Sehirler ORDER BY SehirAdi";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                ilComboListesiDetay.add(new Il(rs.getInt("SehirID"), rs.getString("SehirAdi")));
            }
            comboIllerDetay.setItems(ilComboListesiDetay);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "İller (Dashboard) yüklenirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleIlSecimDetay() {
        Il secilenIl = comboIllerDetay.getValue();
        lblIlSalonSayisi.setText("-");
        salonComboListesiDetay.clear();
        comboSalonlarDetay.setValue(null);
        comboSalonlarDetay.setDisable(true);
        filmComboListesiDetay.clear();
        comboFilmlerDetay.setValue(null);
        comboFilmlerDetay.setDisable(true);
        seansListesiDetay.clear();
        flowPaneKoltuklarDetay.getChildren().clear();
        setKoltuklarPlaceholder("Lütfen bir seans seçerek koltuk durumunu görüntüleyin.");
        lblSeciliSeansBosKoltuk.setText("Seans Seçiniz...");

        if (secilenIl == null) return;

        String countQuery = "SELECT COUNT(*) AS SalonSayisi FROM SinemaSalonlari WHERE SehirID = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement countPs = conn.prepareStatement(countQuery)) {
            countPs.setInt(1, secilenIl.getSehirID());
            ResultSet countRs = countPs.executeQuery();
            if (countRs.next()) lblIlSalonSayisi.setText(String.valueOf(countRs.getInt("SalonSayisi")));
        } catch (SQLException e) { e.printStackTrace(); lblIlSalonSayisi.setText("Hata");}

        String salonQuery = "SELECT SalonID, SalonAdi, SehirID, Kapasite FROM SinemaSalonlari WHERE SehirID = ? ORDER BY SalonAdi";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement salonPs = conn.prepareStatement(salonQuery)) {
            salonPs.setInt(1, secilenIl.getSehirID());
            ResultSet salonRs = salonPs.executeQuery();
            while (salonRs.next()) {
                salonComboListesiDetay.add(new Salon(
                        salonRs.getInt("SalonID"), salonRs.getString("SalonAdi"),
                        salonRs.getInt("SehirID"), secilenIl.getSehirAdi(), salonRs.getInt("Kapasite")
                ));
            }
            comboSalonlarDetay.setItems(salonComboListesiDetay);
            if (!salonComboListesiDetay.isEmpty()) comboSalonlarDetay.setDisable(false);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Salonlar (Dashboard) yüklenirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleSalonSecimDetay() {
        Salon secilenSalon = comboSalonlarDetay.getValue();
        filmComboListesiDetay.clear();
        comboFilmlerDetay.setValue(null);
        comboFilmlerDetay.setDisable(true);
        seansListesiDetay.clear();
        flowPaneKoltuklarDetay.getChildren().clear();
        setKoltuklarPlaceholder("Lütfen bir seans seçerek koltuk durumunu görüntüleyin.");
        lblSeciliSeansBosKoltuk.setText("Seans Seçiniz...");

        if (secilenSalon == null) return;

        String filmQuery = "SELECT DISTINCT f.FilmID, f.Baslik, f.SureDakika, f.Aciklama, f.YayinTarihi, f.AfisURL, f.SistemeEklenmeTarihi, f.FragmanURL " + // Tur bilgisi burada çekilmiyor, Film nesnesi eksik kalabilir.
                           "FROM Filmler f " +
                           "JOIN Seanslar s ON f.FilmID = s.FilmID " +
                           "WHERE s.SalonID = ? AND s.SeansZamani >= NOW() " +
                           "ORDER BY f.Baslik";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement filmPs = conn.prepareStatement(filmQuery)) {
            filmPs.setInt(1, secilenSalon.getSalonID());
            ResultSet filmRs = filmPs.executeQuery();
            while (filmRs.next()) {
                LocalDate yayinTarihi = filmRs.getDate("YayinTarihi") != null ? filmRs.getDate("YayinTarihi").toLocalDate() : null;
                LocalDate sistemeEklenme = filmRs.getDate("SistemeEklenmeTarihi") != null ? filmRs.getDate("SistemeEklenmeTarihi").toLocalDate() : null;
                filmComboListesiDetay.add(new Film(
                        filmRs.getInt("FilmID"), filmRs.getString("Baslik"), null, // Tur null
                        filmRs.getInt("SureDakika"), filmRs.getString("Aciklama"), yayinTarihi,
                        filmRs.getString("AfisURL"), sistemeEklenme, filmRs.getString("FragmanURL")
                ));
            }
            comboFilmlerDetay.setItems(filmComboListesiDetay);
            if (!filmComboListesiDetay.isEmpty()) comboFilmlerDetay.setDisable(false);
            else showAlert("Bilgi", "Bu salonda gösterimde olan aktif film bulunamadı.", Alert.AlertType.INFORMATION);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Filmler (Dashboard) yüklenirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleFilmSecimDetay() {
        Film secilenFilm = comboFilmlerDetay.getValue();
        Salon secilenSalon = comboSalonlarDetay.getValue();
        seansListesiDetay.clear();
        flowPaneKoltuklarDetay.getChildren().clear();
        setKoltuklarPlaceholder("Lütfen bir seans seçerek koltuk durumunu görüntüleyin.");
        lblSeciliSeansBosKoltuk.setText("Seans Seçiniz...");

        if (secilenFilm == null || secilenSalon == null) return;

        String seansQuery = "SELECT s.SeansID, s.SeansZamani ,s.IptalEdildiMi " +
                            "FROM Seanslar s " +
                            "WHERE s.FilmID = ? AND s.SalonID = ? AND s.SeansZamani >= NOW() " +
                            "AND IFNULL(s.IptalEdildiMi, 0) = 0 " +

                            "ORDER BY s.SeansZamani";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(seansQuery)) {
            pstmt.setInt(1, secilenFilm.getFilmID());
            pstmt.setInt(2, secilenSalon.getSalonID());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                LocalDateTime seansZamani = rs.getTimestamp("SeansZamani").toLocalDateTime();
                boolean iptalEdildi = rs.getBoolean("IptalEdildiMi");
                seansListesiDetay.add(new Seans(rs.getInt("SeansID"), secilenFilm, secilenSalon, seansZamani, iptalEdildi)); 
            }
            if (seansListesiDetay.isEmpty()) {
                showAlert("Bilgi", "Bu film için bu salonda aktif seans bulunamadı.", Alert.AlertType.INFORMATION);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Seanslar (Dashboard-Detay) yüklenirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleSeansSecimListeden() {
        Seans secilenSeans = listViewSeanslarDetay.getSelectionModel().getSelectedItem();
        flowPaneKoltuklarDetay.getChildren().clear();

        if (secilenSeans == null) {
            lblSeciliSeansBosKoltuk.setText("Seans Seçiniz...");
            setKoltuklarPlaceholder("Lütfen bir seans seçerek koltuk durumunu görüntüleyin.");
            return;
        }

        List<String> doluKoltukNumaralari = new ArrayList<>();
        // Düzeltilmiş sorgu: Sadece aktif rezervasyonları al
        String doluKoltukQuery = "SELECT KoltukNumarasi FROM Rezervasyonlar WHERE SeansID = ? AND IptalEdildiMi = 0";
        int doluKoltukSayisi = 0;
        int salonKapasitesi = secilenSeans.getSalon() != null ? secilenSeans.getSalon().getKapasite() : 15; // Dinamik kapasite


        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(doluKoltukQuery)) {
            pstmt.setInt(1, secilenSeans.getSeansID());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                doluKoltukNumaralari.add(rs.getString("KoltukNumarasi"));
            }
            doluKoltukSayisi = doluKoltukNumaralari.size();
            if (rs != null) rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Dolu koltuklar (Dashboard) yüklenirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
            lblSeciliSeansBosKoltuk.setText("Hata!");
            return;
        }

        int bosKoltuk = salonKapasitesi - doluKoltukSayisi;
        lblSeciliSeansBosKoltuk.setText("Seçili Seans ("+ secilenSeans.getFormatliSeansZamani() +") - Boş: " + bosKoltuk + " / Dolu: " + doluKoltukSayisi);

        for (int i = 1; i <= salonKapasitesi; i++) {
            final String koltukNo = String.valueOf(i);
            Button koltukButonu = new Button(koltukNo);
            koltukButonu.setPrefSize(40, 30);
            koltukButonu.setDisable(true); 

            if (doluKoltukNumaralari.contains(koltukNo)) {
            	koltukButonu.setStyle(
                        "-fx-background-color: #FF6347; " + 
                        "-fx-text-fill: white;");
            } else {
                koltukButonu.setStyle(
                    "-fx-background-color: #90EE90; " + 
                    "-fx-text-fill: #2E7D32;"      
                );
            }
            koltukButonu.getStyleClass().add("koltuk-buton-dashboard");
            flowPaneKoltuklarDetay.getChildren().add(koltukButonu);
        }
        
        if (flowPaneKoltuklarDetay.getChildren().isEmpty()){
            setKoltuklarPlaceholder("Bu seans için koltuk bilgisi yüklenemedi veya salon boş.");
        } else {
            clearKoltuklarPlaceholder(); 
        }
    }

    private void setKoltuklarPlaceholder(String text) {
        if (flowPaneKoltuklarDetay == null) return;
        clearKoltuklarPlaceholder();
        placeholderLabelKoltuklar = new Label(text);
        placeholderLabelKoltuklar.setStyle("-fx-padding: 10px; -fx-text-fill: #546E7A; -fx-font-style: italic;");
        flowPaneKoltuklarDetay.getChildren().add(placeholderLabelKoltuklar);
    }

    private void clearKoltuklarPlaceholder() {
        if (flowPaneKoltuklarDetay == null) return;
        if (placeholderLabelKoltuklar != null && flowPaneKoltuklarDetay.getChildren().contains(placeholderLabelKoltuklar)) {
            flowPaneKoltuklarDetay.getChildren().remove(placeholderLabelKoltuklar);
        }
        placeholderLabelKoltuklar = null;
    }

    @FXML
    private void handleAnaMenu() {
        try {
            Stage stage = (Stage) btnAnaMenu.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MainDashboard.fxml"));
            if (loader.getLocation() == null) {
                showAlert("Dosya Hatası", "'MainDashboard.fxml' bulunamadı.", Alert.AlertType.ERROR);
                return;
            }
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Ana Sayfa - Sinema Otomasyonu");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Yönlendirme Hatası", "Ana menüye dönülürken hata: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (btnAnaMenu != null && btnAnaMenu.getScene() != null && btnAnaMenu.getScene().getWindow() != null) {
             alert.initOwner(btnAnaMenu.getScene().getWindow());
        }
        alert.showAndWait();
    }
}