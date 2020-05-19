package no.nav.personbruker.dittnav.varsel.bestiller.common.database

interface BrukernotifikasjonRepository<T> {

    suspend fun createInOneBatch(entities: List<T>)

    suspend fun createOneByOneToFilterOutTheProblematicEvents(entities: List<T>)

}
