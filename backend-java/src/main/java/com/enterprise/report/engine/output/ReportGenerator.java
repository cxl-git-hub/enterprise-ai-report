package com.enterprise.report.engine.output;

import com.enterprise.report.enums.ReportFormat;
import com.enterprise.report.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.xslf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class ReportGenerator {

    public byte[] generate(ReportFormat format, String templateName, Map<String, Object> data) {
        return switch (format) {
            case WORD -> generateWord(templateName, data);
            case PPT -> generatePpt(templateName, data);
            case PDF -> generatePdf(templateName, data);
        };
    }

    private byte[] generateWord(String templateName, Map<String, Object> data) {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XWPFParagraph title = document.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(20);
            titleRun.setText("AI Generated Report");

            XWPFParagraph subtitle = document.createParagraph();
            subtitle.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun subtitleRun = subtitle.createRun();
            subtitleRun.setFontSize(12);
            subtitleRun.setText("Generated at: " + java.time.LocalDateTime.now());

            document.createParagraph();

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setBold(true);
                run.setFontSize(14);
                run.setText(entry.getKey() + ": ");

                XWPFRun valueRun = paragraph.createRun();
                valueRun.setFontSize(12);
                valueRun.setText(String.valueOf(entry.getValue()));
            }

            document.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(500, "Failed to generate Word report: " + e.getMessage());
        }
    }

    private byte[] generatePpt(String templateName, Map<String, Object> data) {
        try (XMLSlideShow ppt = new XMLSlideShow();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSLFSlideMaster master = ppt.getSlideMasters().get(0);
            XSLFSlideLayout layout = master.getLayout(SlideLayout.TITLE);

            XSLFSlide slide = ppt.createSlide(layout);
            XSLFTextShape titleShape = slide.getPlaceholder(0);
            titleShape.setText("AI Generated Report");

            XSLFTextShape subtitleShape = slide.getPlaceholder(1);
            subtitleShape.setText("Generated at: " + java.time.LocalDateTime.now());

            XSLFSlide contentSlide = ppt.createSlide();
            XSLFTextShape contentShape = contentSlide.createAutoShape();
            contentShape.setAnchor(new java.awt.Rectangle(50, 50, 600, 400));

            StringBuilder content = new StringBuilder();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                content.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            contentShape.setText(content.toString());

            ppt.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(500, "Failed to generate PPT report: " + e.getMessage());
        }
    }

    private byte[] generatePdf(String templateName, Map<String, Object> data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(out);
            com.itextpdf.kernel.pdf.PdfDocument pdf = new com.itextpdf.kernel.pdf.PdfDocument(writer);
            com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf);

            com.itextpdf.layout.element.Paragraph title = new com.itextpdf.layout.element.Paragraph("AI Generated Report")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
            document.add(title);

            com.itextpdf.layout.element.Paragraph subtitle = new com.itextpdf.layout.element.Paragraph(
                    "Generated at: " + java.time.LocalDateTime.now())
                    .setFontSize(12)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
            document.add(subtitle);

            document.add(new com.itextpdf.layout.element.Paragraph(" "));

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                com.itextpdf.layout.element.Paragraph paragraph = new com.itextpdf.layout.element.Paragraph()
                        .add(new com.itextpdf.layout.element.Text(entry.getKey() + ": ").setBold().setFontSize(14))
                        .add(new com.itextpdf.layout.element.Text(String.valueOf(entry.getValue())).setFontSize(12));
                document.add(paragraph);
            }

            document.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(500, "Failed to generate PDF report: " + e.getMessage());
        }
    }
}
