package org.djezzy.pfe.util;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class PdfImageUtil {
    public List<String> toBase64PngPages(Path pdfPath) {
        List<String> pages = new ArrayList<>();
        try (PDDocument document = PDDocument.load(pdfPath.toFile(), MemoryUsageSetting.setupTempFileOnly())) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, 100);
                pages.add(toBase64Png(image));
                image.flush();
            }
            return pages;
        } catch (IOException ex) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Unable to parse uploaded PDF");
        }
    }

    private String toBase64Png(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }
}

