import default.DependencyGroup

object JacksonDataType14: default.JacksonDatatypeDefaults {
    override val version = "2.14.2"
}

object Avro: DependencyGroup {
    override val groupId get() = "io.confluent"
    override val version get() = "6.2.1"

    val avroSerializer get() = dependency("kafka-avro-serializer")
    val schemaRegistry get() = dependency("kafka-schema-registry")
}

object Doknotifikasjon: DependencyGroup {
    override val groupId get() = "com.github.navikt"
    override val version get() = "1.2020.11.16-09.27-d037b30bb0ea"

    val schemas get() = dependency("doknotifikasjon-schemas")
}
