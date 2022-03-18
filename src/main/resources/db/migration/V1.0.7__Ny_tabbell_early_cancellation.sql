
CREATE TABLE IF NOT EXISTS early_cancellation(
   eventid character varying(50) primary key,
   appnavn character varying(100),
   fodselsnummer character varying(50),
   systembruker character varying(100)
)
