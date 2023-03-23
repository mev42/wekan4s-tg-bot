package app.model

import java.time.ZonedDateTime

case class LoginResponse(id: String, token: String, tokenExpires: ZonedDateTime)
