// application/VizyondakiFilmlerController.java
package application;

import application.model.Film;
import application.model.Il; 
import application.model.Tur;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox; 
import javafx.scene.control.DatePicker; 
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter; 
import javafx.application.HostServices;


import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate; 
import java.util.ArrayList; 
import java.util.List; 

public class VizyondakiFilmlerController {

    @FXML private TilePane tilePaneFilmler;
    @FXML private ScrollPane scrollPaneFilmler;
    @FXML private Button btnAnaMenuDon;

    @FXML private ComboBox<Il> comboIllerFiltre;
    @FXML private DatePicker datePickerFiltre;
    @FXML private Button btnFiltreTemizle;

    private ObservableList<Il> ilFiltreListesi = FXCollections.observableArrayList();
    private final Il TUM_ILLER_SECENEGI = new Il(-1, "Tüm İller"); 

    private final String DEFAULT_POSTER_URL = "/application/icons/Pervancinema.png";

    public void initialize() {
        illeriYukleFiltreComboBox();
        datePickerFiltre.setValue(LocalDate.now()); // Varsayılan olarak bugünün tarihi

        // Filtreler değiştiğinde filmleri yeniden yükle
        comboIllerFiltre.setOnAction(event -> filmleriYukle());
        datePickerFiltre.setOnAction(event -> filmleriYukle());

        filmleriYukle(); // Başlangıçta filmleri yükle 
    }

    private void illeriYukleFiltreComboBox() {
        ilFiltreListesi.clear();
        ilFiltreListesi.add(TUM_ILLER_SECENEGI); // 

        String query = "SELECT SehirID, SehirAdi FROM Sehirler ORDER BY SehirAdi";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                ilFiltreListesi.add(new Il(rs.getInt("SehirID"), rs.getString("SehirAdi")));
            }
            comboIllerFiltre.setItems(ilFiltreListesi);
            comboIllerFiltre.setValue(TUM_ILLER_SECENEGI); 

            comboIllerFiltre.setConverter(new StringConverter<Il>() {
                @Override
                public String toString(Il il) {
                    return il == null ? null : il.getSehirAdi();
                }
                @Override
                public Il fromString(String string) { 
                    if (string == null) return null;
                    if (string.equals(TUM_ILLER_SECENEGI.getSehirAdi())) return TUM_ILLER_SECENEGI;
                    return ilFiltreListesi.stream()
                                          .filter(il -> il.getSehirAdi().equals(string))
                                          .findFirst()
                                          .orElse(null);
                }
            });

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "İller yüklenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    private void handleFiltreTemizle() {
        comboIllerFiltre.setValue(TUM_ILLER_SECENEGI);
        datePickerFiltre.setValue(LocalDate.now());
       
        
        filmleriYukle();
    }


    private void filmleriYukle() {
        tilePaneFilmler.getChildren().clear(); // Önceki filmleri temizle
        ObservableList<Film> vizyondakiFilmListesi = FXCollections.observableArrayList();

        Il secilenIl = comboIllerFiltre.getValue();
        LocalDate secilenTarih = datePickerFiltre.getValue();

        // Eğer tarih seçilmemişse varsayılan olarak bugünü kullan
        if (secilenTarih == null) { 
             secilenTarih = LocalDate.now(); 
             datePickerFiltre.setValue(secilenTarih); 
        }


        StringBuilder sql = new StringBuilder(
            "SELECT DISTINCT f.FilmID, f.Baslik, f.AfisURL, f.Aciklama, f.SureDakika, f.YayinTarihi, f.SistemeEklenmeTarihi, f.TurID, t.TurAdi, f.FragmanURL " +
            "FROM Filmler f " +
            "JOIN Seanslar s ON f.FilmID = s.FilmID " +
            "JOIN SinemaSalonlari sl ON s.SalonID = sl.SalonID " +
            "JOIN Sehirler c ON sl.SehirID = c.SehirID " +
            "LEFT JOIN Turler t ON f.TurID = t.TurID " +
            "WHERE 1=1 " 
        );

        List<Object> parametreler = new ArrayList<>();

        // Tarih Filtresi
        if (secilenTarih.isEqual(LocalDate.now())) {
            sql.append("AND DATE(s.SeansZamani) = CURDATE() AND s.SeansZamani >= NOW() ");
        } else if (secilenTarih.isAfter(LocalDate.now())) {
            sql.append("AND DATE(s.SeansZamani) = ? ");
            parametreler.add(java.sql.Date.valueOf(secilenTarih));
        } else { 
            sql.append("AND DATE(s.SeansZamani) = ? "); 
            parametreler.add(java.sql.Date.valueOf(secilenTarih));

        }

        // İl Filtresi
        if (secilenIl != null && secilenIl.getSehirID() != TUM_ILLER_SECENEGI.getSehirID()) { 
            sql.append("AND c.SehirID = ? ");
            parametreler.add(secilenIl.getSehirID());
        }

        sql.append("ORDER BY f.Baslik ASC");

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < parametreler.size(); i++) {
                pstmt.setObject(i + 1, parametreler.get(i));
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Tur tur = null;
                int turID = rs.getInt("TurID");
                if (!rs.wasNull()) {
                    tur = new Tur(turID, rs.getString("TurAdi"));
                }
                
                LocalDate yayinTarihi = rs.getDate("YayinTarihi") != null ? rs.getDate("YayinTarihi").toLocalDate() : null;
                LocalDate sistemeEklenmeTarihi = rs.getDate("SistemeEklenmeTarihi") != null ? rs.getDate("SistemeEklenmeTarihi").toLocalDate() : null;
                String fragmanURL = rs.getString("FragmanURL");

                vizyondakiFilmListesi.add(new Film(
                        rs.getInt("FilmID"), rs.getString("Baslik"), tur,
                        rs.getInt("SureDakika"), rs.getString("Aciklama"), yayinTarihi,
                        rs.getString("AfisURL"), sistemeEklenmeTarihi, fragmanURL
                ));
            }
             if (rs != null) rs.close();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Vizyondaki filmler yüklenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            return; 
        }

        if (vizyondakiFilmListesi.isEmpty()) {
            Label placeholder = new Label("Belirtilen kriterlere uygun vizyonda film bulunmamaktadır.");
            placeholder.setStyle("-fx-font-size: 16px; -fx-text-fill: #555555; -fx-padding: 20px;");
          
            tilePaneFilmler.getChildren().add(placeholder);
        } else {
            for (Film film : vizyondakiFilmListesi) {
                VBox posterKutusu = createPosterKutusu(film);
                tilePaneFilmler.getChildren().add(posterKutusu);
            }
        }
    }

    private VBox createPosterKutusu(Film film) {
        VBox kutu = new VBox(8); // Elemanlar arası boşluk
        kutu.getStyleClass().add("poster-tile");
        kutu.setAlignment(Pos.TOP_CENTER);

        ImageView afisView = new ImageView();
        afisView.setFitWidth(160);
        afisView.setFitHeight(240);
        afisView.setPreserveRatio(false);
        afisView.getStyleClass().add("image-view");

        String afisURL = film.getAfisURL();
        Image imageToLoad; // Bu değişken, URL'den yüklenmeye çalışılacak Image nesnesini tutacak

        if (afisURL != null && !afisURL.trim().isEmpty()) {
            try {
                imageToLoad = new Image(afisURL, true); // Arka planda yükle 

                // Listener'ı bu spesifik 'imageToLoad' nesnesine ekle
                final Image finalImageAttempt = imageToLoad; // Lambda için effectively final kopya
                finalImageAttempt.errorProperty().addListener((obs, oldError, newError) -> {
                    if (newError) {
                        System.err.println("Afiş yüklenemedi (asenkron listener): " + afisURL +
                                           (finalImageAttempt.getException() != null ? " - " + finalImageAttempt.getException().getMessage() : ""));
                        // Hata durumunda ImageView'a varsayılan resmi ata
                        afisView.setImage(new Image(getClass().getResourceAsStream(DEFAULT_POSTER_URL)));
                    }
                });

                // İlk oluşturma denemesinde hemen bir hata oluştu mu kontrol et
                if (imageToLoad.isError()) {
                    System.err.println("Afiş yüklenemedi (ilk senkron kontrol): " + afisURL +
                                       (imageToLoad.getException() != null ? " - " + imageToLoad.getException().getMessage() : ""));
              
                    afisView.setImage(new Image(getClass().getResourceAsStream(DEFAULT_POSTER_URL)));
                } else {
                    afisView.setImage(imageToLoad);
                }

            } catch (IllegalArgumentException iae) { // URL formatı bozuksa
                System.err.println("Geçersiz Afiş URL'si: " + afisURL + " - " + iae.getMessage());
                afisView.setImage(new Image(getClass().getResourceAsStream(DEFAULT_POSTER_URL)));
            } catch (Exception e) { 
                System.err.println("Afiş URL'si (" + afisURL + ") yüklenirken genel istisna: " + e.getMessage());
                afisView.setImage(new Image(getClass().getResourceAsStream(DEFAULT_POSTER_URL)));
            }
        } else {
            // URL boş veya null ise varsayılan resmi ata
            afisView.setImage(new Image(getClass().getResourceAsStream(DEFAULT_POSTER_URL)));
        }

        Label baslikLabel = new Label(film.getBaslik());
        kutu.getChildren().addAll(afisView, baslikLabel);

        kutu.setOnMouseClicked(event -> {
            filmDetaylariniGoster(film);
        });

        return kutu;
    }
    
    private void filmDetaylariniGoster(Film film) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FilmDetayEkrani.fxml"));
            Parent root = loader.load();

            FilmDetayController controller = loader.getController();
            if (controller != null) {
                controller.initData(film);
            } else {
                System.err.println("FilmDetayController yüklenemedi!");
                showAlert("Hata", "Film detayları controller'ı yüklenemedi.", Alert.AlertType.ERROR);
                return;
            }

            Stage detayStage = new Stage();
            detayStage.setTitle("Film Detayları: " + film.getBaslik());
            detayStage.setScene(new Scene(root));
            detayStage.initModality(Modality.WINDOW_MODAL);

            Stage ownerStage = null;
            if (tilePaneFilmler != null && tilePaneFilmler.getScene() != null && tilePaneFilmler.getScene().getWindow() != null) {
                ownerStage = (Stage) tilePaneFilmler.getScene().getWindow();
                detayStage.initOwner(ownerStage);
                
                // HostServices'ı aktar
                if (ownerStage.getProperties().containsKey("hostServices")) {
                    HostServices hostServices = (HostServices) ownerStage.getProperties().get("hostServices");
                    detayStage.getProperties().put("hostServices", hostServices);
                } else {
                    System.err.println("UYARI (VizyondakiFilmlerController): Ana pencerede 'hostServices' bulunamadı. Fragman linki çalışmayabilir.");
                }
            } else {
                 System.err.println("UYARI (VizyondakiFilmlerController): FilmDetayEkrani için sahip pencere (owner) bulunamadı. 'hostServices' aktarılamadı.");
            }
            detayStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Yükleme Hatası", "Film detayları ekranı yüklenirken bir sorun oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
        } catch (NullPointerException e) { 
             e.printStackTrace();
             showAlert("Dosya Hatası", "FilmDetayEkrani.fxml dosyası bulunamadı. Lütfen dosya yolunu kontrol edin.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleAnaMenuDon() {
        try {
            Stage stage = (Stage) btnAnaMenuDon.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("MainDashboard.fxml"));
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Ana Sayfa - Sinema Otomasyonu");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Yönlendirme Hatası", "Ana menüye dönülürken bir hata oluştu.", Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        // Sahip pencereyi ayarla (mümkünse)
        if (btnAnaMenuDon != null && btnAnaMenuDon.getScene() != null && btnAnaMenuDon.getScene().getWindow() != null) {
            alert.initOwner(btnAnaMenuDon.getScene().getWindow());
        }
        alert.showAndWait();
    }
}