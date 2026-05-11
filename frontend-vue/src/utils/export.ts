/**
 * Data export utilities for CSV, JSON, Markdown, Excel, and SQL formats
 */

export function exportToCsv(filename: string, columns: string[], rows: Record<string, unknown>[]) {
  const BOM = '\uFEFF' // UTF-8 BOM for Chinese support
  const header = columns.join(',')
  const body = rows
    .map((row) =>
      columns
        .map((col) => {
          const val = row[col]
          if (val === null || val === undefined) return ''
          const str = String(val)
          if (str.includes(',') || str.includes('\n') || str.includes('"')) {
            return `"${str.replace(/"/g, '""')}"`
          }
          return str
        })
        .join(',')
    )
    .join('\n')

  const csv = BOM + header + '\n' + body
  downloadFile(filename + '.csv', csv, 'text/csv;charset=utf-8')
}

export function exportToJson(filename: string, data: unknown) {
  const json = JSON.stringify(data, null, 2)
  downloadFile(filename + '.json', json, 'application/json')
}

export function exportToMarkdown(filename: string, columns: string[], rows: Record<string, unknown>[]) {
  const header = '| ' + columns.join(' | ') + ' |'
  const separator = '| ' + columns.map(() => '---').join(' | ') + ' |'
  const body = rows
    .map((row) => '| ' + columns.map((col) => String(row[col] ?? '')).join(' | ') + ' |')
    .join('\n')

  const md = header + '\n' + separator + '\n' + body
  downloadFile(filename + '.md', md, 'text/markdown')
}

/**
 * Export SQL query to .sql file
 */
export function exportToSql(filename: string, sql: string, disclaimer?: string) {
  let content = ''
  if (disclaimer) {
    content += `-- ${disclaimer}\n-- Generated at: ${new Date().toISOString()}\n\n`
  }
  content += sql
  downloadFile(filename + '.sql', content, 'application/sql')
}

/**
 * Export data to Excel-compatible HTML table (opens in Excel)
 */
export function exportToExcel(filename: string, columns: string[], rows: Record<string, unknown>[], sheetName?: string) {
  const escapeHtml = (s: string) =>
    s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

  let html = `<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">
<head><meta charset="utf-8"><!--[if gte mso 9]><xml><x:ExcelWorkbook><x:ExcelWorksheets><x:ExcelWorksheet>
<x:Name>${escapeHtml(sheetName || 'Sheet1')}</x:Name><x:WorksheetOptions><x:DisplayGridlines/></x:WorksheetOptions>
</x:ExcelWorksheet></x:ExcelWorksheets></x:ExcelWorkbook></xml><![endif]--></head><body><table border="1">`

  // Header
  html += '<tr>' + columns.map((c) => `<th style="background:#4472C4;color:#fff;font-weight:bold;padding:6px">${escapeHtml(c)}</th>`).join('') + '</tr>'

  // Rows
  for (const row of rows) {
    html += '<tr>' + columns.map((c) => {
      const val = row[c]
      const str = val === null || val === undefined ? '' : String(val)
      // Check if it's a number
      const isNum = typeof val === 'number' || (typeof val === 'string' && !isNaN(Number(val)) && str.trim() !== '')
      const style = isNum ? 'style="mso-number-format:\\@;text-align:right"' : ''
      return `<td ${style}>${escapeHtml(str)}</td>`
    }).join('') + '</tr>'
  }

  html += '</table></body></html>'
  downloadFile(filename + '.xls', html, 'application/vnd.ms-excel;charset=utf-8')
}

/**
 * Export with AI disclaimer footer
 */
export function exportWithDisclaimer(
  filename: string,
  columns: string[],
  rows: Record<string, unknown>[],
  format: 'csv' | 'json' | 'md' | 'excel',
  disclaimer: string
) {
  if (format === 'excel') {
    // Add disclaimer as last row
    const disclaimerRow: Record<string, unknown> = {}
    disclaimerRow[columns[0]] = disclaimer
    for (let i = 1; i < columns.length; i++) disclaimerRow[columns[i]] = ''
    exportToExcel(filename, columns, [...rows, disclaimerRow])
  } else if (format === 'csv') {
    // Append disclaimer after data
    const BOM = '\uFEFF'
    const header = columns.join(',')
    const body = rows.map((row) =>
      columns.map((col) => {
        const val = row[col]
        if (val === null || val === undefined) return ''
        const str = String(val)
        if (str.includes(',') || str.includes('\n') || str.includes('"')) return `"${str.replace(/"/g, '""')}"`
        return str
      }).join(',')
    ).join('\n')
    const csv = BOM + header + '\n' + body + '\n\n-- ' + disclaimer
    downloadFile(filename + '.csv', csv, 'text/csv;charset=utf-8')
  } else {
    exportTableData(filename, columns, rows, format)
  }
}

function downloadFile(filename: string, content: string, mimeType: string) {
  const blob = new Blob([content], { type: mimeType })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

/**
 * Export table data with format detection
 */
export function exportTableData(
  filename: string,
  columns: string[],
  rows: Record<string, unknown>[],
  format: 'csv' | 'json' | 'md' | 'excel' = 'csv'
) {
  switch (format) {
    case 'csv':
      exportToCsv(filename, columns, rows)
      break
    case 'json':
      exportToJson(filename, rows)
      break
    case 'md':
      exportToMarkdown(filename, columns, rows)
      break
    case 'excel':
      exportToExcel(filename, columns, rows)
      break
  }
}
