let styleMounted = false

const mountStyle = () => {
  if (styleMounted) return
  const style = document.createElement('style')
  style.id = 'app-dialog-style'
  style.textContent = `
  .app-dialog-mask{position:fixed;inset:0;background:rgba(0,0,0,.42);display:flex;align-items:center;justify-content:center;z-index:5000}
  .app-dialog{width:min(88vw,360px);background:#fff;border-radius:14px;box-shadow:0 12px 26px rgba(0,0,0,.2);padding:14px}
  .app-dialog-title{margin:0 0 8px;font-size:16px;color:#1f1f1f;font-weight:700}
  .app-dialog-message{margin:0 0 10px;color:#666;font-size:14px;line-height:1.5;white-space:pre-wrap}
  .app-dialog-input{width:100%;box-sizing:border-box;border:1px solid #e8e8e8;border-radius:10px;padding:10px;font-size:14px;outline:none}
  .app-dialog-actions{margin-top:12px;display:flex;gap:10px}
  .app-dialog-btn{flex:1;height:36px;border:none;border-radius:18px;font-size:14px;cursor:pointer}
  .app-dialog-btn.cancel{background:#f3f3f3;color:#666}
  .app-dialog-btn.ok{background:#1677ff;color:#fff}
  .app-toast{position:fixed;left:50%;bottom:90px;transform:translateX(-50%);z-index:5100;background:rgba(0,0,0,.82);color:#fff;padding:8px 14px;border-radius:18px;font-size:13px;max-width:78vw;word-break:break-word}
  `
  document.head.appendChild(style)
  styleMounted = true
}

const mountHost = () => {
  let host = document.getElementById('app-dialog-host')
  if (!host) {
    host = document.createElement('div')
    host.id = 'app-dialog-host'
    document.body.appendChild(host)
  }
  return host
}

const createButton = (label: string, className: string, onClick: () => void) => {
  const btn = document.createElement('button')
  btn.className = `app-dialog-btn ${className}`
  btn.textContent = label
  btn.onclick = onClick
  return btn
}

export const showAlert = (message: string, duration = 1800): Promise<void> => {
  mountStyle()
  const host = mountHost()
  return new Promise((resolve) => {
    const toast = document.createElement('div')
    toast.className = 'app-toast'
    toast.textContent = String(message || '')
    host.appendChild(toast)
    window.setTimeout(() => {
      toast.remove()
      resolve()
    }, duration)
  })
}

export const showConfirm = (message: string, title = '提示'): Promise<boolean> => {
  mountStyle()
  const host = mountHost()
  return new Promise((resolve) => {
    const mask = document.createElement('div')
    mask.className = 'app-dialog-mask'

    const box = document.createElement('div')
    box.className = 'app-dialog'

    const h3 = document.createElement('h3')
    h3.className = 'app-dialog-title'
    h3.textContent = title

    const p = document.createElement('p')
    p.className = 'app-dialog-message'
    p.textContent = String(message || '')

    const actions = document.createElement('div')
    actions.className = 'app-dialog-actions'

    const done = (ok: boolean) => {
      mask.remove()
      resolve(ok)
    }

    actions.appendChild(createButton('取消', 'cancel', () => done(false)))
    actions.appendChild(createButton('确定', 'ok', () => done(true)))

    box.appendChild(h3)
    box.appendChild(p)
    box.appendChild(actions)
    mask.appendChild(box)
    host.appendChild(mask)
  })
}

export const showPrompt = (
  message: string,
  defaultValue = '',
  title = '请输入'
): Promise<string | null> => {
  mountStyle()
  const host = mountHost()
  return new Promise((resolve) => {
    const mask = document.createElement('div')
    mask.className = 'app-dialog-mask'

    const box = document.createElement('div')
    box.className = 'app-dialog'

    const h3 = document.createElement('h3')
    h3.className = 'app-dialog-title'
    h3.textContent = title

    const p = document.createElement('p')
    p.className = 'app-dialog-message'
    p.textContent = String(message || '')

    const input = document.createElement('input')
    input.className = 'app-dialog-input'
    input.value = defaultValue || ''

    const actions = document.createElement('div')
    actions.className = 'app-dialog-actions'

    const done = (value: string | null) => {
      mask.remove()
      resolve(value)
    }

    actions.appendChild(createButton('取消', 'cancel', () => done(null)))
    actions.appendChild(createButton('确定', 'ok', () => done(input.value)))

    box.appendChild(h3)
    box.appendChild(p)
    box.appendChild(input)
    box.appendChild(actions)
    mask.appendChild(box)
    host.appendChild(mask)

    window.setTimeout(() => input.focus(), 10)
  })
}

export const installDialogPolyfill = () => {
  window.alert = (message?: any) => {
    void showAlert(String(message || ''))
  }
}
