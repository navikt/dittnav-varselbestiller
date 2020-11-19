CREATE TABLE IF NOT EXISTS doknotifikasjon(
    bestillingsid character varying(100) primary key,
    eventtype character varying(50),
    systembruker character varying(100),
    eventtidspunkt timestamp without time zone
)
