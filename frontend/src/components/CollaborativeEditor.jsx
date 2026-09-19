import Editor from '@monaco-editor/react'
import { useEffect, useRef, useState } from 'react'
import * as Y from 'yjs'
import { MonacoBinding } from 'y-monaco'
import { StompYjsProvider, colorForUser } from '../lib/yjsProvider'

export const DEFAULT_SOURCE = `public class Main {
    public static void main(String[] args) {
        System.out.println("Hello from Hive!");

        for (int i = 0; i < 5; i++) {
            System.out.println("Count: " + i);
        }
    }
}
`

export default function CollaborativeEditor({
  client,
  connected,
  roomId,
  user,
  canEdit,
  initialContent,
  onContentChange,
  onPeersChange,
  language = 'java',
}) {
  const [editor, setEditor] = useState(null)
  const contentChangeRef = useRef(onContentChange)
  contentChangeRef.current = onContentChange
  const peersChangeRef = useRef(onPeersChange)
  peersChangeRef.current = onPeersChange

  useEffect(() => {
    if (!editor || !client || !connected || !user) return undefined

    const doc = new Y.Doc({ gc: true })
    const text = doc.getText('monaco')
    if (initialContent) text.insert(0, initialContent)

    let decorations = []
    const provider = new StompYjsProvider({
      client,
      roomId,
      doc,
      canEdit,
      userId: user.id,
      username: user.username,
      onAwareness: (peers) => {
        peersChangeRef.current?.(peers)
        decorations = paintRemoteCursors(editor, peers, decorations)
      },
    })

    const binding = new MonacoBinding(text, editor.getModel(), new Set([editor]))
    const report = () => contentChangeRef.current?.(text.toString() || DEFAULT_SOURCE)
    text.observe(report)
    report()

    const cursorListener = editor.onDidChangeCursorPosition((event) =>
      provider.publishAwareness({ line: event.position.lineNumber, column: event.position.column }),
    )

    return () => {
      cursorListener.dispose()
      binding.destroy()
      provider.destroy()
      doc.destroy()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editor, client, connected, roomId, user, canEdit])

  return (
    <Editor
      height="100%"
      language={language?.toLowerCase() || 'java'}
      defaultLanguage={language?.toLowerCase() || 'java'}
      theme="better-stack-dark"
      value={undefined}
      beforeMount={defineBetterStackTheme}
      onMount={setEditor}
      loading={<div className="p-6 text-xs text-muted-steel font-mono">Initializing Monaco editor…</div>}
      options={{
        readOnly: !canEdit,
        fontSize: 13,
        fontFamily: '"JetBrains Mono", ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
        minimap: { enabled: true, maxColumn: 60, renderCharacters: false },
        scrollBeyondLastLine: false,
        smoothScrolling: true,
        automaticLayout: true,
        padding: { top: 12, bottom: 12 },
        renderLineHighlight: canEdit ? 'line' : 'none',
        lineNumbersMinChars: 3,
        scrollbar: { verticalScrollbarSize: 8, horizontalScrollbarSize: 8 },
      }}
    />
  )
}

function defineBetterStackTheme(monaco) {
  monaco.editor.defineTheme('better-stack-dark', {
    base: 'vs-dark',
    inherit: true,
    rules: [
      { token: 'keyword', foreground: '98a4f7', fontStyle: 'bold' },
      { token: 'string', foreground: '34d399' },
      { token: 'comment', foreground: '646e87', fontStyle: 'italic' },
      { token: 'number', foreground: 'f59e0b' },
      { token: 'type', foreground: 'c9d3ee' },
      { token: 'delimiter', foreground: '939db8' },
    ],
    colors: {
      'editor.background': '#0f101a',
      'editorGutter.background': '#0f101a',
      'editor.lineHighlightBackground': '#151621',
      'editorLineNumber.foreground': '#353a4d',
      'editorLineNumber.activeForeground': '#c9d3ee',
      'editor.selectionBackground': '#2e3458',
      'editorCursor.foreground': '#98a4f7',
      'editorIndentGuide.background1': '#191b29',
      'editorIndentGuide.activeBackground1': '#26293d',
    },
  })
}

function paintRemoteCursors(editor, peers, previous) {
  const model = editor.getModel()
  if (!model) return previous
  const lineCount = model.getLineCount()
  const range = (line, column) => ({
    startLineNumber: Math.min(Math.max(1, line), lineCount),
    startColumn: Math.max(1, column ?? 1),
    endLineNumber: Math.min(Math.max(1, line), lineCount),
    endColumn: Math.max(2, (column ?? 1) + 1),
  })

  return editor.deltaDecorations(
    previous,
    peers.slice(0, 24).map((peer) => ({
      range: range(peer.cursor?.line ?? 1, peer.cursor?.column),
      options: {
        className: 'remote-caret',
        after: {
          content: ` ${peer.name ?? 'collaborator'}`,
          inlineClassName: 'remote-caret-label',
        },
        overviewRuler: { color: colorForUser(peer.userId), position: 4 },
      },
    })),
  )
}
