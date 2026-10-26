package com.hotel.desktop.customer;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Component;
import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

public final class ReceiptPdfExporter {

    private ReceiptPdfExporter() {
    }

    public static void chooseAndExport(
            Component parent, String suggestedName, String heading, List<String> lines) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Download receipt");
        chooser.setSelectedFile(new java.io.File(suggestedName + ".pdf"));
        chooser.setFileFilter(new FileNameExtensionFilter("PDF document", "pdf"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        java.io.File selected = chooser.getSelectedFile();
        if (!selected.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
            selected = new java.io.File(selected.getParentFile(), selected.getName() + ".pdf");
        }
        if (selected.exists()) {
            int replace = JOptionPane.showConfirmDialog(
                    parent, "Replace the existing file " + selected.getName() + "?",
                    "Confirm overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (replace != JOptionPane.YES_OPTION) {
                return;
            }
        }
        Document document = new Document();
        try (FileOutputStream output = new FileOutputStream(selected)) {
            document.setMargins(48, 48, 44, 44);
            PdfWriter.getInstance(document, output);
            document.open();
            Font headingFont = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(28, 30, 37));
            Font metaFont = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(104, 108, 117));
            Paragraph title = new Paragraph(heading, headingFont);
            title.setSpacingAfter(6);
            document.add(title);
            document.add(new Paragraph("HOTEL RESERVATION  ·  OFFICIAL RECEIPT", metaFont));
            document.add(new Paragraph(" "));
            PdfPTable details = new PdfPTable(new float[]{1.0f, 1.45f});
            details.setWidthPercentage(100);
            details.setSpacingBefore(12);
            details.setSpacingAfter(10);
            for (String line : lines) {
                int separator = line.indexOf(':');
                if (separator < 0) {
                    PdfPCell section = new PdfPCell(new Phrase(line,
                            new Font(Font.HELVETICA, 10, Font.BOLD, new Color(104, 108, 117))));
                    section.setColspan(2);
                    section.setBorderColor(new Color(226, 228, 232));
                    section.setPadding(8);
                    details.addCell(section);
                    continue;
                }
                String label = line.substring(0, separator).trim();
                String value = line.substring(separator + 1).trim();
                boolean total = label.equalsIgnoreCase("total");
                Font rowFont = new Font(Font.HELVETICA, total ? 11 : 10,
                        total ? Font.BOLD : Font.NORMAL,
                        total ? new Color(28, 30, 37) : new Color(72, 76, 84));
                PdfPCell labelCell = new PdfPCell(new Phrase(label, rowFont));
                PdfPCell valueCell = new PdfPCell(new Phrase(value, rowFont));
                labelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                for (PdfPCell cell : new PdfPCell[]{labelCell, valueCell}) {
                    cell.setBorderColor(new Color(226, 228, 232));
                    cell.setPadding(8);
                    if (total) {
                        cell.setBackgroundColor(new Color(248, 245, 233));
                    }
                }
                details.addCell(labelCell);
                details.addCell(valueCell);
            }
            document.add(details);
            document.add(new Paragraph("Thank you for choosing us.", metaFont));
            document.close();
            JOptionPane.showMessageDialog(parent,
                    "Receipt saved to " + selected.getAbsolutePath(),
                    "Receipt downloaded", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | DocumentException exception) {
            if (document.isOpen()) {
                document.close();
            }
            JOptionPane.showMessageDialog(parent,
                    "Unable to save the receipt: " + exception.getMessage(),
                    "Receipt download failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
