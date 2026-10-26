# Git and GitHub Workflow

This guide is for the Object-Oriented Hotel Reservation System team using Git with Apache NetBeans or Visual Studio Code. Commands can be run from Git Bash, PowerShell, or an IDE terminal, unless otherwise noted.

The repository is a Maven multi-module project:

- `hotel-core` — shared models, repositories, services, exceptions, database configuration, schema, and business rules.
- `hotel-desktop` — Java Swing application for administrator and customer workspaces.

Run Maven commands from the repository root, where the parent `pom.xml` is located, unless a command explicitly says otherwise.

## 1. Important: the repository history was reset

The `main` branch history was reset and force-pushed. **All collaborators must create a fresh clone of the current GitHub repository.** Do not continue using an old clone as the basis for new work.

If an old clone contains uncommitted or unpushed work, back it up outside the repository first. Do not merge the old clone's outdated history into the reset `main` unless the repository owner explicitly coordinates that recovery.

## 2. Clone the current repository

Clone the current GitHub repository:

```bash
git clone https://github.com/Barata-MSC/HotelReservation-OOP1.git
cd HotelReservation-OOP1
git status
git rev-parse --show-toplevel
```

If the repository URL or folder name has changed, use the URL shown on the repository's GitHub **Code** menu and the folder name created by `git clone`.

Open the **root/parent `pom.xml`** in NetBeans or VS Code. Do not treat `hotel-core` or `hotel-desktop` as the parent project. The root project should declare the Maven modules.

## 3. Everyday workflow

### A. Update `main` and create a task branch

Start each task from the latest shared `main` branch:

```bash
git switch main
git pull --ff-only origin main
git switch -c feature/short-task-name
```

Use a branch that describes one task. Examples aligned with the current project plan:

- `feature/core-room-model`
- `feature/room-repository`
- `feature/walk-in-reservations`
- `feature/desktop-room-management`
- `feature/desktop-billing`
- `feature/desktop-housekeeping`
- `feature/availability-validation`
- `feature/online-reservations`
- `fix/date-overlap-validation`
- `docs/readme-setup`

Use lowercase names with hyphens. Avoid spaces. Since `hotel-core` is shared, coordinate changes to its public methods, models, and database schema with teammates working in `hotel-desktop`.

### B. Check the changes

Before staging files:

```bash
git status
git diff
```

`git status` shows the current branch and modified, staged, or untracked files. `git diff` previews changes that have not been staged.

### C. Build and test the relevant Maven modules

Run these commands from the repository root. The root Maven build should be configured to include the existing modules.

Build the current multi-module project and run its configured tests:

```bash
mvn clean install
```

Test the shared core:

```bash
mvn -pl hotel-core test
```

Test the desktop module and also build required modules, including `hotel-core`:

```bash
mvn -pl hotel-desktop -am test
```

Use only commands for modules that exist in the checked-out branch. If a Maven command fails, resolve the failure or document the blocker before requesting a merge. Do not mark a feature complete merely because the code compiles; test its behavior too.

### D. Stage and commit

Prefer staging the files you intended to change:

```bash
git add hotel-core/src/main/java/com/hotel/core/service/AvailabilityService.java
git diff --cached
git commit -m "Add reservation availability validation"
```

The example path and class name are illustrative: stage the actual files in your branch. To stage all intended changes, including deletions, use:

```bash
git add -A
git diff --cached
git commit -m "Add walk-in reservation workflow"
```

Always inspect the staged diff before committing. Do not commit passwords, local database credentials, generated output, IDE-private settings, or unrelated changes.

### E. Push your branch

For the first push of a new branch:

```bash
git push -u origin feature/short-task-name
```

After the upstream is set, you can usually run:

```bash
git push
```

### F. Open a pull request

On GitHub:

1. Open the repository and create a pull request.
2. Set the feature branch as the **compare/source** branch and `main` as the **base/target** branch.
3. Fill in the pull request description using the repository's [pull request template](.github/PULL_REQUEST_TEMPLATE.md). Explain the motivation, summarize the implementation, and report the exact validation performed (including any tests not run and why).
4. Review the diff and resolve feedback with your team.
5. Merge only after review and successful checks, as applicable.

Do not normally commit directly to `main`. Keep pull requests focused and small enough for teammates to review.

When an AI assistant completes work that changes project files, it should provide a ready-to-use pull request title and detailed description following the same template. This is a draft for the contributor; the assistant must not create, push, or open the pull request unless explicitly asked.

### G. After the pull request is merged

Update your local `main` and remove the local feature branch:

```bash
git switch main
git pull --ff-only origin main
git branch -d feature/short-task-name
git fetch origin --prune
```

GitHub may automatically delete the remote branch after merge. If it does not, and the team agrees to remove it:

```bash
git push origin --delete feature/short-task-name
```

`git branch -d` deletes a local branch only; it does not delete a branch on GitHub.

## 4. Keeping your branch up to date

If `main` changes while you are working, first save your work with a commit or safely stash it. Then fetch and integrate the latest `main` into your feature branch:

```bash
git status
git add -A
git commit -m "Describe the work in progress"
git fetch origin
git merge origin/main
```

Only commit the work-in-progress if it is safe and appropriate to share on the branch; otherwise use `git stash push -u -m "work in progress"` and restore it after updating. If a merge reports conflicts, open each conflicted file, resolve the conflict markers carefully, stage the resolved files, and finish the merge as Git instructs. Run relevant tests before pushing.

Do not resolve a conflict by automatically discarding another teammate's work. Ask the teammate responsible for the affected file when the intended result is unclear.

`git pull` fetches and integrates changes. It does **not** upload your commits; use `git push` to publish them to GitHub.

## 5. Team coordination for the multi-module project

- **Work on shared core and Swing workflows.** The administrator and customer workspaces both live in `hotel-desktop`.
- **Keep business rules in `hotel-core`.** Do not independently reimplement availability, reservation validation, guest-classification rules, or billing formulas inside Swing UI code.
- **Coordinate shared-core changes.** If a change alters a model, service method, enum, schema, or repository contract, tell teammates whose modules depend on it and include any necessary updates and tests in the pull request.
- **Coordinate database schema changes.** `hotel-core/src/main/resources/db/schema.sql` is shared by both workspaces. Discuss table or column changes before merging, and note any required local database update in the pull request.
- **Avoid overlapping edits.** When possible, agree on file or module ownership before multiple people change the same files.
- **Build from the repository root.** This verifies that the parent POM and module dependencies work together.

## 6. Useful branch and repository commands

```bash
git branch                     # List local branches
git branch -a                  # List local and remote-tracking branches
git fetch origin --prune       # Refresh remote branches and prune deleted references
git switch main                # Switch to main
git switch -c feature/name     # Create and switch to a new branch
git remote -v                  # Show configured remotes
git log --oneline --decorate -10  # Review recent commits
```

Delete a merged local branch:

```bash
git branch -d feature/short-task-name
```

Use `git branch -D` only when you deliberately want to force-delete an unmerged local branch and have confirmed that its work is no longer needed.

## 7. `.gitignore` and local files

The repository's `.gitignore` should exclude generated Maven output and local IDE files, including:

```gitignore
**/target/
nbproject/private/
```

Keep the existing `.gitignore` rules as well. `target/` contains generated build output and should not be committed. `nbproject/private/` contains local NetBeans project settings and should not be shared as project source.

Do not commit local passwords, tokens, private connection settings, or real guest data. If a secret has already been committed, removing it in a later commit does not erase it from Git history. Rotate or revoke the secret and inform the repository owner.

If `.env` is already tracked and should become local-only, remove it from Git's index while keeping the local file:

```bash
git rm --cached .env
git add .gitignore
git commit -m "Stop tracking local environment file"
```

## 8. Dangerous commands — do not use for routine cleanup

### `git reset --hard origin/main`

This changes the current tracked working tree and branch state to match `origin/main`. Uncommitted changes to tracked files can be lost. Check `git status` and back up work before considering a reset. It is not a routine way to cancel a single edit.

### `git clean -fdx`

This permanently removes untracked and ignored files, potentially including local configuration, `.env` files, and generated or downloaded content. Preview first:

```bash
git clean -ndx
```

Only run `git clean -fdx` after inspecting the preview and confirming that every listed file can be deleted. Most contributors will not need this command.

### Deleting the `.git` directory

Removing `.git` detaches that folder from its repository metadata. Do not do this to fix ordinary Git problems. Use a fresh clone when appropriate, and back up needed local work first.

### Force-pushing

Do not force-push to `main`. The repository owner has already reset its history; normal team work should use feature branches and pull requests. Force-pushing can overwrite published history and disrupt other collaborators.

## 9. Quick reference

| Goal | Command |
|---|---|
| Check working tree | `git status` |
| Preview unstaged changes | `git diff` |
| Stage a selected file | `git add <file>` |
| Stage all intended changes | `git add -A` |
| Preview staged changes | `git diff --cached` |
| Commit changes | `git commit -m "Describe change"` |
| Push a branch for the first time | `git push -u origin <branch>` |
| Push later commits | `git push` |
| Update local `main` | `git switch main` then `git pull --ff-only origin main` |
| Fetch remote branch updates | `git fetch origin --prune` |
| Switch branch | `git switch <branch>` |
| Create a task branch | `git switch -c feature/<name>` |
| Test shared core | `mvn -pl hotel-core test` |
| Test desktop with required modules | `mvn -pl hotel-desktop -am test` |
| Build the multi-module project | `mvn clean install` |
