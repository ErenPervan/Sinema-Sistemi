// application/PrintableTicketController.java
package application;

import application.model.KullaniciBileti;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javafx.embed.swing.SwingFXUtils; 
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage; 
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

public class PrintableTicketController {

    @FXML private Label lblSinemaAdi;
    @FXML private Label lblBiletFilmAdi;
    @FXML private Label lblBiletSalonAdi;
    @FXML private Label lblBiletSehirAdi;
    @FXML private Label lblBiletSeansZamani;
    @FXML private Label lblBiletKoltukNo;
    @FXML private Label lblBiletAdSoyad;
    @FXML private Label lblBiletRezervasyonID;
    @FXML private Label lblBiletAlinmaTarihi;
    @FXML private ImageView imgQRCode;
    @FXML private Button btnIndirQR;
    @FXML private Button btnPrintTicket;
    @FXML private Button btnCloseTicket;

    public void initData(KullaniciBileti bilet) {
        if (bilet == null) {
            lblBiletFilmAdi.setText("Bilet bilgisi alınamadı.");
            lblBiletSalonAdi.setText("-");
            lblBiletSehirAdi.setText("-");
            lblBiletSeansZamani.setText("-");
            lblBiletKoltukNo.setText("-");
            lblBiletAdSoyad.setText("-");
            lblBiletRezervasyonID.setText("-");
            lblBiletAlinmaTarihi.setText("-");
            if (imgQRCode != null) imgQRCode.setImage(null);
            if (btnIndirQR != null) btnIndirQR.setDisable(true);
            return;
        }
        
        lblBiletFilmAdi.setText(bilet.getFilmAdi() != null ? bilet.getFilmAdi() : "-");
        lblBiletSalonAdi.setText(bilet.getSalonAdi() != null ? bilet.getSalonAdi() : "-");
        lblBiletSehirAdi.setText(bilet.getSehirAdi() != null ? bilet.getSehirAdi() : "-");
        lblBiletSeansZamani.setText(bilet.getSeansZamaniStr() != null ? bilet.getSeansZamaniStr() : "-");
        lblBiletKoltukNo.setText(bilet.getKoltukNumarasi() != null ? bilet.getKoltukNumarasi() : "-");
        String adSoyad = (bilet.getMusteriAdi() != null ? bilet.getMusteriAdi() : "") + " " + 
                         (bilet.getMusteriSoyadi() != null ? bilet.getMusteriSoyadi() : "");
        lblBiletAdSoyad.setText(!adSoyad.trim().isEmpty() ? adSoyad.trim() : "-");
        String rezervasyonIdString = String.valueOf(bilet.getRezervasyonID());
        lblBiletRezervasyonID.setText(rezervasyonIdString);
        lblBiletAlinmaTarihi.setText(bilet.getBiletAlinmaTarihiStr() != null ? bilet.getBiletAlinmaTarihiStr() : "-");

        // QR Kod İçeriği için JSON String Oluşturma
        StringBuilder jsonData = new StringBuilder();
        jsonData.append("{");
        jsonData.append("\"rezervasyonID\":\"").append(bilet.getRezervasyonID()).append("\",\n");
        jsonData.append("\"filmAdi\":\"").append(escapeJsonValue(bilet.getFilmAdi())).append("\",\n");
        jsonData.append("\"salonAdi\":\"").append(escapeJsonValue(bilet.getSalonAdi())).append("\",\n");
        jsonData.append("\"sehirAdi\":\"").append(escapeJsonValue(bilet.getSehirAdi())).append("\",\n");
        jsonData.append("\"seansZamani\":\"").append(escapeJsonValue(bilet.getSeansZamaniStr())).append("\",\n");
        jsonData.append("\"koltukNo\":\"").append(escapeJsonValue(bilet.getKoltukNumarasi())).append("\",\n");
        jsonData.append("\"musteriAdSoyad\":\"").append(escapeJsonValue(adSoyad.trim())).append("\",\n");
        jsonData.append("\"satinAlmaTarihi\":\"").append(escapeJsonValue(bilet.getBiletAlinmaTarihiStr())).append("\",\n");
        jsonData.append("\"sinemaAdi\":\"").append(escapeJsonValue("Pervan Sinema")).append("\"");
        jsonData.append("}");
        
        String qrData = jsonData.toString();
        System.out.println("QR Kod Verisi: " + qrData); //KONSOL TEST

        // QR Kodu Üret ve Ata
        if (imgQRCode != null) {
            try {
                javafx.scene.image.Image qrImage = generateQRCodeImage(qrData, 150, 150);
                imgQRCode.setImage(qrImage);
                if (btnIndirQR != null) btnIndirQR.setDisable(qrImage == null);
            } catch (WriterException e) {
                System.err.println("QR Kod üretilirken hata oluştu: " + e.getMessage());
                e.printStackTrace();
                imgQRCode.setImage(null);
                if (btnIndirQR != null) btnIndirQR.setDisable(true);
            }
        } else {
             System.err.println("HATA: imgQRCode @FXML alanı null, FXML dosyasını kontrol edin.");
             if (btnIndirQR != null) btnIndirQR.setDisable(true);
        }
    }

    private String escapeJsonValue(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    private javafx.scene.image.Image generateQRCodeImage(String text, int width, int height) throws WriterException {
        Hashtable<EncodeHintType, Object> hintMap = new Hashtable<>();
        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hintMap.put(EncodeHintType.MARGIN, 1);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hintMap);

        WritableImage writableImage = new WritableImage(width, height);
        PixelWriter pixelWriter = writableImage.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (bitMatrix.get(x, y)) {
                    pixelWriter.setColor(x, y, Color.BLACK);
                } else {
                    pixelWriter.setColor(x, y, Color.WHITE);
                }
            }
        }
        return writableImage;
    }

    @FXML
    private void handleIndirQR() {
        if (imgQRCode == null || imgQRCode.getImage() == null) {
            showAlert("Hata", "İndirilecek QR kod resmi bulunmuyor veya üretilemedi.", Alert.AlertType.WARNING);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("QR Kodu Kaydet");
        String initialFileName = "bilet_qr_";
        if (lblBiletRezervasyonID != null && lblBiletRezervasyonID.getText() != null && !lblBiletRezervasyonID.getText().equals("-")) {
            initialFileName += lblBiletRezervasyonID.getText();
        } else {
            initialFileName += "bilinmeyen";
        }
        initialFileName += ".png";
        fileChooser.setInitialFileName(initialFileName);

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Resimleri (*.png)", "*.png")
        );
        

        File file = fileChooser.showSaveDialog(imgQRCode.getScene().getWindow());

        if (file != null) {
            try {
                javafx.scene.image.Image fxImage = imgQRCode.getImage();
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(fxImage, null);
                
                boolean success = ImageIO.write(bufferedImage, "png", file);
                if(success) {
                    showAlert("Başarılı", "QR Kod başarıyla kaydedildi:\n" + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Kayıt Hatası", "QR Kod dosyaya yazılamadı. Format desteklenmiyor olabilir.", Alert.AlertType.ERROR);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                showAlert("Kayıt Hatası", "QR Kod kaydedilirken bir G/Ç hatası oluştu: " + ex.getMessage(), Alert.AlertType.ERROR);
            } catch (Exception ex) { 
                ex.printStackTrace();
                showAlert("Kayıt Hatası", "QR Kod kaydedilirken beklenmedik bir hata oluştu: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handlePrintTicket() {
        showAlert("Yazdırma Simülasyonu", "Bilet yazdırma işlemi burada tetiklenecek.", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleCloseTicket() {
        Stage stage = (Stage) btnCloseTicket.getScene().getWindow();
        stage.close();
    }
    
    
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (btnCloseTicket != null && btnCloseTicket.getScene() != null && btnCloseTicket.getScene().getWindow() != null) {
            alert.initOwner(btnCloseTicket.getScene().getWindow());
        }
        alert.showAndWait();
    }
}