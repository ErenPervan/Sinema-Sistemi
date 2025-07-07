// application/AuthHelper.java
package application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AuthHelper {

    /**
     * Verilen düz metin şifreyi SHA-256 ile hash'ler.
     * @param plainPassword Hash'lenecek düz metin şifre.
     * @return Hash'lenmiş şifrenin hexadecimal string temsili.
     * @throws IllegalArgumentException Eğer şifre null veya boş ise.
     * @throws RuntimeException Eğer SHA-256 algoritması bulunamazsa (NoSuchAlgorithmException'ı sarmalar).
     */
    public static String hashPasswordSHA256(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            // Bu kontrol, checkPasswordSHA256'da plainPassword için de geçerli olacak.
            throw new IllegalArgumentException("Hashlenecek şifre boş olamaz.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(
                    plainPassword.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-256 algoritması bulunamadı: " + e.getMessage());
            throw new RuntimeException("SHA-256 algoritması mevcut değil, sistem yapılandırmasını kontrol edin.", e);
        }
    }

    /**
     * Verilen düz metin şifreyi, daha önce SHA-256 ile hash'lenmiş bir şifreyle karşılaştırır.
     * @param plainPassword Kullanıcının girdiği düz metin şifre
     * @param haslanmis sifre
     * @return şifrelerde eşleşme varsa true yoksa false döndürür 
     * @throws IllegalArgumentException Eğer plainPassword null veya boş ise 
     * @throws RuntimeException Eğer SHA-256 algoritması bulunamazsa 
     */
    public static boolean checkPasswordSHA256(String plainPassword, String hashedPasswordFromDB) {
        if (hashedPasswordFromDB == null || hashedPasswordFromDB.isEmpty()) {

            return false;
        }
     
        String hashedPlainPassword = hashPasswordSHA256(plainPassword);
        return hashedPlainPassword.equals(hashedPasswordFromDB);
    }
}