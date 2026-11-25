package dev.pokesmells.requiem

// The supplied cue opens up near seven seconds and builds again near seventeen.
internal fun recapPosition(progress: Float): Float = when {
    progress < 0.26f -> 0.15f * progress / 0.26f
    progress < 0.63f -> 0.15f + 0.24f * (progress - 0.26f) / 0.37f
    else -> {
        val rush = (progress - 0.63f) / 0.37f
        0.39f + 0.61f * rush * (0.40f + 0.60f * rush * rush)
    }
}
