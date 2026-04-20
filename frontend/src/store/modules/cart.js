/**
 * 购物车状态管理
 * 严格按开发文档3.6节设计
 */
import { defineStore } from 'pinia'
import { getCartList, addToCart, updateCartQuantity, deleteCartItems } from '@/api/cart'

export const useCartStore = defineStore('cart', {
  state: () => ({
    cartList: [], // 购物车列表
    selectedIds: [], // 选中的购物车项ID
    totalAmount: 0, // 选中总价
    totalCount: 0 // 选中数量
  }),
  
  getters: {
    // 按发布者分组
    groupedCartList: (state) => {
      const groups = {}
      state.cartList.forEach(item => {
        const sellerId = item.sellerId || item.authorId || 'default'
        if (!groups[sellerId]) {
          groups[sellerId] = {
            sellerId,
            sellerName: item.authorName || '艺术家',
            sellerAvatar: item.authorAvatar || '',
            items: []
          }
        }
        groups[sellerId].items.push(item)
      })
      return Object.values(groups)
    },
    
    // 是否有选中项
    hasSelected: (state) => state.selectedIds.length > 0,
    
    // 购物车总数
    cartCount: (state) => state.cartList.reduce((sum, item) => sum + item.quantity, 0)
  },
  
  actions: {
    // 从本地存储初始化
    initFromStorage() {
      try {
        const savedCart = uni.getStorageSync('cartList')
        if (savedCart) {
          this.cartList = JSON.parse(savedCart)
          this.recalculate()
        }
      } catch (e) {
        console.error('初始化购物车失败', e)
      }
    },

    // 保存到本地存储
    saveToStorage() {
      try {
        uni.setStorageSync('cartList', JSON.stringify(this.cartList))
      } catch (e) {
        console.error('保存购物车失败', e)
      }
    },

    // 获取购物车列表
    async fetchCartList() {
      try {
        const list = await getCartList()
        this.cartList = list || []
        this.recalculate()
        return list
      } catch (e) {
        this.cartList = []
        return []
      }
    },
    
    // 添加到购物车
    async addToCart(artworkId, quantity = 1) {
      try {
        await addToCart(artworkId, quantity)
        await this.fetchCartList()
        uni.showToast({ title: '已加入购物车', icon: 'success' })
        return true
      } catch (e) {
        return false
      }
    },
    
    // 更新数量
    async updateQuantity(id, quantity) {
      try {
        await updateCartQuantity(id, quantity)
        const item = this.cartList.find(i => i.id === id)
        if (item) {
          item.quantity = quantity
        }
        this.recalculate()
        return true
      } catch (e) {
        return false
      }
    },
    
    // 删除购物车项
    async removeItems(ids) {
      try {
        await deleteCartItems(ids)
        this.cartList = this.cartList.filter(item => !ids.includes(item.id))
        this.selectedIds = this.selectedIds.filter(id => !ids.includes(id))
        this.recalculate()
        return true
      } catch (e) {
        return false
      }
    },
    
    // 切换选中状态
    toggleSelect(id) {
      const index = this.selectedIds.indexOf(id)
      if (index > -1) {
        this.selectedIds.splice(index, 1)
      } else {
        this.selectedIds.push(id)
      }
      this.recalculate()
    },
    
    // 全选/取消全选
    toggleSelectAll() {
      if (this.selectedIds.length === this.cartList.length) {
        this.selectedIds = []
      } else {
        this.selectedIds = this.cartList.map(item => item.id)
      }
      this.recalculate()
    },
    
    // 选中指定项
    selectItems(ids) {
      this.selectedIds = [...new Set([...this.selectedIds, ...ids])]
      this.recalculate()
    },
    
    // 清除选择
    clearSelection() {
      this.selectedIds = []
      this.totalAmount = 0
      this.totalCount = 0
    },
    
    // 重新计算总价和数量
    recalculate() {
      const selectedItems = this.cartList.filter(item => this.selectedIds.includes(item.id))
      this.totalCount = selectedItems.reduce((sum, item) => sum + item.quantity, 0)
      this.totalAmount = selectedItems.reduce((sum, item) => sum + item.price * item.quantity, 0)
      this.saveToStorage()
    },

    // 从购物车移除
    removeFromCart(id) {
      this.cartList = this.cartList.filter(item => item.id !== id)
      this.selectedIds = this.selectedIds.filter(sid => sid !== id)
      this.recalculate()
    },

    // 取消全选
    unselectAll() {
      this.selectedIds = []
      this.totalAmount = 0
      this.totalCount = 0
    },

    // 全选
    selectAll() {
      this.selectedIds = this.cartList.map(item => item.id)
      this.recalculate()
    }
  }
})
