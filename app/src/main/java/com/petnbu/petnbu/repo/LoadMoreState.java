package com.petnbu.petnbu.repo;

public class LoadMoreState {
    private final boolean running;
    private final String errorMessage;
    private boolean handledError = false;

    public LoadMoreState(boolean running, String errorMessage) {
        this.running = running;
        this.errorMessage = errorMessage;
    }

    public boolean isRunning() {
        return running;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorMessageIfNotHandled() {
        if (handledError) {
            return null;
        }
        handledError = true;
        return errorMessage;
    }

    @Override
    public String toString() {
        return "LoadMoreState{" +
                "running=" + running +
                ", errorMessage='" + errorMessage + '\'' +
                ", handledError=" + handledError +
                '}';
    }
}
