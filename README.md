Yüz tanıma ile klasör açma sistemi
Bu proje, Java ve OpenCV kütüphanesi kullanılarak geliştirilmiş, kamera aracılığıyla kullanıcıların yüzlerini tanır.
Doğrulama başlatıldığında yüz tanınırsa hedeflenen klasör açılır.

Projenin amacı

alternatif bir şifreleme yöntemi yapmak.
kullanıcıların sadece kendi yüzlerini göstererek şahsi bilgisayarlarındaki belirlenen klasöre erişebilmesini sağlar
Sistem kullanıcının yüz hatlarından matematiksel bir iz çıkarır
bunu yerel bir veritabanında saklar ve canlı kamera görüntüsüyle karşılaştırarak güvenli erişim kontrolü sağlar.

Öne Çıkan Özellikler
Canlı Yüz Algılama: Haar Cascade sınıflandırıcısı kullanarak kameradan anlık olarak insan yüzünü tespit eder.
Otomatik Veri Seti Toplama: Tek tıkla kullanıcı adına özel 500 adet yüz fotoğrafı çeker ve bunları düzenler.
Veritabanı: Kullanıcı bilgilerini ve fotoğrafların binary hallerini yerel bir SQLite veritabanında saklar.
Toplanan fotoğrafların ortalama histogram grafiğini çıkartarak hızlı ve internet gerektirmeyen yerel bir yapay zeka modeli eğitir

Kullanılan Teknolojiler

Arayüz Kütüphanesi: Java Swing & AWT
Görüntü İşleme: OpenCV
Veritabanı: SQLite
Algoritma: Haar Cascade ve Histogram Karşılaştırma
