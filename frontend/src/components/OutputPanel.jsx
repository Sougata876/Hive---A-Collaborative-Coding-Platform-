import { useState } from 'react'
import { Badge, ErrorText, Spinner } from './ui'

const RATE_LIMIT = /rate.?limit|too many requests|429/i

function statusMeta(result, running) {
  if (running) return { tone: 'link', label: 'Running', spinner: true }
  if (!result) return { tone: 'zinc', label: 'Idle' }
  switch (result.status) {
    case 'SUCCESS':
      return { tone: 'green', label: 'Exit 0' }
    case 'FAILED':
      return {
        tone: 'red',
        label: typeof result.exitCode === 'number' ? `Exit ${result.exitCode}` : 'Failed',
      }
    case 'TIMEOUT':
      return { tone: 'red', label: 'Timed out' }
    case 'ERROR':
      return { tone: 'red', label: 'Sandbox error' }
    default:
      return { tone: 'zinc', label: result.status ?? 'Unknown' }
  }
}

/**
 * Output / Terminal panel matching Screen 6:
 * Tabs: Output (active), Error
 * Clear button
 * Formatted console output with exit code.
 */
export default function OutputPanel({ result, running, error, canRun, onClear }) {
  const [activeTab, setActiveTab] = useState('output') // 'output' | 'error'
  const status = statusMeta(result, running)
  const hasStdout = Boolean(result?.stdout)
  const hasStderr = Boolean(result?.stderr)
  const nothingToClear = !result && !error

  return (
    <section className="flex h-full min-h-0 flex-col gap-2" aria-label="Execution output">
      {/* Console Header Bar */}
      <header className="flex items-center justify-between gap-2 border-b border-gunmetal/80 pb-2">
        {/* Tabs: Output, Error */}
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setActiveTab('output')}
            className={`rounded-md px-2.5 py-1 text-xs font-medium transition-colors ${
              activeTab === 'output'
                ? 'bg-carbon-surface text-pure-white border border-gunmetal'
                : 'text-muted-steel hover:text-frost'
            }`}
          >
            Output
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('error')}
            className={`flex items-center gap-1.5 rounded-md px-2.5 py-1 text-xs font-medium transition-colors ${
              activeTab === 'error'
                ? 'bg-carbon-surface text-red-400 border border-gunmetal'
                : 'text-muted-steel hover:text-red-400'
            }`}
          >
            Error
            {(hasStderr || error) && <span className="h-1.5 w-1.5 rounded-full bg-red-400" />}
          </button>
        </div>

        <div className="flex items-center gap-2">
          {typeof result?.durationMs === 'number' && (
            <span className="text-[11px] text-muted-steel font-mono">{result.durationMs}ms</span>
          )}
          <Badge tone={status.tone}>
            {status.spinner && <Spinner className="h-3 w-3 text-periwinkle-glow" />}
            {status.label}
          </Badge>
          <button
            type="button"
            onClick={onClear}
            disabled={running || nothingToClear}
            className="flex items-center gap-1 rounded-md px-2 py-0.5 text-xs text-muted-steel hover:text-pure-white disabled:opacity-40 transition-colors"
          >
            <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
            Clear
          </button>
        </div>
      </header>

      {/* Terminal View */}
      <div
        aria-live="polite"
        aria-busy={running || undefined}
        className="min-h-0 flex-1 overflow-auto rounded-lg border border-gunmetal/80 bg-[#08090f] p-3 font-mono text-xs leading-relaxed text-frost select-text"
      >
        {running ? (
          <div className="flex h-full items-center justify-center gap-2 text-muted-steel">
            <Spinner className="h-4 w-4 text-periwinkle-glow" />
            <span>Compiling and executing in isolated Docker sandbox…</span>
          </div>
        ) : (
          <>
            {activeTab === 'error' && (
              <div>
                {error && (
                  <div>
                    <ErrorText>{error}</ErrorText>
                    {RATE_LIMIT.test(error) && (
                      <p className="mt-1 text-xs text-muted-steel">
                        This room reached its per-minute run limit. Please wait a moment before trying again.
                      </p>
                    )}
                  </div>
                )}
                {hasStderr && <div className="text-red-400 whitespace-pre-wrap">{result.stderr}</div>}
                {!error && !hasStderr && (
                  <div className="text-muted-steel">No errors reported.</div>
                )}
              </div>
            )}

            {activeTab === 'output' && (
              <div>
                {result?.status === 'TIMEOUT' && (
                  <p className="mb-2 text-red-400">
                    Execution exceeded the 10s time limit and the sandbox was terminated.
                  </p>
                )}
                {hasStdout && <div className="text-pure-white whitespace-pre-wrap">{result.stdout}</div>}
                {result && typeof result.exitCode === 'number' && (
                  <div className="mt-2 pt-2 border-t border-gunmetal/40 text-[11px] text-muted-steel font-mono">
                    Process exited with code {result.exitCode}
                  </div>
                )}
                {!result && !error && (
                  <div className="text-muted-steel">
                    {canRun
                      ? 'Press "Run ▶" above to compile and execute your code.'
                      : 'You have read-only access in this room.'}
                  </div>
                )}
              </div>
            )}
          </>
        )}
      </div>
    </section>
  )
}
