package io.github.jayfunc.carpelines.data.backup.module

/**
 * One backup module. Handlers are registered per [io.github.jayfunc.carpelines.data.backup.model.BackupSection],
 * which is the only place a module's identity lives.
 */
interface BackupModuleHandler {
    /** Serialize current app data for this module into a JSON string. */
    suspend fun export(): String

    /** Capture current state as a JSON snapshot for rollback. */
    suspend fun snapshot(): String

    /** Clear existing data and restore from a JSON payload. */
    suspend fun restore(payload: String)

    /** Rollback to a previous snapshot (for transactional restore). */
    suspend fun rollback(snapshot: String)
}

/**
 * Thrown by a handler whose payload is valid but cannot be applied to this library, for example
 * a legacy payload in a format the current page no longer reads, or a module that needs the
 * library to be scanned first.
 *
 * The restore keeps going and reports the message as a warning instead of failing and rolling
 * back every other module, and the handler must not have changed anything before throwing.
 */
class ModuleRestoreSkippedException(message: String) : Exception(message)
