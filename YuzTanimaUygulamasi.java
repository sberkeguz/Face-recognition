import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class YuzTanimaUygulamasi extends JFrame {

    private JLabel kameraEkrani;
    //kamera 
    private VideoCapture camera; 
    //kamera durumu
    private boolean kameraCalisiyor = false; 

    public YuzTanimaUygulamasi(){
        //Opencv ekleme
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        //Pencere ayarıları
        setTitle("Yüz Tanıma ve Veri Toplama");
        setSize(800,600);
        //uygulamayı kapatmak
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //ekranın ortasına açış
        setLocationRelativeTo(null);
        
        //Ekranı bölgelere ayırmak
        setLayout(new BorderLayout());

        //SingsConstants.center ===> metni ortaya yazmak için
        kameraEkrani = new JLabel("Kamera başlatılamadı...", SwingConstants.CENTER);
        
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

        //Butonları eklemek
        altPanel.add(btnKaydet);
        altPanel.add(btnKilitAc);
        add(altPanel, BorderLayout.SOUTH);
       

        //kamerayı açmak 
        KamerayiBaslat();
    }

    public void KamerayiBaslat() {
        //0 ana kamera
        camera = new VideoCapture(0);
        kameraCalisiyor=true;

        //while döngüsü için yeni arayuz
        new Thread(() -> {
            //matris (sayı tablosu oluyormuş)
            Mat frame = new Mat();
        while (kameraCalisiyor && camera.read(frame)){
            //gelen foto boş değiles
            if(!frame.empty()){
                //matrisi resme çevir
                BufferedImage Resim = mat2BufferedImage(frame);

                //yazı sil resim koy
                kameraEkrani.setIcon(new ImageIcon(Resim));
            }
        }
    
    }).start();
}

public void kamerayiKapat(){
    kameraCalisiyor=false;
    //kamera var mı ve camera.isOpened() kamera açık mı
    if(camera != null && camera.isOpened()){
        //kamerayı kapat
        camera.release();
    }
}

//Buffered image == ara belleğe alınmış fotoğraf
//matristen resme
public BufferedImage mat2BufferedImage(Mat m){
    //varsayılan gri 
    int type = BufferedImage.TYPE_BYTE_GRAY;
    //eğer fotoğraf renkliyse maviyeşik kırmızı kullan
    //channels>1 === çok katmanlı/kanallıysa.
    if(m.channels()>1){
        type =BufferedImage.TYPE_3BYTE_BGR;
    }

        // katman sütun satır
    int bufferSize = m.channels() *m.cols()*m.rows();
    // boş dizi
    byte[] b = new byte[bufferSize];
    //(0,0)== ilk koordinat verilerin aktarılacağı
    //b boş dizi
    //m.get= m in verilerini al 
    //m in verilerini 0,0 noktasından başlayarak b dizisine doldurr
    m.get(0,0,b);

    BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
    final byte[] targetPixels =((DataBufferByte) image.getRaster().getDataBuffer()).getData();
    //Dizi kopyalama
    System.arraycopy(b,0,targetPixels,0,b.length);
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