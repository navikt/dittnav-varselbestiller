package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.varselbestiller.common.database.H2Database
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.`with message`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.sql.SQLException

class doknotifikasjonQueriesTest {

    private val database = H2Database()

    private val doknotifikasjonBeskjed: Doknotifikasjon = DoknotifikasjonObjectMother.giveMeDoknotifikasjon("B-test-001", Eventtype.BESKJED)
    private val doknotifikasjonOppgave: Doknotifikasjon = DoknotifikasjonObjectMother.giveMeDoknotifikasjon("O-test-002", Eventtype.OPPGAVE)

    init {
        createDoknotifikasjoner(listOf(doknotifikasjonBeskjed, doknotifikasjonOppgave))
    }

    private fun createDoknotifikasjoner(doknotifikasjoner: List<Doknotifikasjon>) {
        runBlocking {
            database.dbQuery {
                createDoknotifikasjoner(doknotifikasjoner)
            }
        }
    }

    @AfterAll
    fun tearDown() {
        runBlocking {
            database.dbQuery {
                deleteAllDoknotifikasjon()
            }
        }
    }

    @Test
    fun `Finner Doknotifikasjon med bestillingsid`() {
        runBlocking {
            val result = database.dbQuery { getDoknotifikasjonForBestillingsid(doknotifikasjonBeskjed.bestillingsid) }
            result `should be equal to` doknotifikasjonBeskjed
        }
    }

    @Test
    fun `Kaster Exception hvis Doknotifikasjon med bestillingsid ikke finnes`() {
        invoking {
            runBlocking {
                database.dbQuery { getDoknotifikasjonForBestillingsid("idFinnesIkke") }
            }
        } `should throw` SQLException::class `with message` "Found no rows"
    }
}
