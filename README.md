<h1 align="center">FlagDeck</h1>

<p align="center">
  A LaunchDarkly-inspired feature flag management platform for creating, targeting, rolling out, and measuring feature flags.
</p>

## Built With

- [React 19](https://react.dev/) and [Vite 8](https://vite.dev/) for the frontend
- [Bun](https://bun.sh/) for JavaScript workspace installation
- [JDK 21](https://openjdk.org/projects/jdk/21/) and [Spring Boot 3.4](https://spring.io/projects/spring-boot) for the HTTP API
- The committed [Gradle 9.7](https://gradle.org/) wrapper for backend builds and execution
- [MongoDB](https://www.mongodb.com/) and [Spring Data MongoDB](https://spring.io/projects/spring-data-mongodb) for persistence
- Feature validators and shared exception handling for request validation
- [Java JWT](https://github.com/auth0/java-jwt) and [bcrypt](https://github.com/patrickfav/bcrypt) for authentication

## Product overview

FlagDeck lets a team manage feature flags across environments, target specific users and
segments, roll out changes gradually, gate production changes behind a second person's
approval, measure experiments, and review or roll back the full change history of every flag.

### Capabilities

1. **Feature flag management** - Create boolean or multivariate flags with a key, description,
   default value, tags, and a temporary/permanent designation. Toggle, edit, or archive them
   from the flags list.
2. **Environment-based configuration** - Every project has its own Development, Staging, and
   Production environments, each with an independent flag configuration. Switch environments
   from the top bar to see (and edit) what each one currently serves.
3. **User and segment targeting** - Define reusable audience segments from user attributes
   (plan, country, custom attributes) or explicit key lists, then write ordered targeting
   rules that serve a variation to a named user or a whole segment.
4. **Progressive rollouts** - Split traffic across variations by percentage using a stable,
   deterministic per-user hash, so a given user always lands on the same variation. Raise the
   percentage over time and watch the live split.
5. **Approval workflows** - Any change to a **production** environment is proposed, not applied.
   A different teammate must approve, reject, or schedule it for a future time before it goes
   live; the requester cannot approve their own change.
6. **Experimentation and metrics** - Record exposure and conversion events per variation and
   compare conversion rate, relative lift, and statistical significance (a two-proportion
   z-test) side by side.
7. **Change history and rollbacks** - Every flag and configuration change is recorded with who
   changed what and when. Restore any earlier environment configuration in one click.

## Project Structure

```text
.
├── backend/                     # Spring Boot API, business logic, and MongoDB persistence
│   ├── src/main/java/           # Feature packages: auth, projects, environments, flags,
│   │                             #   audiences (segments/users), approvals, experiments, history
│   ├── src/main/resources/      # Application configuration
│   └── gradle/wrapper/          # Pinned Gradle wrapper runtime
├── frontend/
│   ├── src/features/            # Product views and interactions, one folder per capability
│   ├── src/shared/               # API client, auth/workspace context, reusable controls
│   └── public/                  # Local static media
├── .vscode/launch.json          # Spring Boot debugger configuration
├── hackerrank.yml               # HackerRank install and run configuration
└── setup.sh                     # Backend, MongoDB, and seed setup
```

## Getting Started

### Prerequisites

- Bun 1.3 or later
- JDK 21
- MongoDB 8.0 or later on `127.0.0.1:27017`

Gradle is provided through the committed wrapper.

### Development Setup

1. Clone the repository, then open the project directory.
2. Install the pinned JavaScript workspace.

   ```bash
   bun install
   ```

3. Start the complete application.

   ```bash
   bun start
   ```

   Startup builds the backend when needed, checks MongoDB, restores the seeded baseline, and
   launches the frontend and backend.

4. Open [http://localhost:3000](http://localhost:3000) and sign in.

   ```text
   Email: alex.morgan@flagdeck.com
   Password: password123
   ```

   Every seeded account (Alex Morgan, Jordan Lee, Sam Rivera, Taylor Chen, Priya Patel) shares
   that password - sign in as a second account to approve or reject another teammate's
   Production change request.

The frontend runs on port `3000`, the API runs on port `8000`, and health is available at
[http://localhost:8000/api/v1/health](http://localhost:8000/api/v1/health).

### Commands

| Command | Purpose |
|---|---|
| `bun start` | Seeds MongoDB and starts Spring Boot and Vite together. |
| `bun run seed` | Restores the deterministic MongoDB baseline through Gradle. |
| `bun run dev:backend` | Starts only the Spring Boot API on port `8000`. |
| `bun run dev:frontend` | Starts only Vite on port `3000`. |

HackerRank installs the application with `bun install && bash setup.sh --seed` and runs it with
`bun start`.

## Seeded data

The seed creates two projects (**Consumer Web App**, **Mobile Application**), each with three
environments, a roster of app users, a few audience segments, and a handful of feature flags
that demonstrate every capability above end to end - including a pending approval request, a
scheduled one, a rejected one, an in-progress percentage rollout, segment-based targeting, and
an experiment with a statistically significant winning variation.
