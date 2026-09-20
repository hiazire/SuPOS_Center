package com.example.supos.data.model

object OrderStateMachine {

    const val STATE_PENDING = "PENDING"
    const val STATE_ACCEPTED = "ACCEPTED"
    const val STATE_READY = "READY"
    const val STATE_FULFILLED = "FULFILLED"
    const val STATE_CANCELED = "CANCELED"

    private val TERMINAL_STATES = setOf(STATE_CANCELED, STATE_FULFILLED)

    private val ALLOWED_TRANSITIONS: Map<String, Set<String>> = mapOf(
        STATE_PENDING to setOf(STATE_ACCEPTED, STATE_READY, STATE_CANCELED),
        STATE_ACCEPTED to setOf(STATE_READY, STATE_FULFILLED, STATE_CANCELED),
        STATE_READY to setOf(STATE_FULFILLED, STATE_CANCELED),
        STATE_FULFILLED to emptySet(),
        STATE_CANCELED to emptySet()
    )

    fun isTerminal(state: String): Boolean {
        return TERMINAL_STATES.contains(state.uppercase())
    }

    fun canTransition(currentState: String, targetState: String): Boolean {
        val current = currentState.uppercase()
        val target = targetState.uppercase()

        if (current == target) return false
        if (isTerminal(current)) return false

        val allowed = ALLOWED_TRANSITIONS[current] ?: return false
        return allowed.contains(target)
    }

    fun getValidNextStates(currentState: String): Set<String> {
        return ALLOWED_TRANSITIONS[currentState.uppercase()] ?: emptySet()
    }
}
