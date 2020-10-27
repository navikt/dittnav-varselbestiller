# dittnav-varselbestiller

Microservice som brukes for å lese inn eventer fra DittNAV sine kafka-topics, 
appen filtrerer på ekstern-varslings-event og sender disse videre til varsling/dokument topic-en. 
De varsler bruker enten på epost eller SMS.

# Kom i gang
1. Bygge dittnav-varselbestiller:
    * bygge og kjøre enhetstester: `gradle clean test`
    * bygge og kjøre integrasjonstester: `gradle clean build`
2. Start lokal instans av Kafka og Postgres ved å kjøre `docker-compose up -d`
3. Start konsumenten ved å kjøre kommandoen `gradle runServer`

# Feilsøking
For å være sikker på at man får en ny tom database og tomme kafka-topics kan man kjøre kommandoen: `docker-compose down -v`

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan rettes mot https://github.com/orgs/navikt/teams/personbruker

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #team-personbruker.
