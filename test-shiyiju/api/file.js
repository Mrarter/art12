"use strict";

const vendor = require("../common/vendor.js");
require("./request.js");

const BASE_URL = "http://127.0.0.1:8080/api";

exports.uploadFile = (filePath, type = "image") => new Promise((resolve, reject) => {
  const token = vendor.index.getStorageSync("token");
  vendor.index.uploadFile({
    url: BASE_URL + "/file/upload",
    filePath,
    name: "file",
    header: {
      Authorization: token ? "Bearer " + token : ""
    },
    formData: { type },
    success: (res) => {
      try {
        const data = JSON.parse(res.data);
        if (data.code === 200 && data.data) {
          resolve(data.data);
        } else {
          vendor.index.showToast({ title: "上传失败", icon: "none" });
          reject(new Error("上传失败"));
        }
      } catch (err) {
        reject(err);
      }
    },
    fail: (err) => {
      console.error("上传失败:", err);
      vendor.index.showToast({ title: "上传失败", icon: "none" });
      reject(err);
    }
  });
});
