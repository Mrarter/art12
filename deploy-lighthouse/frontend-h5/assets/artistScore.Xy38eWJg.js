import{r as e}from"./request.CqExrr0I.js";function r(r){return new Promise((t,s)=>{e.get(`/artist/score/${r}`).then(e=>t(e||null)).catch(e=>{"NOT_FOUND"!==e.message?s(e):t(null)})})}export{r as g};
