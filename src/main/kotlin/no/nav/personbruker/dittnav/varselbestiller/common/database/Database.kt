package no.nav.personbruker.dittnav.varselbestiller.common.database

import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.personbruker.dittnav.varselbestiller.common.database.exception.RetriableDatabaseException
import no.nav.personbruker.dittnav.varselbestiller.common.database.exception.UnretriableDatabaseException
import no.nav.personbruker.dittnav.varselbestiller.health.HealthCheck
import no.nav.personbruker.dittnav.varselbestiller.health.HealthStatus
import no.nav.personbruker.dittnav.varselbestiller.health.Status
import org.postgresql.util.PSQLException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.*

val log: Logger = LoggerFactory.getLogger(Database::class.java)

interface Database: HealthCheck {

    val dataSource: HikariDataSource

    suspend fun <T> dbQuery(operationToExecute: Connection.() -> T): T = withContext(Dispatchers.IO) {
        dataSource.connection.use { openConnection ->
            try {
                openConnection.operationToExecute().apply {
                    openConnection.commit()
                }

            } catch (e: Exception) {
                try {
                    openConnection.rollback()
                } catch (rollbackException: Exception) {
                    e.addSuppressed(rollbackException)
                }
                throw e
            }
        }
    }


    suspend fun <T> queryWithExceptionTranslation(operationToExecute: Connection.() -> T): T {
        return translateExternalExceptionsToInternalOnes {
            dbQuery {
                operationToExecute()
            }
        }
    }

    override suspend fun status(): HealthStatus {
        val serviceName = "Database"
        return withContext(Dispatchers.IO) {
            try {
                dbQuery { prepareStatement("""SELECT * FROM varselbestilling LIMIT 1""").execute() }
                HealthStatus(serviceName, Status.OK, "200 OK")
            } catch (e: Exception) {
                log.error("Selftest mot databasen feilet", e)
                HealthStatus(serviceName, Status.ERROR, "Feil mot DB")
            }
        }
    }
}

inline fun <T> translateExternalExceptionsToInternalOnes(databaseActions: () -> T): T {
    return try {
        databaseActions()

    } catch (te: SQLTransientException) {
        val message = "Skriving til databasen feilet grunnet en periodisk feil."
        throw RetriableDatabaseException(message, te)

    } catch (re: SQLRecoverableException) {
        val message = "Skriving til databasen feilet grunnet en periodisk feil."
        throw RetriableDatabaseException(message, re)

    } catch (pe: PSQLException) {
        val message = "Det skjedde en SQL relatert feil ved skriving til databasen."
        val ure = UnretriableDatabaseException(message, pe)
        pe.sqlState?.map { sqlState -> ure.addContext("sqlState", sqlState) }
        throw ure

    } catch (se: SQLException) {
        val message = "Det skjedde en SQL relatert feil ved skriving til databasen."
        val ure = UnretriableDatabaseException(message, se)
        se.sqlState?.map { sqlState -> ure.addContext("sqlState", sqlState) }
        throw ure

    } catch (e: Exception) {
        val message = "Det skjedde en ukjent feil ved skriving til databasen."
        throw UnretriableDatabaseException(message, e)
    }
}
