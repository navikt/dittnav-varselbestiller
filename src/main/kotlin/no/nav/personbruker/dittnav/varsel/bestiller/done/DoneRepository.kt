package no.nav.personbruker.dittnav.varsel.bestiller.done

import no.nav.personbruker.dittnav.varsel.bestiller.beskjed.setBeskjederAktivflagg
import no.nav.personbruker.dittnav.varsel.bestiller.common.database.Database
import no.nav.personbruker.dittnav.varsel.bestiller.common.database.entity.Brukernotifikasjon
import no.nav.personbruker.dittnav.varsel.bestiller.common.database.entity.getBrukernotifikasjonFromViewForEventIds
import no.nav.personbruker.dittnav.varsel.bestiller.innboks.setInnboksEventerAktivFlag
import no.nav.personbruker.dittnav.varsel.bestiller.oppgave.setOppgaverAktivFlag

class DoneRepository(private val database: Database) {

    suspend fun writeDoneEventsForBeskjedToCache(doneEvents: List<Done>) {
        if (doneEvents.isEmpty()) {
            return
        }
        database.queryWithExceptionTranslation {
            setBeskjederAktivflagg(doneEvents, false)
        }
    }

    suspend fun writeDoneEventsForOppgaveToCache(doneEvents: List<Done>) {
        if (doneEvents.isEmpty()) {
            return
        }
        database.queryWithExceptionTranslation {
            setOppgaverAktivFlag(doneEvents, false)
        }
    }

    suspend fun writeDoneEventsForInnboksToCache(doneEvents: List<Done>) {
        if (doneEvents.isEmpty()) {
            return
        }
        database.queryWithExceptionTranslation {
            setInnboksEventerAktivFlag(doneEvents, false)
        }
    }

    suspend fun writeDoneEventsToCache(doneEvents: List<Done>) {
        if (doneEvents.isEmpty()) {
            return
        }
        database.queryWithExceptionTranslation {
            createDoneEvents(doneEvents)
        }
    }

    suspend fun fetchBrukernotifikasjonerFromViewForEventIds(eventIds: List<String>): List<Brukernotifikasjon> {
        var resultat = emptyList<Brukernotifikasjon>()
        database.queryWithExceptionTranslation {
            resultat = getBrukernotifikasjonFromViewForEventIds(eventIds)
        }
        return resultat
    }

    suspend fun fetchAllDoneEvents(): List<Done> {
        var resultat = emptyList<Done>()
        database.queryWithExceptionTranslation {
            resultat = getAllDoneEvent()
        }
        return resultat
    }

    suspend fun deleteDoneEventsFromCache(doneEventsToDelete: List<Done>) {
        database.queryWithExceptionTranslation {
            deleteDoneEvents(doneEventsToDelete)
        }
    }

}
