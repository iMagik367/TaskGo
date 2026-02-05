package com.taskgoapp.taskgo.core.model

/**
 * Funções de extensão para o Result customizado
 */

/**
 * Extrai o valor de Success ou lança exceção
 */
fun <T> Result<T>.getOrThrow(): T = when (this) {
    is Result.Success -> data
    is Result.Error -> throw exception
    is Result.Loading -> throw IllegalStateException("Result is still loading")
}

/**
 * Extrai o valor de Success ou retorna valor padrão
 */
fun <T> Result<T>.getOrElse(defaultValue: (Throwable) -> T): T = when (this) {
    is Result.Success -> data
    is Result.Error -> defaultValue(exception)
    is Result.Loading -> defaultValue(IllegalStateException("Result is still loading"))
}

/**
 * Verifica se é Success
 */
val <T> Result<T>.isSuccess: Boolean
    get() = this is Result.Success

/**
 * Verifica se é Error
 */
val <T> Result<T>.isError: Boolean
    get() = this is Result.Error

/**
 * Verifica se é Loading
 */
val <T> Result<T>.isLoading: Boolean
    get() = this is Result.Loading

/**
 * Obtém a exceção se for Error, null caso contrário
 */
fun <T> Result<T>.exceptionOrNull(): Throwable? = when (this) {
    is Result.Error -> exception
    else -> null
}

/**
 * Obtém os dados se for Success, null caso contrário
 */
fun <T> Result<T>.getDataOrNull(): T? = when (this) {
    is Result.Success -> data
    else -> null
}

/**
 * Transforma o Result usando uma função
 */
fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> Result.Error(exception)
    is Result.Loading -> Result.Loading
}

/**
 * Executa ação se for Success
 */
fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) {
        action(data)
    }
    return this
}

/**
 * Executa ação se for Error
 */
fun <T> Result<T>.onFailure(action: (Throwable) -> Unit): Result<T> {
    if (this is Result.Error) {
        action(exception)
    }
    return this
}

/**
 * Fold: transforma Result em outro tipo
 */
fun <T, R> Result<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (Throwable) -> R
): R = when (this) {
    is Result.Success -> onSuccess(data)
    is Result.Error -> onFailure(exception)
    is Result.Loading -> onFailure(IllegalStateException("Result is still loading"))
}
