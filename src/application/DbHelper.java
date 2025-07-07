// application/DbHelper.java
package application;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DbHelper {

    private static Properties appProps = new Properties();

    static {
        String configFilePath = "/application/config.properties";
        try (InputStream input = DbHelper.class.getResourceAsStream(configFilePath)) {
            if (input == null) {
                System.err.println("KRİTİK HATA: config.properties dosyasi classpath içinde bulunamadi! Beklenen yol: " + configFilePath);
                throw new RuntimeException("Yapılandırma dosyası bulunamadı.");
            } else {
                appProps.load(input);
                System.out.println("config.properties başarıyla yüklendi.");
            }
        } catch (IOException ex) {
            System.err.println("config.properties dosyasi yüklenirken G/Ç hatasi: " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Yapılandırma dosyası okunamadı.", ex);
        }
    }

    /**
     * Properties dosyasından belirtilen anahtarın değerini alır.
     * @param key Okunacak anahtar.
     * @return Anahtarın değeri veya bulunamazsa null.
     */
    public static String getProperty(String key) {
        String value = appProps.getProperty(key);
        if (value == null) {
            System.err.println("UYARI: config.properties dosyasında '" + key + "' anahtarı bulunamadı.");
        }
        return value;
    }

    /**
     * Properties dosyasından belirtilen anahtarın değerini alır.
     * Eğer anahtar bulunamazsa, belirtilen varsayılan değeri döndürür.
     * @param key Okunacak anahtar.
     * @param defaultValue Anahtar bulunamazsa döndürülecek değer.
     * @return Anahtarın değeri veya varsayılan değer.
     */
    public static String getProperty(String key, String defaultValue) {
        return appProps.getProperty(key, defaultValue);
    }

    /**
     * Veritabanı bağlantısı kurar. Bağlantı bilgilerini properties dosyasından alır.
     * @return Connection nesnesi.
     * @throws SQLException Bağlantı başarısız olursa veya yapılandırma eksikse.
     */
    public static Connection getConnection() throws SQLException {
        try {
            String url = getProperty("db.url");
            String user = getProperty("db.user");
            String pass = getProperty("db.password");

            if (url == null || user == null || pass == null) {
                throw new SQLException("Veritabanı bağlantı bilgileri (url, user, password) config.properties dosyasında eksik.");
            }
            
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(url, user, pass);

        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Sürücüsü bulunamadı: " + e.getMessage());
            throw new SQLException("MySQL JDBC Sürücüsü bulunamadı.", e);
        }
    }

    /**
     * Verilen veritabanı bağlantısını güvenli bir şekilde kapatır.
     * @param conn Kapatılacak Connection nesnesi.
     */
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("Veritabanı bağlantısı kapatılırken hata: " + e.getMessage());
            }
        }
    }
}