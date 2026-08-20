import { useEffect, useState } from 'react'

type Board = number[][]

type AuthMode = 'login' | 'signup'
type OpponentType = 'computer' | 'user'
type GameSymbol = 'X' | 'O'

type GameState = {
  type: 'waiting' | 'turn' | 'draw' | 'win'
  playerId?: string | null
}

type GamePlayer = {
  userId: string
  symbol: GameSymbol
}

type Game = {
  id: string
  createdAt: string
  board: {
    cells: Board
  }
  state: GameState
  players: GamePlayer[]
  computerSymbol: GameSymbol | null
}

type UserProfile = {
  id: string
  login: string
}

type TopPlayer = {
  userId: string
  login: string
  winRatio: number
}

type AuthState = {
  login: string
  userId: string
  accessToken: string
  refreshToken: string
}

type JwtResponse = {
  type: string
  accessToken: string
  refreshToken: string
}

const emptyCell = 0
const authStorageKey = 'tictactoe-auth'
const activeGameStorageKey = 'tictactoe-active-game-id'
const computerPlayerId = '00000000-0000-0000-0000-000000000001'

function App() {
  const [authMode, setAuthMode] = useState<AuthMode>('login')
  const [auth, setAuth] = useState<AuthState | null>(null)
  const [login, setLogin] = useState('')
  const [password, setPassword] = useState('')
  const [activeGame, setActiveGame] = useState<Game | null>(null)
  const [availableGames, setAvailableGames] = useState<Game[]>([])
  const [completedGames, setCompletedGames] = useState<Game[]>([])
  const [leaderboard, setLeaderboard] = useState<TopPlayer[]>([])
  const [userProfiles, setUserProfiles] = useState<Record<string, UserProfile>>({})
  const [error, setError] = useState<string | null>(null)
  const [isBusy, setIsBusy] = useState(false)
  const [isLeaderboardOpen, setIsLeaderboardOpen] = useState(false)
  const [isHistoryOpen, setIsHistoryOpen] = useState(false)

  useEffect(() => {
    const storedAuth = loadStoredAuth()
    if (!storedAuth) {
      return
    }

    setAuth(storedAuth)
    setLogin(storedAuth.login)
    void bootstrapSession(storedAuth)
  }, [])

  useEffect(() => {
    if (!auth) {
      return
    }

    void refreshAvailableGames(auth)
    const intervalId = window.setInterval(() => {
      void refreshAvailableGames(auth)
    }, 4000)

    return () => window.clearInterval(intervalId)
  }, [auth])

  useEffect(() => {
    if (!auth || !activeGame) {
      return
    }

    const intervalId = window.setInterval(() => {
      void refreshActiveGame(activeGame.id, auth)
    }, 2000)

    return () => window.clearInterval(intervalId)
  }, [auth, activeGame])

  useEffect(() => {
    if (!isLeaderboardOpen && !isHistoryOpen) {
      return
    }

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsLeaderboardOpen(false)
        setIsHistoryOpen(false)
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isHistoryOpen, isLeaderboardOpen])

  useEffect(() => {
    if (!auth) {
      return
    }

    const userIds = new Set<string>()
    if (activeGame?.state.playerId) {
      userIds.add(activeGame.state.playerId)
    }
    activeGame?.players.forEach((player) => userIds.add(player.userId))
    availableGames.forEach((game) => {
      if (game.state.playerId) {
        userIds.add(game.state.playerId)
      }
      game.players.forEach((player) => userIds.add(player.userId))
    })
    completedGames.forEach((game) => {
      if (game.state.playerId) {
        userIds.add(game.state.playerId)
      }
      game.players.forEach((player) => userIds.add(player.userId))
    })

    const missingUserIds = [...userIds].filter((userId) => !userProfiles[userId])
    if (missingUserIds.length === 0) {
      return
    }

    void Promise.all(missingUserIds.map((userId) => loadUserProfile(userId)))
  }, [auth, activeGame, availableGames, completedGames, userProfiles])

  async function bootstrapSession(authState: AuthState) {
    setError(null)

    const storedGameId = loadStoredGameId()
    if (storedGameId) {
      const restoredGame = await fetchGame(storedGameId, authState)
      if (restoredGame) {
        setActiveGame(restoredGame)
      } else {
        clearStoredGameId()
        setActiveGame(null)
      }
    }

    await refreshAvailableGames(authState)
    await refreshCompletedGames(authState)
  }

  async function handleAuthSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()

    setIsBusy(true)
    setError(null)

    try {
      if (authMode === 'signup') {
        const signUpResponse = await fetch('/signup', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ login, password }),
        })

        if (!signUpResponse.ok) {
          const payload = (await signUpResponse.json().catch(() => null)) as { error?: string } | null
          throw new Error(payload?.error ?? 'Failed to sign up')
        }
      }

      const nextAuth = await loginUser(login, password)
      setAuth(nextAuth)
      saveStoredAuth(nextAuth)
      await bootstrapSession(nextAuth)
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Unexpected error')
    } finally {
      setIsBusy(false)
    }
  }

  async function createGame(opponentType: OpponentType) {
    if (!auth) {
      return
    }

    setIsBusy(true)
    setError(null)

    try {
      const response = await fetch('/game', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...createAuthHeaders(auth.accessToken),
        },
        body: JSON.stringify({ opponentType }),
      })

      if (response.status === 401) {
        await handleUnauthorized(auth)
        return
      }

      if (!response.ok) {
        const payload = (await response.json().catch(() => null)) as { error?: string } | null
        throw new Error(payload?.error ?? 'Failed to create game')
      }

      const game = (await response.json()) as Game
      setActiveGame(game)
      saveStoredGameId(game.id)
      await refreshAvailableGames(auth)
      await refreshCompletedGames(auth)
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Unexpected error')
    } finally {
      setIsBusy(false)
    }
  }

  async function joinGame(gameId: string) {
    if (!auth) {
      return
    }

    setIsBusy(true)
    setError(null)

    try {
      const response = await fetch(`/game/${gameId}/join`, {
        method: 'POST',
        headers: createAuthHeaders(auth.accessToken),
      })

      if (response.status === 401) {
        await handleUnauthorized(auth)
        return
      }

      if (!response.ok) {
        const payload = (await response.json().catch(() => null)) as { error?: string } | null
        throw new Error(payload?.error ?? 'Failed to join game')
      }

      const game = (await response.json()) as Game
      setActiveGame(game)
      saveStoredGameId(game.id)
      await refreshAvailableGames(auth)
      await refreshCompletedGames(auth)
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Unexpected error')
    } finally {
      setIsBusy(false)
    }
  }

  async function handleMove(rowIndex: number, columnIndex: number) {
    if (!auth || !activeGame || isBusy) {
      return
    }

    if (activeGame.board.cells[rowIndex][columnIndex] !== emptyCell) {
      return
    }

    if (activeGame.state.type !== 'turn' || activeGame.state.playerId !== auth.userId) {
      return
    }

    setIsBusy(true)
    setError(null)

    try {
      const response = await fetch(`/game/${activeGame.id}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...createAuthHeaders(auth.accessToken),
        },
        body: JSON.stringify({ rowIndex, columnIndex }),
      })

      if (response.status === 401) {
        await handleUnauthorized(auth)
        return
      }

      if (!response.ok) {
        const payload = (await response.json().catch(() => null)) as { error?: string } | null
        throw new Error(payload?.error ?? 'Move was rejected')
      }

      const updatedGame = (await response.json()) as Game
      setActiveGame(updatedGame)
      saveStoredGameId(updatedGame.id)
      await refreshCompletedGames(auth)
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Unexpected error')
    } finally {
      setIsBusy(false)
    }
  }

  async function refreshAvailableGames(authState = auth) {
    if (!authState) {
      return
    }

    try {
      setError(null)
      const response = await fetch('/game', {
        headers: createAuthHeaders(authState.accessToken),
      })

      if (response.status === 401) {
        await handleUnauthorized(authState)
        return
      }

      if (response.status === 404 || response.status === 204) {
        setAvailableGames([])
        return
      }

      if (!response.ok) {
        setAvailableGames([])
        return
      }

      const games = (await response.json()) as Game[]
      setAvailableGames(games)
    } catch {
      setAvailableGames([])
    }
  }

  async function refreshCompletedGames(authState = auth) {
    if (!authState) {
      return
    }

    try {
      setError(null)
      const response = await fetch('/game/history', {
        headers: createAuthHeaders(authState.accessToken),
      })

      if (response.status === 401) {
        await handleUnauthorized(authState)
        return
      }

      if (response.status === 404 || response.status === 204) {
        setCompletedGames([])
        return
      }

      if (!response.ok) {
        setCompletedGames([])
        return
      }

      const games = (await response.json()) as Game[]
      setCompletedGames(games)
    } catch {
      setCompletedGames([])
    }
  }

  async function loadLeaderboard(authState = auth) {
    if (!authState) {
      return
    }

    try {
      setError(null)
      const response = await fetch('/game/leaderboard?limit=10', {
        headers: createAuthHeaders(authState.accessToken),
      })

      if (response.status === 401) {
        await handleUnauthorized(authState)
        return
      }

      if (response.status === 404 || response.status === 204) {
        setLeaderboard([])
        return
      }

      if (!response.ok) {
        setLeaderboard([])
        return
      }

      const players = (await response.json()) as TopPlayer[]
      setLeaderboard(players)
    } catch (requestError) {
      setLeaderboard([])
    }
  }

  async function refreshActiveGame(gameId: string, authState = auth) {
    if (!authState) {
      return
    }

    const refreshedGame = await fetchGame(gameId, authState)
    if (!refreshedGame) {
      setActiveGame(null)
      clearStoredGameId()
      return
    }

    setActiveGame(refreshedGame)
  }

  async function fetchGame(gameId: string, authState: AuthState): Promise<Game | null> {
    try {
      const response = await fetch(`/game/${gameId}`, {
        headers: createAuthHeaders(authState.accessToken),
      })

      if (response.status === 401) {
        await handleUnauthorized(authState)
        return null
      }

      if (response.status === 404) {
        return null
      }

      if (!response.ok) {
        throw new Error('Failed to load game')
      }

      return (await response.json()) as Game
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Unexpected error')
      return null
    }
  }

  async function loadUserProfile(userId: string) {
    if (!auth || userId === computerPlayerId) {
      return
    }

    try {
      const response = await fetch(`/user/${userId}`, {
        headers: createAuthHeaders(auth.accessToken),
      })

      if (response.status === 401) {
        await handleUnauthorized(auth)
        return
      }

      if (!response.ok) {
        return
      }

      const profile = (await response.json()) as UserProfile
      setUserProfiles((currentProfiles) => {
        if (currentProfiles[profile.id]) {
          return currentProfiles
        }

        return {
          ...currentProfiles,
          [profile.id]: profile,
        }
      })
    } catch {
      return
    }
  }

  function handleLogout() {
    clearStoredAuth()
    clearStoredGameId()
    setAuth(null)
    setActiveGame(null)
    setAvailableGames([])
    setCompletedGames([])
    setLeaderboard([])
    setUserProfiles({})
    setError(null)
    setIsLeaderboardOpen(false)
    setIsHistoryOpen(false)
  }

  async function handleUnauthorized(authState: AuthState) {
    const refreshedAuth = await refreshAccessToken(authState)
    if (refreshedAuth) {
      setAuth(refreshedAuth)
      saveStoredAuth(refreshedAuth)
      return
    }

    handleLogout()
    setError('Authorization failed. Please sign in again.')
  }

  if (!auth) {
    return (
      <main className="page authPage">
        <section className="authScreen">
          <div className="authScreenIntro">
            <h1>Tic-Tac-Toe</h1>
            <p className="subtitle">Create an account or sign in to play against the computer or another player.</p>
          </div>

          <section className="authCard authCard-screen">
            <div className="authTabs" role="tablist" aria-label="Authentication mode">
              <button
                className={`authTab ${authMode === 'login' ? 'authTab-active' : ''}`}
                onClick={() => setAuthMode('login')}
                disabled={isBusy}
                type="button"
              >
                Login
              </button>
              <button
                className={`authTab ${authMode === 'signup' ? 'authTab-active' : ''}`}
                onClick={() => setAuthMode('signup')}
                disabled={isBusy}
                type="button"
              >
                Sign up
              </button>
            </div>

            <form className="authForm authForm-screen" onSubmit={(event) => void handleAuthSubmit(event)}>
              <label className="field">
                <span className="fieldLabel">Login</span>
                <input
                  className="textInput"
                  value={login}
                  onChange={(event) => setLogin(event.target.value)}
                  disabled={isBusy}
                  autoComplete="username"
                  required
                />
              </label>
              <label className="field">
                <span className="fieldLabel">Password</span>
                <input
                  className="textInput"
                  type="password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  disabled={isBusy}
                  autoComplete={authMode === 'login' ? 'current-password' : 'new-password'}
                  required
                />
              </label>
              <button className="primaryButton authButton" disabled={isBusy} type="submit">
                {isBusy ? 'Please wait...' : authMode === 'login' ? 'Login' : 'Create account'}
              </button>
            </form>

            {error ? <div className="errorBox authErrorBox">{error}</div> : null}
          </section>
        </section>
      </main>
    )
  }

  const board = activeGame?.board.cells ?? createEmptyBoard()
  const playerSymbol = activeGame?.players.find((player) => player.userId === auth.userId)?.symbol ?? null
  const status = describeGameStatus(activeGame, auth.userId, userProfiles)

  return (
    <main className="page">
      <section className="panel panel-wide">
        <div className="hero">
          <div>
            <h1>Tic-Tac-Toe</h1>
            <p className="subtitle">Launch a private game against the AI or open a lobby and wait for another player.</p>
          </div>

          <div className="heroActions">
            <button
              className="ghostButton"
              onClick={() => {
                setIsHistoryOpen(true)
                void refreshCompletedGames()
              }}
              disabled={isBusy}
            >
              Game history
            </button>
            <button
              className="ghostButton"
              onClick={() => {
                setIsLeaderboardOpen(true)
                void loadLeaderboard()
              }}
              disabled={isBusy}
            >
              Leaderboard
            </button>
            <button className="ghostButton" onClick={handleLogout} disabled={isBusy}>
              Logout
            </button>
          </div>
        </div>

        <div className="actionGrid">
          <button className="primaryButton" onClick={() => void createGame('computer')} disabled={isBusy}>
            Play vs computer
          </button>
          <button className="secondaryButton" onClick={() => void createGame('user')} disabled={isBusy}>
            Open multiplayer lobby
          </button>
          <button className="ghostButton" onClick={() => void refreshAvailableGames()} disabled={isBusy}>
            Refresh lobby
          </button>
        </div>

        <div className="metaGrid">
          <div className="card">
            <span className="label">Current Game</span>
            <span className="value mono">{activeGame?.id ?? 'No active game selected'}</span>
          </div>
          <div className="card">
            <span className="label">Created</span>
            <span className="value">{activeGame ? formatDate(activeGame.createdAt) : 'No active game selected'}</span>
          </div>
          <div className="card">
            <span className="label">Status</span>
            <span className="value">{status}</span>
          </div>
          <div className="card">
            <span className="label">Player</span>
            <span className="value">{auth.login}</span>
          </div>
          <div className="card">
            <span className="label">Symbol</span>
            <span className="value">{playerSymbol ?? 'Join a game to get a symbol'}</span>
          </div>
        </div>

        {error ? <div className="errorBox">{error}</div> : null}

        <div className="workspaceGrid">
          <section className="card boardCard">
            <div className="sectionHeader">
              <h2>Board</h2>
              {activeGame ? (
                <button className="ghostButton ghostButton-small" onClick={() => void refreshActiveGame(activeGame.id)} disabled={isBusy}>
                  Refresh game
                </button>
              ) : null}
            </div>

            {activeGame ? (
              <>
                <div className="board" aria-label="Tic-tac-toe board">
                  {board.map((row, rowIndex) =>
                    row.map((cell, columnIndex) => (
                      <button
                        key={`${rowIndex}-${columnIndex}`}
                        className={`cell cell-${cell}`}
                        onClick={() => void handleMove(rowIndex, columnIndex)}
                        disabled={
                          isBusy ||
                          cell !== emptyCell ||
                          activeGame.state.type !== 'turn' ||
                          activeGame.state.playerId !== auth.userId
                        }
                      >
                        {renderCell(cell)}
                      </button>
                    )),
                  )}
                </div>

                <div className="legend">
                  <span><strong>Your symbol</strong> = {playerSymbol ?? '?'}</span>
                  {activeGame.computerSymbol ? <span><strong>Computer</strong> = {activeGame.computerSymbol}</span> : null}
                </div>
              </>
            ) : (
              <div className="emptyState">
                Create a game or join one from the lobby to start playing.
              </div>
            )}
          </section>

          <section className="card lobbyCard">
            <div className="sectionHeader">
              <h2>Open Lobbies</h2>
              <span className="label">Waiting for a second player</span>
            </div>

            {availableGames.length === 0 ? (
              <div className="emptyState">No multiplayer lobbies are open right now.</div>
            ) : (
              <div className="gameList">
                {availableGames.map((game) => {
                  const host = game.players[0]
                  const hostName = resolvePlayerName(host.userId, game, userProfiles, auth.userId)

                  return (
                    <article key={game.id} className="gameListItem">
                      <div>
                        <div className="value">{hostName}</div>
                        <div className="label">Created {formatDate(game.createdAt)}</div>
                        <div className="label mono">{game.id}</div>
                      </div>
                      <button className="secondaryButton secondaryButton-small" onClick={() => void joinGame(game.id)} disabled={isBusy}>
                        Join
                      </button>
                    </article>
                  )
                })}
              </div>
            )}
          </section>

        </div>
      </section>

      {isLeaderboardOpen ? (
        <div className="modalOverlay" onClick={() => setIsLeaderboardOpen(false)} role="presentation">
          <section
            className="modalCard"
            role="dialog"
            aria-modal="true"
            aria-labelledby="leaderboard-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="sectionHeader">
              <div>
                <p className="eyebrow">Top Players</p>
                <h2 id="leaderboard-title">Leaderboard</h2>
              </div>
              <div className="heroActions">
                <button className="ghostButton ghostButton-small" onClick={() => void loadLeaderboard()} disabled={isBusy}>
                  Refresh
                </button>
                <button className="ghostButton ghostButton-small" onClick={() => setIsLeaderboardOpen(false)}>
                  Close
                </button>
              </div>
            </div>

            {leaderboard.length === 0 ? (
              <div className="emptyState">No leaderboard data yet.</div>
            ) : (
              <div className="leaderboardTableWrap">
                <table className="leaderboardTable">
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>Player</th>
                      <th>Win ratio</th>
                    </tr>
                  </thead>
                  <tbody>
                    {leaderboard.map((player, index) => (
                      <tr key={player.userId}>
                        <td>{index + 1}</td>
                        <td>{player.userId === auth.userId ? 'You' : player.login}</td>
                        <td>{formatRatio(player.winRatio)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </div>
      ) : null}

      {isHistoryOpen ? (
        <div className="modalOverlay" onClick={() => setIsHistoryOpen(false)} role="presentation">
          <section
            className="modalCard"
            role="dialog"
            aria-modal="true"
            aria-labelledby="history-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="sectionHeader">
              <div>
                <p className="eyebrow">Completed Matches</p>
                <h2 id="history-title">Game History</h2>
              </div>
              <div className="heroActions">
                <button className="ghostButton ghostButton-small" onClick={() => void refreshCompletedGames()} disabled={isBusy}>
                  Refresh
                </button>
                <button className="ghostButton ghostButton-small" onClick={() => setIsHistoryOpen(false)}>
                  Close
                </button>
              </div>
            </div>

            {completedGames.length === 0 ? (
              <div className="emptyState">No game history yet.</div>
            ) : (
              <div className="gameList">
                {completedGames.map((game) => (
                  <article key={game.id} className="gameListItem gameListItem-stack">
                    <div>
                      <div className="value">{describeGameStatus(game, auth.userId, userProfiles)}</div>
                      <div className="label">{formatDate(game.createdAt)}</div>
                      <div className="label mono">{game.id}</div>
                    </div>
                    <button
                      className="ghostButton ghostButton-small"
                      onClick={() => {
                        setActiveGame(game)
                        saveStoredGameId(game.id)
                        setIsHistoryOpen(false)
                      }}
                      disabled={isBusy}
                    >
                      Open
                    </button>
                  </article>
                ))}
              </div>
            )}
          </section>
        </div>
      ) : null}
    </main>
  )
}

async function loginUser(login: string, password: string): Promise<AuthState> {
  const response = await fetch('/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ login, password }),
  })

  if (!response.ok) {
    throw new Error(response.status === 401 ? 'Invalid login or password' : 'Failed to log in')
  }

  const payload = (await response.json()) as JwtResponse
  const userProfileResponse = await fetch('/user', {
    headers: createAuthHeaders(payload.accessToken),
  })

  if (!userProfileResponse.ok) {
    throw new Error('Failed to load current user')
  }

  const profile = (await userProfileResponse.json()) as UserProfile

  return {
    login,
    userId: profile.id,
    accessToken: payload.accessToken,
    refreshToken: payload.refreshToken,
  }
}

async function refreshAccessToken(auth: AuthState): Promise<AuthState | null> {
  try {
    const response = await fetch('/login/access-token', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ refreshToken: auth.refreshToken }),
    })

    if (!response.ok) {
      return null
    }

    const payload = (await response.json()) as JwtResponse
    return {
      ...auth,
      accessToken: payload.accessToken,
      refreshToken: payload.refreshToken,
    }
  } catch {
    return null
  }
}

function createAuthHeaders(accessToken: string) {
  return {
    Authorization: `Bearer ${accessToken}`,
  }
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function formatRatio(value: number) {
  return value.toFixed(2)
}

function describeGameStatus(game: Game | null, authUserId: string, userProfiles: Record<string, UserProfile>) {
  if (!game) {
    return 'Create a game or join a waiting lobby.'
  }

  if (game.state.type === 'waiting') {
    return 'Waiting for another player to join.'
  }

  if (game.state.type === 'draw') {
    return 'Draw.'
  }

  if (game.state.type === 'turn') {
    return game.state.playerId === authUserId
      ? 'Your turn.'
      : `${resolvePlayerName(game.state.playerId ?? '', game, userProfiles, authUserId)} is thinking or choosing a move.`
  }

  if (game.state.type === 'win') {
    return game.state.playerId === authUserId
      ? 'You win.'
      : `${resolvePlayerName(game.state.playerId ?? '', game, userProfiles, authUserId)} wins.`
  }

  return 'Game in progress.'
}

function resolvePlayerName(userId: string, game: Game, userProfiles: Record<string, UserProfile>, authUserId: string) {
  if (!userId) {
    return 'Unknown player'
  }

  if (userId === authUserId) {
    return 'You'
  }

  if (game.computerSymbol && !game.players.some((player) => player.userId === userId)) {
    return 'Computer'
  }

  const knownProfile = userProfiles[userId]
  if (knownProfile) {
    return knownProfile.login
  }

  return game.players.some((player) => player.userId === userId) ? 'Opponent' : 'Unknown player'
}

function loadStoredAuth(): AuthState | null {
  const rawValue = window.localStorage.getItem(authStorageKey)
  if (!rawValue) {
    return null
  }

  try {
    return JSON.parse(rawValue) as AuthState
  } catch {
    window.localStorage.removeItem(authStorageKey)
    return null
  }
}

function saveStoredAuth(auth: AuthState) {
  window.localStorage.setItem(authStorageKey, JSON.stringify(auth))
}

function clearStoredAuth() {
  window.localStorage.removeItem(authStorageKey)
}

function loadStoredGameId() {
  return window.localStorage.getItem(activeGameStorageKey)
}

function saveStoredGameId(gameId: string) {
  window.localStorage.setItem(activeGameStorageKey, gameId)
}

function clearStoredGameId() {
  window.localStorage.removeItem(activeGameStorageKey)
}

function renderCell(cell: number) {
  if (cell === 1) {
    return 'X'
  }

  if (cell === 2) {
    return 'O'
  }

  return ''
}

function createEmptyBoard() {
  return Array.from({ length: 3 }, () => Array.from({ length: 3 }, () => emptyCell))
}

export default App
