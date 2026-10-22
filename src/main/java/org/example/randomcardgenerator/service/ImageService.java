package org.example.randomcardgenerator.service;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.randomcardgenerator.utils.AppConstants;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class ImageService {
    // Condiviso tra griglia e dettaglio: una sola richiesta per ciascuna carta.
    private static final CardImageStore STORE =
            new CardImageStore(Path.of(AppConstants.IMAGE_DIR), 500);
    private static final Object REQUEST_KEY = new Object();

    public File getLocalImageFile(int imageId) {
        return new File(AppConstants.IMAGE_DIR, imageId + ".jpg");
    }

    /** Chiamare dal thread JavaFX. Invalida anche eventuali richieste precedenti. */
    public void clearImage(ImageView view) {
        view.getProperties().put(REQUEST_KEY, new Object());
        view.setImage(null);
    }

    /** status riceve "" al successo, altrimenti un messaggio da mostrare. */
    public void loadInto(ImageView view, int imageId, Consumer<String> status) {
        Object request = new Object();
        view.getProperties().put(REQUEST_KEY, request);
        view.setImage(null);
        status.accept("Caricamento...");

        URI source = URI.create("https://images.ygoprodeck.com/images/cards/" + imageId + ".jpg");
        STORE.get(imageId, source).whenComplete((file, error) -> Platform.runLater(() -> {
            // Cambiando selezione, una risposta vecchia non deve sostituire la nuova carta.
            if (view.getProperties().get(REQUEST_KEY) != request) return;
            if (error != null) {
                reportFailure(imageId, error, status);
                return;
            }
            try (InputStream input = Files.newInputStream(file.toPath())) {
                Image image = new Image(input);
                if (image.isError()) {
                    reportFailure(imageId, image.getException(), status);
                } else {
                    view.setImage(image);
                    status.accept("");
                }
            } catch (Exception failure) {
                reportFailure(imageId, failure, status);
            }
        }));
    }

    private void reportFailure(int id, Throwable error, Consumer<String> status) {
        String reason = error == null ? "formato non supportato" : error.getMessage();
        System.err.println("Immagine carta " + id + ": " + reason);
        status.accept("Immagine non disponibile");
    }

    // Compatibilita con CardJsonParser: usa la stessa coda e la stessa cache.
    public void downloadImageAsync(String imageUrl, int cardId) {
        try {
            STORE.get(cardId, URI.create(imageUrl)).whenComplete((file, error) -> {
                if (error != null) System.err.println("Immagine carta " + cardId + ": " + error.getMessage());
            });
        } catch (IllegalArgumentException error) {
            System.err.println("URL immagine non valido per la carta " + cardId);
        }
    }
}
