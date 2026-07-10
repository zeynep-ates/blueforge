import { API_BASE_URL, API_KEY } from '@/api/mutator'

// Plain <a href download> links can't carry the X-API-Key header, so once auth is enabled they'd
// 401 silently. Fetching with the header and triggering the download from the resulting blob keeps
// export authenticated while still producing a real browser download.
export async function downloadFile(path: string): Promise<void> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: API_KEY ? { 'X-API-Key': API_KEY } : {},
  })

  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}`)
  }

  const disposition = response.headers.get('Content-Disposition')
  const filename = disposition?.match(/filename="([^"]+)"/)?.[1] ?? 'export'

  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}
