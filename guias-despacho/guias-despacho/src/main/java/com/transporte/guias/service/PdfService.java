package com.transporte.guias.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.transporte.guias.exception.BusinessException;
import com.transporte.guias.model.GuiaDespacho;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    @Value("${app.efs.base-path:/mnt/efs/guias}")
    private String efsBasePath;

    private static final Font TITLE_FONT   = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
    private static final Font HEADER_FONT  = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font NORMAL_FONT  = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL);
    private static final Font SMALL_FONT   = new Font(Font.FontFamily.HELVETICA, 9,  Font.ITALIC);

    /**
     * Genera el PDF de la guía en el EFS y retorna la ruta absoluta del archivo.
     */
    public String generarPdf(GuiaDespacho guia) {
        String dirPath = buildDirPath(guia);
        String fileName = "guia_" + guia.getNumeroGuia() + ".pdf";
        String fullPath = dirPath + "/" + fileName;

        try {
            // Crear directorios si no existen
            Files.createDirectories(Paths.get(dirPath));

            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(fullPath));
            document.open();

            agregarEncabezado(document, guia);
            agregarDatosGuia(document, guia);
            agregarTablaDetalles(document, guia);
            agregarPiePagina(document, guia);

            document.close();
        } catch (DocumentException | IOException e) {
            throw new BusinessException("Error al generar PDF: " + e.getMessage());
        }

        return fullPath;
    }

    private void agregarEncabezado(Document doc, GuiaDespacho guia) throws DocumentException {
        Paragraph titulo = new Paragraph("GUÍA DE DESPACHO", TITLE_FONT);
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingAfter(4);
        doc.add(titulo);

        Paragraph numero = new Paragraph("N° " + guia.getNumeroGuia(), HEADER_FONT);
        numero.setAlignment(Element.ALIGN_CENTER);
        numero.setSpacingAfter(20);
        doc.add(numero);

        LineSeparator line = new LineSeparator();
        doc.add(new Chunk(line));
        doc.add(Chunk.NEWLINE);
    }

    private void agregarDatosGuia(Document doc, GuiaDespacho guia) throws DocumentException {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(10);
        tabla.setSpacingAfter(10);

        agregarFila(tabla, "Transportista:",     guia.getTransportista());
        agregarFila(tabla, "Destinatario:",       guia.getDestinatario());
        agregarFila(tabla, "Dirección destino:",  guia.getDireccionDestino());
        agregarFila(tabla, "Fecha de despacho:",  guia.getFechaDespacho().format(fmt));
        agregarFila(tabla, "Estado:",             guia.getEstado().name());

        doc.add(tabla);
    }

    private void agregarTablaDetalles(Document doc, GuiaDespacho guia) throws DocumentException {
        doc.add(new Paragraph("Detalle de carga", HEADER_FONT));
        doc.add(Chunk.NEWLINE);

        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);

        PdfPCell hDesc = new PdfPCell(new Phrase("Descripción", HEADER_FONT));
        PdfPCell hPeso = new PdfPCell(new Phrase("Peso (kg)",   HEADER_FONT));
        hDesc.setBackgroundColor(BaseColor.LIGHT_GRAY);
        hPeso.setBackgroundColor(BaseColor.LIGHT_GRAY);
        tabla.addCell(hDesc);
        tabla.addCell(hPeso);

        tabla.addCell(new Phrase(guia.getDescripcionCarga(), NORMAL_FONT));
        tabla.addCell(new Phrase(
                guia.getPesoCarga() != null ? guia.getPesoCarga() + " kg" : "No especificado",
                NORMAL_FONT));

        doc.add(tabla);
        doc.add(Chunk.NEWLINE);
    }

    private void agregarPiePagina(Document doc, GuiaDespacho guia) throws DocumentException {
        LineSeparator line = new LineSeparator();
        doc.add(new Chunk(line));
        doc.add(Chunk.NEWLINE);

        Paragraph pie = new Paragraph(
                "Documento generado automáticamente - " + guia.getFechaCreacion(), SMALL_FONT);
        pie.setAlignment(Element.ALIGN_CENTER);
        doc.add(pie);
    }

    private void agregarFila(PdfPTable tabla, String label, String valor) {
        PdfPCell cLabel = new PdfPCell(new Phrase(label, HEADER_FONT));
        PdfPCell cValor = new PdfPCell(new Phrase(valor != null ? valor : "-", NORMAL_FONT));
        cLabel.setBorder(Rectangle.BOTTOM);
        cValor.setBorder(Rectangle.BOTTOM);
        tabla.addCell(cLabel);
        tabla.addCell(cValor);
    }

    /** Retorna la ruta EFS esperada sin generar el archivo (para consultas). */
    public String getExpectedPath(GuiaDespacho guia) {
        return buildDirPath(guia) + "/guia_" + guia.getNumeroGuia() + ".pdf";
    }

    private String buildDirPath(GuiaDespacho guia) {
        String fecha = guia.getFechaDespacho().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String transportistaNorm = guia.getTransportista().replaceAll("\\s+", "_").toLowerCase();
        return efsBasePath + "/" + fecha + "/" + transportistaNorm;
    }
}
