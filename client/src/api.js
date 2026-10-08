// Every request goes through /api (proxied to the Spring Boot API in dev and on Vercel)
export async function api(path, { method = 'GET', body } = {}) {
  const res = await fetch('/api' + path, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
    credentials: 'include',
  })
  const json = await res.json().catch(() => null)
  if (!json?.success) {
    const err = new Error(json?.error?.message || 'Something went wrong. Please try again.')
    err.details = json?.error?.details
    throw err
  }
  return json.data
}

// PayHere needs a real browser form POST, not fetch
export function postToPayHere(action) {
  const form = document.createElement('form')
  form.method = 'POST'
  form.action = action.actionUrl
  for (const [name, value] of Object.entries(action.fields)) {
    const input = document.createElement('input')
    input.type = 'hidden'
    input.name = name
    input.value = value
    form.appendChild(input)
  }
  document.body.appendChild(form)
  form.submit()
}
