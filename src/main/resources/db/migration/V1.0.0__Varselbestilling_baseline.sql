CREATE TABLE IF NOT EXISTS varselbestilling(
    bestillingsid character varying(100) primary key,
    eventid character varying(50),
    fodselsnummer character varying(50),
    systembruker character varying(100),
    eventtidspunkt timestamp without time zone
)
