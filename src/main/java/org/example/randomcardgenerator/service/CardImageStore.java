package org.example.randomcardgenerator.service;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Cache locale: download seriali, file validati e nessun JPEG parziale pubblicato. */
final class CardImageStore {
    private final Path directory;
    private final long delayMillis;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "card-image-download");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<Integer, CompletableFuture<File>> pending = new HashMap<>();
    private final Map<Integer, Long> failures = new HashMap<>();
    private long nextDownloadAt;

    CardImageStore(Path directory, long delayMillis) {
        this.directory = directory;
        this.delayMillis = delayMillis;
    }

    synchronized CompletableFuture<File> get(int id, URI source) {
        if (pending.containsKey(id)) return pending.get(id);
        CompletableFuture<File> result = new CompletableFuture<>();
        pending.put(id, result);
        worker.execute(() -> {
            try {
                File file = obtain(id, source);
                synchronized (this) { failures.remove(id); }
                result.complete(file);
            } catch (Exception error) {
                synchronized (this) {
                    // Una selezione ripetuta non deve martellare il server offline.
                    failures.putIfAbsent(id, System.currentTimeMillis());
                }
                result.completeExceptionally(error);
            } finally {
                synchronized (this) { pending.remove(id, result); }
            }
        });
        return result;
    }

    private File obtain(int id, URI source) throws IOException, InterruptedException {
        Path destination = directory.resolve(id + ".jpg");
        if (validImage(destination)) return destination.toFile();
        synchronized (this) {
            Long failedAt = failures.get(id);
            if (failedAt != null && System.currentTimeMillis() - failedAt < 30_000) {
                throw new IOException("Download non riuscito: riprova fra 30 secondi");
            }
        }
        Files.createDirectories(directory);
        long wait = nextDownloadAt - System.currentTimeMillis();
        if (wait > 0) Thread.sleep(wait);
        nextDownloadAt = System.currentTimeMillis() + delayMillis;

        HttpURLConnection connection = (HttpURLConnection) source.toURL().openConnection();
        connection.setConnectTimeout(5_000);
        connection.setReadTimeout(10_000);
        connection.setRequestProperty("User-Agent", "RandomCardGenerator/1.0");
        Path temporary = null;
        try {
            int status = connection.getResponseCode();
            if (status != 200) throw new IOException("HTTP " + status + " per la carta " + id);
            temporary = Files.createTempFile(directory, id + "-", ".part");
            try (InputStream input = connection.getInputStream()) {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            if (!validImage(temporary)) throw new IOException("Immagine non valida per la carta " + id);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return destination.toFile();
        } finally {
            connection.disconnect();
            if (temporary != null) Files.deleteIfExists(temporary);
        }
    }

    private boolean validImage(Path path) {
        try {
            return Files.isRegularFile(path) && Files.size(path) > 0
                    && ImageIO.read(path.toFile()) != null;
        } catch (IOException error) {
            return false;
        }
    }
}
