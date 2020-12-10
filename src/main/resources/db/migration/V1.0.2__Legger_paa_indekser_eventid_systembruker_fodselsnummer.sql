CREATE INDEX IF NOT EXISTS varselbestilling_index_for_fields_used_in_find_query
    ON varselbestilling (eventid, systembruker, fodselsnummer);
