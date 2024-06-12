package projet.client;

import projet.server.ScreenSharingInterface;
import projet.common.ScreenshotUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Timer;
import java.util.TimerTask;

public class ScreenSharingClient {
    public static void main(String[] args) {
        try {
            // Se connecter au serveur RMI
            ScreenSharingInterface server = (ScreenSharingInterface) Naming.lookup("rmi://localhost/ScreenSharingServer");

            // Créer une fenêtre pour afficher les captures d'écran
            JFrame frame = new JFrame();
            JLabel imageLabel = new JLabel();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(imageLabel);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setVisible(true);

            // Ajouter un écouteur pour redimensionner l'image lorsque la fenêtre est redimensionnée
            frame.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    resizeImageLabel(imageLabel, frame.getSize());
                }
            });

            // Ajouter un écouteur pour capturer les clics de souris et les envoyer au serveur
            imageLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        int serverWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
                        int serverHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
                        int clientWidth = imageLabel.getWidth();
                        int clientHeight = imageLabel.getHeight();

                        int x = e.getX() * serverWidth / clientWidth;
                        int y = e.getY() * serverHeight / clientHeight;

                        server.sendMouseClick(x, y);
                    } catch (RemoteException ex) {
                        ex.printStackTrace();
                    }
                }
            });

            // Ajouter un écouteur pour capturer les frappes clavier et les envoyer au serveur
            frame.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    try {
                        server.sendKeyPress(e.getKeyCode());
                    } catch (RemoteException ex) {
                        ex.printStackTrace();
                    }
                }
            });

            // Boutons pour envoyer et recevoir des fichiers
            JButton sendFileButton = new JButton("Send File");
            JButton receiveFileButton = new JButton("Receive File");
            frame.getContentPane().add(sendFileButton, BorderLayout.NORTH);
            frame.getContentPane().add(receiveFileButton, BorderLayout.SOUTH);

            // Action pour envoyer un fichier au serveur
            sendFileButton.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser();
                if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    try {
                        byte[] data = Files.readAllBytes(file.toPath());
                        server.sendFile(file.getName(), data);
                        JOptionPane.showMessageDialog(frame, "File sent successfully!");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(frame, "Error sending file: " + ex.getMessage());
                    }
                }
            });

            // Action pour recevoir un fichier du serveur
            receiveFileButton.addActionListener(e -> {
                try {
                    String[] files = server.listFiles();
                    String selectedFile = (String) JOptionPane.showInputDialog(frame, "Select file to receive:", "Receive File", JOptionPane.QUESTION_MESSAGE, null, files, files[0]);
                    if (selectedFile != null) {
                        byte[] data = server.receiveFile(selectedFile);
                        File receiveDir = new File(System.getProperty("user.home") + "/Desktop/receive");
                        if (!receiveDir.exists()) {
                            receiveDir.mkdir();
                        }
                        File file = new File(receiveDir, selectedFile);
                        Files.write(file.toPath(), data);
                        JOptionPane.showMessageDialog(frame, "File received and saved to Desktop/receive");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Error receiving file: " + ex.getMessage());
                }
            });

            // Timer pour capturer l'écran et envoyer les captures d'écran au serveur en continu
            Timer captureTimer = new Timer();
            captureTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        byte[] imageBytes = captureScreenWithCursor();
                        if (imageBytes != null) {
                            server.sendScreenshot(imageBytes);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 0, 17); // 17 ms pour 60 FPS

            // Timer pour recevoir les captures d'écran du serveur et les afficher en continu
            Timer receiveTimer = new Timer();
            receiveTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        byte[] receivedImageBytes = server.receiveScreenshot();
                        if (receivedImageBytes != null) {
                            BufferedImage receivedImage = ImageIO.read(new ByteArrayInputStream(receivedImageBytes));
                            if (receivedImage != null) {
                                imageLabel.setIcon(new ImageIcon(receivedImage));
                                resizeImageLabel(imageLabel, frame.getSize());
                                frame.repaint();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 0, 17); // 17 ms pour 60 FPS

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Méthode pour redimensionner l'image affichée dans le JLabel
    private static void resizeImageLabel(JLabel imageLabel, Dimension frameSize) {
        if (imageLabel.getIcon() != null) {
            Image image = ((ImageIcon) imageLabel.getIcon()).getImage();
            Image scaledImage = image.getScaledInstance(frameSize.width, frameSize.height, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(scaledImage));
        }
    }

    // Méthode pour capturer l'écran avec le curseur
    private static byte[] captureScreenWithCursor() {
        try {
            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage screenCapture = robot.createScreenCapture(screenRect);

            // Capture la position du curseur et dessine un cercle rouge à cette position
            Point cursorLocation = MouseInfo.getPointerInfo().getLocation();
            Graphics2D g = screenCapture.createGraphics();
            g.setColor(new Color(255, 0, 0, 128));
            g.fillOval(cursorLocation.x, cursorLocation.y, 15, 15);
            g.dispose();

            // Convertit l'image capturée en tableau d'octets
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(screenCapture, "jpg", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
