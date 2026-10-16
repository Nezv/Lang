package app.lang.shared.content

import app.lang.shared.model.Lesson
import app.lang.shared.model.Sentence

/** Read-only access to learning content. Today it serves the built-in lessons. */
class ContentRepository(private val lessons: List<Lesson> = SeedContent.lessons) {
    private val sentencesById: Map<String, Sentence> = lessons.flatMap { it.sentences }.associateBy { it.id }

    fun lessons(): List<Lesson> = lessons

    fun sentence(id: String): Sentence? = sentencesById[id]
}
