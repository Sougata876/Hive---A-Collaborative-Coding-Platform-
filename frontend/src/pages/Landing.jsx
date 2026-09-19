import { Link } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { Button, Card, HiveLogo } from '../components/ui'

const FEATURE_STRIP = [
  {
    title: 'Real-time Collaboration',
    icon: (
      <svg className="h-5 w-5 text-periwinkle-glow" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
      </svg>
    ),
  },
  {
    title: 'Live Chat',
    icon: (
      <svg className="h-5 w-5 text-periwinkle-glow" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
      </svg>
    ),
  },
  {
    title: 'Code Execution',
    icon: (
      <svg className="h-5 w-5 text-periwinkle-glow" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
    ),
  },
  {
    title: 'Room-based Workspaces',
    icon: (
      <svg className="h-5 w-5 text-periwinkle-glow" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
      </svg>
    ),
  },
  {
    title: 'Built for Developers',
    icon: (
      <svg className="h-5 w-5 text-periwinkle-glow" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
      </svg>
    ),
  },
]

const FEATURES_DETAILED = [
  {
    title: 'Conflict-Free Editing',
    description: 'Powered by Yjs CRDT and Monaco Editor. Type simultaneously on the same file without conflicting edits or race conditions.',
  },
  {
    title: 'Sandboxed Code Runner',
    description: 'Every code run spawns an ephemeral, isolated Docker container with strict CPU, memory, network, and timeout guards.',
  },
  {
    title: 'Strict Role-Based Security',
    description: 'Owner, Editor, and Viewer permissions enforced on every WebSocket frame, message publication, and REST request.',
  },
]

export default function Landing() {
  const { user } = useAuth()

  return (
    <div className="min-h-screen bg-void-black text-frost">
      {/* ─── Top Navigation Bar ────────────────────────────────────────── */}
      <header className="sticky top-0 z-40 border-b border-gunmetal bg-void-black/80 backdrop-blur-md">
        <div className="mx-auto flex h-14 max-w-6xl items-center justify-between px-4 sm:px-6">
          <Link to="/" className="transition-opacity hover:opacity-90">
            <HiveLogo size="md" />
          </Link>

          <nav className="hidden items-center gap-6 text-sm md:flex">
            <a href="#features" className="text-muted-steel transition-colors hover:text-pure-white">
              Features
            </a>
            <a href="#about" className="text-muted-steel transition-colors hover:text-pure-white">
              About
            </a>
            {user ? (
              <Link to="/rooms" className="text-muted-steel transition-colors hover:text-pure-white">
                My Rooms
              </Link>
            ) : (
              <Link to="/login" className="text-muted-steel transition-colors hover:text-pure-white">
                Login
              </Link>
            )}
          </nav>

          <div className="flex items-center gap-3">
            {user ? (
              <Link to="/rooms">
                <Button size="sm">Enter Workspace</Button>
              </Link>
            ) : (
              <>
                <Link to="/login" className="hidden sm:inline-block">
                  <Button variant="ghost" size="sm">
                    Login
                  </Button>
                </Link>
                <Link to="/register">
                  <Button size="sm">Get Started</Button>
                </Link>
              </>
            )}
          </div>
        </div>
      </header>

      {/* ─── Hero Section ──────────────────────────────────────────────── */}
      <main>
        <section className="relative overflow-hidden py-16 lg:py-24">
          <div
            aria-hidden="true"
            className="pointer-events-none absolute -top-40 left-1/2 -translate-x-1/2 h-[500px] w-[800px] rounded-full bg-[radial-gradient(circle,rgba(91,99,211,0.15)_0%,transparent_70%)]"
          />

          <div className="mx-auto max-w-6xl px-4 sm:px-6">
            <div className="grid items-center gap-12 lg:grid-cols-12">
              {/* Hero Left Column */}
              <div className="lg:col-span-6">
                <h1 className="text-4xl font-medium tracking-tight text-pure-white sm:text-5xl lg:text-6xl">
                  Welcome to Hive. <br />
                  Build together. <br />
                  <span className="text-transparent bg-clip-text bg-gradient-to-r from-[#98a4f7] to-[#7c87f7]">
                    In real time.
                  </span>
                </h1>
                <p className="mt-6 text-base leading-relaxed text-muted-steel sm:text-lg">
                  A modern collaborative coding platform with real-time editing, live chat, and secure code execution.
                </p>

                <div className="mt-8 flex flex-wrap items-center gap-4">
                  <Link to={user ? '/rooms' : '/register'}>
                    <Button size="lg">
                      Start Coding <span className="text-base">→</span>
                    </Button>
                  </Link>
                  {!user && (
                    <Link to="/login">
                      <Button variant="outline" size="lg">
                        Sign In
                      </Button>
                    </Link>
                  )}
                </div>
              </div>

              {/* Hero Right Column: Interactive Mockup Screen */}
              <div className="relative lg:col-span-6">
                <div className="relative rounded-2xl border border-gunmetal bg-carbon-surface shadow-2xl overflow-hidden inset-rim">
                  {/* Window Bar */}
                  <div className="flex items-center justify-between border-b border-gunmetal bg-[#12131d] px-4 py-2.5">
                    <div className="flex items-center gap-2">
                      <span className="h-2.5 w-2.5 rounded-full bg-[#ef4444]" />
                      <span className="h-2.5 w-2.5 rounded-full bg-[#eab308]" />
                      <span className="h-2.5 w-2.5 rounded-full bg-[#22c55e]" />
                    </div>
                    {/* Tab */}
                    <div className="flex items-center gap-2 rounded-t-md bg-carbon-surface px-3 py-1 text-xs text-pure-white border-t border-x border-gunmetal">
                      <span>Main.java</span>
                      <span className="text-muted-steel hover:text-pure-white cursor-pointer text-[10px]">✕</span>
                    </div>
                    <div className="w-10" />
                  </div>

                  {/* Floating 3-online popup badge matching screenshot */}
                  <div className="absolute right-4 top-14 z-20 w-44 rounded-xl border border-gunmetal bg-void-black/95 p-3 shadow-xl backdrop-blur-md">
                    <div className="flex items-center justify-between border-b border-gunmetal/60 pb-2 mb-2">
                      <span className="text-[11px] font-medium text-periwinkle-glow">3 online</span>
                      <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
                    </div>
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <span className="flex h-5 w-5 items-center justify-center rounded-full bg-blue-500 text-[9px] font-bold text-white">
                            A
                          </span>
                          <span className="text-xs text-pure-white">Alice</span>
                        </div>
                        <span className="rounded bg-emerald-500/15 px-1.5 py-0.5 text-[9px] font-medium text-emerald-400">
                          OWNER
                        </span>
                      </div>
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <span className="flex h-5 w-5 items-center justify-center rounded-full bg-emerald-500 text-[9px] font-bold text-white">
                            B
                          </span>
                          <span className="text-xs text-pure-white">Bob</span>
                        </div>
                        <span className="rounded bg-iris-blue/15 px-1.5 py-0.5 text-[9px] font-medium text-periwinkle-glow">
                          EDITOR
                        </span>
                      </div>
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <span className="flex h-5 w-5 items-center justify-center rounded-full bg-purple-500 text-[9px] font-bold text-white">
                            C
                          </span>
                          <span className="text-xs text-pure-white">Charlie</span>
                        </div>
                        <span className="rounded bg-purple-500/15 px-1.5 py-0.5 text-[9px] font-medium text-purple-300">
                          VIEWER
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Code Mockup with Live Cursors */}
                  <div className="relative bg-[#0b0c14] p-5 font-mono text-xs leading-relaxed select-none">
                    <div className="space-y-1">
                      <div className="flex">
                        <span className="w-6 text-right text-gunmetal select-none">1</span>
                        <span className="pl-4 text-periwinkle-glow">public class <span className="text-pure-white">Main</span> &#123;</span>
                      </div>
                      <div className="flex">
                        <span className="w-6 text-right text-gunmetal select-none">2</span>
                        <span className="pl-8 text-periwinkle-glow">public static void <span className="text-pure-white">main</span>(String[] args) &#123;</span>
                      </div>
                      <div className="flex relative">
                        <span className="w-6 text-right text-gunmetal select-none">3</span>
                        <span className="pl-12 text-secondary">
                          System.out.println(<span className="text-emerald-400">&quot;Hello from Hive!&quot;</span>);
                        </span>
                      </div>
                      <div className="flex">
                        <span className="w-6 text-right text-gunmetal select-none">4</span>
                        <span className="pl-8 text-frost">&#125;</span>
                      </div>
                      <div className="flex">
                        <span className="w-6 text-right text-gunmetal select-none">5</span>
                        <span className="pl-4 text-frost">&#125;</span>
                      </div>
                    </div>

                    {/* Remote Cursor Flags with Names Matching Screenshot */}
                    {/* Alice pointer */}
                    <div className="absolute left-[38%] top-[45%] flex flex-col items-start pointer-events-none transition-all duration-300">
                      <svg className="h-4 w-4 text-blue-500 drop-shadow" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M4 0l16 12-7 1.5 4.5 9-3 1.5-4.5-9-6 5.5v-20.5z" />
                      </svg>
                      <span className="-mt-1 rounded bg-blue-500 px-1.5 py-0.5 text-[9px] font-semibold text-white shadow">
                        Alice
                      </span>
                    </div>

                    {/* Bob pointer */}
                    <div className="absolute left-[62%] top-[65%] flex flex-col items-start pointer-events-none transition-all duration-300">
                      <svg className="h-4 w-4 text-emerald-400 drop-shadow" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M4 0l16 12-7 1.5 4.5 9-3 1.5-4.5-9-6 5.5v-20.5z" />
                      </svg>
                      <span className="-mt-1 rounded bg-emerald-500 px-1.5 py-0.5 text-[9px] font-semibold text-white shadow">
                        Bob
                      </span>
                    </div>

                    {/* Charlie pointer */}
                    <div className="absolute left-[32%] top-[78%] flex flex-col items-start pointer-events-none transition-all duration-300">
                      <svg className="h-4 w-4 text-purple-400 drop-shadow" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M4 0l16 12-7 1.5 4.5 9-3 1.5-4.5-9-6 5.5v-20.5z" />
                      </svg>
                      <span className="-mt-1 rounded bg-purple-500 px-1.5 py-0.5 text-[9px] font-semibold text-white shadow">
                        Charlie
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* ─── Bottom Feature Strip (5 Icons Matching Screenshot) ───────── */}
        <section className="border-y border-gunmetal bg-carbon-surface/60 py-8">
          <div className="mx-auto max-w-6xl px-4 sm:px-6">
            <div className="grid grid-cols-2 gap-6 sm:grid-cols-3 md:grid-cols-5 text-center">
              {FEATURE_STRIP.map((item) => (
                <div key={item.title} className="flex flex-col items-center justify-center p-3">
                  <div className="mb-2.5 flex h-10 w-10 items-center justify-center rounded-xl border border-gunmetal bg-void-black">
                    {item.icon}
                  </div>
                  <span className="text-xs font-medium text-pure-white">{item.title}</span>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* ─── Detailed Features Section ─────────────────────────────────── */}
        <section id="features" className="py-20">
          <div className="mx-auto max-w-6xl px-4 sm:px-6">
            <div className="text-center">
              <p className="text-xs font-medium uppercase tracking-wider text-periwinkle-glow">
                Architecture & Performance
              </p>
              <h2 className="mt-3 text-3xl font-medium text-pure-white">Engineered for low latency and security</h2>
              <p className="mx-auto mt-4 max-w-2xl text-sm text-muted-steel">
                Every component is built for speed and strict isolation — from conflict-free CRDT sync to ephemeral Docker execution sandboxes.
              </p>
            </div>

            <div className="mt-14 grid gap-6 md:grid-cols-3">
              {FEATURES_DETAILED.map((feat) => (
                <Card key={feat.title} className="p-6">
                  <h3 className="text-base font-medium text-pure-white">{feat.title}</h3>
                  <p className="mt-2 text-xs leading-relaxed text-muted-steel">{feat.description}</p>
                </Card>
              ))}
            </div>
          </div>
        </section>

        {/* ─── About Section ─────────────────────────────────────────────── */}
        <section id="about" className="border-t border-gunmetal py-16 bg-carbon-surface/30">
          <div className="mx-auto max-w-4xl px-4 text-center sm:px-6">
            <h2 className="text-2xl font-medium text-pure-white">Start building together today</h2>
            <p className="mt-3 text-sm text-muted-steel">
              Spin up a room in seconds, share an invite code, and collaborate with your team with zero configuration.
            </p>
            <div className="mt-8 flex justify-center">
              <Link to={user ? '/rooms' : '/register'}>
                <Button size="md">Get Started for Free →</Button>
              </Link>
            </div>
          </div>
        </section>
      </main>

      {/* ─── Footer ────────────────────────────────────────────────────── */}
      <footer className="border-t border-gunmetal bg-void-black py-8">
        <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-4 px-4 text-xs text-muted-steel sm:flex-row sm:px-6">
          <HiveLogo size="sm" />
          <p>© {new Date().getFullYear()} Hive. Built for developers.</p>
        </div>
      </footer>
    </div>
  )
}