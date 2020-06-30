package no.nav.personbruker.dittnav.varsel.bestiller.common.objectmother

import no.nav.personbruker.dittnav.varsel.bestiller.common.database.entity.Brukernotifikasjon
import no.nav.personbruker.dittnav.varsel.bestiller.config.EventType

object BrukernotifikasjonObjectMother {

    fun giveMeOneOfEachEventType(): List<Brukernotifikasjon> {
        return listOf(giveMeBeskjed(), giveMeInnboks(), giveMeOppgave())
    }

    fun giveMeBeskjed(): Brukernotifikasjon {
        return Brukernotifikasjon("b-1", "dummySystembruker", EventType.BESKJED, "123")
    }

    fun giveMeBeskjed(fodselsnummer: String): Brukernotifikasjon {
        return Brukernotifikasjon("b-1", "dummySystembruker", EventType.BESKJED, fodselsnummer)
    }

    fun giveMeInnboks(): Brukernotifikasjon {
        return Brukernotifikasjon("i-1", "dummySystembruker", EventType.INNBOKS, "123")
    }

    fun giveMeInnboks(fodselsnummer: String): Brukernotifikasjon {
        return Brukernotifikasjon("i-1", "dummySystembruker", EventType.INNBOKS, fodselsnummer)
    }

    fun giveMeOppgave(): Brukernotifikasjon {
        return Brukernotifikasjon("o-1", "dummySystembruker", EventType.OPPGAVE, "123")
    }

    fun giveMeOppgave(fodselsnummer: String): Brukernotifikasjon {
        return Brukernotifikasjon("o-1", "dummySystembruker", EventType.OPPGAVE, fodselsnummer)
    }

}
