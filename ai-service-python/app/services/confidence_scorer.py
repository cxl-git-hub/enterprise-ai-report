"""Confidence scoring service for AI-generated outputs."""

import re
from typing import Dict, Any, List, Optional


class ConfidenceScorer:
    """Calculates confidence scores for AI-generated SQL and analysis results."""

    # Common SQL patterns that indicate high confidence
    HIGH_CONFIDENCE_PATTERNS = [
        r'\bSELECT\b.*\bFROM\b',
        r'\bGROUP\s+BY\b',
        r'\bORDER\s+BY\b',
        r'\bWHERE\b',
        r'\bJOIN\b.*\bON\b',
        r'\bSUM\s*\(',
        r'\bCOUNT\s*\(',
        r'\bAVG\s*\(',
        r'\bMAX\s*\(',
        r'\bMIN\s*\(',
    ]

    # Patterns that indicate potential issues (lower confidence)
    LOW_CONFIDENCE_PATTERNS = [
        r'\bUNION\b',  # Complex queries
        r'\bSUBQUERY\b',
        r'\bCASE\s+WHEN\b.*\bCASE\s+WHEN\b',  # Multiple nested CASE
        r'\bHAVING\b.*\bHAVING\b',
    ]

    @staticmethod
    def score_nl2sql(
        natural_query: str,
        generated_sql: str,
        schema_columns: Optional[List[Dict[str, Any]]] = None,
    ) -> Dict[str, Any]:
        """Calculate confidence score for NL2SQL generation."""
        score = 70  # Base score
        reasons = []

        if not generated_sql or not generated_sql.strip():
            return {"score": 0, "level": "low", "reasons": ["SQL生成失败"]}

        sql_upper = generated_sql.upper().strip()

        # Check basic SQL structure
        if re.search(r'\bSELECT\b', sql_upper) and re.search(r'\bFROM\b', sql_upper):
            score += 10
            reasons.append("SQL结构完整")
        else:
            score -= 20
            reasons.append("SQL结构不完整")

        # Check if SQL references known columns from schema
        if schema_columns:
            column_names = {col.get("name", "").lower() for col in schema_columns}
            sql_lower = generated_sql.lower()
            matched_cols = sum(1 for col in column_names if col in sql_lower)
            if matched_cols >= 2:
                score += 10
                reasons.append(f"引用了{matched_cols}个已知字段")
            elif matched_cols == 0:
                score -= 15
                reasons.append("未匹配到已知字段")

        # Check for common patterns
        pattern_matches = sum(
            1 for p in ConfidenceScorer.HIGH_CONFIDENCE_PATTERNS
            if re.search(p, generated_sql, re.IGNORECASE)
        )
        if pattern_matches >= 3:
            score += 5
            reasons.append("使用了标准SQL模式")

        # Check for complex/problematic patterns
        issue_matches = sum(
            1 for p in ConfidenceScorer.LOW_CONFIDENCE_PATTERNS
            if re.search(p, generated_sql, re.IGNORECASE)
        )
        if issue_matches > 0:
            score -= 10
            reasons.append("包含复杂查询结构")

        # Query complexity matching
        query_keywords = len(natural_query.split())
        sql_keywords = len(generated_sql.split())
        if query_keywords > 5 and sql_keywords < 5:
            score -= 10
            reasons.append("SQL过于简单，可能遗漏查询意图")

        # Ensure score is in range
        score = max(0, min(100, score))

        level = "high" if score >= 80 else "medium" if score >= 60 else "low"

        return {
            "score": score,
            "level": level,
            "reasons": reasons,
        }

    @staticmethod
    def score_analysis(
        query: str,
        analysis_type: str,
        result_data: Dict[str, Any],
    ) -> Dict[str, Any]:
        """Calculate confidence score for analysis results."""
        score = 65  # Base score for analysis
        reasons = []

        # Check if narrative exists and is substantial
        narrative = result_data.get("narrative", "")
        if len(narrative) > 100:
            score += 10
            reasons.append("分析内容详实")
        elif len(narrative) < 30:
            score -= 10
            reasons.append("分析内容过少")

        # Check if findings exist
        findings = result_data.get("findings", [])
        if len(findings) >= 2:
            score += 10
            reasons.append(f"包含{len(findings)}个发现")
        elif len(findings) == 0:
            score -= 5
            reasons.append("未产生具体发现")

        # Check if chart data exists
        chart_data = result_data.get("chart_config") or result_data.get("chartData")
        if chart_data:
            score += 5
            reasons.append("包含可视化数据")

        # Analysis type specific adjustments
        if analysis_type == "trend" and "趋势" in narrative:
            score += 5
        if analysis_type == "anomaly" and ("异常" in narrative or "异常" in narrative):
            score += 5

        score = max(0, min(100, score))
        level = "high" if score >= 80 else "medium" if score >= 60 else "low"

        return {
            "score": score,
            "level": level,
            "reasons": reasons,
        }

    @staticmethod
    def extract_data_sources(
        generated_sql: Optional[str] = None,
        dataset_ids: Optional[List[int]] = None,
        schema_columns: Optional[List[Dict[str, Any]]] = None,
    ) -> List[Dict[str, Any]]:
        """Extract data source citations from SQL and context."""
        sources = []

        if dataset_ids:
            for ds_id in dataset_ids:
                source = {
                    "datasetId": ds_id,
                    "fields": [],
                }

                # Try to extract fields from SQL
                if generated_sql:
                    # Simple field extraction from SELECT clause
                    select_match = re.search(
                        r'SELECT\s+(.*?)\s+FROM', generated_sql, re.IGNORECASE | re.DOTALL
                    )
                    if select_match:
                        fields_str = select_match.group(1)
                        # Extract field names (handle aliases)
                        fields = []
                        for field in fields_str.split(','):
                            field = field.strip()
                            # Remove aliases
                            parts = field.split(' AS ')
                            parts = field.split(' as ')
                            field_name = parts[0].strip()
                            # Remove function wrappers
                            field_name = re.sub(r'\w+\(([^)]+)\)', r'\1', field_name)
                            # Remove table prefixes
                            field_name = field_name.split('.')[-1].strip().strip('"').strip("'")
                            if field_name and field_name != '*' and not field_name.isdigit():
                                fields.append(field_name)
                        source["fields"] = fields[:10]  # Limit to 10 fields

                sources.append(source)

        return sources
