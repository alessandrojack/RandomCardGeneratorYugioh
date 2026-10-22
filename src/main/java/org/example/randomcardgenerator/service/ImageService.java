package org.example.randomcardgenerator.service;

import org.example.randomcardgenerator.utils.AppConstants;
import java.io.*;
import java.net.URL;

public class ImageService {

    public ImageService() {
        new File(AppConstants.IMAGE_DIR).mkdirs();
    }

    public File getLocalImageFile(int imageId) {
        return new File(AppConstants.IMAGE_DIR + File.separator + imageId + ".jpg");
    }

    public void downloadImageAsync(String imageUrl, int cardId) {
        File file = getLocalImageFile(cardId);
        if (file.exists() && file.length() > 0) return;

        // Eseguiamo in un thread separato per non bloccare il caricamento JSON
        new Thread(() -> {
            try (BufferedInputStream in = new BufferedInputStream(new URL(imageUrl).openStream());
                 FileOutputStream fileOutputStream = new FileOutputStream(file)) {

                byte[] dataBuffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }
            } catch (Exception e) {
                System.err.println("Errore download immagine " + cardId);
            }
        }).start();
    }
}