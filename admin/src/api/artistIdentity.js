import request from './request'

export function getIdentityAuditList(params) {
  return request({
    url: '/artist/identity/audit-list',
    method: 'get',
    params
  })
}

export function getIdentityDetail(artistId) {
  return request.silentGet(`/artist/identity/${artistId}`)
}

export function auditArtistIdentity(data) {
  return request({
    url: '/artist/identity/audit',
    method: 'post',
    data
  })
}

export function saveArtistIdentity(data) {
  return request({
    url: '/artist/identity/save',
    method: 'post',
    data
  })
}
