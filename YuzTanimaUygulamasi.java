import org.opencv.core.*;
import org.opencv.imgcodecs.*;
import org.opencv.imgproc.*;
import org.opencv.objdetect.*;
import org.opencv.videoio.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
 
public class YuzTanimaUygulamasi extends JFrame {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
 
    private static final String DB_URL = "jdbc:sqlite:facereco.db";
 
    private JLabel kameraEkrani;
    private VideoCapture camera;
    private volatile boolean kameraCalisiyor = false;
    private CascadeClassifier yuzDedektoru;
    private Mat ortalamaHistogram = null;
    private volatile boolean kayitYapiliyor = false;
    private volatile int kayitSayaci = 0:
    private volatile int aktifKullaniciId = -1;
    private volatile String aktifKullaniciAd = "";
    private volatile boolean kilitAcmaModu = false;
    
    private volatile int basariliTanimaSayaci = 0;
    public YuzTanimaUygulamasi() {

        yuzDedektoru = new CascadeClassifier("haarcascade_frontalface_default.xml");
        tablolarıOlustur();
 
        try {
            ortalamaHistogram = histogramYukle();
        } catch (IOException e) {

        }

        setTitle("Yüz Tanıma ve Veri Toplama");
        setSize(800, 600);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLocationRelativeTo(null);
 
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                kamerayiKapat();
            }
        });

        setLayout(new BorderLayout());

        kameraEkrani = new JLabel("Kamera baslatilmadi...", SwingConstants.CENTER);
 

        add(kameraEkrani, BorderLayout.CENTER);
 

        JPanel altPanel = new JPanel();
        altPanel.setLayout(new FlowLayout());
        altPanel.setBackground(Color.DARK_GRAY);
 
        JButton btnKaydet = new JButton("Yüzümü Kaydet");
        JButton btnKilitAc = new JButton("Klasör Kilidini Aç");

        btnKaydet.addActionListener(e -> {
            String kullaniciAdi = JOptionPane.showInputDialog(this, "Adinizi girin:");
 
            if (kullaniciAdi == null || kullaniciAdi.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Ad girilmedi, kayit iptal edildi.");
                return;
            }
 
            aktifKullaniciId = kullaniciyiDBKaydet(kullaniciAdi.trim());
            aktifKullaniciAd = kullaniciAdi.trim();
 
            if (aktifKullaniciId == -1) {
                JOptionPane.showMessageDialog(this, "Veritabani hatasi!");
                return;
            }
 
            File klasor = new File("dataset");

            if (!klasor.exists()) {

                klasor.mkdir();
            }
 

            kayitYapiliyor = true;
            kayitSayaci = 0;
            JOptionPane.showMessageDialog(this, kullaniciAdi + " icin kayit basliyor!");
        });
 

        btnKilitAc.addActionListener(e -> {
            new Thread(() -> modeliEgit()).start();
        });
 

        altPanel.add(btnKaydet);
        altPanel.add(btnKilitAc);
        add(altPanel, BorderLayout.SOUTH);
 
        KamerayiBaslat();
    }
 
    private Connection baglantiKur() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return DriverManager.getConnection(DB_URL);
    }
 

    private void tablolarıOlustur() {
        String kullanicilarSQL = "CREATE TABLE IF NOT EXISTS Kullanicilar (id INTEGER PRIMARY KEY AUTOINCREMENT, ad TEXT NOT NULL, kayit_tarihi DATETIME DEFAULT CURRENT_TIMESTAMP)";
        String fotograflarSQL = "CREATE TABLE IF NOT EXISTS YuzFotograflari (id INTEGER PRIMARY KEY AUTOINCREMENT, kullanici_id INTEGER, fotograf BLOB NOT NULL, tarih DATETIME DEFAULT CURRENT_TIMESTAMP)";
        try (Connection conn = baglantiKur();
             Statement stmt = conn.createStatement()) {
            stmt.execute(kullanicilarSQL);
            stmt.execute(fotograflarSQL);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
 
    private int kullaniciyiDBKaydet(String ad) {
        String sql = "INSERT INTO Kullanicilar (ad) VALUES (?)";
        try (Connection conn = baglantiKur();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, ad);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return -1;
    }
 
    private void fotografiKaydet(Mat yuz, int kullaniciId, int fotoNo) {
        String dosyaYolu = "dataset/yuz_" + fotoNo + ".jpg";
        Imgcodecs.imwrite(dosyaYolu, yuz);
 

        if (fotoNo % 10 == 0) {
            try {
                File dosya = new File(dosyaYolu);
                byte[] fotografBytes = new byte[(int) dosya.length()];
                try (FileInputStream fis = new FileInputStream(dosya)) {
                    fis.read(fotografBytes);
                }
                String sql = "INSERT INTO YuzFotograflari (kullanici_id, fotograf) VALUES (?, ?)";
                try (Connection conn = baglantiKur();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, kullaniciId);
                    stmt.setBytes(2, fotografBytes);
                    stmt.executeUpdate();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
 

    private Mat yuzHistogramiHesapla(Mat grayYuz) {
        Mat hist = new Mat();
        List<Mat> matListesi = new ArrayList<>();
        matListesi.add(grayYuz);
        Imgproc.calcHist(matListesi, new MatOfInt(0), new Mat(), hist,
                new MatOfInt(256), new MatOfFloat(0f, 256f));
        Core.normalize(hist, hist, 0, 1, Core.NORM_MINMAX);
        return hist;
    }
 

    private void histogramKaydet(Mat hist) throws IOException {
        float[] data = new float[256];
        hist.get(0, 0, data);
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream("model_hist.bin"))) {
            for (float f : data) dos.writeFloat(f);
        }
    }
 

    private Mat histogramYukle() throws IOException {
        float[] data = new float[256];
        try (DataInputStream dis = new DataInputStream(new FileInputStream("model_hist.bin"))) {
            for (int i = 0; i < 256; i++) data[i] = dis.readFloat();
        }
        Mat hist = new Mat(256, 1, CvType.CV_32F);
        hist.put(0, 0, data);
        return hist;
    }
 

    public void modeliEgit() {
        File klasor = new File("dataset");
        File[] dosyalar = klasor.listFiles();

        if (dosyalar == null || dosyalar.length == 0) {
            SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, "Once yüzünü kaydetmelisin")
            );
            return;
        }
 
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

        camera = new VideoCapture(0);
        kameraCalisiyor = true;
 

        new Thread(() -> {

            Mat frame = new Mat();
            while (kameraCalisiyor && camera.read(frame)) {

                if (!frame.empty()) {
 
                    MatOfRect yuzler = new MatOfRect();
                    yuzDedektoru.detectMultiScale(frame, yuzler); 
 
                    for (Rect yuzDikdortgeni : yuzler.toArray()) {
 
                        Mat griYuz = new Mat();
                        Imgproc.cvtColor(frame, griYuz, Imgproc.COLOR_BGR2GRAY);
                        Mat kirpilmisYuz = new Mat(griYuz, yuzDikdortgeni);
                        Mat boyutlandirilmisYuz = new Mat();
                        Imgproc.resize(kirpilmisYuz, boyutlandirilmisYuz, new Size(200, 200));
                        if (kayitYapiliyor) {
                            kayitSayaci++;
                            fotografiKaydet(boyutlandirilmisYuz, aktifKullaniciId, kayitSayaci);
 
                            if (kayitSayaci >= 500) {
                                kayitYapiliyor = false;
                                SwingUtilities.invokeLater(() ->
                                    JOptionPane.showMessageDialog(null, "fotoğraflar kaydedildi")
                                );
                            }
                        }
 


                        if (kilitAcmaModu && ortalamaHistogram != null) {
 
                            Mat mevcutHist = yuzHistogramiHesapla(boyutlandirilmisYuz);

                            double benzerlik = Imgproc.compareHist(mevcutHist, ortalamaHistogram, 0);
                            mevcutHist.release();
 

                            if (benzerlik > 0.85) {
                                basariliTanimaSayaci++;
                                Imgproc.rectangle(frame, yuzDikdortgeni.tl(), yuzDikdortgeni.br(), new Scalar(0, 255, 0), 2);
                                Imgproc.putText(frame, aktifKullaniciAd + " Tanindi", yuzDikdortgeni.tl(), Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 255, 0), 2);
                            } else {

                                basariliTanimaSayaci = 0;
                                Imgproc.rectangle(frame, yuzDikdortgeni.tl(), yuzDikdortgeni.br(), new Scalar(0, 0, 255), 2);
                                Imgproc.putText(frame, "Yabanci", yuzDikdortgeni.tl(), Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 0, 255), 2);
                            }
 

                            if (basariliTanimaSayaci >= 5) {

                                kilitAcmaModu = false;
                                basariliTanimaSayaci = 0;
                                try {
                                    Runtime.getRuntime().exec("explorer.exe .");
                                    SwingUtilities.invokeLater(() ->
                                        JOptionPane.showMessageDialog(null, "Klasor aciliyor.")
                                    );
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        } else if (!kayitYapiliyor) {
                            Imgproc.rectangle(frame, yuzDikdortgeni.tl(), yuzDikdortgeni.br(), new Scalar(255, 0, 0), 2); 
                        }
 
                        griYuz.release();
                        kirpilmisYuz.release();
                        boyutlandirilmisYuz.release();
                    }
 

                    BufferedImage Resim = mat2BufferedImage(frame);

                    SwingUtilities.invokeLater(() -> kameraEkrani.setIcon(new ImageIcon(Resim)));
                }
            }
            frame.release();
        }).start();
    }
 
    public void kamerayiKapat() {
        kameraCalisiyor = false;

        if (camera != null && camera.isOpened()) {

            camera.release();
        }
    }
 

    public BufferedImage mat2BufferedImage(Mat m) {

        int type = BufferedImage.TYPE_BYTE_GRAY;

        if (m.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }


        int bufferSize = m.channels() * m.cols() * m.rows();

        byte[] b = new byte[bufferSize];

        m.get(0, 0, b);
 
        BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        System.arraycopy(b, 0, targetPixels, 0, b.length);
        return image;
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            YuzTanimaUygulamasi app = new YuzTanimaUygulamasi();

            app.setVisible(true);
        });
    }
}
// kodu başlatmak ==
/*
javac -cp ".;lib\opencv-4120.jar;lib\sqlite-jdbc.jar" YuzTanimaUygulamasi.java
java -cp ".;lib\opencv-4120.jar;lib\sqlite-jdbc.jar" "-Djava.library.path=." YuzTanimaUygulamasi
*/