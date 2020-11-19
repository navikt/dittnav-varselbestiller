package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.varselbestiller.common.database.H2Database
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test

class DoknotifikasjonTeardownQueriesTest {

    private val database = H2Database()

    private val doknotifikasjon1 = DoknotifikasjonObjectMother.giveMeDoknotifikasjon("B-test-001", Eventtype.BESKJED)
    private val doknotifikasjon2 = DoknotifikasjonObjectMother.giveMeDoknotifikasjon("B-test-002", Eventtype.BESKJED)
    private val doknotifikasjon3 = DoknotifikasjonObjectMother.giveMeDoknotifikasjon("O-test-001", Eventtype.OPPGAVE)

    @Test
    fun `Skal slette alle rader i doknotifikasjon-tabellen`() {
        runBlocking {
            createDoknotifikasjoner(listOf(doknotifikasjon1, doknotifikasjon2, doknotifikasjon3))
            var result = database.dbQuery { getAllDoknotifikasjon() }
            result.size `should be equal to` 3
            deleteAllDoknotifikasjoner()
            result = database.dbQuery { getAllDoknotifikasjon() }
            result.isEmpty() `should be equal to` true
        }
    }

    private suspend fun createDoknotifikasjoner(doknotifikasjoner: List<Doknotifikasjon>) {
        database.dbQuery {
            createDoknotifikasjoner(doknotifikasjoner)
        }
    }

    private suspend fun deleteAllDoknotifikasjoner() {
        database.dbQuery {
            deleteAllDoknotifikasjon()
        }
    }
}



