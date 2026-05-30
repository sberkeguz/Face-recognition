import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class Yuztanimauygulamasi extends JFrame {

    private JLabel kameraEkrani;
    //kamera 
    private VideoCapture camera; 
    //kamera durumu
    private boolean kameraCalisiyor = false; 

    public Yuztanimauygulamasi(){
        //Opencv ekleme
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        //Pencere ayarıları
        setTitle("Yüz Tanıma ve Veri Toplama");
        setSize(800,600);
        //uygulamayı kapatmak
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //ekranın ortasına açış
        setLocationRelativeTo(null);
        
        //SingsConstants.center ===> metni ortaya yazmak için
        kameraEkrani = ne JLabel("Kamera başlatılamd",SwingConstants.CENTER)
        
        add(kameraEkrani);

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
            if(!frame.empty){
                //matrisi resme çevir
                BufferedImage Resim = mat2BufferedImage(frame);

                //yazı sil resim koy
                kameraEkrani.setIcon(new ImageIcon(Resim));
            }
        }
    
    }).start();
}


public BufferedImage mat2BufferedImage(mat m){
    
}
