package projet.common;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

// Classe utilitaire pour capturer l'écran
public class ScreenshotUtils {
    // Méthode pour capturer l'écran entier et retourner l'image sous forme de tableau d'octets
    public static byte[] captureScreen() throws AWTException, IOException {
        // Définir la zone de capture de l'écran (taille de l'écran)
        Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

        // Capturer l'écran entier en tant qu'image BufferedImage
        BufferedImage capture = new Robot().createScreenCapture(screenRect);

        // Convertir l'image capturée en tableau d'octets
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(capture, "jpg", baos); // Écrire l'image dans le ByteArrayOutputStream en format JPEG

        // Retourner le tableau d'octets contenant l'image capturée
        return baos.toByteArray();
    }
}
