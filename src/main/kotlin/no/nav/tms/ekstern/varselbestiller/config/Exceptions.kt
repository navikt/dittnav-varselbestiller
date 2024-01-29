package no.nav.tms.ekstern.varselbestiller.config

open class AbstractCustomException(message: String, cause: Throwable?, identifier: String?) :
    Exception(message, cause) {

    private val context = mutableMapOf<String, Any>()

    init {
        if (identifier != null) {
            context["identifier"] = identifier
        }
    }

    override fun toString(): String {
        return when (context.isNotEmpty()) {
            true -> super.toString() + ", context: $context"
            false -> super.toString()
        }
    }

}

class FieldValidationException(message: String, cause: Throwable? = null) :
    AbstractCustomException(message, cause, null)
