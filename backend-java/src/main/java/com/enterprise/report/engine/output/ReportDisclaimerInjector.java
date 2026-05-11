package com.enterprise.report.engine.output;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adds AI disclaimer watermarks and data lineage info to generated reports.
 */
@Slf4j
@Component
public class ReportDisclaimerInjector {

    /** Standard AI disclaimer text */
    public static final String DISCLAIMER_TEXT = 
        "本报告由 AI 辅助生成，数据仅供参考，请以原始数据源为准。基于 AI 生成的内容可能存在偏差，重要决策请核实原始数据。";

    /** Short watermark text for headers/footers */
    public static final String WATERMARK_TEXT = "AI 生成 · 仅供参考";

    /**
     * Get disclaimer HTML block for embedding in reports.
     */
    public String getDisclaimerHtml() {
        return """
            <div style="margin-top:40px;padding:16px;background:#fffbe6;border:1px solid #ffe58f;border-radius:8px;font-size:12px;color:#666;">
                <p><strong>⚠️ 免责声明</strong></p>
                <p>%s</p>
                <p style="font-size:11px;color:#999;margin-top:8px;">生成时间: %s | 系统自动生成</p>
            </div>
            """.formatted(DISCLAIMER_TEXT, java.time.LocalDateTime.now());
    }

    /**
     * Get disclaimer text for plain text formats.
     */
    public String getDisclaimerText() {
        return "\n\n--- 免责声明 ---\n" + DISCLAIMER_TEXT + "\n生成时间: " + java.time.LocalDateTime.now() + "\n";
    }

    /**
     * Get data lineage section HTML.
     */
    public String getLineageHtml(String sourceName, String fields, String timeRange) {
        return """
            <div style="margin-top:16px;padding:12px;background:#e6f4ff;border:1px solid #91caff;border-radius:8px;font-size:12px;">
                <p><strong>📊 数据溯源</strong></p>
                <p>数据来源: %s</p>
                %s%s
            </div>
            """.formatted(
                sourceName,
                fields != null ? "<p>涉及字段: " + fields + "</p>" : "",
                timeRange != null ? "<p>数据范围: " + timeRange + "</p>" : ""
            );
    }

    /**
     * Wrap report content with disclaimer.
     */
    public String wrapWithDisclaimer(String content, String sourceName, String fields, String timeRange) {
        StringBuilder sb = new StringBuilder();
        sb.append(content);
        sb.append(getDisclaimerHtml());
        if (sourceName != null) {
            sb.append(getLineageHtml(sourceName, fields, timeRange));
        }
        return sb.toString();
    }
}
