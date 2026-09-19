package com.hive.execution;

public enum ExecutionStatus {
    /** The program ran to completion with exit code 0. */
    SUCCESS,
    /** The program ran but exited non-zero, or failed to compile. */
    FAILED,
    /** The program exceeded the wall-clock limit and its container was killed. */
    TIMEOUT,
    /** The sandbox itself could not run — Docker missing, image unavailable, etc. */
    ERROR
}
