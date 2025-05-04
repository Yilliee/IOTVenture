const BASE_URL = 'http://localhost:3000/api/player';
const LOGIN_URL = BASE_URL + "/login";
const LOGOUT_URL = BASE_URL + "/logout";

const axios = require('axios');
async function sendRequest(creds, url) {
  try {
    const res = await axios.post(url, creds);
    console.log(res.data);
  } catch (err) {
    //console.log("We got an error: ", err);
    console.log("We got an error: ");
  }
}
async function main() {
  await sendRequest({
    "username": "what_a_name",
    "password": "what_a_password"
  }, LOGIN_URL);
  await sendRequest({
    "username": "what_a_name",
    "password": "what_a_password"
  }, LOGIN_URL);
  await sendRequest({
    "username": "what_a_name",
    "password": "what_a_password"
  }, LOGOUT_URL);
}
main();
