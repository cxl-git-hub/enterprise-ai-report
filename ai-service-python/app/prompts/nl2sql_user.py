"""NL2SQL user prompt template."""

NL2SQL_USER_TEMPLATE = """Based on the database schema provided, generate a SQL query to answer the following question:

## Question
{query}

## Requirements
- Use only the tables and columns defined in the schema above
- Ensure the query is safe and efficient
- Include appropriate WHERE clauses for filtering
- Add LIMIT clause if the user didn't specify one (default: {max_rows})

Generate the SQL query now.
"""
