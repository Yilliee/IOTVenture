import crypto from "crypto"

function hash_password(password) {
  const salt = crypto.randomBytes(16).toString("hex")
  const hash = crypto.pbkdf2Sync(password, salt, 1000, 64, "sha512").toString("hex")
  return `${salt}:${hash}`
}

function verify_password(storedPassword, inputPassword) {
  const [salt, hash] = storedPassword.split(":")
  const inputHash = crypto.pbkdf2Sync(inputPassword, salt, 1000, 64, "sha512").toString("hex")
  return hash === inputHash
}

export { hash_password, verify_password }
