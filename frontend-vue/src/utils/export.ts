/**
 * Data export utilities for CSV and Excel formats
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
          // Escape quotes and wrap in quotes if contains comma/newline/quote
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
  format: 'csv' | 'json' | 'md' = 'csv'
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
  }
}
