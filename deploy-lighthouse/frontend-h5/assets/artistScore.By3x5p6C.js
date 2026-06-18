import{r as e}from"./user.DLUwzuDo.js";function r(r){return new Promise((s,t)=>{e.get(`/artist/score/${r}`).then(e=>s(e||null)).catch(e=>{"NOT_FOUND"!==e.message?t(e):s(null)})})}export{r as g};
