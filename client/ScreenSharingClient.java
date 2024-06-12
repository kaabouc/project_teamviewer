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
            ScreenSharingInterface server = (ScreenSharingInterface) Naming.lookup("rmi://localhost/ScreenSharingServer");

            JFrame frame = new JFrame();
            JLabel imageLabel = new JLabel();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(imageLabel);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setVisible(true);

            frame.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    resizeImageLabel(imageLabel, frame.getSize());
                }
            });

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

            JButton sendFileButton = new JButton("Send File");
            JButton receiveFileButton = new JButton("Receive File");
            frame.getContentPane().add(sendFileButton, BorderLayout.NORTH);
            frame.getContentPane().add(receiveFileButton, BorderLayout.SOUTH);

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
            }, 0, 17);

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
            }, 0, 17);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void resizeImageLabel(JLabel imageLabel, Dimension frameSize) {
        if (imageLabel.getIcon() != null) {
            Image image = ((ImageIcon) imageLabel.getIcon()).getImage();
            Image scaledImage = image.getScaledInstance(frameSize.width, frameSize.height, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(scaledImage));
        }
    }

    private static byte[] captureScreenWithCursor() {
        try {
            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage screenCapture = robot.createScreenCapture(screenRect);

            Point cursorLocation = MouseInfo.getPointerInfo().getLocation();
            Graphics2D g = screenCapture.createGraphics();
            g.setColor(new Color(255, 0, 0, 128));
            g.fillOval(cursorLocation.x, cursorLocation.y, 15, 15);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(screenCapture, "jpg", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
