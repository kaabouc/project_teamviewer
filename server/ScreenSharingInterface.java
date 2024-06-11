package projet.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ScreenSharingInterface extends Remote {
    void sendScreenshot(byte[] image) throws RemoteException;
    byte[] receiveScreenshot() throws RemoteException;

    void sendMouseClick(int x, int y) throws RemoteException;
    void sendKeyPress(int keyCode) throws RemoteException;

    // Méthodes pour envoyer et recevoir des fichiers
    void sendFile(String fileName, byte[] data) throws RemoteException;
    byte[] receiveFile(String fileName) throws RemoteException;
    String[] listFiles() throws RemoteException;
}
