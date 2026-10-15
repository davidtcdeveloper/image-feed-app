package com.example.imagefeed.core.network

/**
 * Filters [this] to only those elements whose key (determined by [selector]) does not already exist
 * in [existing], while also filtering intra-collection duplicates within [this].
 */
inline fun <T, K> Iterable<T>.filterDistinctAgainst(
    existing: Collection<T>,
    selector: (T) -> K,
): List<T> {
    val existingKeys = existing.mapTo(mutableSetOf(), selector)
    return filter { existingKeys.add(selector(it)) }
}
