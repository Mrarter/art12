function getBrowserWindow() {
  if (typeof window === 'undefined') return null
  return window
}

let nativeWechatLoginInFlight = false

export function hasNativeWechatLoginBridge() {
  const win = getBrowserWindow()
  return !!(win?.YibenNative?.wechatLogin || win?.__YIBEN_WECHAT_LOGIN__)
}

export function hasNativeAlipayPayBridge() {
  const win = getBrowserWindow()
  return !!win?.YibenNative?.alipayPay
}

export function requestNativeWechatLogin(payload = {}) {
  const win = getBrowserWindow()
  if (!win?.YibenNative?.wechatLogin) {
    return Promise.reject(new Error('APP 暂未接入原生微信登录'))
  }
  if (nativeWechatLoginInFlight) {
    return Promise.reject(new Error('微信登录正在进行，请稍候'))
  }

  const requestId = `wxlogin_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
  nativeWechatLoginInFlight = true

  return new Promise((resolve, reject) => {
    const cleanup = () => {
      nativeWechatLoginInFlight = false
      win.clearTimeout(timeoutId)
      win.removeEventListener('yiben:wechat-login-result', handleResult)
    }

    const handleResult = (event) => {
      const detail = event?.detail || {}
      if (detail.requestId !== requestId) return
      cleanup()

      if (detail.ok && detail.code) {
        resolve({
          code: detail.code,
          loginScene: 'app'
        })
        return
      }

      reject(new Error(detail.message || '微信登录失败'))
    }

    const timeoutId = win.setTimeout(() => {
      cleanup()
      reject(new Error('微信登录超时，请稍后重试'))
    }, 120000)

    win.addEventListener('yiben:wechat-login-result', handleResult)

    try {
      win.YibenNative.wechatLogin({
        ...payload,
        requestId
      })
    } catch (error) {
      cleanup()
      reject(error instanceof Error ? error : new Error('原生微信登录调用失败'))
    }
  })
}

export function requestNativeAlipayPay(orderInfo) {
  const win = getBrowserWindow()
  if (!win?.YibenNative?.alipayPay) {
    return Promise.reject(new Error('APP 暂未接入原生支付宝支付'))
  }
  if (!orderInfo) {
    return Promise.reject(new Error('支付宝支付参数异常'))
  }

  const requestId = `alipay_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`

  return new Promise((resolve, reject) => {
    const cleanup = () => {
      win.clearTimeout(timeoutId)
      win.removeEventListener('yiben:alipay-pay-result', handleResult)
    }

    const handleResult = (event) => {
      const detail = event?.detail || {}
      if (detail.requestId !== requestId) return
      cleanup()

      if (detail.ok) {
        resolve(detail.result || {})
        return
      }

      reject(new Error(detail.message || '支付宝支付未完成'))
    }

    const timeoutId = win.setTimeout(() => {
      cleanup()
      reject(new Error('支付宝支付超时，请稍后重试'))
    }, 120000)

    win.addEventListener('yiben:alipay-pay-result', handleResult)

    try {
      win.YibenNative.alipayPay({
        requestId,
        orderInfo
      })
    } catch (error) {
      cleanup()
      reject(error instanceof Error ? error : new Error('原生支付宝支付调用失败'))
    }
  })
}
