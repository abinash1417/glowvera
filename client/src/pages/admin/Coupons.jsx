import { useEffect, useState } from 'react'
import { api } from '../../api.js'
import { rs } from '../../format.js'
import { Badge, Card, Errors, btnGhost, btnPrimary, errMessages, inputClass } from './Ui.jsx'

const EMPTY = {
  code: '', description: '', discountType: 'PERCENT', value: '', maxRs: '', minRs: '',
  usageLimit: '', perUserLimit: '1', startsAt: '', expiresAt: '', active: true,
}

// rupees (form) <-> cents (API)
const toCents = (v) => (v === '' ? null : Math.round(Number(v) * 100))
const toRs = (c) => (c == null ? '' : String(c / 100))
// <input type="datetime-local"> works in local time; the API uses ISO instants
const toIso = (v) => (v ? new Date(v).toISOString() : null)
const toLocal = (iso) => {
  if (!iso) return ''
  const d = new Date(iso)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const describe = (c) => (c.discountType === 'PERCENT'
  ? `${c.discountValue}% off${c.maxDiscountCents ? ` (max ${rs(c.maxDiscountCents)})` : ''}`
  : `${rs(c.discountValue)} off`)

function CouponForm({ editing, onSaved, onCancel }) {
  const [f, setF] = useState(editing ? {
    code: editing.code, description: editing.description ?? '', discountType: editing.discountType,
    value: editing.discountType === 'PERCENT' ? String(editing.discountValue) : toRs(editing.discountValue),
    maxRs: toRs(editing.maxDiscountCents), minRs: editing.minOrderCents ? toRs(editing.minOrderCents) : '',
    usageLimit: editing.usageLimit ?? '', perUserLimit: String(editing.perUserLimit),
    startsAt: toLocal(editing.startsAt), expiresAt: toLocal(editing.expiresAt), active: editing.active,
  } : EMPTY)
  const [errors, setErrors] = useState([])
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setF({ ...f, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })
  const percent = f.discountType === 'PERCENT'

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setErrors([])
    const body = {
      code: f.code,
      description: f.description || null,
      discountType: f.discountType,
      discountValue: percent ? Number(f.value) : toCents(f.value),
      maxDiscountCents: percent ? toCents(f.maxRs) : null,
      minOrderCents: toCents(f.minRs) ?? 0,
      usageLimit: f.usageLimit === '' ? null : Number(f.usageLimit),
      perUserLimit: Number(f.perUserLimit) || 1,
      startsAt: toIso(f.startsAt),
      expiresAt: toIso(f.expiresAt),
      active: f.active,
    }
    try {
      await api(editing ? `/admin/coupons/${editing.id}` : '/admin/coupons', { method: editing ? 'PUT' : 'POST', body })
      onSaved()
    } catch (err) {
      setErrors(errMessages(err))
    } finally {
      setBusy(false)
    }
  }

  const field = (title, children, hint) => (
    <label className="block text-sm"><span className="mb-1 block text-muted">{title}</span>{children}{hint && <span className="text-xs text-muted">{hint}</span>}</label>
  )

  return (
    <Card title={editing ? `Edit ${editing.code}` : 'New coupon'}>
      <form onSubmit={submit} className="grid gap-3 sm:grid-cols-2">
        {field('Code', <input required disabled={!!editing} value={f.code} onChange={(e) => setF({ ...f, code: e.target.value.toUpperCase() })} placeholder="WELCOME10" className={inputClass} />)}
        {field('Description (optional)', <input value={f.description} onChange={set('description')} maxLength={150} className={inputClass} />)}
        {field('Type', (
          <select value={f.discountType} onChange={set('discountType')} className={inputClass}>
            <option value="PERCENT">Percentage (%)</option>
            <option value="FIXED">Fixed amount (Rs.)</option>
          </select>
        ))}
        {field(percent ? 'Percent off (1-100)' : 'Amount off (Rs.)', <input required type="number" min="1" step={percent ? '1' : '0.01'} max={percent ? 100 : undefined} value={f.value} onChange={set('value')} className={inputClass} />)}
        {percent && field('Maximum discount (Rs., optional)', <input type="number" min="1" step="0.01" value={f.maxRs} onChange={set('maxRs')} className={inputClass} />)}
        {field('Minimum order (Rs., optional)', <input type="number" min="0" step="0.01" value={f.minRs} onChange={set('minRs')} className={inputClass} />, 'Counted on the subtotal before delivery')}
        {field('Total uses allowed (empty = unlimited)', <input type="number" min="1" value={f.usageLimit} onChange={set('usageLimit')} className={inputClass} />)}
        {field('Uses per customer', <input required type="number" min="1" max="100" value={f.perUserLimit} onChange={set('perUserLimit')} className={inputClass} />)}
        {field('Starts (optional)', <input type="datetime-local" value={f.startsAt} onChange={set('startsAt')} className={inputClass} />)}
        {field('Expires (optional)', <input type="datetime-local" value={f.expiresAt} onChange={set('expiresAt')} className={inputClass} />)}
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={f.active} onChange={set('active')} /> Active</label>
        <div className="sm:col-span-2"><Errors list={errors} /></div>
        <div className="flex gap-2 sm:col-span-2">
          <button disabled={busy} className={btnPrimary}>{busy ? 'Saving...' : 'Save coupon'}</button>
          <button type="button" onClick={onCancel} className={btnGhost}>Cancel</button>
        </div>
      </form>
    </Card>
  )
}

export default function Coupons() {
  const [list, setList] = useState(null)
  const [error, setError] = useState('')
  const [form, setForm] = useState(null) // null = hidden, {} = new, coupon = editing

  const load = () => api('/admin/coupons').then(setList).catch((e) => setError(e.message))
  useEffect(() => { load() }, [])

  const toggle = (c) =>
    api(`/admin/coupons/${c.id}`, {
      method: 'PUT',
      body: {
        code: c.code, description: c.description, discountType: c.discountType, discountValue: c.discountValue,
        maxDiscountCents: c.maxDiscountCents, minOrderCents: c.minOrderCents, usageLimit: c.usageLimit,
        perUserLimit: c.perUserLimit, startsAt: c.startsAt, expiresAt: c.expiresAt, active: !c.active,
      },
    }).then(load).catch((e) => setError(e.message))

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-3">
        <h1 className="font-display text-3xl font-semibold">Coupons</h1>
        {!form && <button onClick={() => setForm({})} className={btnPrimary}>New coupon</button>}
      </div>

      {error && <p className="text-rose">{error}</p>}

      {form && (
        <CouponForm
          key={form.id ?? 'new'}
          editing={form.id ? form : null}
          onSaved={() => { setForm(null); load() }}
          onCancel={() => setForm(null)}
        />
      )}

      <Card>
        {!list && !error && <p className="text-muted">Loading...</p>}
        {list?.length === 0 && <p className="text-sm text-muted">No coupons yet. Create your first one.</p>}
        <ul className="divide-y divide-gold/20">
          {list?.map((c) => (
            <li key={c.id} className="flex flex-wrap items-center justify-between gap-3 py-3 text-sm">
              <div>
                <p className="flex flex-wrap items-center gap-2"><b>{c.code}</b> <Badge value={c.state} /></p>
                <p>{describe(c)}{c.minOrderCents > 0 && <span className="text-muted"> · min order {rs(c.minOrderCents)}</span>}</p>
                <p className="text-xs text-muted">
                  Used {c.usedCount}{c.usageLimit ? ` / ${c.usageLimit}` : ''} · {c.perUserLimit} per customer
                  {c.expiresAt && ` · expires ${new Date(c.expiresAt).toLocaleString()}`}
                </p>
              </div>
              <div className="flex gap-2">
                <button onClick={() => { setForm(c); window.scrollTo({ top: 0, behavior: 'smooth' }) }} className={btnGhost}>Edit</button>
                <button onClick={() => toggle(c)} className={btnGhost}>{c.active ? 'Deactivate' : 'Activate'}</button>
              </div>
            </li>
          ))}
        </ul>
      </Card>
    </div>
  )
}
