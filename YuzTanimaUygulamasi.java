import org.opencv.core.*;
import org.opencv.imgcodecs.*;
import org.opencv.imgproc.*;
import org.opencv.objdetect.*;
import org.opencv.videoio.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class YuzTanimaUygulamasi extends JFrame {

    //Opencv ekleme
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private JLabel kameraEkrani;
    //kamera 
    private VideoCapture camera;
    //kamera durumu 
    private volatile boolean kameraCalisiyor = false;

    //yüzü algılıyor/buluyo
    private CascadeClassifier yuzDedektoru;

    // Yüzü ezberlemek ve kim olduğunu anlamak
    private Mat ortalamaHistogram = null;

    
    private volatile boolean kayitYapiliyor = false;
    // sayac 
    private volatile int kayitSayaci = 0;

    private volatile boolean kilitAcmaModu = false;
    // ardarda tanıma 
    private volatile int basariliTanimaSayaci = 0;

    public YuzTanimaUygulamasi() {

        yuzDedektoru = new CascadeClassifier("haarcascade_frontalface_default.xml");

        //classifier yüklendi mi kontrol et
        if (yuzDedektoru.empty()) {
            JOptionPane.showMessageDialog(null, "haarcascade_frontalface_default.xml bulunamadi!");
        }

        
        try {
            ortalamaHistogram = histogramYukle();
        } catch (IOException e) {
            // Model henüz yok, sorun değil
        }

        //Pencere ayarıları
        setTitle("Yüz Tanıma ve Veri Toplama");
        setSize(800, 600);
        //uygulamayı kapatmak
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //ekranın ortasına açış
        setLocationRelativeTo(null);


        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                kamerayiKapat();
            }
        });

        //Ekranı bölgelere ayırmak
        setLayout(new BorderLayout());

        //SingsConstants.center ===> metni ortaya yazmak için
        kameraEkrani = new JLabel("Kamera baslatilmadi...", SwingConstants.CENTER);

        //ekranın merkezine kamera
        add(kameraEkrani, BorderLayout.CENTER);

        //butonlar
        JPanel altPanel = new JPanel();
        //butonları yan yana dizer
        altPanel.setLayout(new FlowLayout());
        //arka plan rengi
        altPanel.setBackground(Color.DARK_GRAY);

        JButton btnKaydet = new JButton("Yüzümü Kaydet");
        JButton btnKilitAc = new JButton("Klasör Kilidini Aç");

        //Yüzümü kaydet butonuna bastığımda olacaklar
        btnKaydet.addActionListener(e -> {
            File klasor = new File("dataset");
            //eğer klasör yoksa make directory ile oluşturuyoruz
            if (!klasor.exists()) {
                // Dataset klasörü yoksa oluşturur
                klasor.mkdir();
            }
            
            // Kameraya kayıt emrini verir
            kayitYapiliyor = true;
            kayitSayaci = 0;
            JOptionPane.showMessageDialog(this, "Kayit basliyor");
        });

        //Klasör kilidini aç butonuna bastığında
        btnKilitAc.addActionListener(e -> {


            new Thread(() -> modeliEgit()).start();
        });

        //Butonları eklemek
        altPanel.add(btnKaydet);
        altPanel.add(btnKilitAc);
        add(altPanel, BorderLayout.SOUTH);

        //kamerayı açmak
        KamerayiBaslat();
    }

    //histogram 
    private Mat yuzHistogramiHesapla(Mat grayYuz) {
        Mat hist = new Mat();
        List<Mat> matListesi = new ArrayList<>();
        matListesi.add(grayYuz);
        Imgproc.calcHist(matListesi, new MatOfInt(0), new Mat(), hist,
                new MatOfInt(256), new MatOfFloat(0f, 256f));
        Core.normalize(hist, hist, 0, 1, Core.NORM_MINMAX);
        return hist;
    }

    //dosyaya kaydet
    private void histogramKaydet(Mat hist) throws IOException {
        float[] data = new float[256];
        hist.get(0, 0, data);
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream("model_hist.bin"))) {
            for (float f : data) dos.writeFloat(f);
        }
    }

    //dosyadan yükle
    private Mat histogramYukle() throws IOException {
        float[] data = new float[256];
        try (DataInputStream dis = new DataInputStream(new FileInputStream("model_hist.bin"))) {
            for (int i = 0; i < 256; i++) data[i] = dis.readFloat();
        }
        Mat hist = new Mat(256, 1, CvType.CV_32F);
        hist.put(0, 0, data);
        return hist;
    }

    //yapay zekayı eğitme
    public void modeliEgit() {
        File klasor = new File("dataset");
        File[] dosyalar = klasor.listFiles();
        //veya = ||
        if (dosyalar == null || dosyalar.length == 0) {


            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "Once yüzünü kaydetmelisin")
            );
            return;
        }

        // Tüm yüz fotoğraflarının histogramını hesapla ve ortalamasını al
        Mat toplamHist = Mat.zeros(256, 1, CvType.CV_32F);
        int gecerliSayac = 0;

        for (File dosya : dosyalar) {
            Mat resim = Imgcodecs.imread(dosya.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
            if (!resim.empty()) {
                Mat hist = yuzHistogramiHesapla(resim);
                Core.add(toplamHist, hist, toplamHist);


                hist.release();
                gecerliSayac++;
            }
            resim.release();
        }

        if (gecerliSayac == 0) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "doğru yüz fotoğrafı bulunamadı")
            );
            toplamHist.release();
            return;
        }


        ortalamaHistogram = new Mat();
        Core.divide(toplamHist, Scalar.all(gecerliSayac), ortalamaHistogram);
        toplamHist.release();


        try {
            histogramKaydet(ortalamaHistogram);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        kilitAcmaModu = true;
        basariliTanimaSayaci = 0;

        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, "Yüzünüz öğrenildi")
        );
    }

    public void KamerayiBaslat() {
        //0 ana kamera
        camera = new VideoCapture(0);
        kameraCalisiyor = true;

        //while döngüsü için yeni arayüz
        new Thread(() -> {
            //matris (sayı tablosu oluyormuş)
            Mat frame = new Mat();
            while (kameraCalisiyor && camera.read(frame)) {
                //gelen foto boş değilse
                if (!frame.empty()) {

                    MatOfRect yuzler = new MatOfRect();
                    // Ekranda yüz ara
                    yuzDedektoru.detectMultiScale(frame, yuzler); 

                    for (Rect yuzDikdortgeni : yuzler.toArray()) {

                        Mat griYuz = new Mat();
                        Imgproc.cvtColor(frame, griYuz, Imgproc.COLOR_BGR2GRAY);
                        Mat kirpilmisYuz = new Mat(griYuz, yuzDikdortgeni);
                        Mat boyutlandirilmisYuz = new Mat();
                        Imgproc.resize(kirpilmisYuz, boyutlandirilmisYuz, new Size(200, 200));

                        // yüzümü kaydet butonuna basıldıysa
                        if (kayitYapiliyor) {
                            kayitSayaci++;
                            Imgcodecs.imwrite("dataset/yuz_" + kayitSayaci + ".jpg", boyutlandirilmisYuz);

                            if (kayitSayaci >= 500) {
                                kayitYapiliyor = false;
                                SwingUtilities.invokeLater(() -> {
                                    JOptionPane.showMessageDialog(null, "500 adet fotograf klasore kaydedildi!");
                                });
                            }
                        }

                        // kilidi aça basıldıysa
                        // FIX: ortalamaHistogram null kontrolü eklendi
                        if (kilitAcmaModu && ortalamaHistogram != null) {

                            // Mevcut yüzü hesapla
                            Mat mevcutHist = yuzHistogramiHesapla(boyutlandirilmisYuz);

                            // Korelasyon 1 ve --1
 

                            double benzerlik = Imgproc.compareHist(mevcutHist, ortalamaHistogram, 0);
                            mevcutHist.release();

                            // tanındıysan
                            if (benzerlik > 0.85) {
                                basariliTanimaSayaci++;

                                Imgproc.rectangle(frame, yuzDikdortgeni.tl(), yuzDikdortgeni.br(), new Scalar(0, 255, 0), 2);
                                Imgproc.putText(frame, "b-erke Tanindi", yuzDikdortgeni.tl(), Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 255, 0), 2);
                            } else {
                                // Tanıyamazsa veya başkasıysa sayacı sıfırla
                                basariliTanimaSayaci = 0; 
                                Imgproc.rectangle(frame, yuzDikdortgeni.tl(), yuzDikdortgeni.br(), new Scalar(0, 0, 255), 2);
                                Imgproc.putText(frame, "Yabanci", yuzDikdortgeni.tl(), Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 0, 255), 2);
                            }

                            // 5 kere tanırsa klasörü aç
                            if (basariliTanimaSayaci >= 5) {
                                // Taramayı durdur
                                kilitAcmaModu = false;
                                basariliTanimaSayaci = 0;
                                try {
                                    Runtime.getRuntime().exec("explorer.exe .");
                                    SwingUtilities.invokeLater(() -> {
                                        JOptionPane.showMessageDialog(null, "Klasor aciliyor.");
                                    });
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        } else if (!kayitYapiliyor) {
                            Imgproc.rectangle(frame, yuzDikdortgeni.tl(), yuzDikdortgeni.br(), new Scalar(255, 0, 0), 2); // Sadece Mavi çerçeve çizer
                        }


                        griYuz.release();
                        kirpilmisYuz.release();
                        boyutlandirilmisYuz.release();
                    }

                    //matrisi resme çevir
                    BufferedImage Resim = mat2BufferedImage(frame);

                    //yazı sil resim koy 
                    SwingUtilities.invokeLater(() -> kameraEkrani.setIcon(new ImageIcon(Resim)));
                }
            }
            frame.release();
        }).start();
    }

    public void kamerayiKapat() {
        kameraCalisiyor = false;
        //kamera var mı ve camera.isOpened() kamera açık mı
        if (camera != null && camera.isOpened()) {
            //kamerayı kapat
            camera.release();
        }
    }

    //Buffered image == ara belleğe alınmış fotoğraf
    //matristen resme
    public BufferedImage mat2BufferedImage(Mat m) {
        //varsayılan gri
        int type = BufferedImage.TYPE_BYTE_GRAY;
        //eğer fotoğraf renkliyse maviyeşik kırmızı kullan
        //channels>1 === çok katmanlı/kanallıysa.
        if (m.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }

        // katman sütun satır
        int bufferSize = m.channels() * m.cols() * m.rows();
        // boş dizi
        byte[] b = new byte[bufferSize];
        //(0,0)== ilk koordinat verilerin aktarılacağı
        //b boş dizi
        //m.get= m in verilerini al
        //m in verilerini 0,0 noktasından başlayarak b dizisine doldurr
        m.get(0, 0, b);

        BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        //Dizi kopyalama
        System.arraycopy(b, 0, targetPixels, 0, b.length);
        return image;
    }

    //java arayğz kütüphanesi
    public static void main(String[] args) {
        //invoke later sonra çalıştır
        SwingUtilities.invokeLater(() -> {
            YuzTanimaUygulamasi app = new YuzTanimaUygulamasi();
            //ekranda göster
            app.setVisible(true);
        });
    }
}
// kodu başlatmak ==
/*
java -cp ".;lib\opencv-4120.jar" "-Djava.library.path=." YuzTanimaUygulamasi
*/
""