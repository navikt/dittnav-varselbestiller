ALTER TABLE varselbestilling DROP CONSTRAINT varselbestilling_pkey;

ALTER TABLE varselbestilling ADD PRIMARY KEY (eventid);
