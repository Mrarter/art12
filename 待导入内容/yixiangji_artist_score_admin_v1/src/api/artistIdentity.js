import request from './request'

export function getIdentityAuditList(params) {
  return request({
    url: '/api/admin/artist/identity/audit-list',
    method: 'get',
    params
  })
}

export function getIdentityDetail(artistId) {
  return request({
    url: `/api/admin/artist/identity/${artistId}`,
    method: 'get'
  })
}

export function auditArtistIdentity(data) {
  return request({
    url: '/api/admin/artist/identity/audit',
    method: 'post',
    data
  })
}
