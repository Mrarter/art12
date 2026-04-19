/**
 * 社区相关 API
 */
import { get, post } from './request'

/**
 * 获取帖子列表
 */
export function getPosts(params) {
  return get('/community/posts', params)
}

/**
 * 获取帖子详情
 */
export function getPostDetail(postId) {
  return get('/community/posts/' + postId)
}

/**
 * 发帖
 */
export function createPost(data) {
  return post('/community/posts', data)
}

/**
 * 点赞帖子
 */
export function likePost(postId) {
  return post('/community/posts/' + postId + '/like')
}

/**
 * 取消点赞
 */
export function unlikePost(postId) {
  return post('/community/posts/' + postId + '/unlike')
}

/**
 * 评论帖子
 */
export function commentPost(postId, data) {
  return post('/community/posts/' + postId + '/comments', data)
}

/**
 * 获取评论列表
 */
export function getComments(postId) {
  return get('/community/posts/' + postId + '/comments')
}

/**
 * 获取话题列表
 */
export function getTopics() {
  return get('/community/topics')
}
