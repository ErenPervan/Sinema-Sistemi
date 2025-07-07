// application/KoltukSecimController.java
package application;

import application.model.KullaniciBileti;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;


import java.util.ArrayList;
import java.util.List;

public class KoltukSecimController {

    @FXML private Label lblFilmAdi;
    @FXML private Label lblSalonVeSeans;
    @FXML private FlowPane flowPaneKoltuklar;
    @FXML private Label lblSecilenKoltuk;
    @FXML private TextField txtMusteriAdi;
    @FXML private TextField txtMusteriSoyadi;
    @FXML private Button btnBiletAl;
    @FXML private Button btnGeri;

    private int seansID;
    private int filmID;
    private int salonID;
    private String filmAdiStr;
    private String salonAdiStr;
    private String seansZamaniStr;
    private String aktifSeciliKoltukNo = null;

    private int salonKapasitesi = 15; 
    private List<Button> koltukButonlari = new ArrayList<>();

    private final String TEMEL_KOLTUK_STILI = "-fx-font-weight: bold; -fx-font-size: 12px; -fx-border-color: #B0BEC5; -fx-border-width: 1px; -fx-border-radius: 3px; -fx-background-radius: 3px; -fx-padding: 5px 8px;";
    private final String BOS_KOLTUK_STILI = TEMEL_KOLTUK_STILI + " -fx-background-color: #A5D6A7; -fx-text-fill: #1B5E20;";
    private final String DOLU_KOLTUK_FALLBACK_STILI = TEMEL_KOLTUK_STILI + " -fx-background-color: #EF9A9A; -fx-text-fill: white; -fx-opacity: 0.8;";
    private final String SECILI_KOLTUK_STILI = TEMEL_KOLTUK_STILI + " -fx-background-color: #7986CB; -fx-text-fill: white; -fx-border-color: #3F51B5; -fx-border-width: 2px;";

    private boolean biletBasariylaAlindi = false;
    private Image doluKoltukResmi;

    public void initialize() {
        lblSecilenKoltuk.setText("Seçilen Koltuk: -");
        try {
            doluKoltukResmi = new Image(getClass().getResourceAsStream("/application/icons/reserved1.png"));
            if (doluKoltukResmi.isError()) {
                 System.err.println("Dolu koltuk resmi yüklenirken hata (initialize): " + (doluKoltukResmi.getException() != null ? doluKoltukResmi.getException().getMessage() : "Bilinmeyen resim yükleme hatası"));
                 doluKoltukResmi = null;
            }
        } catch (NullPointerException e) {
            System.err.println("Dolu koltuk resmi '/application/icons/reserved1.png' bulunamadı veya erişilemiyor.");
            doluKoltukResmi = null;
        } catch (Exception e) {
            System.err.println("Dolu koltuk resmi yüklenirken beklenmedik bir hata: " + e.getMessage());
            e.printStackTrace();
            doluKoltukResmi = null;
        }

        UserSession session = UserSession.getInstance();
        if(session.isLoggedIn()){
            try (Connection conn = DbHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT Ad, Soyad FROM Kullanicilar WHERE KullaniciID = ?")) {
                pstmt.setInt(1, session.getKullaniciID());
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String ad = rs.getString("Ad");
                    String soyad = rs.getString("Soyad");
                    if (ad != null && !ad.isEmpty()) txtMusteriAdi.setText(ad);
                    if (soyad != null && !soyad.isEmpty()) txtMusteriSoyadi.setText(soyad);
                }
                if (rs != null) rs.close();
            } catch (SQLException e) {
                System.err.println("Kullanıcı bilgileri alınırken hata: " + e.getMessage());
            }
        }
    }

    public void initData(int seansID, int filmID, int salonID, String filmAdi, String salonAdi, String seansZamani) {
        this.seansID = seansID;
        this.filmID = filmID;
        this.salonID = salonID;
        this.filmAdiStr = filmAdi;
        this.salonAdiStr = salonAdi;
        this.seansZamaniStr = seansZamani;
        this.biletBasariylaAlindi = false;

        this.salonKapasitesi = getSalonKapasitesiDB(this.salonID);

        lblFilmAdi.setText("Film Adı: " + (this.filmAdiStr != null ? this.filmAdiStr : "N/A"));
        lblSalonVeSeans.setText("Salon: " + (this.salonAdiStr != null ? this.salonAdiStr : "N/A") +
                                " (" + this.salonKapasitesi + " koltuk)" +
                                " - Seans: " + (this.seansZamaniStr != null ? this.seansZamaniStr : "N/A"));

        doluKoltuklariYukleVeGoster();
    }
    
    private int getSalonKapasitesiDB(int salonId) {
        String query = "SELECT Kapasite FROM SinemaSalonlari WHERE SalonID = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, salonId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("Kapasite");
            }
            if (rs != null) rs.close();
        } catch (SQLException e) {
            System.err.println("Salon kapasitesi alınırken hata: " + e.getMessage());
        }
        return 15; 
    }

    private void doluKoltuklariYukleVeGoster() {
        flowPaneKoltuklar.getChildren().clear();
        koltukButonlari.clear();
        aktifSeciliKoltukNo = null;
        lblSecilenKoltuk.setText("Seçilen Koltuk: -");

        List<String> doluKoltukNumaralari = new ArrayList<>();
        String query = "SELECT KoltukNumarasi FROM Rezervasyonlar WHERE SeansID = ? AND IptalEdildiMi = 0";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, this.seansID);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                doluKoltukNumaralari.add(rs.getString("KoltukNumarasi"));
            }
            if (rs != null) rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Dolu koltuklar yüklenirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
            return;
        }

        double butonGenisligi = 70; 
        double butonYuksekligi = 60;

        for (int i = 1; i <= this.salonKapasitesi; i++) {
            final String koltukNo = String.valueOf(i);
            Button koltukButonu = new Button(); 
            koltukButonu.setPrefSize(butonGenisligi, butonYuksekligi);
            koltukButonu.setStyle(TEMEL_KOLTUK_STILI + " -fx-alignment: CENTER;");

            if (doluKoltukNumaralari.contains(koltukNo)) {
                if (doluKoltukResmi != null && !doluKoltukResmi.isError()) {
                    ImageView iconView = new ImageView(doluKoltukResmi);
                    iconView.setFitWidth(butonGenisligi * 0.65); 
                    iconView.setFitHeight(butonYuksekligi * 0.65);
                    iconView.setPreserveRatio(true);
                    koltukButonu.setGraphic(iconView);
                    koltukButonu.setText(""); 
                    koltukButonu.setStyle(koltukButonu.getStyle() + " -fx-background-color: #f0f0f0; -fx-padding: 4px;");
                } else {
                    koltukButonu.setText("X"); 
                    koltukButonu.setStyle(DOLU_KOLTUK_FALLBACK_STILI);
                }
                koltukButonu.setDisable(true);
            } else {
                koltukButonu.setText(koltukNo);
                koltukButonu.setGraphic(null);
                koltukButonu.setStyle(BOS_KOLTUK_STILI);
                koltukButonu.setDisable(false);
                koltukButonu.setOnAction(event -> {
                    for (Button btn : koltukButonlari) {
                        if (!btn.isDisabled() && btn.getGraphic() == null) { 
                            btn.setStyle(BOS_KOLTUK_STILI);
                        }
                    }
                    Button tiklananButon = (Button)event.getSource();
                    tiklananButon.setStyle(SECILI_KOLTUK_STILI);
                    aktifSeciliKoltukNo = koltukNo;
                    lblSecilenKoltuk.setText("Seçilen Koltuk: " + aktifSeciliKoltukNo);
                });
            }
            koltukButonlari.add(koltukButonu);
            flowPaneKoltuklar.getChildren().add(koltukButonu);
        }
    }

    public boolean isBiletBasariylaAlindi() {
        return biletBasariylaAlindi;
    }

    @FXML
    private void handleBiletAl() {
        if (aktifSeciliKoltukNo == null) {
            showAlert("Eksik Seçim", "Lütfen bir koltuk seçin.", Alert.AlertType.WARNING);
            return;
        }
        String musteriAdi = txtMusteriAdi.getText().trim();
        String musteriSoyadi = txtMusteriSoyadi.getText().trim();

        if (musteriAdi.isEmpty() || musteriSoyadi.isEmpty()) {
            showAlert("Eksik Bilgi", "Lütfen adınızı ve soyadınızı girin.", Alert.AlertType.WARNING);
            return;
        }

        UserSession session = UserSession.getInstance();
        if (!session.isLoggedIn()) {
            showAlert("Giriş Gerekli", "Bilet alabilmek için giriş yapmış olmalısınız.", Alert.AlertType.ERROR);
            return;
        }
        int kullaniciID = session.getKullaniciID();
        LocalDateTime biletAlinmaZamani = LocalDateTime.now(); // Bilet alınma zamanını başta belirle
        int yeniRezervasyonID = -1; 

        Connection conn = null;
        PreparedStatement pstmtUpdate = null;
        PreparedStatement pstmtInsert = null;
        PreparedStatement pstmtSelectCancelled = null;
        ResultSet rsCancelled = null;
        int affectedRows = 0;

        String selectCancelledQuery = "SELECT RezervasyonID FROM Rezervasyonlar WHERE SeansID = ? AND KoltukNumarasi = ? AND IptalEdildiMi = 1";
        
        String updateCancelledReactivateQuery = "UPDATE Rezervasyonlar SET " +
                                                "KullaniciID = ?, " +
                                                "BiletMusteriAdi = ?, " +
                                                "BiletMusteriSoyadi = ?, " +
                                                "RezervasyonZamani = ?, " + 
                                                "IptalEdildiMi = 0, " +
                                                "IptalZamani = NULL, " +
                                                "HatirlatmaSeviyesi = 0, " +
                                                "SonHatirlatmaZamani = NULL " +
                                                "WHERE RezervasyonID = ?"; 

        String insertNewQuery = "INSERT INTO Rezervasyonlar " +
                                "(KullaniciID, SeansID, KoltukNumarasi, BiletMusteriAdi, BiletMusteriSoyadi, RezervasyonZamani, IptalEdildiMi) " +
                                "VALUES (?, ?, ?, ?, ?, ?, 0)";

        try {
            conn = DbHelper.getConnection();
            conn.setAutoCommit(false); // Transaction başlat

            // 1. İptal edilmiş bir kayıt var mı diye kontrol et ve ID'sini al
            pstmtSelectCancelled = conn.prepareStatement(selectCancelledQuery);
            pstmtSelectCancelled.setInt(1, this.seansID);
            pstmtSelectCancelled.setString(2, aktifSeciliKoltukNo);
            rsCancelled = pstmtSelectCancelled.executeQuery();
            
            int cancelledRezervasyonID = 0;
            if (rsCancelled.next()) {
                cancelledRezervasyonID = rsCancelled.getInt("RezervasyonID");
            }

            if (cancelledRezervasyonID > 0) { // İptal edilmiş kayıt bulundu, onu güncelle
                pstmtUpdate = conn.prepareStatement(updateCancelledReactivateQuery);
                pstmtUpdate.setInt(1, kullaniciID);
                pstmtUpdate.setString(2, musteriAdi);
                pstmtUpdate.setString(3, musteriSoyadi);
                pstmtUpdate.setTimestamp(4, Timestamp.valueOf(biletAlinmaZamani));
                pstmtUpdate.setInt(5, cancelledRezervasyonID); 
                affectedRows = pstmtUpdate.executeUpdate();
                if (affectedRows > 0) {
                    yeniRezervasyonID = cancelledRezervasyonID; // Güncellenen kaydın ID'si
                }
            } else { // İptal edilmiş kayıt yok, yeni bir rezervasyon ekle
                pstmtInsert = conn.prepareStatement(insertNewQuery, Statement.RETURN_GENERATED_KEYS);
                pstmtInsert.setInt(1, kullaniciID);
                pstmtInsert.setInt(2, this.seansID);
                pstmtInsert.setString(3, aktifSeciliKoltukNo);
                pstmtInsert.setString(4, musteriAdi);
                pstmtInsert.setString(5, musteriSoyadi);
                pstmtInsert.setTimestamp(6, Timestamp.valueOf(biletAlinmaZamani));
                affectedRows = pstmtInsert.executeUpdate();

                if (affectedRows > 0) {
                    ResultSet generatedKeys = pstmtInsert.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        yeniRezervasyonID = generatedKeys.getInt(1);
                    }
                    if (generatedKeys != null) generatedKeys.close();
                }
            }

            if (affectedRows > 0 && yeniRezervasyonID != -1) {
                conn.commit(); // İşlem başarılı, commit et
                this.biletBasariylaAlindi = true;
                showAlert("Bilet Başarılı", "Bilet başarıyla alındı!\nFilm: " + filmAdiStr + "\nKoltuk: " + aktifSeciliKoltukNo, Alert.AlertType.INFORMATION);

                String aliciEposta = fetchUserEmail(kullaniciID);
                if (aliciEposta != null && !aliciEposta.isEmpty()) {
                    final String finalAliciEposta = aliciEposta;
                    final String finalMusteriAdi = musteriAdi;
                    final String finalMusteriSoyadi = musteriSoyadi;
                    final String finalAktifSeciliKoltukNo = aktifSeciliKoltukNo;
                    new Thread(() -> {
                        EmailService emailService = new EmailService();
                        String konu = "Sinema Biletiniz Onaylandı: " + filmAdiStr;
                        String icerik = "Merhaba " + finalMusteriAdi + " " + finalMusteriSoyadi + ",\n\n" +
                                        "Sinema biletiniz başarıyla oluşturulmuştur.\n\n" +
                                        "Bilet Detayları:\n" +
                                        "---------------------------------\n" +
                                        "Film Adı    : " + filmAdiStr + "\n" +
                                        "Salon       : " + salonAdiStr + "\n" +
                                        "Seans Tarihi: " + seansZamaniStr + "\n" +
                                        "Koltuk No   : " + finalAktifSeciliKoltukNo + "\n" +
                                        "Ad Soyad    : " + finalMusteriAdi + " " + finalMusteriSoyadi + "\n" +
                                        "---------------------------------\n\n" +
                                        "İyi seyirler dileriz!\n" +
                                        "Pervan Sinema";
                        boolean emailGonderildi = emailService.sendEmail(finalAliciEposta, konu, icerik);
                        Platform.runLater(() -> {
                            if (emailGonderildi) {
                                System.out.println("Bilet e-postası başarıyla gönderildi: " + finalAliciEposta);
                            } else {
                                System.err.println("Bilet e-postası gönderilemedi: " + finalAliciEposta);
                            }
                        });
                    }).start();
                } else {
                    System.err.println("Bilet e-postası gönderilemedi: Alıcı e-posta adresi bulunamadı/alınamadı.");
                }
                
            
                String filmTuru = fetchFilmTuru(this.filmID);
                String sehirAdi = fetchSehirAdi(this.salonID);
                LocalDateTime seansZamaniLocalDateTime = parseSeansZamani(this.seansZamaniStr);

                KullaniciBileti gosterilecekBilet = new KullaniciBileti(

                		yeniRezervasyonID, this.filmAdiStr, this.salonAdiStr,

                		sehirAdi != null ? sehirAdi : "Bilinmiyor",

                		seansZamaniLocalDateTime, this.aktifSeciliKoltukNo,

                		biletAlinmaZamani,

                		filmTuru != null ? filmTuru : "Belirtilmemiş",

                		musteriAdi, musteriSoyadi,

                		false

                		);

                		showPrintableTicket(gosterilecekBilet);
                

                Stage currentStage = (Stage) btnBiletAl.getScene().getWindow();
                currentStage.close();

            } else {
                if (conn != null) conn.rollback();
                this.biletBasariylaAlindi = false;
                showAlert("Hata", "Bilet alınırken bir sorun oluştu (işlem yapılamadı).", Alert.AlertType.ERROR);
            }
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            this.biletBasariylaAlindi = false;
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) { 
                 showAlert("Koltuk Dolu", "Bu koltuk başka bir işlemle alındı veya zaten dolu. Lütfen farklı bir koltuk seçin.", Alert.AlertType.ERROR);
                 doluKoltuklariYukleVeGoster(); // Ekranı yenile
            } else {
                showAlert("Veritabanı Hatası", "Bilet kaydedilirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
            }
            e.printStackTrace();
        } finally {
            if (rsCancelled != null) try { rsCancelled.close(); } catch (SQLException e) { e.printStackTrace(); }
            if (pstmtSelectCancelled != null) try { pstmtSelectCancelled.close(); } catch (SQLException e) { e.printStackTrace(); }
            if (pstmtUpdate != null) try { pstmtUpdate.close(); } catch (SQLException e) { e.printStackTrace(); }
            if (pstmtInsert != null) try { pstmtInsert.close(); } catch (SQLException e) { e.printStackTrace(); }
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // autoCommit'i geri al
                    DbHelper.closeConnection(conn);
                } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    private LocalDateTime parseSeansZamani(String seansZamaniStr) {
        if (seansZamaniStr == null || seansZamaniStr.trim().isEmpty() || seansZamaniStr.equalsIgnoreCase("N/A")) {
            return null;
        }
        try {
            return LocalDateTime.parse(seansZamaniStr, KullaniciBileti.DT_FORMATTER);
        } catch (Exception e) {
            System.err.println("Seans zamanı (" + seansZamaniStr + ") ayrıştırılırken hata: " + e.getMessage());
            return null; 
        }
    }

    private String fetchUserEmail(int kullaniciID) {
        String query = "SELECT Eposta FROM Kullanicilar WHERE KullaniciID = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmtUser = conn.prepareStatement(query)) {
            pstmtUser.setInt(1, kullaniciID);
            ResultSet rsUser = pstmtUser.executeQuery();
            if (rsUser.next()) {
                return rsUser.getString("Eposta");
            }
             if (rsUser != null) rsUser.close();
        } catch (SQLException ex) {
            System.err.println("Kullanıcı e-postası alınırken hata: " + ex.getMessage());
        }
        return null;
    }

    private String fetchFilmTuru(int filmID) {
        String query = "SELECT t.TurAdi FROM Filmler f LEFT JOIN Turler t ON f.TurID = t.TurID WHERE f.FilmID = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, filmID);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String turAdi = rs.getString("TurAdi");
                return rs.wasNull() ? null : turAdi;
            }
             if (rs != null) rs.close();
        } catch (SQLException e) {
            System.err.println("Film türü alınırken hata: " + e.getMessage());
        }
        return null;
    }

    private String fetchSehirAdi(int salonID) {
        String query = "SELECT c.SehirAdi FROM SinemaSalonlari s JOIN Sehirler c ON s.SehirID = c.SehirID WHERE s.SalonID = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, salonID);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("SehirAdi");
            }
            if (rs != null) rs.close();
        } catch (SQLException e) {
            System.err.println("Şehir adı alınırken hata: " + e.getMessage());
        }
        return null;
    }
    
    private void showPrintableTicket(KullaniciBileti bilet) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("PrintableTicket.fxml"));
            Parent root = loader.load();

            PrintableTicketController controller = loader.getController();
            if (bilet == null) {
                System.err.println("showPrintableTicket çağrıldı ama bilet nesnesi null.");
                showAlert("Bilet Hatası", "Bilet bilgileri oluşturulamadı.", Alert.AlertType.ERROR);
                return;
            }
            controller.initData(bilet);

            Stage stage = new Stage();
            stage.setTitle("Biletiniz - Rezervasyon ID: " + bilet.getRezervasyonID());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL); 
            
            Stage ownerStage = (Stage) btnBiletAl.getScene().getWindow(); 
            if (ownerStage != null) { 
                 stage.initOwner(ownerStage);
            }
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Yükleme Hatası", "Bilet görüntüleme ekranı yüklenirken bir G/Ç hatası oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
        } catch (NullPointerException e) {
             e.printStackTrace();
             if (e.getMessage() != null && e.getMessage().contains("PrintableTicket.fxml")) {
                showAlert("Dosya Hatası", "PrintableTicket.fxml dosyası bulunamadı. Lütfen dosya yolunu kontrol edin.", Alert.AlertType.ERROR);
             } else if (e.getMessage() != null && e.getMessage().toLowerCase().contains("controller") && e.getMessage().toLowerCase().contains("null")) {
                showAlert("Controller Hatası", "PrintableTicketController yüklenemedi.", Alert.AlertType.ERROR);
             } else {
                showAlert("Null Hatası", "Bilet görüntüleme ekranı açılırken bir null değeriyle karşılaşıldı: " + e.getMessage(), Alert.AlertType.ERROR);
             }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Beklenmedik Hata", "Bilet görüntüleme ekranı açılırken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleGeri() {
        this.biletBasariylaAlindi = false;
        Stage stage = (Stage) btnGeri.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (btnBiletAl != null && btnBiletAl.getScene() != null && btnBiletAl.getScene().getWindow() != null) {
            alert.initOwner(btnBiletAl.getScene().getWindow());
        }
        alert.showAndWait();
    }
}