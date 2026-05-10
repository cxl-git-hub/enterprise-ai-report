"""Analysis user prompt template."""

ANALYSIS_USER_TEMPLATE = """Analyze the following data and answer the question:

## Data Context
{data_context}

## Schema Information
{schema_context}

## Analysis Request
{query}

## Analysis Type: {analysis_type}

Provide your analysis as a structured JSON response with summary, insights, recommendations, and metrics.
"""
