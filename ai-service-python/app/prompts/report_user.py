"""Report generation user prompt template."""

REPORT_USER_TEMPLATE = """Generate a comprehensive business report based on the following information:

## Report Title: {title}

## Description
{description}

## KPI Data
{kpi_context}

## Supporting Data
{data_context}

## Report Template
{template_context}

Generate the complete report in Markdown format. Ensure all sections are well-developed with specific data points and actionable recommendations.
"""
