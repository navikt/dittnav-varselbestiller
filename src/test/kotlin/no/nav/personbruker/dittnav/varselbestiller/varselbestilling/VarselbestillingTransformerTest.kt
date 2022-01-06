package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import no.nav.personbruker.dittnav.varselbestiller.beskjed.AvroBeskjedInternObjectMother
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.AvroDoknotifikasjonObjectMother
import no.nav.personbruker.dittnav.varselbestiller.nokkel.AvroNokkelInternObjectMother
import no.nav.personbruker.dittnav.varselbestiller.oppgave.AvroOppgaveInternObjectMother
import no.nav.personbruker.dittnav.varselbestiller.innboks.AvroInnboksObjectMother
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be null`
import org.junit.jupiter.api.Test

class VarselbestillingTransformerTest {

    @Test
    fun `Skal transformere fra Beskjed`() {
        val avroNokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(1)
        val avroBeskjed =  AvroBeskjedInternObjectMother.createBeskjedIntern()
        val avroDoknotifikasjon = AvroDoknotifikasjonObjectMother.createDoknotifikasjon("B-test-001")
        val varselbestilling = VarselbestillingTransformer.fromBeskjed(avroNokkel, avroBeskjed, avroDoknotifikasjon)

        varselbestilling.bestillingsId `should be equal to` avroDoknotifikasjon.getBestillingsId()
        varselbestilling.eventId `should be equal to` avroNokkel.getEventId()
        varselbestilling.fodselsnummer `should be equal to` avroNokkel.getFodselsnummer()
        varselbestilling.systembruker `should be equal to` avroNokkel.getSystembruker()
        varselbestilling.appnavn `should be equal to` avroNokkel.getAppnavn()
        varselbestilling.bestillingstidspunkt.`should not be null`()
    }

    @Test
    fun `Skal transformere fra Oppgave`() {
        val avroNokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(1)
        val avroOppgave =  AvroOppgaveInternObjectMother.createOppgave()
        val avroDoknotifikasjon = AvroDoknotifikasjonObjectMother.createDoknotifikasjon("O-test-001")
        val varselbestilling = VarselbestillingTransformer.fromOppgave(avroNokkel, avroOppgave, avroDoknotifikasjon)

        varselbestilling.bestillingsId `should be equal to` avroDoknotifikasjon.getBestillingsId()
        varselbestilling.eventId `should be equal to` avroNokkel.getEventId()
        varselbestilling.fodselsnummer `should be equal to` avroNokkel.getFodselsnummer()
        varselbestilling.systembruker `should be equal to` avroNokkel.getSystembruker()
        varselbestilling.appnavn `should be equal to` avroNokkel.getAppnavn()
        varselbestilling.bestillingstidspunkt.`should not be null`()
    }

    @Test
    fun `Skal transformere fra Innboks`() {
        val avroNokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(1)
        val avroInnboks =  AvroInnboksObjectMother.createInnboks()
        val avroDoknotifikasjon = AvroDoknotifikasjonObjectMother.createDoknotifikasjon("I-test-001")
        val varselbestilling = VarselbestillingTransformer.fromInnboks(avroNokkel, avroInnboks, avroDoknotifikasjon)

        varselbestilling.bestillingsId `should be equal to` avroDoknotifikasjon.getBestillingsId()
        varselbestilling.eventId `should be equal to` avroNokkel.getEventId()
        varselbestilling.fodselsnummer `should be equal to` avroInnboks.getFodselsnummer()
        varselbestilling.systembruker `should be equal to` avroNokkel.getSystembruker()
        varselbestilling.bestillingstidspunkt.`should not be null`()
    }
}
