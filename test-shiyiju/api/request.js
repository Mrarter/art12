"use strict";

const vendor = require("../common/vendor.js");

const API_ORIGIN = "http://192.168.1.109:8080";
const FILE_ORIGIN = "http://192.168.1.109:8087";
const BASE_URL = API_ORIGIN + "/api";
const LOCAL_FILE_ORIGIN = "http://localhost:8087";
const TIMEOUT = 30000;

const buildQueryString = (data) => {
  if (!data) return "";
  const params = [];
  for (const key in data) {
    if (data[key] !== void 0 && data[key] !== null && data[key] !== "") {
      params.push(`${encodeURIComponent(key)}=${encodeURIComponent(data[key])}`);
    }
  }
  return params.length > 0 ? "?" + params.join("&") : "";
};

const normalizeResourceUrls = (value) => {
  if (typeof value === "string") {
    if (value.startsWith(LOCAL_FILE_ORIGIN)) {
      return FILE_ORIGIN + value.slice(LOCAL_FILE_ORIGIN.length);
    }
    if (value.startsWith("http://127.0.0.1:8087")) {
      return FILE_ORIGIN + value.slice("http://127.0.0.1:8087".length);
    }
    if (value.startsWith("http://192.168.")) {
      return value.replace(/^http:\/\/192\.168\.\d+\.\d+:(8080|8087)/, (_, port) => port === "8087" ? FILE_ORIGIN : API_ORIGIN);
    }
    return value;
  }

  if (Array.isArray(value)) {
    return value.map(normalizeResourceUrls);
  }

  if (value && typeof value === "object") {
    const normalized = {};
    for (const key in value) {
      normalized[key] = normalizeResourceUrls(value[key]);
    }
    return normalized;
  }

  return value;
};

const request = (options) => {
  return new Promise((resolve, reject) => {
    let url = BASE_URL + options.url;
    if (options.method !== "POST" && options.method !== "PUT" && options.method !== "DELETE" && options.data) {
      const queryString = buildQueryString(options.data);
      if (queryString) {
        url += queryString;
      }
    }

    vendor.index.request({
      url,
      method: options.method || "GET",
      data: options.method === "POST" || options.method === "PUT" ? options.data : void 0,
      header: {
        "Content-Type": "application/json",
        Authorization: vendor.index.getStorageSync("token") ? "Bearer " + vendor.index.getStorageSync("token") : "",
        "X-User-Id": (vendor.index.getStorageSync("userInfo") || {}).id || ""
      },
      timeout: TIMEOUT,
      success: (res) => {
        if (res.statusCode === 200) {
          if (res.data && res.data.code === 200) {
            resolve(normalizeResourceUrls(res.data.data));
          } else if (res.data && res.data.code === 401) {
            vendor.index.showToast({ title: "请先登录", icon: "none" });
            vendor.index.navigateTo({ url: "/pages/login/index" });
            reject(new Error("未授权"));
          } else {
            console.warn("API 返回错误:", res.data);
            reject(new Error(res.data && res.data.message || "API错误"));
          }
        } else {
          console.warn("HTTP 错误:", res.statusCode);
          reject(new Error("HTTP错误: " + res.statusCode));
        }
      },
      fail: (err) => {
        console.warn("请求失败:", { url, err });
        reject(new Error("网络请求失败"));
      }
    });
  });
};

request.get = function(url, data) {
  return request({ url, method: "GET", data });
};

request.post = function(url, data) {
  return request({ url, method: "POST", data });
};

request.put = function(url, data) {
  return request({ url, method: "PUT", data });
};

request.delete = function(url, data) {
  return request({ url, method: "DELETE", data });
};

exports.request = request;
