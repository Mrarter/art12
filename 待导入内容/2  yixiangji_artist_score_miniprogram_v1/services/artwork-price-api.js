const request = require('../utils/request');

function triggerDailyIncrease(artworkId) {
  return request({
    url: `/api/artwork/price/daily/${artworkId}`,
    method: 'POST'
  });
}

function triggerSaleIncrease(artworkId) {
  return request({
    url: `/api/artwork/price/sale/${artworkId}`,
    method: 'POST'
  });
}

function triggerCollectIncrease(artworkId) {
  return request({
    url: `/api/artwork/price/collect/${artworkId}`,
    method: 'POST'
  });
}

module.exports = {
  triggerDailyIncrease,
  triggerSaleIncrease,
  triggerCollectIncrease
};
