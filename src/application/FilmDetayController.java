// application/FilmDetayController.java
package application;

import application.model.Film;
import application.model.Salon;
import application.model.Seans;
import application.model.Tur;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.application.HostServices;
import java.net.URISyntaxException;
import java.net.URI;

public class FilmDetayController {

    @FXML private Label lblFilmDetayBaslik;
    @FXML private ImageView imgFilmDetayAfis;
    @FXML private Label lblFilmDetayTur;
    @FXML private Label lblFilmDetayYayinTarihi;
    @FXML private Label lblFilmDetaySure;
    @FXML private TextArea txtFilmDetayAciklama;
    @FXML private ComboBox<Seans> comboFilmSeanslari;
    @FXML private Label lblSeciliSeansBosKoltuk;
    @FXML private Button btnBiletAlDetay;
    @FXML private Button btnGeriDetay;
    @FXML 
    private Button btnFragmaniIzle;
    

    private Film secilenFilm;
    private ObservableList<Seans> seansListesi = FXCollections.observableArrayList();
    private Seans aktifSeciliSeans = null;

    private final String DEFAULT_POSTER_URL = "/application/icons/default_poster.png";

    public void initialize() {
        comboFilmSeanslari.setConverter(new StringConverter<Seans>() {
            @Override
            public String toString(Seans seans) {
                if (seans == null) return null;
                return seans.getSehirAdi() + " - " + seans.getSalonAdi() + " - " + seans.getFormatliSeansZamani();
            }

            @Override
            public Seans fromString(String string) {
                return null;
            }
        });

        comboFilmSeanslari.setItems(seansListesi);
        comboFilmSeanslari.setOnAction(event -> seansSecildi());
        btnBiletAlDetay.setDisable(true);
    }

    public void initData(Film film) {
        this.secilenFilm = film;
        lblFilmDetayBaslik.setText(film.getBaslik());
        txtFilmDetayAciklama.setText(film.getAciklama() != null ? film.getAciklama() : "Açıklama bulunmuyor.");

        if (film.getTur() != null && film.getTur().getTurAdi() != null) {
            lblFilmDetayTur.setText(film.getTur().getTurAdi());
        } else {
            lblFilmDetayTur.setText("Belirtilmemiş");
        }

        if (film.getYayinTarihi() != null) {
            lblFilmDetayYayinTarihi.setText(film.getYayinTarihi().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        } else {
            lblFilmDetayYayinTarihi.setText("-");
        }
        lblFilmDetaySure.setText(film.getSureDakika() > 0 ? String.valueOf(film.getSureDakika()) + " dk" : "-");

        Image afisImg;
        if (film.getAfisURL() != null && !film.getAfisURL().trim().isEmpty()) {
            try {
                afisImg = new Image(film.getAfisURL(), true); 
                if(afisImg.isError()){
                    System.err.println("Afiş yüklenemedi (initData - isError): " + film.getAfisURL());
                    afisImg = new Image(getClass().getResourceAsStream(DEFAULT_POSTER_URL));
                }
            } catch (Exception e) {
                System.err.println("Afiş yüklenemedi (initData - exception): " + film.getAfisURL() + " - " + e.getMessage());
                afisImg = new Image(getClass().getResourceAsStream(DEFAULT_POSTER_URL));
            }
        } else {
            afisImg = new Image(getClass().getResourceAsStream(DEFAULT_POSTER_URL));
        }
        imgFilmDetayAfis.setImage(afisImg);
        if (btnFragmaniIzle != null) {
            String fragmanURL = film.getFragmanURL();
            if (fragmanURL != null && !fragmanURL.trim().isEmpty()) {
                btnFragmaniIzle.setDisable(false);
            } else {
                btnFragmaniIzle.setDisable(true); 
            }
        }

        seanslariYukle(film.getFilmID());
    }

    private void seanslariYukle(int filmId) {
        seansListesi.clear();
        aktifSeciliSeans = null;
        btnBiletAlDetay.setDisable(true);
        lblSeciliSeansBosKoltuk.setText("Lütfen bir seans seçin.");

        String query = "SELECT s.SeansID, s.SeansZamani, s.IptalEdildiMi, " + 
                "sl.SalonID, sl.SalonAdi, sl.Kapasite AS SalonKapasite, " +
                "c.SehirID, c.SehirAdi, " +
                "(sl.Kapasite - (SELECT COUNT(*) FROM Rezervasyonlar r WHERE r.SeansID = s.SeansID AND r.IptalEdildiMi = 0)) AS BosKoltukSayisi " +
                "FROM Seanslar s " +
                "JOIN SinemaSalonlari sl ON s.SalonID = sl.SalonID " +
                "JOIN Sehirler c ON sl.SehirID = c.SehirID " +
                "WHERE s.FilmID = ? AND s.SeansZamani >= NOW() " +
                "AND IFNULL(s.IptalEdildiMi, 0) = 0 " + 
                "ORDER BY c.SehirAdi, sl.SalonAdi, s.SeansZamani";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, filmId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Salon salon = new Salon(rs.getInt("SalonID"), rs.getString("SalonAdi"), rs.getInt("SehirID"), rs.getString("SehirAdi"), rs.getInt("SalonKapasite"));
                LocalDateTime seansZamani = rs.getTimestamp("SeansZamani").toLocalDateTime();
                boolean iptalEdildi = rs.getBoolean("IptalEdildiMi"); 

                Seans seans = new Seans(rs.getInt("SeansID"), this.secilenFilm, salon, seansZamani, iptalEdildi); 
                seansListesi.add(seans);
            }
            if (rs != null) rs.close(); 
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Seanslar yüklenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
        }

        if (seansListesi.isEmpty()) {
            comboFilmSeanslari.setPromptText("Bu film için uygun seans bulunamadı.");
            lblSeciliSeansBosKoltuk.setText("");
        } else {
            comboFilmSeanslari.setPromptText("Seans Seçiniz");
        }
    }

    private void seansSecildi() {
        aktifSeciliSeans = comboFilmSeanslari.getValue();
        if (aktifSeciliSeans != null) {
            int bosKoltuk = getBosKoltukSayisiForSeans(aktifSeciliSeans.getSeansID(), aktifSeciliSeans.getSalon().getKapasite());

            lblSeciliSeansBosKoltuk.getStyleClass().removeAll("info-label-low-seats", "info-label-no-seats");
            lblSeciliSeansBosKoltuk.getStyleClass().add("info-label-dashboard");

            if (bosKoltuk <= 0) {
                lblSeciliSeansBosKoltuk.setText("Tüm Koltuklar Dolu!");
                lblSeciliSeansBosKoltuk.getStyleClass().add("info-label-no-seats");
                btnBiletAlDetay.setDisable(true);
            } else if (bosKoltuk <= 5) {
                lblSeciliSeansBosKoltuk.setText("Boş Koltuk: " + bosKoltuk + " (Az Kaldı!)");
                lblSeciliSeansBosKoltuk.getStyleClass().add("info-label-low-seats");
                btnBiletAlDetay.setDisable(false);
            } else {
                lblSeciliSeansBosKoltuk.setText("Boş Koltuk Sayısı: " + bosKoltuk);
                btnBiletAlDetay.setDisable(false);
            }
        } else {
            lblSeciliSeansBosKoltuk.setText("Lütfen bir seans seçin.");
            btnBiletAlDetay.setDisable(true);
        }
    }

    private int getBosKoltukSayisiForSeans(int seansID, int salonKapasitesi) {
    	String countQuery = "SELECT COUNT(*) AS DoluKoltukSayisi FROM Rezervasyonlar WHERE SeansID = ? AND IptalEdildiMi = 0";        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(countQuery)) {
            pstmt.setInt(1, seansID);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int doluKoltuk = rs.getInt("DoluKoltukSayisi");
                if (rs != null) rs.close(); 
                return salonKapasitesi - doluKoltuk;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; 
    }


    @FXML
    private void handleBiletAlDetay() {
        if (aktifSeciliSeans == null) {
            showAlert("Seans Seçilmedi", "Lütfen bilet almak için bir seans seçin.", Alert.AlertType.WARNING);
            return;
        }
        if (!UserSession.getInstance().isLoggedIn()) {
            showAlert("Giriş Gerekli", "Bilet alabilmek için lütfen giriş yapınız.", Alert.AlertType.WARNING);
            return;
        }
        int bosKoltuk = getBosKoltukSayisiForSeans(aktifSeciliSeans.getSeansID(), aktifSeciliSeans.getSalon().getKapasite());
        if (bosKoltuk <= 0) {
             showAlert("Koltuk Yok", "Bu seansta boş koltuk bulunmamaktadır.", Alert.AlertType.INFORMATION);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("KoltukSecimEkranı.fxml"));
            Parent root = loader.load();
            KoltukSecimController controller = loader.getController();
            controller.initData(
                aktifSeciliSeans.getSeansID(),
                secilenFilm.getFilmID(),
                aktifSeciliSeans.getSalon().getSalonID(),
                secilenFilm.getBaslik(),
                aktifSeciliSeans.getSalon().getSalonAdi(),
                aktifSeciliSeans.getFormatliSeansZamani()
            );

            Stage koltukStage = new Stage();
            koltukStage.setTitle("Koltuk Seçimi: " + secilenFilm.getBaslik());
            koltukStage.setScene(new Scene(root));
            koltukStage.initModality(Modality.WINDOW_MODAL);
            koltukStage.initOwner((Stage) btnBiletAlDetay.getScene().getWindow()); 

            koltukStage.setOnHidden(event -> {
                seansSecildi(); 
            });

            koltukStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Yükleme Hatası", "Koltuk seçimi ekranı yüklenirken bir sorun oluştu.", Alert.AlertType.ERROR);
        }
    }
    @FXML
    private void handleFragmaniIzle() {
        if (secilenFilm == null || secilenFilm.getFragmanURL() == null || secilenFilm.getFragmanURL().trim().isEmpty()) {
            showAlert("Fragman Bulunamadı", "Bu film için bir fragman URL'si belirtilmemiş.", Alert.AlertType.INFORMATION);
            return;
        }

        String fragmanURL = secilenFilm.getFragmanURL();

        
        //FRAGMAN İÇİN HORSERVİCES OLAYLARI
        HostServices hostServices = null;
        if (btnFragmaniIzle != null && btnFragmaniIzle.getScene() != null && btnFragmaniIzle.getScene().getWindow() != null) {
            hostServices = (HostServices) btnFragmaniIzle.getScene().getWindow().getProperties().get("hostServices");
        }

        if (hostServices != null) {            // URL'nin geçerli bir web URL'si olup olmadığını basitçe kontrol edebiliriz (http/https ile başlıyor mu?)
            if (fragmanURL.toLowerCase().startsWith("http://") || fragmanURL.toLowerCase().startsWith("https://")) {
                hostServices.showDocument(fragmanURL);
            } else {
 
                if (fragmanURL.matches("^[a-zA-Z0-9_-]{11}$")) { // Tipik YouTube ID formatı
                    hostServices.showDocument("http://www.youtube.com/embed/VIDEO_ID");
                } else {
                    showAlert("Geçersiz URL", "Fragman URL'si geçerli bir web adresi değil: " + fragmanURL, Alert.AlertType.ERROR);
                }
            }
        } else {
            showAlert("Hata", "Tarayıcı açılamadı. HostServices kullanılamıyor.", Alert.AlertType.ERROR);

            //Test kısımları
            // try {
            //     if (java.awt.Desktop.isDesktopSupported() &&
            //         (fragmanURL.toLowerCase().startsWith("http://") || fragmanURL.toLowerCase().startsWith("https://"))) {
            //         java.awt.Desktop.getDesktop().browse(new URI(fragmanURL));
            //     } else if (fragmanURL.matches("^[a-zA-Z0-9_-]{11}$")) {
            //            java.awt.Desktop.getDesktop().browse(new URI("http://www.youtube.com/embed/VIDEO_ID"));
            //     } else {
            //            showAlert("Geçersiz URL", "Fragman URL'si geçerli bir web adresi değil.", Alert.AlertType.ERROR);
            //     }
            // } catch (IOException | URISyntaxException | UnsupportedOperationException e) {
            //     showAlert("Hata", "Fragman tarayıcıda açılırken bir sorun oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            //     e.printStackTrace();
            // }
        }
    }

    @FXML
    private void handleGeriDetay() {
        Stage stage = (Stage) btnGeriDetay.getScene().getWindow();
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