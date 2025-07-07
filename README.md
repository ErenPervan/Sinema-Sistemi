# Cinema Ticket Automation System

A desktop application for cinema management and ticket sales, developed using Java and JavaFX.

## 🚀 Features

- **Movie Management:** Add, update, and delete movies in the system.
- **Session Management:** Create and manage sessions for movies.
- **Seat Selection:** Interactive seat selection through a visual interface.
- **Ticket Sales:** Issue tickets for customers and record sales transactions.
- **Database Integration:** All data (movies, sessions, sales) is stored in a MySQL database.

## 🛠️ Technologies Used

- **Programming Language:** Java
- **User Interface (UI):** JavaFX
- **Database:** MySQL
- **IDE:** Eclipse

## ⚙️ Setup and Installation

To run this project on your local machine, follow these steps:

1.  **Clone the Repository:**
    ```bash
    git clone [https://github.com/ErenPervan/Sinema-Sistemi.git](https://github.com/ErenPervan/Sinema-Sistemi.git)
    ```

2.  **Import into Eclipse:**
    - Open Eclipse and import the project via `File > Import > General > Existing Projects into Workspace`.

3.  **Set Up the Database:**
    - Create a new database in your MySQL server (e.g., `cinemadb`).
    - Create the necessary tables for the project. (If available, import the `.sql` file).

4.  **Create the Configuration File:**
    - Create a copy of the `config.properties.example` file, which is located in the source folder (e.g., `src/application`).
    - Rename the copy to `config.properties` and update it with your local database credentials:
      ```properties
      db.url=jdbc:mysql://localhost:3306/your_database_name
      db.user=your_username
      db.password=your_password
      ```

5.  **Run the Project:**
    - In Eclipse, find the main class of the project (e.g., `Main.java`), right-click on it, and select `Run As > Java Application`.




# Sinema Bilet Otomasyon Sistemi

Bu proje, Java ve JavaFX teknolojileri kullanılarak geliştirilmiş bir masaüstü sinema yönetim ve bilet satış uygulamasıdır.

## 🚀 Özellikler

- **Film Yönetimi:** Sisteme yeni filmler ekleme, mevcut filmleri güncelleme ve silme.
- **Seans Yönetimi:** Filmler için yeni seanslar oluşturma ve yönetme.
- **Koltuk Seçimi:** Görsel bir arayüz üzerinden interaktif koltuk seçimi.
- **Bilet Satışı:** Müşteriler için bilet kesme ve satış işlemlerini kaydetme.
- **Veritabanı Entegrasyonu:** Tüm verilerin (filmler, seanslar, satışlar) MySQL veritabanında saklanması.

## 🛠️ Kullanılan Teknolojiler

- **Programlama Dili:** Java
- **Arayüz (UI):** JavaFX
- **Veritabanı:** MySQL
- **Geliştirme Ortamı (IDE):** Eclipse

## ⚙️ Kurulum ve Çalıştırma

Projeyi yerel makinenizde çalıştırmak için aşağıdaki adımları izleyebilirsiniz:

1.  **Depoyu Klonlayın:**
    ```bash
    git clone [https://github.com/ErenPervan/Sinema-Sistemi.git](https://github.com/ErenPervan/Sinema-Sistemi.git)
    ```

2.  **Projeyi Eclipse'e Aktarın:**
    - Eclipse'i açın ve `File > Import > General > Existing Projects into Workspace` adımlarını izleyerek projeyi çalışma alanınıza dahil edin.

3.  **Veritabanını Ayarlayın:**
    - MySQL sunucunuzda bir veritabanı oluşturun (örneğin, `sinemadb`).
    - Proje için gerekli tabloları oluşturun. (Eğer varsa, `.sql` dosyasını içe aktarın).

4.  **Yapılandırma Dosyasını Oluşturun:**
    - Projenin kaynak kodları içinde (`src/application` gibi) bulunan `config.properties.example` dosyasının bir kopyasını oluşturun.
    - Kopyanın adını `config.properties` olarak değiştirin ve içindeki veritabanı bilgilerini kendi yerel ayarlarınızla güncelleyin:
      ```properties
      db.url=jdbc:mysql://localhost:3306/veritabani_adiniz
      db.user=kullanici_adiniz
      db.password=sifreniz
      ```

5.  **Projeyi Çalıştırın:**
    - Eclipse'de projenin ana sınıfını (`Main.java` gibi) bulun, sağ tıklayın ve `Run As > Java Application` seçeneği ile uygulamayı başlatın.



