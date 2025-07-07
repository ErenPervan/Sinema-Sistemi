// application/ReminderService.java
package application;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReminderService {

    private static final DateTimeFormatter DT_FORMATTER_EMAIL = DateTimeFormatter.ofPattern("dd MMMM yyyy, EEEE HH:mm");

    // Hatırlatma zaman aralıkları (saat cinsinden)
    private static class ReminderInterval {
        int hoursBefore;
        int level;       
        String description; 

        ReminderInterval(int hoursBefore, int level, String description) {
            this.hoursBefore = hoursBefore;
            this.level = level;
            this.description = description;
        }
    }

    // SAATLER kaç saate bir hatıtrlatılacağı 
    private static final List<ReminderInterval> REMINDER_INTERVALS = List.of(
        new ReminderInterval(6, 1, "6 Saat Hatırlatması"),
        new ReminderInterval(3, 2, "3 Saat Hatırlatması"),
        new ReminderInterval(1, 3, "1 Saat Hatırlatması")
        


    );

    public void sendUpcomingSessionReminders() {
        System.out.println(LocalDateTime.now() + ": Yaklaşan seans hatırlatıcıları kontrol ediliyor...");
        EmailService emailService = new EmailService();

        for (ReminderInterval interval : REMINDER_INTERVALS) {
          
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime targetTimeStart = now.plusHours(interval.hoursBefore - 1); 
            LocalDateTime targetTimeEnd = now.plusHours(interval.hoursBefore + 1);

            String query = "SELECT r.RezervasyonID, r.KullaniciID, r.KoltukNumarasi, " +
                           "u.Eposta, u.Ad AS KullaniciAd, u.Soyad AS KullaniciSoyad, " +
                           "f.Baslik AS FilmBaslik, sl.SalonAdi, s.SeansZamani, c.SehirAdi " +
                           "FROM Rezervasyonlar r " +
                           "JOIN Kullanicilar u ON r.KullaniciID = u.KullaniciID " +
                           "JOIN Seanslar s ON r.SeansID = s.SeansID " +
                           "JOIN Filmler f ON s.FilmID = f.FilmID " +
                           "JOIN SinemaSalonlari sl ON s.SalonID = sl.SalonID " +
                           "JOIN Sehirler c ON sl.SehirID = c.SehirID " +
                           "WHERE r.IptalEdildiMi = 0 " + 
                           "AND s.SeansZamani BETWEEN ? AND ? " +
                           "AND r.HatirlatmaSeviyesi < ?"; 
            try (Connection conn = DbHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setTimestamp(1, Timestamp.valueOf(targetTimeStart));
                pstmt.setTimestamp(2, Timestamp.valueOf(targetTimeEnd));
                pstmt.setInt(3, interval.level);

                ResultSet rs = pstmt.executeQuery();
                List<Integer> successfullyNotifiedReservationIds = new ArrayList<>();

                while (rs.next()) {
                    int rezervasyonID = rs.getInt("RezervasyonID");
                    String kullaniciEposta = rs.getString("Eposta");
                    String kullaniciAd = rs.getString("KullaniciAd") != null ? rs.getString("KullaniciAd") : "";
                    String kullaniciSoyad = rs.getString("KullaniciSoyad") != null ? rs.getString("KullaniciSoyad") : "";
                    String filmBaslik = rs.getString("FilmBaslik");
                    String salonAdi = rs.getString("SalonAdi");
                    String sehirAdi = rs.getString("SehirAdi");
                    LocalDateTime seansZamani = rs.getTimestamp("SeansZamani").toLocalDateTime();
                    String koltukNo = rs.getString("KoltukNumarasi");

                    if (kullaniciEposta == null || kullaniciEposta.trim().isEmpty()) {
                        System.err.println("Rezervasyon ID " + rezervasyonID + " için e-posta adresi bulunamadı, hatırlatma gönderilemiyor.");
                        continue;
                    }

                    String konu = "Sinema Seans Hatırlatması: " + filmBaslik;
                    String icerik = "Merhaba " + kullaniciAd + " " + kullaniciSoyad + ",\n\n" +
                                    "Yaklaşan sinema seansınızı hatırlatmak istedik:\n\n" +
                                    "Film Adı      : " + filmBaslik + "\n" +
                                    "Şehir         : " + sehirAdi + "\n" +
                                    "Salon         : " + salonAdi + "\n" +
                                    "Seans Zamanı  : " + seansZamani.format(DT_FORMATTER_EMAIL) + "\n" +
                                    "Koltuk No     : " + koltukNo + "\n\n" +
                                    interval.description + ". İyi seyirler dileriz!\n" +
                                    "Pervan Sinema";

                    System.out.println(interval.description + " gönderiliyor: " + kullaniciEposta + " - Film: " + filmBaslik);
                    boolean gonderildi = emailService.sendEmail(kullaniciEposta, konu, icerik);

                    if (gonderildi) {
                        successfullyNotifiedReservationIds.add(rezervasyonID);
                    } else {
                        System.err.println(interval.description + " gönderilemedi: " + kullaniciEposta + " - Film: " + filmBaslik);
                    }
                }
                rs.close();

                if (!successfullyNotifiedReservationIds.isEmpty()) {
                    String updateQuery = "UPDATE Rezervasyonlar SET HatirlatmaSeviyesi = ?, SonHatirlatmaZamani = NOW() WHERE RezervasyonID = ?";
                    try (PreparedStatement updatePstmt = conn.prepareStatement(updateQuery)) {
                        for (Integer rezId : successfullyNotifiedReservationIds) {
                            updatePstmt.setInt(1, interval.level);
                            updatePstmt.setInt(2, rezId);
                            updatePstmt.addBatch();
                        }
                        updatePstmt.executeBatch();
                        System.out.println(interval.description + ": " + successfullyNotifiedReservationIds.size() + " adet bilet için hatırlatma durumu güncellendi.");
                    }
                }

            } catch (SQLException e) {
                System.err.println("Hatırlatma e-postaları gönderilirken veritabanı hatası ("+ interval.description +"): " + e.getMessage());
                e.printStackTrace();
            }
        }
         System.out.println(LocalDateTime.now() + ": Hatırlatma kontrolü tamamlandı.");
    }
}