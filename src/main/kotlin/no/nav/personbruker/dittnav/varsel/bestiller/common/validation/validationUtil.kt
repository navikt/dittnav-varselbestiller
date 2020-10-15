package no.nav.personbruker.dittnav.varsel.bestiller.common.validation

import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.FieldValidationException

private val fodselsnummerRegEx = """[\d]{1,11}""".toRegex()
private val epostPreferertKanalRegex = """epost""".toRegex(setOf(RegexOption.IGNORE_CASE))
private val smsPreferertKanalRegex = """sms""".toRegex(setOf(RegexOption.IGNORE_CASE))
private val isEksternVarslingRegex = """true""".toRegex(setOf(RegexOption.IGNORE_CASE))


fun validateFodselsnummer(field: String): String {
    validateNonNullField(field, "fødselsnummer")
    if (isNotValidFodselsnummer(field)) {
        val fve = FieldValidationException("Feltet fodselsnummer kan kun innholde siffer, og maks antall er 11.")
        fve.addContext("rejectedFieldValue", field)
        throw fve
    }
    return field
}

private fun isNotValidFodselsnummer(field: String) = !fodselsnummerRegEx.matches(field)

fun validateNonNullFieldMaxLength(field: String, fieldName: String, maxLength: Int): String {
    validateNonNullField(field, fieldName)
    return validateMaxLength(field, fieldName, maxLength)
}

fun validateMaxLength(field: String, fieldName: String, maxLength: Int): String {
    if (field.length > maxLength) {
        val fve = FieldValidationException("Feltet $fieldName kan ikke inneholde mer enn $maxLength tegn.")
        fve.addContext("rejectedFieldValue", field)
        throw fve
    }
    return field
}

fun validateNonNullField(field: String?, fieldName: String): String {
    if (field.isNullOrBlank()) {
        throw FieldValidationException("$fieldName var null eller tomt.")
    }
    return field
}

fun validateNumberField(field: String, fieldName: String): String {
    try {
        field.toInt()
    } catch (e: Exception) {
        throw FieldValidationException("$fieldName var ikke et tall.")
    }
    return field
}

fun validatePrefererteKanaler(field: String, fieldName: String): String {
    if (!epostPreferertKanalRegex.matches(field) && !smsPreferertKanalRegex.matches(field)) {
        throw FieldValidationException("$fieldName inneholdt ikke EPOST eller SMS.")
    }
    return field
}

fun validateSikkerhetsnivaa(sikkerhetsnivaa: Int): Int {
    return when (sikkerhetsnivaa) {
        3, 4 -> sikkerhetsnivaa
        else -> throw FieldValidationException("Sikkerhetsnivaa kan bare være 3 eller 4.")
    }
}
