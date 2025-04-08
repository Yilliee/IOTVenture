const BASE_URL = 'http://localhost:3000/api/admin';
const REG_URL = BASE_URL + "/register";

const axios = require('axios');
async function sendRequest(creds, url) {
  try {
    const res = await axios.post(url, creds);
    console.log(res.data);
  } catch (err) {
    //console.log("We got an error: ", err);
    console.log("We got an error");
  }
}
async function main() {
  await sendRequest({
    "name": "what_a_name",
    "password": "what_a_password",
    "sessions_allowed_count": 5
  }, REG_URL);
  await sendRequest({
    "name": "what_a_name",
    "password": "what_a_password",
    "sessions_allowed_count": 3
  }, REG_URL);
  await sendRequest({
    "name": "what_a_name2",
    "password": "what_a_password",
  }, REG_URL);
}
main();
