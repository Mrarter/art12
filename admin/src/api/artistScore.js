import request from './request'
import { requestApi } from './request'

export function getArtistScoreList(params) {
  return request({
    url: '/artist/score/list',
    method: 'get',
    params
  })
}

export function getArtistScoreDetail(artistId) {
  return requestApi({
    url: '/artist/score/' + artistId,
    method: 'get'
  })
}

export function recalculateArtistScore(artistId) {
  return request({
    url: `/artist/score/recalculate/${artistId}`,
    method: 'post'
  })
}

export function manualAdjustArtistScore(data) {
  return request({
    url: '/artist/score/manual-adjust',
    method: 'post',
    data
  })
}
