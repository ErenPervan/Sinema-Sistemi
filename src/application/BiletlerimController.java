// application/BiletlerimController.java
package application;

import application.model.KullaniciBileti;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType; 
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow; import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback; 

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp; 
import java.time.LocalDateTime;
import java.util.Optional; 

public class BiletlerimController {

    @FXML private TableView<KullaniciBileti> tableViewBiletlerim;
    @FXML private TableColumn<KullaniciBileti, String> colFilmAdi;
    @FXML private TableColumn<KullaniciBileti, String> colTurAdi;
    @FXML private TableColumn<KullaniciBileti, String> colSehirAdi;
    @FXML private TableColumn<KullaniciBileti, String> colSalonAdi;
    @FXML private TableColumn<KullaniciBileti, String> colSeansZamani;
    @FXML private TableColumn<KullaniciBileti, String> colKoltukNo;
    @FXML private TableColumn<KullaniciBileti, String> colAlinmaTarihi;
    @FXML private TableColumn<KullaniciBileti, Void> colBiletGoruntule;
    @FXML private TableColumn<KullaniciBileti, Void> colIptalEt;     @FXML private Button btnAnaMenuDon;
    @FXML private CheckBox checkEskiBiletleriGoster;

    private ObservableList<KullaniciBileti> biletListesi = FXCollections.observableArrayList();
    
    
    private static final long IPTAL_LIMIT_SAAT = 1; 

    public void initialize() {
        colFilmAdi.setCellValueFactory(new PropertyValueFactory<>("filmAdi"));
        colTurAdi.setCellValueFactory(new PropertyValueFactory<>("turAdi"));
        colSehirAdi.setCellValueFactory(new PropertyValueFactory<>("sehirAdi"));
        colSalonAdi.setCellValueFactory(new PropertyValueFactory<>("salonAdi"));
        colSeansZamani.setCellValueFactory(new PropertyValueFactory<>("seansZamaniStr"));
        colKoltukNo.setCellValueFactory(new PropertyValueFactory<>("koltukNumarasi"));
        colAlinmaTarihi.setCellValueFactory(new PropertyValueFactory<>("biletAlinmaTarihiStr"));

        colBiletGoruntule.setCellFactory(param -> new TableCell<>() {
            private final Button btnGoruntule = new Button("Görüntüle");
            {
                btnGoruntule.setOnAction(event -> {
                    KullaniciBileti bilet = getTableView().getItems().get(getIndex());
                    if (bilet != null) handleBiletGoruntule(bilet);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnGoruntule);
            }
        });

        colIptalEt.setCellFactory(param -> new TableCell<>() {
            private final Button btnIptal = new Button("İptal Et");
            {
                btnIptal.getStyleClass().add("delete-button");
                btnIptal.setOnAction(event -> {
                    KullaniciBileti bilet = getTableView().getItems().get(getIndex());
                    if (bilet != null) handleBiletIptal(bilet);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    KullaniciBileti bilet = getTableView().getItems().get(getIndex());
                    if (bilet != null && !bilet.isIptalEdildi() && 
                        bilet.getSeansZamaniGercek() != null && 
                        LocalDateTime.now().isBefore(bilet.getSeansZamaniGercek().minusHours(IPTAL_LIMIT_SAAT))) {
                        setGraphic(btnIptal);
                    } else {
                        setGraphic(null); 
                    }
                }
            }
        });
//SET STYLE İLE RENK PALETLERİNİ AYARLAMA
        tableViewBiletlerim.setRowFactory(tv -> new TableRow<KullaniciBileti>() {
            @Override
            protected void updateItem(KullaniciBileti bilet, boolean empty) {
                super.updateItem(bilet, empty);
                if (bilet == null || empty) {
                    setStyle("");
                } else if (bilet.isIptalEdildi()) {
                    setStyle("-fx-control-inner-background: #e0e0e0; -fx-text-fill: #757575; -fx-opacity: 0.7;"); 
                } else if (bilet.getSeansZamaniGercek() != null && bilet.getSeansZamaniGercek().isBefore(LocalDateTime.now())) {
                    setStyle("-fx-control-inner-background: #fff0f0; -fx-opacity: 0.8;"); 
                }
                 else {
                    setStyle(""); 
                }
            }
        });


        if (checkEskiBiletleriGoster != null) {
            checkEskiBiletleriGoster.setOnAction(event -> kullaniciBiletleriniYukle());
        }

        tableViewBiletlerim.setItems(biletListesi);
        kullaniciBiletleriniYukle();
    }

    private void kullaniciBiletleriniYukle() {
        biletListesi.clear();
        UserSession session = UserSession.getInstance();
        if (!session.isLoggedIn()) {
            showAlert("Giriş Gerekli", "Biletlerinizi görmek için lütfen giriş yapın.", Alert.AlertType.WARNING);
            tableViewBiletlerim.setPlaceholder(new Label("Biletlerinizi görmek için giriş yapmalısınız."));
            return;
        }

        int kullaniciID = session.getKullaniciID();
        boolean eskiBiletleriGosterSelected = (checkEskiBiletleriGoster != null && checkEskiBiletleriGoster.isSelected());

        StringBuilder queryBuilder = new StringBuilder(
            "SELECT r.RezervasyonID, f.Baslik AS FilmBaslik, t.TurAdi, c.SehirAdi, sl.SalonAdi, " +
            "s.SeansZamani, r.KoltukNumarasi, r.RezervasyonZamani, " +
            "r.BiletMusteriAdi, r.BiletMusteriSoyadi, r.IptalEdildiMi " + 
            "FROM Rezervasyonlar r " +
            "JOIN Seanslar s ON r.SeansID = s.SeansID " +
            "JOIN Filmler f ON s.FilmID = f.FilmID " +
            "LEFT JOIN Turler t ON f.TurID = t.TurID " +
            "JOIN SinemaSalonlari sl ON s.SalonID = sl.SalonID " +
            "JOIN Sehirler c ON sl.SehirID = c.SehirID " +
            "WHERE r.KullaniciID = ? ");

        if (!eskiBiletleriGosterSelected) {
            queryBuilder.append("AND (s.SeansZamani >= NOW() OR r.IptalEdildiMi = 1) ");
        }
        
        queryBuilder.append("ORDER BY s.SeansZamani DESC, r.RezervasyonZamani DESC");

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(queryBuilder.toString())) {
            
            pstmt.setInt(1, kullaniciID);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                LocalDateTime seansZamani = rs.getTimestamp("SeansZamani") != null ? rs.getTimestamp("SeansZamani").toLocalDateTime() : null;
                LocalDateTime rezervasyonZamani = rs.getTimestamp("RezervasyonZamani") != null ? rs.getTimestamp("RezervasyonZamani").toLocalDateTime() : null;
                String turAdi = rs.getString("TurAdi");
                String musteriAdi = rs.getString("BiletMusteriAdi");
                String musteriSoyadi = rs.getString("BiletMusteriSoyadi");
                boolean iptalEdildi = rs.getBoolean("IptalEdildiMi");
                
                biletListesi.add(new KullaniciBileti(
                        rs.getInt("RezervasyonID"),
                        rs.getString("FilmBaslik"),
                        rs.getString("SalonAdi"),
                        rs.getString("SehirAdi"),
                        seansZamani,
                        rs.getString("KoltukNumarasi"),
                        rezervasyonZamani,
                        turAdi == null ? "Belirtilmemiş" : turAdi,
                        musteriAdi == null ? "" : musteriAdi,
                        musteriSoyadi == null ? "" : musteriSoyadi,
                        iptalEdildi 
                ));
            }
            if (rs != null) rs.close();

             if (biletListesi.isEmpty()) {
                if (eskiBiletleriGosterSelected) {
                    tableViewBiletlerim.setPlaceholder(new Label("Hiç biletiniz bulunmamaktadır."));
                } else {
                    tableViewBiletlerim.setPlaceholder(new Label("Gelecek veya aktif seanslar için biletiniz bulunmamaktadır (iptal edilenler hariç)."));
                }
            }


        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Biletleriniz yüklenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            tableViewBiletlerim.setPlaceholder(new Label("Biletler yüklenirken bir hata oluştu."));
        }
    }
    
    private void handleBiletIptal(KullaniciBileti bilet) {
        if (bilet.isIptalEdildi()) {
            showAlert("Geçersiz İşlem", "Bu bilet zaten iptal edilmiş.", Alert.AlertType.INFORMATION);
            return;
        }

        if (bilet.getSeansZamaniGercek() == null || 
            !LocalDateTime.now().isBefore(bilet.getSeansZamaniGercek().minusHours(IPTAL_LIMIT_SAAT))) {
            showAlert("İptal Süresi Doldu", "Bu biletin seans zamanı geçtiği veya iptal süresi dolduğu için iptal edilemez.\n(İptal için seansa en az " + IPTAL_LIMIT_SAAT + " saat kalmış olmalıdır.)", Alert.AlertType.WARNING);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Bilet İptal Onayı");
        alert.setHeaderText("Bileti İptal Etmek Üzeresiniz");
        alert.setContentText(bilet.getFilmAdi() + " filmi için " + bilet.getSeansZamaniStr() + 
                             " seansındaki " + bilet.getKoltukNumarasi() + " numaralı koltuğa ait bileti iptal etmek istediğinizden emin misiniz?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String updateQuery = "UPDATE Rezervasyonlar SET IptalEdildiMi = 1, IptalZamani = NOW() WHERE RezervasyonID = ?";
            try (Connection conn = DbHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
                
                pstmt.setInt(1, bilet.getRezervasyonID());
                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    showAlert("Bilet İptal Edildi", "Biletiniz başarıyla iptal edildi.", Alert.AlertType.INFORMATION);
                    kullaniciBiletleriniYukle(); 
                } else {
                    showAlert("Hata", "Bilet iptal edilirken bir sorun oluştu.", Alert.AlertType.ERROR);
                }

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Veritabanı Hatası", "Bilet iptal edilirken veritabanı hatası oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void handleBiletGoruntule(KullaniciBileti bilet) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("PrintableTicket.fxml"));
            Parent root = loader.load();

            PrintableTicketController controller = loader.getController();
            controller.initData(bilet);

            Stage stage = new Stage();
            stage.setTitle("Bilet Detayı - Rezervasyon ID: " + bilet.getRezervasyonID());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            if (tableViewBiletlerim.getScene() != null && tableViewBiletlerim.getScene().getWindow() != null) {
                stage.initOwner(tableViewBiletlerim.getScene().getWindow());
            }
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Yükleme Hatası", "Bilet görüntüleme ekranı yüklenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
        } catch (NullPointerException e) {
            e.printStackTrace();
            showAlert("Dosya Hatası", "PrintableTicket.fxml dosyası bulunamadı.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleAnaMenuDon() {
         try {
            Stage stage = (Stage) btnAnaMenuDon.getScene().getWindow();
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
            showAlert("Yönlendirme Hatası", "Ana menüye dönülürken bir hata oluştu.", Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (btnAnaMenuDon != null && btnAnaMenuDon.getScene() != null && btnAnaMenuDon.getScene().getWindow() != null && btnAnaMenuDon.getScene().getWindow().isShowing()) {
             alert.initOwner(btnAnaMenuDon.getScene().getWindow());
        }
        alert.showAndWait();
    }
}