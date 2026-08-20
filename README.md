# TicTacToe
# ❌⭕ Tic-Tac-Toe Full-Stack Application

A full-stack **Tic-Tac-Toe** application built with **Kotlin/Ktor** on the backend and **React + TypeScript** on the frontend.

The project combines persistent storage, JWT-based authentication, multiplayer game sessions, player-vs-computer gameplay, match history, leaderboard statistics, and a browser-based user interface.

![UI_game_VS_computer](/img/game_vs_computer_ui.png)

---

## 📖 About the Project

The project started as a backend Tic-Tac-Toe server and gradually evolved into a complete full-stack application.

The backend provides:

* user registration and authentication;
* persistent storage in PostgreSQL;
* multiplayer game management;
* player-vs-computer games;
* JWT access and refresh tokens;
* game history;
* player leaderboard.

On top of the original assignment, I also implemented a **React frontend** that integrates the entire API into a playable browser application.

Users can:

* create an account;
* log in;
* start a game against the computer;
* open a multiplayer lobby;
* join other players' games;
* make moves directly on the board;
* view completed matches;
* browse the leaderboard;
* restore an active session after reloading the page.

---

## ✨ Features

### 👤 User Accounts

* User registration
* User login
* UUID-based user identity
* Persistent users in PostgreSQL
* Current-user profile retrieval
* User lookup by UUID

### 🔐 JWT Authentication

The final version of the project uses **JWT Bearer authentication**.

It supports:

* access tokens;
* refresh tokens;
* access-token validation;
* refresh-token validation;
* token renewal;
* user UUID stored in JWT claims;
* protected API endpoints;
* automatic access-token refresh on the frontend.

Only public authentication-related endpoints can be accessed without authorization.

---

## 🎮 Gameplay

The application supports both:

* **Player vs. Computer**
* **Player vs. Player**

Game functionality includes:

* game creation;
* multiplayer lobby creation;
* retrieving available games;
* joining a waiting game;
* retrieving the current game;
* submitting moves;
* player symbol assignment;
* turn validation;
* win detection;
* draw detection;
* explicit game states.

---

## 🧠 Game States

Each game has an explicit state:

```text
Waiting for players
        │
        ▼
  Player's turn
     │       │
     │       ├──────────► Draw
     │
     ├───────────────► You win
     │
     └───────────────► You lose
```

Internally, both win and loss are represented by the same win game state containing the winner's UUID. The frontend derives whether the authenticated user won or lost from that UUID.

---

## 🌐 Frontend

The frontend is built with **React and TypeScript** and provides a complete UI for the backend API.

### Authentication

The application provides separate login and signup flows.

After successful authentication:

* the access token is used for Bearer authentication;
* the refresh token is stored for token renewal;
* the authenticated user profile is loaded;
* the session is persisted in browser storage.

---

## 🔄 Session Management

Authentication state is persisted using `localStorage`.

The frontend stores:

* the authenticated user's login;
* user UUID;
* access token;
* refresh token;
* active game ID.

When the application starts, it restores the previous session and attempts to reload the active game.

If the access token expires, the frontend automatically tries to refresh it using the refresh token.

If token refresh fails, the local session is cleared and the user is logged out.

---

## 🕹️ Game UI

The frontend includes an interactive Tic-Tac-Toe board.

A move can only be submitted when:

* a game is active;
* the selected cell is empty;
* the game is in the `turn` state;
* the authenticated user is the current player.

The UI displays:

* current game ID;
* creation time;
* game status;
* current player;
* assigned symbol;
* board state;
* computer symbol when applicable.

---

## 👥 Multiplayer Lobby

Users can create a multiplayer lobby and wait for another player.

The frontend periodically refreshes the list of available games and displays:

* the host player;
* game creation time;
* game ID;
* a Join action.

The available-game list is refreshed automatically at regular intervals.

---

## 🔁 Live Game Updates

For active games, the frontend periodically reloads the current game state from the server.

This allows multiplayer users to see the opponent's moves without manually reloading the page.

```text
React Client
     │
     │ periodic GET
     ▼
Ktor REST API
     │
     ▼
Current Game State
     │
     ▼
Updated Board
```

---

## 📜 Game History

Authenticated users can view their completed matches.

The history interface displays:

* game result;
* creation date;
* game ID.

A completed game can also be reopened from the history view.

---

## 🏆 Leaderboard

The application provides a leaderboard based on player performance.

The backend calculates the win ratio for each player and returns the top N players sorted by performance.

The frontend displays:

* leaderboard position;
* login;
* win ratio.

---

## 🏗️ Architecture

```text
                    ┌──────────────────────┐
                    │   React Frontend     │
                    │     TypeScript       │
                    └──────────┬───────────┘
                               │
                         HTTP / JSON
                         Bearer JWT
                               │
                               ▼
                    ┌──────────────────────┐
                    │      Ktor API        │
                    │                      │
                    │ Auth / Users / Games │
                    │ History / Ranking    │
                    └──────────┬───────────┘
                               │
                  ┌────────────┴────────────┐
                  │                         │
                  ▼                         ▼
        ┌──────────────────┐      ┌──────────────────┐
        │   JwtProvider    │      │    Services      │
        │                  │      │                  │
        │ Access Token     │      │ UserService      │
        │ Refresh Token    │      │ GameService      │
        └──────────────────┘      │ AuthService      │
                                  └────────┬─────────┘
                                           │
                                           ▼
                                  ┌──────────────────┐
                                  │     Exposed      │
                                  └────────┬─────────┘
                                           │
                                           ▼
                                  ┌──────────────────┐
                                  │    PostgreSQL    │
                                  └──────────────────┘
```

---

## 🔐 Authentication Flow

```text
Client
  │
  │ login + password
  ▼
POST /login
  │
  ▼
AuthService
  │
  ├── validate credentials
  │
  ▼
JwtProvider
  │
  ├── accessToken
  └── refreshToken
  │
  ▼
React Client
```

For protected requests:

```text
Authorization: Bearer <accessToken>
```

When the server responds with `401 Unauthorized`, the frontend attempts to obtain a new access token using the refresh token.

---

## 💾 Persistence

Application state is stored in **PostgreSQL**.

Database access is implemented using **Exposed**.

Persistent data includes:

* users;
* games;
* participants;
* game state;
* creation timestamps;
* completed games;
* data used for leaderboard statistics.

---

## 🛠️ Tech Stack

### Backend

* **Kotlin**
* **Ktor**
* **PostgreSQL**
* **Exposed**
* **JWT**
* **Bearer Authentication**
* **REST API**
* **JSON**

### Frontend

* **React**
* **TypeScript**
* **Fetch API**
* **Browser Local Storage**

---

## 🚀 Build and Run

### Requirements

* Docker with Docker Compose (or Podman)
* Node.js and npm, only if you want to build the frontend locally outside Docker

### Run with Docker Compose

From the project root, build the application image and start the app with PostgreSQL:

```bash
docker compose up --build
```

To run it in the background:

```bash
docker compose up --build -d
```

The application will be available at:

```text
http://localhost:8080
```

PostgreSQL is exposed on host port `5433` and uses the credentials from `docker-compose.yml`.

### Rebuild the Frontend for Docker

The Docker image builds the React frontend inside the `frontend-builder` stage and copies `view/dist` into the final backend image. After changing frontend code, rebuild and recreate the app container:

```bash
docker compose build app
docker compose up -d --force-recreate app
```

If Docker reuses an old cached layer, rebuild without cache:

```bash
docker compose build --no-cache app
docker compose up -d --force-recreate app
```

Then hard-refresh the browser page.

### Build the Frontend Locally

To build only the frontend outside Docker:

```bash
cd view
npm ci
npm run build
```

This writes the frontend bundle to `view/dist`. The Docker image still needs to be rebuilt before Docker serves that updated bundle.

### Stop the Project

```bash
docker compose down
```

To remove the PostgreSQL volume as well:

```bash
docker compose down -v
```

---

## 🎯 What I Practiced

This project gave me hands-on experience with building and integrating a complete stateful web application.

### Backend

* designing a layered Kotlin backend;
* configuring PostgreSQL in Ktor;
* using Exposed for persistence;
* replacing in-memory storage with a relational database;
* implementing authentication and authorization;
* migrating from HTTP Basic Auth to JWT;
* generating and validating access and refresh tokens;
* using JWT claims;
* protecting API routes;
* modeling multiplayer game state;
* implementing PvP and PvE logic;
* querying completed user games;
* performing aggregation for leaderboard statistics.

### Frontend

* building a React application around an existing REST API;
* integrating authentication flows;
* managing JWT access and refresh tokens;
* restoring user sessions from browser storage;
* handling automatic token refresh;
* implementing multiplayer polling;
* managing async application state;
* building a multiplayer lobby;
* rendering game history and leaderboard data;
* coordinating frontend state with server-side game state.
