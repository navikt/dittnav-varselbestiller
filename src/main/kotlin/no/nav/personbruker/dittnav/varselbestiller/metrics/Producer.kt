package no.nav.personbruker.dittnav.varselbestiller.metrics

data class Producer(val namespace: String, val appnavn: String) {

    fun getProducerKey(): String {
        return "$namespace:$appnavn"
    }
}
