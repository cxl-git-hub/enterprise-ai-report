package com.enterprise.report.engine.kpi;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.report.entity.Dataset;
import com.enterprise.report.entity.DataSource;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.mapper.DatasetMapper;
import com.enterprise.report.mapper.DataSourceMapper;
import com.enterprise.report.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class KpiDslEvaluator {

    private final DatasetMapper datasetMapper;
    private final DataSourceMapper dataSourceMapper;

    private static final Pattern FUNC_PATTERN = Pattern.compile(
            "(COUNT|SUM|AVG|MIN|MAX)\\s*\\(\\s*(\\w+)\\s*(?:WHERE\\s+(.+?))?\\s*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARITH_PATTERN = Pattern.compile(
            "([\\d.]+)\\s*([+\\-*/])\\s*([\\d.]+)");
    private static final Pattern PARAM_PATTERN = Pattern.compile(":([\\w]+)");

    public BigDecimal evaluate(String expression, Long datasetId, Map<String, Object> params) {
        expression = expression.trim();

        expression = replaceParams(expression, params);

        if (containsArithmetic(expression)) {
            return evaluateArithmeticExpression(expression, datasetId, params);
        }

        Matcher matcher = FUNC_PATTERN.matcher(expression);
        if (matcher.find()) {
            String func = matcher.group(1).toUpperCase();
            String column = matcher.group(2);
            String where = matcher.group(3);

            Long targetDatasetId = resolveDatasetId(column, datasetId);
            // If column is a dataset name, use *; otherwise use the column name
            String aggColumn = "*";
            try {
                Long.parseLong(column);
                // It's a dataset ID, use *
            } catch (NumberFormatException e) {
                // Check if it's a column name (not a dataset name)
                Dataset ds = datasetMapper.selectOne(
                        new LambdaQueryWrapper<Dataset>().eq(Dataset::getName, column));
                if (ds == null) {
                    // It's a column name
                    aggColumn = column;
                }
            }
            return executeAggregation(func, targetDatasetId, aggColumn, where, params);
        }

        try {
            return new BigDecimal(expression);
        } catch (NumberFormatException e) {
            throw new BusinessException(400, "Invalid KPI expression: " + expression);
        }
    }

    private boolean containsArithmetic(String expression) {
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '+' || c == '-' || c == '*' || c == '/') {
                if (i > 0 && i < expression.length() - 1) {
                    return true;
                }
            }
        }
        return false;
    }

    private BigDecimal evaluateArithmeticExpression(String expression, Long datasetId, Map<String, Object> params) {
        String[] parts = expression.split("(?<=[+\\-*/])|(?=[+\\-*/])");
        if (parts.length < 3) {
            throw new BusinessException(400, "Invalid arithmetic expression: " + expression);
        }

        BigDecimal result = evaluatePart(parts[0].trim(), datasetId, params);
        for (int i = 1; i < parts.length; i += 2) {
            String operator = parts[i].trim();
            BigDecimal operand = evaluatePart(parts[i + 1].trim(), datasetId, params);
            result = applyOperator(result, operator, operand);
        }
        return result;
    }

    private BigDecimal evaluatePart(String part, Long datasetId, Map<String, Object> params) {
        part = part.trim();
        Matcher matcher = FUNC_PATTERN.matcher(part);
        if (matcher.find()) {
            String func = matcher.group(1).toUpperCase();
            String column = matcher.group(2);
            String where = matcher.group(3);
            Long targetDatasetId = resolveDatasetId(column, datasetId);
            String aggColumn = "*";
            try {
                Long.parseLong(column);
            } catch (NumberFormatException e) {
                Dataset ds = datasetMapper.selectOne(
                        new LambdaQueryWrapper<Dataset>().eq(Dataset::getName, column));
                if (ds == null) {
                    aggColumn = column;
                }
            }
            return executeAggregation(func, targetDatasetId, aggColumn, where, params);
        }
        try {
            return new BigDecimal(part);
        } catch (NumberFormatException e) {
            throw new BusinessException(400, "Invalid expression part: " + part);
        }
    }

    private BigDecimal applyOperator(BigDecimal left, String operator, BigDecimal right) {
        return switch (operator) {
            case "+" -> left.add(right);
            case "-" -> left.subtract(right);
            case "*" -> left.multiply(right);
            case "/" -> {
                if (right.compareTo(BigDecimal.ZERO) == 0) {
                    throw new BusinessException(400, "Division by zero");
                }
                yield left.divide(right, 10, RoundingMode.HALF_UP);
            }
            default -> throw new BusinessException(400, "Unknown operator: " + operator);
        };
    }

    private Long resolveDatasetId(String dataset, Long defaultDatasetId) {
        try {
            return Long.parseLong(dataset);
        } catch (NumberFormatException e) {
            Dataset ds = datasetMapper.selectOne(
                    new LambdaQueryWrapper<Dataset>().eq(Dataset::getName, dataset));
            if (ds == null) {
                throw new BusinessException(404, "Dataset not found: " + dataset);
            }
            return ds.getId();
        }
    }

    private BigDecimal executeAggregation(String func, Long datasetId, String aggColumn, String where, Map<String, Object> params) {
        Dataset dataset = datasetMapper.selectById(datasetId);
        if (dataset == null) {
            throw new BusinessException(404, "Dataset not found: " + datasetId);
        }

        DataSource dataSource = dataSourceMapper.selectById(dataset.getDataSourceId());
        if (dataSource == null) {
            throw new BusinessException(404, "DataSource not found");
        }

        String tableName = dataset.getTableName();
        // Validate table name to prevent injection
        if (!tableName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new BusinessException(400, "Invalid table name: " + tableName);
        }
        // Validate function name
        if (!func.matches("^(COUNT|SUM|AVG|MIN|MAX)$")) {
            throw new BusinessException(400, "Invalid aggregation function: " + func);
        }
        // Validate column name to prevent injection
        if (!aggColumn.equals("*") && !aggColumn.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new BusinessException(400, "Invalid column name: " + aggColumn);
        }
        String whereClause = where != null ? " WHERE " + replaceParams(where, params) : "";
        String sql = "SELECT " + func + "(" + aggColumn + ") FROM " + tableName + whereClause;

        log.debug("Executing KPI SQL: {}", sql);

        try {
            String password = EncryptionUtil.decrypt(dataSource.getEncryptedPassword());
            try (Connection conn = DriverManager.getConnection(dataSource.getConnectionUrl(), dataSource.getUsername(), password);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
                return BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new BusinessException(500, "KPI execution failed: " + e.getMessage());
        }
    }

    private String replaceParams(String expression, Map<String, Object> params) {
        if (params == null) return expression;
        Matcher matcher = PARAM_PATTERN.matcher(expression);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String paramName = matcher.group(1);
            Object value = params.get(paramName);
            if (value != null) {
                // Sanitize: escape single quotes to prevent SQL injection
                String strValue = value.toString().replace("'", "''");
                String replacement = value instanceof String ? "'" + strValue + "'" : value.toString();
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
