import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

public class VeriToplama{
    public static void main(String[] args) {
        
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        VideoCapture camera = new VideoCapture(0);
        //Yüzün neye benzediğini bilgisayara gösteren veri dosyası
        CascadeClassifier faceDetector = new CascadeClassifier("haarcascade_frontalface_default.xml");

        //Matris
        //Renkli fotoğraf saklama
        Mat frame = new Mat();
        //grayscale halini saklamak için kod
        Mat grayFrame = new Mat();
        int maxfoto=500;
        int sayac =0;

    }
}