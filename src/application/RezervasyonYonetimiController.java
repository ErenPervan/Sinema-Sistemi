// application/RezervasyonYonetimiController.java
package application;

import application.model.AdminRezervasyonDetay;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types; 
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RezervasyonYonetimiController {

    @FXML private TableView<AdminRezervasyonDetay> tableViewRezervasyonlar;
    @FXML private TableColumn<AdminRezervasyonDetay, Integer> colRezID;
    @FXML private TableColumn<AdminRezervasyonDetay, String> colKullanici;
    @FXML private TableColumn<AdminRezervasyonDetay, String> colFilm;
    @FXML private TableColumn<AdminRezervasyonDetay, String> colSalon;
    @FXML private TableColumn<AdminRezervasyonDetay, String> colSehir;
    @FXML private TableColumn<AdminRezervasyonDetay, String> colSeansTarihi;
    @FXML private TableColumn<AdminRezervasyonDetay, String> colKoltuk;
    @FXML private TableColumn<AdminRezervasyonDetay, String> colMusteriAd;
    @FXML private TableColumn<AdminRezervasyonDetay, String> colMusteriSoyad;
    @FXML private TableColumn<AdminRezervasyonDetay, String> colRezTarihi;
    @FXML private TableColumn<AdminRezervasyonDetay, Boolean> colIptalDurumu;
    @FXML private TableColumn<AdminRezervasyonDetay, String> colIptalZamani;
    @FXML private TableColumn<AdminRezervasyonDetay, Void> colAksiyon;

    @FXML private TextField txtFiltreKullaniciAdi;
    @FXML private TextField txtFiltreFilmAdi;
    @FXML private CheckBox checkSadeceIptaller;
    @FXML private Button btnFiltrele;
    @FXML private Button btnFiltreTemizle;
    @FXML private Button btnKapat;

    private ObservableList<AdminRezervasyonDetay> rezervasyonListesi = FXCollections.observableArrayList();

    public void initialize() {
        colRezID.setCellValueFactory(new PropertyValueFactory<>("rezervasyonID"));
        colKullanici.setCellValueFactory(new PropertyValueFactory<>("kullaniciAdi"));
        colFilm.setCellValueFactory(new PropertyValueFactory<>("filmAdi"));
        colSalon.setCellValueFactory(new PropertyValueFactory<>("salonAdi"));
        colSehir.setCellValueFactory(new PropertyValueFactory<>("sehirAdi"));
        colSeansTarihi.setCellValueFactory(new PropertyValueFactory<>("formatliSeansZamani"));
        colKoltuk.setCellValueFactory(new PropertyValueFactory<>("koltukNumarasi"));
        colMusteriAd.setCellValueFactory(new PropertyValueFactory<>("biletMusteriAdi"));
        colMusteriSoyad.setCellValueFactory(new PropertyValueFactory<>("biletMusteriSoyadi"));
        colRezTarihi.setCellValueFactory(new PropertyValueFactory<>("formatliRezervasyonZamani"));
        
        colIptalDurumu.setCellValueFactory(new PropertyValueFactory<>("iptalEdildi"));
        colIptalDurumu.setCellFactory(column -> new TableCell<AdminRezervasyonDetay, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item ? "İPTAL EDİLDİ" : "AKTİF");
                    setStyle(item ? "-fx-text-fill: red; -fx-font-weight: bold;" : "-fx-text-fill: green;");
                }
            }
        });

        colIptalZamani.setCellValueFactory(new PropertyValueFactory<>("formatliIptalZamani"));

        colAksiyon.setCellFactory(param -> new TableCell<AdminRezervasyonDetay, Void>() {
            private final Button btnAksiyon = new Button();

            {
                btnAksiyon.setOnAction(event -> {
                    AdminRezervasyonDetay data = getTableView().getItems().get(getIndex());
                    if (data != null && !data.isIptalEdildi() && 
                        data.getSeansZamani() != null && data.getSeansZamani().isAfter(LocalDateTime.now())) {
                        handleRezervasyonIptalEt(data); 
                    }
                });
            }

            @Override
            public void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                AdminRezervasyonDetay data = getTableRow().getItem();
                
                // Buton sadece aktif ve seans zamanı geçmemiş biletler için gösterilecek ve İptal Et yazacak
                if (!data.isIptalEdildi() && data.getSeansZamani() != null && data.getSeansZamani().isAfter(LocalDateTime.now())) {
                    btnAksiyon.setText("İptal Et");
                    btnAksiyon.getStyleClass().clear(); 
                    btnAksiyon.getStyleClass().addAll("button", "delete-button"); 
                    btnAksiyon.setDisable(false);
                    setGraphic(btnAksiyon);
                } else {
                    setGraphic(null); 
                }
            }
        });

        tableViewRezervasyonlar.setItems(rezervasyonListesi);
        rezervasyonlariYukle();
    }

    @FXML
    private void handleFiltrele() {
        rezervasyonlariYukle();
    }

    @FXML
    private void handleFiltreTemizle() {
        txtFiltreKullaniciAdi.clear();
        txtFiltreFilmAdi.clear();
        checkSadeceIptaller.setSelected(false);
        rezervasyonlariYukle();
    }

    private void rezervasyonlariYukle() {
        rezervasyonListesi.clear();
        StringBuilder sql = new StringBuilder(
            "SELECT r.RezervasyonID, u.KullaniciAdi, f.Baslik AS FilmAdi, sl.SalonAdi, c.SehirAdi, " +
            "s.SeansZamani, r.KoltukNumarasi, r.BiletMusteriAdi, r.BiletMusteriSoyadi, " +
            "r.RezervasyonZamani, r.IptalEdildiMi, r.IptalZamani " +
            "FROM Rezervasyonlar r " +
            "JOIN Kullanicilar u ON r.KullaniciID = u.KullaniciID " +
            "JOIN Seanslar s ON r.SeansID = s.SeansID " +
            "JOIN Filmler f ON s.FilmID = f.FilmID " +
            "JOIN SinemaSalonlari sl ON s.SalonID = sl.SalonID " +
            "JOIN Sehirler c ON sl.SehirID = c.SehirID " +
            "WHERE 1=1 "
        );

        List<Object> parametreler = new ArrayList<>();
        String kullaniciFiltre = txtFiltreKullaniciAdi.getText().trim();
        String filmFiltre = txtFiltreFilmAdi.getText().trim();

        if (!kullaniciFiltre.isEmpty()) {
            sql.append("AND u.KullaniciAdi LIKE ? ");
            parametreler.add("%" + kullaniciFiltre + "%");
        }
        if (!filmFiltre.isEmpty()) {
            sql.append("AND f.Baslik LIKE ? ");
            parametreler.add("%" + filmFiltre + "%");
        }
        if (checkSadeceIptaller.isSelected()) {
            sql.append("AND r.IptalEdildiMi = 1 ");
        }

        sql.append("ORDER BY r.RezervasyonZamani DESC");

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < parametreler.size(); i++) {
                pstmt.setObject(i + 1, parametreler.get(i));
            }
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                LocalDateTime seansZamani = rs.getTimestamp("SeansZamani") != null ? rs.getTimestamp("SeansZamani").toLocalDateTime() : null;
                LocalDateTime rezervasyonZamani = rs.getTimestamp("RezervasyonZamani") != null ? rs.getTimestamp("RezervasyonZamani").toLocalDateTime() : null;
                Timestamp iptalTimestamp = rs.getTimestamp("IptalZamani");
                LocalDateTime iptalZamani = (iptalTimestamp != null) ? iptalTimestamp.toLocalDateTime() : null;

                rezervasyonListesi.add(new AdminRezervasyonDetay(
                    rs.getInt("RezervasyonID"),
                    rs.getString("KullaniciAdi"),
                    rs.getString("FilmAdi"),
                    rs.getString("SalonAdi"),
                    rs.getString("SehirAdi"),
                    seansZamani,
                    rs.getString("KoltukNumarasi"),
                    rs.getString("BiletMusteriAdi"),
                    rs.getString("BiletMusteriSoyadi"),
                    rezervasyonZamani,
                    rs.getBoolean("IptalEdildiMi"),
                    iptalZamani
                ));
            }
             if (rs != null) rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Veritabanı Hatası", "Rezervasyonlar yüklenirken bir hata oluştu: " + e.getMessage(), Alert.AlertType.ERROR);
        }
        if(rezervasyonListesi.isEmpty()){
            tableViewRezervasyonlar.setPlaceholder(new Label("Filtre kriterlerine uygun rezervasyon bulunamadı."));
        }
    }

    private void handleRezervasyonIptalEt(AdminRezervasyonDetay rezervasyon) {
        if (rezervasyon.isIptalEdildi()) {
            showAlert("Geçersiz İşlem", "Bu rezervasyon zaten iptal edilmiş.", Alert.AlertType.INFORMATION);
            return;
        }
        if (rezervasyon.getSeansZamani() == null || rezervasyon.getSeansZamani().isBefore(LocalDateTime.now())) {
            showAlert("Geçersiz İşlem", "Seans zamanı geçmiş bir rezervasyon iptal edilemez.", Alert.AlertType.WARNING);
            return;
        }

        String onayMesaji = rezervasyon.getFilmAdi() + " filmine ait " + rezervasyon.getRezervasyonID() + 
                            " ID'li rezervasyonu iptal etmek istediğinizden emin misiniz?";

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Rezervasyon İptal Onayı");
        alert.setHeaderText(null);
        alert.setContentText(onayMesaji);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String query = "UPDATE Rezervasyonlar SET IptalEdildiMi = 1, IptalZamani = NOW() WHERE RezervasyonID = ?";
            try (Connection conn = DbHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                
                pstmt.setInt(1, rezervasyon.getRezervasyonID());

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    showAlert("Başarılı", "Rezervasyon başarıyla iptal edildi.", Alert.AlertType.INFORMATION);
                    rezervasyonlariYukle(); // Listeyi yenile
                } else {
                    showAlert("Hata", "Rezervasyon iptal edilirken bir sorun oluştu (kayıt etkilenmedi).", Alert.AlertType.ERROR);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Veritabanı Hatası", "Rezervasyon iptal edilirken hata: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
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