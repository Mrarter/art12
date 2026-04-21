/**
 * WebSocket 工具类
 * 用于拍卖实时出价推送
 */

class WebSocketManager {
  constructor() {
    this.socketTask = null
    this.isConnected = false
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectDelay = 3000
    this.heartbeatTimer = null
    this.listeners = new Map()
    this.url = ''
  }

  /**
   * 连接 WebSocket
   * @param {string} url - WebSocket 地址
   */
  connect(url) {
    if (this.socketTask) {
      this.close()
    }

    this.url = url
    
    return new Promise((resolve, reject) => {
      try {
        this.socketTask = uni.connectSocket({
          url,
          success: () => {
            console.log('WebSocket 连接中...')
          },
          fail: (err) => {
            console.error('WebSocket 连接失败', err)
            reject(err)
          }
        })

        // 监听连接打开
        this.socketTask.onOpen(() => {
          console.log('WebSocket 连接成功')
          this.isConnected = true
          this.reconnectAttempts = 0
          this.startHeartbeat()
          this.emit('open', null)
          resolve()
        })

        // 监听消息
        this.socketTask.onMessage((res) => {
          try {
            const data = JSON.parse(res.data)
            console.log('WebSocket 收到消息:', data)
            this.emit('message', data)
            
            // 根据消息类型分发
            this.emit(data.type, data.data)
          } catch (e) {
            console.error('消息解析失败', e)
          }
        })

        // 监听连接关闭
        this.socketTask.onClose(() => {
          console.log('WebSocket 连接关闭')
          this.isConnected = false
          this.stopHeartbeat()
          this.emit('close', null)
          this.attemptReconnect()
        })

        // 监听错误
        this.socketTask.onError((err) => {
          console.error('WebSocket 错误', err)
          this.emit('error', err)
        })

      } catch (e) {
        console.error('WebSocket 创建失败', e)
        reject(e)
      }
    })
  }

  /**
   * 发送消息
   * @param {Object} data - 消息数据
   */
  send(data) {
    if (this.socketTask && this.isConnected) {
      this.socketTask.send({
        data: JSON.stringify(data),
        success: () => {
          console.log('WebSocket 消息发送成功')
        },
        fail: (err) => {
          console.error('WebSocket 消息发送失败', err)
        }
      })
    } else {
      console.warn('WebSocket 未连接，无法发送消息')
    }
  }

  /**
   * 关闭连接
   */
  close() {
    this.stopHeartbeat()
    if (this.socketTask) {
      this.socketTask.close({
        success: () => {
          console.log('WebSocket 主动关闭')
        }
      })
      this.socketTask = null
    }
    this.isConnected = false
    this.reconnectAttempts = this.maxReconnectAttempts // 阻止重连
  }

  /**
   * 尝试重连
   */
  attemptReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.log('WebSocket 重连次数已达上限')
      this.emit('reconnect_failed', null)
      return
    }

    this.reconnectAttempts++
    console.log(`WebSocket 准备第 ${this.reconnectAttempts} 次重连...`)

    setTimeout(() => {
      if (this.url) {
        this.connect(this.url).catch(() => {
          // 重连失败，会在 onClose 中再次尝试
        })
      }
    }, this.reconnectDelay * this.reconnectAttempts)
  }

  /**
   * 开始心跳
   */
  startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatTimer = setInterval(() => {
      if (this.isConnected) {
        this.send({ type: 'ping' })
      }
    }, 30000) // 30秒心跳
  }

  /**
   * 停止心跳
   */
  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  /**
   * 添加事件监听
   * @param {string} event - 事件名
   * @param {Function} callback - 回调函数
   */
  on(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, [])
    }
    this.listeners.get(event).push(callback)
  }

  /**
   * 移除事件监听
   * @param {string} event - 事件名
   * @param {Function} callback - 回调函数
   */
  off(event, callback) {
    if (this.listeners.has(event)) {
      const callbacks = this.listeners.get(event)
      const index = callbacks.indexOf(callback)
      if (index > -1) {
        callbacks.splice(index, 1)
      }
    }
  }

  /**
   * 触发事件
   * @param {string} event - 事件名
   * @param {*} data - 事件数据
   */
  emit(event, data) {
    if (this.listeners.has(event)) {
      this.listeners.get(event).forEach(callback => {
        try {
          callback(data)
        } catch (e) {
          console.error(`事件 ${event} 回调执行出错`, e)
        }
      })
    }
  }
}

// 导出单例
export const wsManager = new WebSocketManager()

// 拍卖相关 WebSocket 工具函数
export default {
  /**
   * 连接拍卖实时推送
   * @param {number} lotId - 拍品ID
   */
  connectAuction(lotId) {
    const baseUrl = getApp().globalData.wsBaseUrl || 'wss://api.shiyiju.com'
    const wsUrl = `${baseUrl}/ws/auction/lot/${lotId}`
    return wsManager.connect(wsUrl)
  },

  /**
   * 断开拍卖连接
   */
  disconnectAuction() {
    wsManager.close()
  },

  /**
   * 监听出价
   * @param {Function} callback - 回调函数
   */
  onBid(callback) {
    wsManager.on('bid', callback)
  },

  /**
   * 监听出价取消
   * @param {Function} callback - 回调函数
   */
  onBidCancel(callback) {
    wsManager.on('bid_cancel', callback)
  },

  /**
   * 监听拍卖开始
   * @param {Function} callback - 回调函数
   */
  onAuctionStart(callback) {
    wsManager.on('auction_start', callback)
  },

  /**
   * 监听拍卖结束
   * @param {Function} callback - 回调函数
   */
  onAuctionEnd(callback) {
    wsManager.on('auction_end', callback)
  },

  /**
   * 监听新出价（用于弹幕）
   * @param {Function} callback - 回调函数
   */
  onNewBid(callback) {
    wsManager.on('new_bid', callback)
  },

  /**
   * 移除所有监听
   */
  offAll() {
    this.listeners.clear()
  }
}
