const BASE_URL = 'http://localhost:3000/api/player/login';
const axios = require('axios');
async function tryLogin(creds) {
  try {
    const res = await axios.post(BASE_URL, creds);
    console.log(res.data);
  } catch (err) {
    //console.log("We got an error: ", err);
    console.log("We got an error: ");
  }
}
async function main() {
  await tryLogin({
    "username": "what_a_name",
    "password": "what_a_password"
  });
  await tryLogin({
    "username": "what_a_name",
    "password": "what_a_password"
  });
  await tryLogin({
    "username": "what_a_name",
    "password": "what_a_password"
  });
  await tryLogin({
    "username": "what_a_name",
    "password": "what_a_wrong_password"
  });
  await tryLogin({
    "username": "what_not aname",
    "password": "what_a_password"
  });
}
main();
