package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.personbruker.dittnav.varselbestiller.beskjed.AvroBeskjedInternObjectMother
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.AvroDoknotifikasjonObjectMother
import no.nav.personbruker.dittnav.varselbestiller.innboks.AvroInnboksInternObjectMother
import no.nav.personbruker.dittnav.varselbestiller.nokkel.AvroNokkelInternObjectMother
import no.nav.personbruker.dittnav.varselbestiller.oppgave.AvroOppgaveInternObjectMother
import org.junit.jupiter.api.Test

class VarselbestillingTransformerTest {

    @Test
    fun `Skal transformere fra Beskjed`() {
        val avroNokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(1)
        val avroBeskjed =  AvroBeskjedInternObjectMother.createBeskjedIntern()
        val varselbestilling = VarselbestillingTransformer.fromBeskjed(avroNokkel, avroBeskjed)

        varselbestilling.bestillingsId shouldBe avroNokkel.getEventId()
        varselbestilling.eventId shouldBe avroNokkel.getEventId()
        varselbestilling.fodselsnummer shouldBe avroNokkel.getFodselsnummer()
        varselbestilling.systembruker shouldBe avroNokkel.getSystembruker()
        varselbestilling.appnavn shouldBe avroNokkel.getAppnavn()
        varselbestilling.bestillingstidspunkt.shouldNotBeNull()
    }

    @Test
    fun `Skal transformere fra Oppgave`() {
        val avroNokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(1)
        val avroOppgave =  AvroOppgaveInternObjectMother.createOppgaveIntern()
        val varselbestilling = VarselbestillingTransformer.fromOppgave(avroNokkel, avroOppgave)

        varselbestilling.bestillingsId shouldBe avroNokkel.getEventId()
        varselbestilling.eventId shouldBe avroNokkel.getEventId()
        varselbestilling.fodselsnummer shouldBe avroNokkel.getFodselsnummer()
        varselbestilling.systembruker shouldBe avroNokkel.getSystembruker()
        varselbestilling.appnavn shouldBe avroNokkel.getAppnavn()
        varselbestilling.bestillingstidspunkt.shouldNotBeNull()
    }

    @Test
    fun `Skal transformere fra Innboks`() {
        val avroNokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(1)
        val avroInnboks =  AvroInnboksInternObjectMother.createInnboksIntern()
        val varselbestilling = VarselbestillingTransformer.fromInnboks(avroNokkel, avroInnboks)

        varselbestilling.bestillingsId shouldBe avroNokkel.getEventId()
        varselbestilling.eventId shouldBe avroNokkel.getEventId()
        varselbestilling.fodselsnummer shouldBe avroNokkel.getFodselsnummer()
        varselbestilling.systembruker shouldBe avroNokkel.getSystembruker()
        varselbestilling.appnavn shouldBe avroNokkel.getAppnavn()
        varselbestilling.bestillingstidspunkt.shouldNotBeNull()
    }
}
