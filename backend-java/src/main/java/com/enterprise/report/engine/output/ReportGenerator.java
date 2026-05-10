package com.enterprise.report.engine.output;

import com.enterprise.report.enums.ReportFormat;
import com.enterprise.report.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
            case EXCEL -> generateExcel(templateName, data);
            case HTML -> generateHtml(templateName, data);
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

    private byte[] generateExcel(String templateName, Map<String, Object> data) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(templateName != null ? templateName : "Report");

            // Title style
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Title row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("AI Generated Report");
            titleCell.setCellStyle(titleStyle);

            // Timestamp row
            Row timeRow = sheet.createRow(1);
            timeRow.createCell(0).setCellValue("Generated at: " + java.time.LocalDateTime.now());

            // Data rows
            int rowNum = 3;
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                Row row = sheet.createRow(rowNum++);
                Cell keyCell = row.createCell(0);
                keyCell.setCellValue(entry.getKey());
                keyCell.setCellStyle(headerStyle);

                Cell valueCell = row.createCell(1);
                Object value = entry.getValue();
                if (value instanceof Number) {
                    valueCell.setCellValue(((Number) value).doubleValue());
                } else {
                    valueCell.setCellValue(String.valueOf(value));
                }
            }

            // Auto-size columns
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(500, "Failed to generate Excel report: " + e.getMessage());
        }
    }

    private byte[] generateHtml(String templateName, Map<String, Object> data) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>").append(templateName != null ? templateName : "AI Report").append("</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: 'Segoe UI', Arial, sans-serif; margin: 40px; color: #333; }\n");
        html.append("h1 { color: #1677ff; border-bottom: 2px solid #1677ff; padding-bottom: 10px; }\n");
        html.append("table { border-collapse: collapse; width: 100%; margin-top: 20px; }\n");
        html.append("th, td { border: 1px solid #e8e8e8; padding: 12px 16px; text-align: left; }\n");
        html.append("th { background: #f0f5ff; color: #1677ff; font-weight: 600; }\n");
        html.append("tr:nth-child(even) { background: #fafafa; }\n");
        html.append("tr:hover { background: #e6f4ff; }\n");
        html.append(".timestamp { color: #999; font-size: 14px; margin-top: 10px; }\n");
        html.append("</style>\n</head>\n<body>\n");

        html.append("<h1>").append(templateName != null ? templateName : "AI Generated Report").append("</h1>\n");
        html.append("<p class=\"timestamp\">Generated at: ").append(java.time.LocalDateTime.now()).append("</p>\n");

        html.append("<table>\n");
        html.append("<tr><th>项目</th><th>值</th></tr>\n");
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            html.append("<tr><td><strong>").append(entry.getKey()).append("</strong></td>");
            html.append("<td>").append(entry.getValue()).append("</td></tr>\n");
        }
        html.append("</table>\n");
        html.append("</body>\n</html>");

        return html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
