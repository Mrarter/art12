Page({
  data: {
    price: 8040,
    displayPrice: 0,
    isFavorited: false
  },

  onLoad() {
    this.animatePrice()
  },

  onShareAppMessage() {
    return {
      title: '静物0751 - 孟儒',
      path: '/pages/artwork/detail/index'
    }
  },

  // 价格递带动画
  animatePrice() {
    let start = 0
    const end = this.data.price
    const duration = 600
    const interval = 30
    const steps = duration / interval
    const step = end / steps

    const timer = setInterval(() => {
      start += step
      if (start >= end) {
        start = end
        clearInterval(timer)
      }
      this.setData({
        displayPrice: Math.floor(start)
      })
    }, interval)
  },

  // 收藏/取消收藏
  toggleFavorite() {
    this.setData({
      isFavorited: !this.data.isFavorited
    })
    wx.showToast({
      title: this.data.isFavorited ? '已收藏' : '已取消收藏',
      icon: this.data.isFavorited ? 'success' : 'none',
      duration: 1500
    })
  },

  // 返回
  goBack() {
    wx.navigateBack({ delta: 1 })
  },

  // 分享
  handleShare() {
    wx.showShareMenu({
      withShareTicket: true,
      menus: ['shareAppMessage', 'shareTimeline']
    })
  },

  // 咨询
  handleConsult() {
    wx.showToast({
      title: '咨询功能开发中',
      icon: 'none',
      duration: 2000
    })
  },

  // 立即收藏（购买）
  handleBuy() {
    wx.showModal({
      title: '确认收藏',
      content: `确定以 ¥${this.data.displayPrice} 收藏此作品？`,
      confirmColor: '#D4AF37',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '处理中...' })
          setTimeout(() => {
            wx.hideLoading()
            wx.showToast({ title: '收藏成功', icon: 'success' })
          }, 1500)
        }
      }
    })
  }
})
