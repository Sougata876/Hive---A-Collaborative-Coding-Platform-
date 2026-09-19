package com.hive.execution;

import com.hive.room.ProgrammingLanguage;

/**
 * Runs untrusted source code in an isolated sandbox.
 *
 * <p>Implementations are chosen by {@link #language()}, so support for another language means adding
 * an executor rather than changing the calling code.
 */
public interface LanguageExecutor {

    ProgrammingLanguage language();

    /**
     * Compiles and runs {@code sourceCode}, capturing its output.
     *
     * <p>Implementations must never run submitted code in the server process: the sandbox boundary is
     * a security requirement, not an optimization.
     */
    ExecutionResult execute(String sourceCode);
}
