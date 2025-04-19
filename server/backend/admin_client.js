const BASE_URL = 'http://localhost:3000/api/admin';
const REG_URL = BASE_URL + "/teams/register";
const DEL_URL = BASE_URL + "/teams/delete";
const UPDATE_URL = BASE_URL + "/teams/update";
const READ_URL = BASE_URL + "/teams";

const axios = require('axios');
async function sendRequest(creds, url, method) {
  try {
    let res;
    if (method == "GET") {
      res = await axios.get(url, creds);
    } else if (method == "POST") {
      res = await axios.post(url, creds);
    }
    console.log(res.data);
  } catch (err) {
    console.log("We got an error: ", err);
    console.log("We got an error");
  }
}
async function main() {
  await sendRequest({
    "name": "what_a_name",
    "password": "what_a_password",
    "sessions_allowed_count": 5
  }, REG_URL, "POST");
  await sendRequest({
    "name": "what_a_name",
    "password": "what_a_password",
    "sessions_allowed_count": 3
  }, REG_URL, "POST");
  await sendRequest({
    "name": "what_a_name2",
    "password": "what_a_password",
  }, REG_URL, "POST");
  await sendRequest({
    "name": "what_a_name2",
    "password": "what_a_password",
  }, DEL_URL, "POST");
  await sendRequest({
    "name": "what_a_name3",
    "password": "what_a_password",
  }, REG_URL, "POST");
  await sendRequest({
    "name": "what_a_name",
    "new_name": "updated_name",
  }, UPDATE_URL, "POST");
  await sendRequest({}, READ_URL, "GET");
}
main();
