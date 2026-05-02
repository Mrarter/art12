const request = require('../utils/request');

function getArtistScore(artistId) {
  return request({
    url: `/api/artist/score/${artistId}`,
    method: 'GET'
  });
}

function recalculateArtistScore(artistId) {
  return request({
    url: `/api/artist/score/recalculate/${artistId}`,
    method: 'POST'
  });
}

module.exports = {
  getArtistScore,
  recalculateArtistScore
};
