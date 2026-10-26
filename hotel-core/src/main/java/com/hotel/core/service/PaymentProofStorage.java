package com.hotel.core.service;

import com.hotel.core.exception.ValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public final class PaymentProofStorage {

    public static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private final Path storageDirectory;

    public PaymentProofStorage(Path storageDirectory) {
        if (storageDirectory == null) {
            throw new IllegalArgumentException("Payment proof directory is required.");
        }
        this.storageDirectory = storageDirectory.toAbsolutePath().normalize();
    }

    public static PaymentProofStorage fromConfiguration() {
        String configured = System.getProperty("hotel.payment.proof.directory");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("HOTEL_PAYMENT_PROOF_DIRECTORY");
        }
        Path directory = configured == null || configured.isBlank()
                ? Path.of(System.getProperty("user.home"), ".hotel-reservation", "payment-proofs")
                : Path.of(configured.trim());
        return new PaymentProofStorage(directory);
    }

    public Path store(Path source) throws IOException {
        String extension = validateFile(source);
        return storeValidatedFile(source, extension);
    }

    public Path storePaymentProof(Path source) throws IOException {
        return storeAnyProof(source);
    }

    public Path storeAnyProof(Path source) throws IOException {
        validateFileSize(source);
        String extension = safeExtension(source);
        return storeValidatedFile(source, extension);
    }

    private Path storeValidatedFile(Path source, String extension) throws IOException {
        Files.createDirectories(storageDirectory);
        Path target = storageDirectory.resolve(UUID.randomUUID() + extension).normalize();
        if (!target.startsWith(storageDirectory)) {
            throw new IOException("The payment proof path is outside the configured directory.");
        }
        try {
            copyBounded(source, target);
            if (!extension.equals(safeExtension(target))) {
                throw new ValidationException("The payment proof file changed while it was being uploaded.");
            }
            return target;
        } catch (IOException | RuntimeException exception) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            throw exception;
        }
    }

    public void deleteStored(Path storedFile) throws IOException {
        Path resolved = resolveStored(storedFile);
        Files.deleteIfExists(resolved);
    }

    public Path resolveStored(Path storedFile) throws IOException {
        if (storedFile == null) {
            throw new IOException("The payment proof path is outside the configured directory.");
        }
        Path resolved = storedFile.toAbsolutePath().normalize();
        if (!resolved.startsWith(storageDirectory) || !Files.isRegularFile(resolved)) {
            throw new IOException("The payment proof path is outside the configured directory or missing.");
        }
        return resolved;
    }

    private static String validateFile(Path source) throws IOException {
        validateFileSize(source);
        byte[] header;
        try (InputStream input = Files.newInputStream(source)) {
            header = input.readNBytes(12);
        }
        if (startsWith(header, new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff})) {
            return ".jpg";
        }
        if (startsWith(header, new byte[]{
                (byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10
        })) {
            return ".png";
        }
        if (header.length >= 12 && startsWith(header, new byte[]{'R', 'I', 'F', 'F'})
                && header[8] == 'W' && header[9] == 'E'
                && header[10] == 'B' && header[11] == 'P') {
            return ".webp";
        }
        if (startsWith(header, new byte[]{'G', 'I', 'F', '8'})
                && header.length >= 6 && (header[4] == '7' || header[4] == '9')
                && header[5] == 'a') {
            return ".gif";
        }
        if (startsWith(header, new byte[]{'B', 'M'})) {
            return ".bmp";
        }
        if (startsWith(header, new byte[]{'%', 'P', 'D', 'F', '-'})) {
            return ".pdf";
        }
        String imageFormat = detectImageFormat(source);
        if (imageFormat != null) {
            return imageFormat;
        }
        throw new ValidationException(
                "Payment proof must be a PNG, JPG/JPEG, WEBP, GIF, BMP, or PDF file.");
    }

    private static void validateFileSize(Path source) throws IOException {
        if (source == null || !Files.isRegularFile(source)) {
            throw new ValidationException("Select a payment proof file.");
        }
        long size = Files.size(source);
        if (size < 1 || size > MAX_FILE_SIZE) {
            throw new ValidationException("Payment proof files must be no larger than 5 MB.");
        }
    }

    private static String safeExtension(Path source) {
        String name = source.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1 || name.length() - dot > 12) {
            return ".bin";
        }
        String extension = name.substring(dot).toLowerCase(Locale.ROOT);
        return extension.matches("\\.[a-z0-9]{1,10}") ? extension : ".bin";
    }

    private static String detectImageFormat(Path source) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(source.toFile())) {
            if (input == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            while (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    return switch (reader.getFormatName().toLowerCase(Locale.ROOT)) {
                        case "jpg", "jpeg" -> ".jpg";
                        case "png" -> ".png";
                        case "webp" -> ".webp";
                        case "gif" -> ".gif";
                        case "bmp" -> ".bmp";
                        default -> null;
                    };
                } finally {
                    reader.dispose();
                }
            }
            return null;
        }
    }

    private static boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (value[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private static void copyBounded(Path source, Path target) throws IOException {
        byte[] buffer = new byte[8192];
        long copied = 0;
        try (InputStream input = Files.newInputStream(source);
             OutputStream output = Files.newOutputStream(
                     target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            int count;
            while ((count = input.read(buffer)) != -1) {
                copied += count;
                if (copied > MAX_FILE_SIZE) {
                    throw new ValidationException("Payment proof files must be no larger than 5 MB.");
                }
                output.write(buffer, 0, count);
            }
        }
    }
}
