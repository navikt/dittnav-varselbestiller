package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import no.nav.personbruker.dittnav.varselbestiller.beskjed.AvroBeskjedObjectMother
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.AvroDoknotifikasjonObjectMother
import no.nav.personbruker.dittnav.varselbestiller.nokkel.AvroNokkelObjectMother
import no.nav.personbruker.dittnav.varselbestiller.oppgave.AvroOppgaveObjectMother
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be null`
import org.junit.jupiter.api.Test

internal class VarselbestillingTransformerTest {

    @Test
    fun `Skal transformere fra Beskjed`() {
        val avroNokkel = AvroNokkelObjectMother.createNokkelWithEventId(1)
        val avroBeskjed =  AvroBeskjedObjectMother.createBeskjed(1)
        val avroDoknotifikasjon = AvroDoknotifikasjonObjectMother.createDoknotifikasjon("B-test-001")
        val varselbestilling = VarselbestillingTransformer.fromBeskjed(avroNokkel, avroBeskjed, avroDoknotifikasjon)

        varselbestilling.bestillingsId `should be equal to` avroDoknotifikasjon.getBestillingsId()
        varselbestilling.eventId `should be equal to` avroNokkel.getEventId()
        varselbestilling.fodselsnummer `should be equal to` avroBeskjed.getFodselsnummer()
        varselbestilling.systembruker `should be equal to` avroNokkel.getSystembruker()
        varselbestilling.bestillingstidspunkt.`should not be null`()
    }

    @Test
    fun `Skal transformere fra Oppgave`() {
        val avroNokkel = AvroNokkelObjectMother.createNokkelWithEventId(1)
        val avroOppgave =  AvroOppgaveObjectMother.createOppgave(1)
        val avroDoknotifikasjon = AvroDoknotifikasjonObjectMother.createDoknotifikasjon("O-test-001")
        val varselbestilling = VarselbestillingTransformer.fromOppgave(avroNokkel, avroOppgave, avroDoknotifikasjon)

        varselbestilling.bestillingsId `should be equal to` avroDoknotifikasjon.getBestillingsId()
        varselbestilling.eventId `should be equal to` avroNokkel.getEventId()
        varselbestilling.fodselsnummer `should be equal to` avroOppgave.getFodselsnummer()
        varselbestilling.systembruker `should be equal to` avroNokkel.getSystembruker()
        varselbestilling.bestillingstidspunkt.`should not be null`()
    }
}
