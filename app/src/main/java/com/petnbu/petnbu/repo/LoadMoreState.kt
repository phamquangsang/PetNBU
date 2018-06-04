package com.petnbu.petnbu.repo

class LoadMoreState(val isRunning: Boolean, val errorMessage: String?) {
    private var handledError = false

    val errorMessageIfNotHandled: String?
        get() {
            if (handledError) {
                return null
            }
            handledError = true
            return errorMessage
        }

    override fun toString(): String {
        return "LoadMoreState{" +
                "running=" + isRunning +
                ", errorMessage='" + errorMessage + '\''.toString() +
                ", handledError=" + handledError +
                '}'.toString()
    }
}
