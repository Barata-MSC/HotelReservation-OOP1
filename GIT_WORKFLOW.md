# Git and GitHub Workflow

This guide is intended for the Hotel Reservation System team using Git in PowerShell, Git Bash, or the NetBeans terminal. Most commands work in all three; Windows-specific examples are marked.

## 1. Everyday workflow (recommended)

### A. Update `main` and create a task branch

Start each new task from the latest shared `main` branch:

```bash
git switch main
git pull --ff-only origin main
git switch -c feature/short-task-name
```

Example:

```bash
git switch main
git pull --ff-only origin main
git switch -c feature/room-management
```

Branch naming suggestions:

- `feature/room-management`
- `feature/reservation-scheduling`
- `feature/billing`
- `fix/date-overlap-validation`
- `docs/readme-setup`

Use lowercase names with hyphens. Do not include spaces in branch names.

### B. Review your changes before staging

```bash
git status
git diff
```

`git status` shows your branch and whether files are modified, staged, or untracked. `git diff` previews unstaged changes.

### C. Stage and commit

Prefer adding specific files when possible:

```bash
git add src/main/java/com/hotel/service/ReservationService.java
git diff --cached
git commit -m "Add reservation availability validation"
```

To stage all intended changes, including file deletions, use:

```bash
git add -A
git diff --cached
git commit -m "Add room management"
```

Check the staged diff before committing. Avoid `git add .` without reviewing the status, as it can accidentally stage secrets, generated files, or unrelated work if your ignore rules are incomplete.

### D. Push the branch

For the first push of a new branch:

```bash
git push -u origin feature/room-management
```

After the upstream is set, you can usually use:

```bash
git push
```

### E. Open a pull request on GitHub

1. Open the repository on GitHub.
2. Create a pull request with your feature branch as the **compare/source** branch and `main` as the **base/target** branch.
3. Describe what changed, why it changed, and how it was tested.
4. Review the diff, resolve feedback, and wait for teammates or checks as required.
5. Merge only when the team is satisfied.

Do not normally commit directly to `main`.

### F. After the pull request is merged

```bash
git switch main
git pull --ff-only origin main
git branch -d feature/room-management
git fetch origin --prune
```

GitHub may automatically delete the remote branch after merging. If it does not and the team intends to delete it, use:

```bash
git push origin --delete feature/room-management
```

`git branch -d` deletes a local branch only. It does not delete a branch on GitHub.

## 2. Clone a repository for the first time

```bash
git clone <GITHUB_REPOSITORY_URL>
cd <REPOSITORY_FOLDER>
git status
```

`git clone` downloads the repository and its history. It is not the same as `git pull`. Do not run `git init` inside an already cloned repository.

Useful navigation commands:

```bash
pwd       # Show current directory in Git Bash/most Unix-like terminals
ls        # List files (also works as a PowerShell alias)
cd <DIR>  # Move into a directory
```

## 3. If `main` has new changes while you are working

First save or commit your own work. Then fetch remote updates:

```bash
git status
git add -A
git commit -m "Describe the work in progress"
git fetch origin
git merge origin/main
```

If the merge reports conflicts, open each conflicted file, resolve the conflict markers, then stage the resolved files and finish the merge as Git instructs. Run tests before pushing. If you are unsure how to resolve a conflict, ask the teammate responsible for the affected code rather than discarding their changes.

`git pull` fetches and integrates changes. It does **not** mean “save my work to GitHub”; use `git push` to upload your commits.

## 4. Check branches

```bash
git branch             # Local branches
git branch -a          # Local and known remote-tracking branches
git fetch origin --prune
```

Delete a merged local branch:

```bash
git branch -d feature/room-management
```

Use `-D` only when you deliberately want to force-delete a local branch that Git considers unmerged. It can discard the branch's easy-to-reach reference to unmerged work.

## 5. Start a brand-new local repository (only if needed)

Use this only when the folder is not already a Git repository and the GitHub repository is empty:

```bash
git init
git add -A
git status
git commit -m "Initial commit"
git branch -M main
git remote add origin <GITHUB_REPOSITORY_URL>
git push -u origin main
```

If the GitHub repository already contains an initial README or other commit, do not blindly follow this sequence; clone the repository first or reconcile the histories deliberately.

## 6. Change the remote repository URL

```bash
git remote -v
git remote set-url origin <NEW_GITHUB_REPOSITORY_URL>
git remote -v
```

Check the URL carefully before pushing so that you do not upload the project to the wrong repository.

## 7. Ignore files and protect secrets

Add sensitive and generated files to `.gitignore` before committing them. For example, exclude `.env`, local-only configuration, and Maven output. If `.env` is already tracked, remove it from the Git index while keeping the local file:

```bash
git rm --cached .env
git add .gitignore
git commit -m "Stop tracking local environment file"
```

**Important:** this does not erase the file or secrets from earlier Git history. If an actual password, token, or key was committed, revoke or rotate it immediately and notify the team.

## 8. Dangerous cleanup commands — avoid for routine work

### `git reset --hard origin/main`

This forces the current tracked working tree and branch state to match `origin/main`; uncommitted changes to tracked files can be lost. It is not a normal “cancel” command. Check `git status` and back up any work you need before using a reset.

### `git clean -fdx`

This permanently removes untracked files and ignored files, which can include local settings, `.env` files, and generated or downloaded content. Preview first:

```bash
git clean -ndx
```

Only run `git clean -fdx` when you have inspected the preview and are certain every listed file can be deleted. Most contributors will never need this command.

### Removing the `.git` directory

PowerShell's `Remove-Item -Recurse -Force .git` removes the local repository metadata and history for that folder. Do not use it to fix ordinary Git problems. Back up the project and use it only when you intentionally want to detach the folder from Git.

### PowerShell execution policy

`Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned` is a PowerShell configuration change, not a Git command, and is not normally required for the Git workflow above. Do not change it unless a specific, understood PowerShell script policy issue requires it.

## 9. Quick reference

| Goal | Command |
|---|---|
| Check current changes | `git status` |
| Preview unstaged changes | `git diff` |
| Stage selected file | `git add <file>` |
| Stage all intended changes | `git add -A` |
| Preview staged changes | `git diff --cached` |
| Commit | `git commit -m "Describe change"` |
| Push first time on branch | `git push -u origin <branch>` |
| Push later commits | `git push` |
| Download/integrate updates | `git pull --ff-only origin main` (while on `main`) |
| Refresh remote branch references | `git fetch origin --prune` |
| Switch branch | `git switch <branch>` |
| Create and switch to branch | `git switch -c feature/<name>` |
