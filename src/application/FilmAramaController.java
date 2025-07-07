// application/FilmAramaController.java
package application;

import application.model.FilmAramaSonucu;
import application.model.Il;
import application.model.Tur;
import javafx.application.HostServices; 
import javafx.application.Platform; 
import javafx.beans.value.ChangeListener; 
import javafx.beans.value.ObservableValue; 
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets; 
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;       
import javafx.scene.image.ImageView; 
import javafx.scene.layout.StackPane; 
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class FilmAramaController {

    @FXML private ComboBox<Il> comboIllerArama;
    @FXML private TextField txtAranacakFilmAdi;
    @FXML private Label lblAranacakFilmAdi;
    @FXML private Button btnFilmAra;
    @FXML private CheckBox checkIleriTarihler;
    @FXML private CheckBox checkTumFilmleriListele;
    @FXML private TableView<FilmAramaSonucu> tableViewFilmAramaSonuclari;
    @FXML private TableColumn<FilmAramaSonucu, String> colAramaFilmAdi;
    @FXML private TableColumn<FilmAramaSonucu, String> colAramaFilmTuru;
    @FXML private TableColumn<FilmAramaSonucu, Void> colAramaAfisGoruntule;
    @FXML private TableColumn<FilmAramaSonucu, String> colAramaSalonAdi;
    @FXML private TableColumn<FilmAramaSonucu, String> colAramaSehirAdi;
    @FXML private TableColumn<FilmAramaSonucu, String> colAramaSeansZamani;
    @FXML private TableColumn<FilmAramaSonucu, Integer> colAramaBosKoltuk;
    @FXML private TableColumn<FilmAramaSonucu, Void> colAramaBiletAl;
    @FXML private Button btnAnaMenuDon;
    @FXML private Button btnTemizleFiltreler;


    @FXML private ComboBox<Tur> comboTurlerArama;
    @FXML private DatePicker datePickerAramaTarih;

    private ObservableList<FilmAramaSonucu> filmAramaListesi = FXCollections.observableArrayList();
    private ObservableList<Il> ilComboListesi = FXCollections.observableArrayList();
    private ObservableList<Tur> turComboListesiArama = FXCollections.observableArrayList();

    private final Tur TUM_TURLER_SECENEGI = new Tur(-1, "Tüm Türler");
    private final String DEFAULT_POSTER_URL = "/application/icons/default_poster.png"; 


    public void initialize() {
        filmAramaListesi.clear();
        tableViewFilmAramaSonuclari.setPlaceholder(new Label("Arama yapmak için filtreleri kullanın ve 'Film Ara' butonuna basın."));
        tableViewFilmAramaSonuclari.setItems(filmAramaListesi);

        comboIllerArama.setConverter(new StringConverter<Il>() {
            @Override public String toString(Il il) { return il == null ? null : il.getSehirAdi(); }
            @Override public Il fromString(String string) { return null; }
        });
        illeriYukleAramaComboBox();

        comboTurlerArama.setConverter(new StringConverter<Tur>() {
            @Override public String toString(Tur tur) { return tur == null ? null : tur.getTurAdi(); }
            @Override public Tur fromString(String string) { return null; }
        });
        filmTurleriniYukleAramaComboBox();

        colAramaFilmAdi.setCellValueFactory(new PropertyValueFactory<>("filmAdi"));
        colAramaFilmTuru.setCellValueFactory(new PropertyValueFactory<>("turAdi"));
        colAramaSalonAdi.setCellValueFactory(new PropertyValueFactory<>("salonAdi"));
        colAramaSehirAdi.setCellValueFactory(new PropertyValueFactory<>("sehirAdi"));
        colAramaSeansZamani.setCellValueFactory(new PropertyValueFactory<>("seansZamaniStr"));
        colAramaBosKoltuk.setCellValueFactory(new PropertyValueFactory<>("bosKoltukSayisi"));

        colAramaAfisGoruntule.setCellFactory(param -> new TableCell<>() {
            private final Button btnGoruntule = new Button("Görüntüle");

            {
                btnGoruntule.setOnAction(event -> {
                    FilmAramaSonucu sonuc = getTableView().getItems().get(getIndex());
                    if (sonuc != null) {
                        handleGoruntuleAfis(sonuc);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    FilmAramaSonucu sonuc = getTableView().getItems().get(getIndex());
                    btnGoruntule.setDisable(sonuc == null || sonuc.getAfisURL() == null || sonuc.getAfisURL().trim().isEmpty());
                    setGraphic(btnGoruntule);
                }
            }
        });

        colAramaBiletAl.setCellFactory(param -> new TableCell<>() {
            private final Button biletAlButonu = new Button("Bilet Al");
            {
                biletAlButonu.getStyleClass().add("action-button");
                biletAlButonu.setOnAction(event -> {
                    FilmAramaSonucu sonuc = getTableView().getItems().get(getIndex());
                    if (sonuc != null) {
                        handleKoltukSecimiAramaSonucu(sonuc);
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    FilmAramaSonucu sonuc = getTableView().getItems().get(getIndex());
                    if (sonuc != null && sonuc.getBosKoltukSayisi() > 0) {
                        biletAlButonu.setDisable(false);
                    } else {
                        biletAlButonu.setDisable(true);
                    }
                    setGraphic(biletAlButonu);
                }
            }
        });

        checkTumFilmleriListele.setOnAction(event -> handleTumFilmleriListeleCheck());
        
        checkIleriTarihler.setOnAction(event -> {
            if (checkIleriTarihler.isSelected()) {
                datePickerAramaTarih.setValue(null);
                datePickerAramaTarih.setDisable(true);
            } else {
                datePickerAramaTarih.setDisable(false);
            }
        });
    }

    private void illeriYukleAramaComboBox() {
        ilComboListesi.clear();
        String query = "SELECT SehirID, SehirAdi FROM Sehirler ORDER BY SehirAdi";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                ilComboListesi.add(new Il(rs.getInt("SehirID"), rs.getString("SehirAdi")));
            }
            comboIllerArama.setItems(ilComboListesi);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Arama için iller yüklenirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void filmTurleriniYukleAramaComboBox() {
        turComboListesiArama.clear();
        turComboListesiArama.add(TUM_TURLER_SECENEGI);

        String query = "SELECT TurID, TurAdi FROM Turler ORDER BY TurAdi";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                turComboListesiArama.add(new Tur(rs.getInt("TurID"), rs.getString("TurAdi")));
            }
            comboTurlerArama.setItems(turComboListesiArama);
            comboTurlerArama.setValue(TUM_TURLER_SECENEGI);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Arama için film türleri yüklenirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    private void handleTumFilmleriListeleCheck() {
        if (checkTumFilmleriListele.isSelected()) {
            txtAranacakFilmAdi.setDisable(true);
            txtAranacakFilmAdi.clear();
            lblAranacakFilmAdi.setDisable(true);
        } else {
            txtAranacakFilmAdi.setDisable(false);
            lblAranacakFilmAdi.setDisable(false);
        }
    }

    @FXML
    private void handleFilmIleGoreAra() {
        Il secilenIl = comboIllerArama.getValue();
        String arananFilmParcasi = txtAranacakFilmAdi.getText().trim();
        boolean ileriTarihleriGoster = checkIleriTarihler.isSelected();
        boolean tumFilmleriGoster = checkTumFilmleriListele.isSelected();
        Tur secilenTur = comboTurlerArama.getValue();
        LocalDate secilenTarih = datePickerAramaTarih.getValue();

        if (secilenIl == null) {
            showAlert("Eksik Bilgi", "Lütfen arama yapmak için bir il seçin.", Alert.AlertType.WARNING);
            return;
        }

        if (!tumFilmleriGoster && arananFilmParcasi.isEmpty()) {
            showAlert("Eksik Bilgi", "Lütfen bir film adı girin veya 'Bu İldeki Tüm Filmleri Listele' seçeneğini işaretleyin.", Alert.AlertType.WARNING);
            return;
        }

        if (!ileriTarihleriGoster && secilenTarih == null) {
            showAlert("Tarih Seçimi Eksik", "Lütfen bir tarih seçin veya 'İleriki Tarihleri de Göster' seçeneğini işaretleyin.", Alert.AlertType.WARNING);
            return;
        }

        filmAramaListesi.clear();
       

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT f.FilmID, f.Baslik AS FilmBaslik, f.AfisURL, sl.SalonID, sl.SalonAdi, c.SehirAdi, s.SeansID, ")
                  .append("t.TurAdi, ")
                  .append("DATE_FORMAT(s.SeansZamani, '%d.%m.%Y %H:%i') AS FormatliSeansZamani, ")
                  //  BosKoltukSayisi hesaplaması
                  .append("(sl.Kapasite - (SELECT COUNT(*) FROM Rezervasyonlar r WHERE r.SeansID = s.SeansID AND r.IptalEdildiMi = 0)) AS BosKoltukSayisi ") 
                  .append("FROM Seanslar s ")
                  .append("JOIN Filmler f ON s.FilmID = f.FilmID ")
                  .append("LEFT JOIN Turler t ON f.TurID = t.TurID ")
                  .append("JOIN SinemaSalonlari sl ON s.SalonID = sl.SalonID ")
                  .append("JOIN Sehirler c ON sl.SehirID = c.SehirID ")
                  .append("WHERE c.SehirID = ? ");

        if (secilenTarih != null) {
            sqlBuilder.append("AND DATE(s.SeansZamani) = ? ");
        } else if (ileriTarihleriGoster) {
            sqlBuilder.append("AND s.SeansZamani >= NOW() ");
        } else { 
            sqlBuilder.append("AND DATE(s.SeansZamani) = CURDATE() AND s.SeansZamani >= NOW() ");
        }

        if (!tumFilmleriGoster && !arananFilmParcasi.isEmpty()) {
            sqlBuilder.append("AND f.Baslik LIKE ? ");
        }

        if (secilenTur != null && secilenTur.getTurID() != -1) {
            sqlBuilder.append("AND f.TurID = ? ");
        }

        sqlBuilder.append("ORDER BY s.SeansZamani, f.Baslik, sl.SalonAdi");

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {

            int paramIndex = 1;
            pstmt.setInt(paramIndex++, secilenIl.getSehirID());

            if (secilenTarih != null) {
                pstmt.setDate(paramIndex++, java.sql.Date.valueOf(secilenTarih));
            }

            if (!tumFilmleriGoster && !arananFilmParcasi.isEmpty()) {
                pstmt.setString(paramIndex++, "%" + arananFilmParcasi + "%");
            }

            if (secilenTur != null && secilenTur.getTurID() != -1) {
                pstmt.setInt(paramIndex++, secilenTur.getTurID());
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String turAdi = rs.getString("TurAdi");
                String afisURL = rs.getString("AfisURL");
                filmAramaListesi.add(new FilmAramaSonucu(
                        rs.getString("FilmBaslik"),
                        rs.getString("SalonAdi"),
                        rs.getString("SehirAdi"),
                        rs.getString("FormatliSeansZamani"),
                        rs.getInt("BosKoltukSayisi"),
                        rs.getInt("SeansID"),
                        rs.getInt("FilmID"),
                        rs.getInt("SalonID"),
                        turAdi == null ? "Belirtilmemiş" : turAdi,
                        afisURL
                ));
            }
            if (rs != null) rs.close();
            
            if (filmAramaListesi.isEmpty()) {
                 showAlert("Sonuç Bulunamadı", "Belirtilen kriterlere uygun film seansı bulunamadı.", Alert.AlertType.INFORMATION);
                 handleAnaMenuDon(); 
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Film arama sırasında bir veritabanı hatası oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            tableViewFilmAramaSonuclari.setPlaceholder(new Label("Arama sırasında bir veritabanı hatası oluştu."));
        }
    }
    
    @FXML
    private void handleTemizleFiltreler() {
        comboIllerArama.getSelectionModel().clearSelection();
        txtAranacakFilmAdi.clear();
        if (turComboListesiArama.contains(TUM_TURLER_SECENEGI)) {
             comboTurlerArama.setValue(TUM_TURLER_SECENEGI);
        } else {
            comboTurlerArama.getSelectionModel().clearSelection();
        }

        datePickerAramaTarih.setValue(null);

        checkTumFilmleriListele.setSelected(false);
        checkIleriTarihler.setSelected(false);

        handleTumFilmleriListeleCheck(); 

        if (checkIleriTarihler.isSelected()) {
            datePickerAramaTarih.setValue(null);
            datePickerAramaTarih.setDisable(true);
        } else {
            datePickerAramaTarih.setDisable(false);
        }

        filmAramaListesi.clear(); 
        tableViewFilmAramaSonuclari.setPlaceholder(new Label("Arama yapmak için filtreleri kullanın ve 'Film Ara' butonuna basın."));
    }


    private void handleKoltukSecimiAramaSonucu(FilmAramaSonucu secilenAramaSonucu) {
        if (secilenAramaSonucu == null) return;

        if (!UserSession.getInstance().isLoggedIn()){
            showAlert("Giriş Gerekli", "Bilet alabilmek için lütfen giriş yapınız.", Alert.AlertType.WARNING);
            return;
        }
        if (secilenAramaSonucu.getBosKoltukSayisi() <= 0) {
            showAlert("Koltuk Yok", "Bu seansta boş koltuk bulunmamaktadır.", Alert.AlertType.INFORMATION);
            return;
        }

        try {
            String fxmlPath = "/application/KoltukSecimEkranı.fxml";
            URL fxmlUrl = getClass().getResource(fxmlPath);
             if (fxmlUrl == null) {
                 showAlert("Dosya Hatası", "'" + fxmlPath + "' bulunamadı.", Alert.AlertType.ERROR);
                 return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            KoltukSecimController koltukSecimController = loader.getController(); 
             if (koltukSecimController == null) { 
                showAlert("Controller Hatası", "Koltuk seçimi ekranının controller'ı yüklenemedi.", Alert.AlertType.ERROR);
                return;
            }
            koltukSecimController.initData( 
                    secilenAramaSonucu.getSeansID(),
                    secilenAramaSonucu.getFilmID(),
                    secilenAramaSonucu.getSalonID(),
                    secilenAramaSonucu.getFilmAdi(),
                    secilenAramaSonucu.getSalonAdi(),
                    secilenAramaSonucu.getSeansZamaniStr()
            );

            Stage koltukStage = new Stage(); 
            koltukStage.setTitle("Koltuk Seçimi ve Bilet Alma");
            koltukStage.setScene(new Scene(root));
            koltukStage.initModality(Modality.APPLICATION_MODAL);
            
            Stage anaPencere = null;
            if(btnFilmAra != null && btnFilmAra.getScene() != null) { 
                 anaPencere = (Stage) btnFilmAra.getScene().getWindow();
            }
            if (anaPencere != null) {
                koltukStage.initOwner(anaPencere);
            }

            koltukStage.showAndWait(); 

            if (koltukSecimController.isBiletBasariylaAlindi()) {
                showAlert("Bilgi", "Biletiniz alınmıştır. Ana menüye yönlendiriliyorsunuz.", Alert.AlertType.INFORMATION);
                handleAnaMenuDon(); 
            } else {
                System.out.println("Bilet alma işlemi tamamlanmadı veya iptal edildi. (FilmAramaController)");

            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Yükleme Hatası", "Koltuk seçimi ekranı açılırken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleGoruntuleAfis(FilmAramaSonucu filmSonucu) {
        String afisURL = filmSonucu.getAfisURL();
        String filmAdi = filmSonucu.getFilmAdi();

        if (afisURL == null || afisURL.trim().isEmpty()) {
            showAlert("Afiş Yok", "Bu film için bir afiş URL'si bulunmamaktadır.", Alert.AlertType.INFORMATION);
            return;
        }

        try {
            Image image = new Image(afisURL, true); 

            image.errorProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    if (newValue) { 
                        System.err.println("Afiş yüklenemedi (Image error listener): " + afisURL);
                        if (image.getException() != null) {
                            image.getException().printStackTrace();
                        }
                        Platform.runLater(() -> { 
                            if (afisURL.toLowerCase().startsWith("http://") || afisURL.toLowerCase().startsWith("https://")) {
                                try {
                                     Stage primaryStage = (Stage) tableViewFilmAramaSonuclari.getScene().getWindow();
                                     HostServices hostServices = (HostServices) primaryStage.getProperties().get("hostServices");

                                     if (hostServices != null) {
                                        hostServices.showDocument(afisURL);
                                     } else {
                                        showAlert("Tarayıcı Açılamadı", "Afiş URL'si: " + afisURL + "\nBu URL'yi tarayıcınızda açabilirsiniz.", Alert.AlertType.INFORMATION);
                                     }
                                } catch (Exception browserEx) {
                                    browserEx.printStackTrace();
                                    showAlert("Hata", "Afiş URL'si tarayıcıda açılırken bir hata oluştu: " + browserEx.getMessage(), Alert.AlertType.ERROR);
                                }
                            } else {
                                showAlert("Afiş Yüklenemedi", "Afiş resmi yüklenemedi. URL geçerli bir web adresi değil veya dosya yolu hatalı.\nURL: " + afisURL, Alert.AlertType.ERROR);
                            }
                        });
                        image.errorProperty().removeListener(this); 
                    }
                }
            });

            image.widthProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    if (newValue.doubleValue() > 0 && image.heightProperty().get() > 0 && !image.isError()) {
                        Platform.runLater(() -> gosterAfisPenceresi(image, filmAdi, afisURL));
                        image.widthProperty().removeListener(this); 
                    }
                }
            });
            
            if (image.getWidth() > 0 && image.getHeight() > 0 && !image.isError()) {
                 gosterAfisPenceresi(image, filmAdi, afisURL);
            } else if (image.isError()) {
                System.err.println("Afiş yüklenemedi (ilk kontrol): " + afisURL);
            }

        } catch (IllegalArgumentException iae) {
            System.err.println("Geçersiz Afiş URL'si (IllegalArgumentException): " + afisURL + " - " + iae.getMessage());
            showAlert("Afiş Yüklenemedi", "Afiş URL'si ('" + afisURL + "') geçerli değil veya desteklenmiyor.", Alert.AlertType.ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Hata", "Afiş görüntülenirken beklenmedik bir hata oluştu: " + e.getMessage() + "\nURL: " + afisURL, Alert.AlertType.ERROR);
        }
    }

    private void gosterAfisPenceresi(Image image, String filmAdi, String afisURLdebug) {
        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);

        double originalWidth = image.getWidth();
        double originalHeight = image.getHeight();

        double maxDisplayWidth = 800;
        double maxDisplayHeight = 700;

        double displayWidth = originalWidth;
        double displayHeight = originalHeight;

        if (originalWidth > 0 && originalHeight > 0) {
            if (displayWidth > maxDisplayWidth) {
                displayHeight = (maxDisplayWidth / displayWidth) * displayHeight;
                displayWidth = maxDisplayWidth;
            }
            if (displayHeight > maxDisplayHeight) {
                displayWidth = (maxDisplayHeight / displayHeight) * displayWidth;
                displayHeight = maxDisplayHeight;
            }
        } else {
            System.err.println("gosterAfisPenceresi çağrıldı ama resim boyutları hala 0. URL: " + afisURLdebug);
            showAlert("Afiş Sorunu", "Afiş resmi boyutları alınamadı.", Alert.AlertType.WARNING);
            return;
        }

        displayWidth = Math.max(displayWidth, 300);
        displayHeight = Math.max(displayHeight, 200);

        imageView.setFitWidth(displayWidth);
        imageView.setFitHeight(displayHeight);

        StackPane layout = new StackPane(imageView);
        layout.setPadding(new Insets(10));

        Stage afisStage = new Stage();
        afisStage.setTitle("Afiş: " + filmAdi);

        if (tableViewFilmAramaSonuclari != null && tableViewFilmAramaSonuclari.getScene() != null && tableViewFilmAramaSonuclari.getScene().getWindow() != null) {
            afisStage.initOwner(tableViewFilmAramaSonuclari.getScene().getWindow());
            afisStage.initModality(Modality.WINDOW_MODAL);
        } else {
             System.err.println("Afiş penceresi için ana pencere (owner) bulunamadı.");
        }

        Scene scene = new Scene(layout);
        afisStage.setScene(scene);

        afisStage.sizeToScene();
        afisStage.centerOnScreen();

        afisStage.showAndWait();
    }


    @FXML
    private void handleAnaMenuDon() {
        try {
            Stage stage = (Stage) btnAnaMenuDon.getScene().getWindow();
            if (stage == null && tableViewFilmAramaSonuclari != null && tableViewFilmAramaSonuclari.getScene() != null) {
                 stage = (Stage) tableViewFilmAramaSonuclari.getScene().getWindow();
            }
            if (stage == null) {
                 System.err.println("Ana Menüye Dönüş: Mevcut sahne (stage) bulunamadı.");
                 showAlert("Hata", "Ana menüye dönmek için mevcut pencereye ulaşılamadı.", Alert.AlertType.ERROR);
                 return;
            }

            String fxmlPath = "/application/MainDashboard.fxml";
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                 showAlert("Dosya Hatası", "'" + fxmlPath + "' bulunamadı.", Alert.AlertType.ERROR);
                 return;
            }
            Parent root = FXMLLoader.load(fxmlUrl);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Ana Sayfa - Sinema Otomasyonu");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Yönlendirme Hatası", "Ana menüye dönülürken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
        }  catch (IllegalStateException e) {
             System.err.println("Ana Menüye Dönüş Hatası (IllegalStateException): " + e.getMessage());
             showAlert("Program Hatası", "Ana menüye dönülürken bir iç hata oluştu. Lütfen geliştiriciye bildirin.", Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        if (tableViewFilmAramaSonuclari != null && tableViewFilmAramaSonuclari.getScene() != null && tableViewFilmAramaSonuclari.getScene().getWindow() != null && tableViewFilmAramaSonuclari.getScene().getWindow().isShowing()) {
            alert.initOwner(tableViewFilmAramaSonuclari.getScene().getWindow());
        }
        alert.showAndWait();
    }
}