// application/EmailService.java
package application;

import java.util.Properties;
// Doğru Jakarta EE importları
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class EmailService {

    private final String host;
    private final String port;
    private final String username;
    private final String password;
    private final boolean tlsEnable;
    private final boolean auth;

    public EmailService() {
        this.host = DbHelper.getProperty("email.host");
        this.port = DbHelper.getProperty("email.port");
        this.username = DbHelper.getProperty("email.username");
        this.password = DbHelper.getProperty("email.password");
        this.tlsEnable = Boolean.parseBoolean(DbHelper.getProperty("email.tls.enable", "true"));
        this.auth = Boolean.parseBoolean(DbHelper.getProperty("email.auth", "true"));

        if (this.host == null || this.port == null || this.username == null || this.password == null ||
            this.host.isEmpty() || this.port.isEmpty() || this.username.isEmpty() || this.password.isEmpty()) {
            System.err.println("KRİTİK E-POSTA AYAR HATASI: Lütfen config.properties dosyasındaki tüm e-posta ayarlarının (host, port, username, password) dolu olduğundan emin olun.");
        }
    }

    public boolean sendEmail(String toEmail, String subject, String body) {
        if (host == null || username == null || password == null || username.isEmpty() || password.isEmpty()) {
            System.err.println("E-posta gönderilemiyor: Gönderici ayarları (host, username, password) eksik veya yüklenemedi.");
            return false;
        }
        if (toEmail == null || toEmail.trim().isEmpty()) {
            System.err.println("E-posta gönderilemiyor: Alıcı e-posta adresi boş olamaz.");
            return false;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", this.host);
        props.put("mail.smtp.port", this.port);
        props.put("mail.smtp.auth", String.valueOf(this.auth));
        
        if (this.tlsEnable) {
            props.put("mail.smtp.starttls.enable", "true");
        }

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return auth ? new PasswordAuthentication(username, password) : null;
            }
        });
        // session.setDebug(true); // HATA AYIKLAMALAR

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(body);

            System.out.println(toEmail + " adresine e-posta gönderiliyor...");
            Transport.send(message);
            System.out.println("E-posta başarıyla gönderildi: " + toEmail);
            return true;
        } catch (MessagingException e) {
            System.err.println("E-posta gönderilirken MessagingException oluştu ('" + toEmail + "' adresine): " + e.getMessage());
            e.printStackTrace();
            Exception nextException = e.getNextException();
            if (nextException != null) {
                System.err.println("İlişkili Exception: ");
                nextException.printStackTrace();
            }
            return false;
        } catch (Exception e) {
            System.err.println("E-posta gönderiminde beklenmedik bir genel hata ('" + toEmail + "' adresine): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    public static void main(String[] args) {
        System.out.println("EmailService test ediliyor...");
//         TEST KISIMLARI
//        EmailService emailService = new EmailService();
//
//        if (emailService.username == null || emailService.username.isEmpty() || 
//            emailService.password == null || emailService.password.isEmpty() ||
//            emailService.host == null || emailService.host.isEmpty()) {
//            System.out.println("Test başarısız: Lütfen config.properties dosyasını ve DbHelper'daki yolu kontrol edin. Gönderici bilgileri yüklenemedi.");
//            return;
//        }
//        System.out.println("Test için kullanılacak gönderici: " + emailService.username);
//        System.out.println("Test için kullanılacak host: " + emailService.host + ":" + emailService.port);
//
//        String aliciEmail = "SENIN_GERCEK_TEST_EMAIL_ADRESIN@example.com"; 
//        String konu = "JavaFX Sinema Uygulaması E-posta Servisi Testi (Tekrar)";
//        String icerik = "Merhaba,\n\nBu e-posta, EmailService sınıfının main metodu üzerinden, konfigürasyon dosyası kullanılarak gönderilmiştir.\n\nUygulama Adı: Sinema Bilet Otomasyonu\nİyi günler!";
//
//        if (emailService.sendEmail(aliciEmail, konu, icerik)) {
//            System.out.println("Test e-postası gönderimi başarılı.");
//        } else {
//            System.out.println("Test e-postası gönderimi başarısız...");
//        }
        
        System.out.println("EmailService test main metodu. E-posta gönderme kısmı yorumlandı.");
    }
    }
