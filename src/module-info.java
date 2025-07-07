// module-info.java
module Sinema2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires javafx.base;
    requires transitive javafx.graphics; // "transitive" eklendi

    // E-POSTA MODÜLLERİ
    requires org.eclipse.angus.mail;
    requires jakarta.activation;
    requires transitive org.eclipse.angus.activation;

    // ZXing (QR Kod için) MODÜLLERİ EKLENDİ
    requires com.google.zxing;
    requires javafx.swing;     
    requires java.desktop; 
    // requires com.google.zxing.core; // QRCodeWriter ve diğer temel sınıflar için
    // requires com.google.zxing.javase; // Gerekirse (MatrixToImageWriter vb. için)

    opens application to javafx.fxml, javafx.graphics;
    opens application.model to javafx.base, javafx.fxml;
    opens application.icons to javafx.graphics; // Eğer icons paketiniz varsa ve FXML'den erişiliyorsa

    exports application;
    // exports application.model; // Eğer model sınıflarınız başka modüller tarafından kullanılacaksa
}