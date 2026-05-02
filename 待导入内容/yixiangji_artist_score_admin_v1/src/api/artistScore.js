import request from './request'

export function getArtistScoreList(params) {
  return request({
    url: '/api/admin/artist/scores',
    method: 'get',
    params
  })
}

export function getArtistScoreDetail(artistId) {
  return request({
    url: `/api/artist/score/${artistId}`,
    method: 'get'
  })
}

export function recalculateArtistScore(artistId) {
  return request({
    url: `/api/artist/score/recalculate/${artistId}`,
    method: 'post'
  })
}

export function manualAdjustArtistScore(data) {
  return request({
    url: '/api/admin/artist/score/manual-adjust',
    method: 'post',
    data
  })
}
