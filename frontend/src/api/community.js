/**
 * 社区模块 API（艺术圈）
 */
import request from './request'

/**
 * 获取帖子列表
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码
 * @param {number} params.pageSize - 每页数量
 * @param {number} params.topicId - 话题ID（可选）
 */
export function getPosts(params) {
  return request({
    url: '/community/posts',
    method: 'GET',
    data: params
  })
}

/**
 * 获取帖子详情
 * @param {number} postId - 帖子ID
 */
export function getPostDetail(postId) {
  return request({
    url: `/community/posts/${postId}`,
    method: 'GET'
  })
}

/**
 * 发帖
 * @param {Object} data - 帖子数据
 * @param {string} data.content - 内容
 * @param {string} data.images - 图片（逗号分隔）
 * @param {number} data.topicId - 话题ID（可选）
 * @param {number} data.artworkId - 关联作品ID（可选）
 */
export function createPost(data) {
  return request({
    url: '/community/posts',
    method: 'POST',
    data
  })
}

/**
 * 点赞帖子
 * @param {number} postId - 帖子ID
 */
export function likePost(postId) {
  return request({
    url: `/community/posts/${postId}/like`,
    method: 'POST'
  })
}

/**
 * 取消点赞
 * @param {number} postId - 帖子ID
 */
export function unlikePost(postId) {
  return request({
    url: `/community/posts/${postId}/unlike`,
    method: 'POST'
  })
}

/**
 * 评论帖子
 * @param {number} postId - 帖子ID
 * @param {Object} data - 评论数据
 * @param {string} data.content - 评论内容
 * @param {number} data.parentId - 父评论ID（回复时使用）
 */
export function commentPost(postId, data) {
  return request({
    url: `/community/posts/${postId}/comments`,
    method: 'POST',
    data
  })
}

/**
 * 获取评论列表
 * @param {number} postId - 帖子ID
 * @param {number} page - 页码
 * @param {number} pageSize - 每页数量
 */
export function getComments(postId, page = 1, pageSize = 20) {
  return request({
    url: `/community/posts/${postId}/comments`,
    method: 'GET',
    data: { page, pageSize }
  })
}

/**
 * 获取话题列表
 */
export function getTopics() {
  return request({
    url: '/community/topics',
    method: 'GET'
  })
}

/**
 * 删除帖子
 * @param {number} postId - 帖子ID
 */
export function deletePost(postId) {
  return request({
    url: `/community/posts/${postId}`,
    method: 'DELETE'
  })
}
