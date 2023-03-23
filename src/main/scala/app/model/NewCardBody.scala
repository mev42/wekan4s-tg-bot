package app.model

case class NewCardBody(
    authorId: String,
    members: Option[String],
    title: String,
    description: String,
    swimlaneId: String
)
