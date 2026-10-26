package com.hotel.core.service;

import com.hotel.core.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentProofStorageTest {

    @TempDir
    Path tempDirectory;

    @Test
    void storesSupportedProofInConfiguredDirectory() throws Exception {
        Path source = tempDirectory.resolve("proof.png");
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", source.toFile());
        Path configuredDirectory = tempDirectory.resolve("stored");

        Path stored = new PaymentProofStorage(configuredDirectory).store(source);

        assertTrue(stored.startsWith(configuredDirectory));
        assertTrue(Files.isRegularFile(stored));
        assertTrue(stored.getFileName().toString().endsWith(".png"));
    }

    @Test
    void storesARealJpegProof() throws Exception {
        Path source = tempDirectory.resolve("proof.jpg");
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "jpeg", source.toFile());

        Path stored = new PaymentProofStorage(tempDirectory.resolve("stored")).store(source);

        assertTrue(stored.getFileName().toString().endsWith(".jpg"));
        assertTrue(Files.isRegularFile(stored));
    }

    @Test
    void storesArbitraryProofTypesWithoutUsingTheOriginalFilename() throws Exception {
        Path source = tempDirectory.resolve("transfer-proof.weird");
        Files.writeString(source, "bank transfer receipt");
        PaymentProofStorage storage = new PaymentProofStorage(tempDirectory.resolve("stored"));

        Path stored = storage.storeAnyProof(source);

        assertTrue(Files.isRegularFile(stored));
        assertTrue(stored.getFileName().toString().endsWith(".weird"));
        assertEquals("bank transfer receipt", Files.readString(stored));
        assertTrue(stored.getFileName().toString().length() < 50);
    }

    @Test
    void detectsProofFormatFromContentWhenExtensionIsWrong() throws Exception {
        Path source = tempDirectory.resolve("payment.png");
        Files.write(source, new byte[] {
                (byte) 0xff, (byte) 0xd8, (byte) 0xff, 0
        });

        Path stored = new PaymentProofStorage(tempDirectory.resolve("stored")).store(source);

        assertTrue(stored.getFileName().toString().endsWith(".jpg"));
        assertTrue(Files.isRegularFile(stored));
    }

    @Test
    void storesGifAndBmpProofs() throws Exception {
        Path gif = tempDirectory.resolve("proof.gif");
        Files.write(gif, new byte[] {'G', 'I', 'F', '8', '9', 'a', 0});
        Path bmp = tempDirectory.resolve("proof.bmp");
        Files.write(bmp, new byte[] {'B', 'M', 0, 0});
        PaymentProofStorage storage = new PaymentProofStorage(tempDirectory.resolve("stored"));

        assertTrue(storage.store(gif).getFileName().toString().endsWith(".gif"));
        assertTrue(storage.store(bmp).getFileName().toString().endsWith(".bmp"));
    }

    @Test
    void rejectsUnsupportedOrMismatchedProofFiles() throws Exception {
        Path source = tempDirectory.resolve("proof.txt");
        Files.writeString(source, "not an image");

        assertThrows(ValidationException.class,
                () -> new PaymentProofStorage(tempDirectory.resolve("stored")).store(source));
    }

    @Test
    void rejectsFilesLargerThanConfiguredMaximum() throws Exception {
        Path source = tempDirectory.resolve("large.proof");
        try (var output = Files.newOutputStream(source)) {
            output.write(new byte[(int) PaymentProofStorage.MAX_FILE_SIZE + 1]);
        }

        assertThrows(ValidationException.class,
                () -> new PaymentProofStorage(tempDirectory.resolve("stored")).storeAnyProof(source));
    }
}
