import request from './request'

export const getMessageList = (params = {}) => {
  return request({
    url: '/order/message/list',
    data: params
  })
}

export const markMessageRead = (messageId) => {
  return request({
    url: `/order/message/read/${messageId}`,
    method: 'PUT'
  })
}

export const getChatConversations = (params = {}) => {
  return request({
    url: '/message/chat/conversations',
    data: params
  })
}

export const getChatHistory = (peerId, params = {}) => {
  return request({
    url: `/message/chat/history/${peerId}`,
    data: params
  })
}

export const sendChatMessage = (data) => {
  return request({
    url: '/message/chat/send',
    method: 'POST',
    data
  })
}

export const markChatRead = (peerId) => {
  return request({
    url: `/message/chat/read/${peerId}`,
    method: 'PUT'
  })
}

export const getChatUnreadCount = () => {
  return request({
    url: '/message/chat/unread-count'
  })
}
