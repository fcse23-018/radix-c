package com.soilcarbon.engine.core

/**
 * Small result type used across the domain boundary. Kotlin's [Result] is avoided in public
 * APIs because it can hide exception types and cannot be serialised.
 */
sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = (this as? Success)?.value

    fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw error.toException()
    }

    companion object {
        fun <T> success(value: T): AppResult<T> = Success(value)
        fun <T> failure(error: AppError): AppResult<T> = Failure(error)
    }
}

sealed class AppError(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    class Camera(message: String, cause: Throwable? = null) : AppError(message, cause)
    class Permission(message: String) : AppError(message)
    class Calibration(message: String) : AppError(message)
    class Inference(message: String, cause: Throwable? = null) : AppError(message, cause)
    class Storage(message: String, cause: Throwable? = null) : AppError(message, cause)
    class Network(message: String, cause: Throwable? = null) : AppError(message, cause)
    class Validation(message: String) : AppError(message)
    class Security(message: String, cause: Throwable? = null) : AppError(message, cause)
    class NotFound(message: String) : AppError(message)
    class Unknown(message: String, cause: Throwable? = null) : AppError(message, cause)

    fun toException(): Exception = when (this) {
        is Camera, is Permission, is Calibration, is Inference, is Storage,
        is Network, is Validation, is Security, is NotFound, is Unknown -> this
    }
}

inline fun <T> runCatchingApp(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (t: Throwable) {
    AppResult.Failure(
        when (t) {
            is AppError -> t
            is SecurityException -> AppError.Security(t.message ?: "Security error", t)
            is IllegalArgumentException -> AppError.Validation(t.message ?: "Invalid value")
            else -> AppError.Unknown(t.message ?: t.javaClass.simpleName, t)
        }
    )
}

inline fun <T, R> AppResult<T>.flatMap(transform: (T) -> AppResult<R>): AppResult<R> = when (this) {
    is AppResult.Success -> transform(value)
    is AppResult.Failure -> this
}
