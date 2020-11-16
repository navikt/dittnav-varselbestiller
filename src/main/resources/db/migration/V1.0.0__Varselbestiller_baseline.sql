CREATE TABLE IF NOT EXISTS varselbestilling(
    bestillingsid primary key,
    eventtype characther varying(50),
    systembruker character varying(100),
    eventtidspunkt timestamp without time zone
)
