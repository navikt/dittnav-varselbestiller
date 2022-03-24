
CREATE TABLE IF NOT EXISTS early_done_event(
   eventid character varying(50) primary key,
   appnavn character varying(100),
   namespace character varying(100),
   fodselsnummer character varying(50),
   systembruker character varying(100),
   tidspunkt timestamp without time zone
)
