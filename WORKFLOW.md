# Workflow

> This document helps contributors get familiar with the workflow of the project.

---

## Branching Strategy
- `main`: Stable, working version.
- `dev`: Active development branch.
- `feature/*`: New features, branched off from `dev`.
  - Example: `feature/score-system`
- `bugfix/*`: Bug fixes, branched off from `dev`.
  - Example: `bugfix/clear-line`
- `hotfix/*`: Critical hotfixes, branched off from `main`.
  - Example: `hotfix/cannot-compile`
- `misc/*`: Updates unrelated to source code (e.g., docs), branched off from `dev`.
   - Example: `misc/docs`

---

## Workflow Steps

1. **Get the latest changes from the branch you're branching off**
   ```bash
   git checkout dev
   git pull
   ```
2. **Create a new branch**
   ```bash
   git checkout -b feature/settings-gui
   ```
3. **Stage and Commit regularly** with clear, meaningful messages:
   ```bash
   git add .
   git commit -m "feat:Add difficulties"
   ```
   Example commit prefixes:
   - `feat` – Add new features
   - `fix` – Fix bugs
   - `chore` – Non-functional changes (e.g., update dependencies)
   - `refactor` – Code changes that improve structure without changing behavior
   - `docs` – Documentation updates
   - `style` – Code formatting (e.g., whitespace, semicolons)
   - `test` – Add or fix tests
   - `perf` – Performance improvements
   - `ci` – Continuous integration-related changes
   - `build` – Build-related changes
   - `revert` – Revert a previous commit
    
   Switch to Full Conventional Commits format if preferred
   ```bash
   git add .
   git commit
   ```
   A Text Editor by your git settings will open:
   ```text
   fix: fix foo to enable bar

   This fixes the broken behavior of the component by doing xyz.
   
   BREAKING CHANGE
   Before this fix foo wasn't enabled at all, behavior changes from <old> to <new>
   ```
4. **Push your branch to the remote**
   ```bash
    git push
   ```
5. **Create a Pull Request (PR)** on GitHub when complete
   - PR from `feature/settings-gui` into `dev` branch
   - Using PR template
6. **Request Review, Approve Then Merge**
   - Request reviews
   - Discuss wither Reviewers
   - Make Changes if needed
   - Wait for approvals and checks to pass
   - Merge the PR
   - Delete the branch if it’s no longer necessary

---

## Tips
- Use descriptive commit messages
- Pull frequently to avoid merge conflicts
- Keep PRs small and focused
- Use GitHub Issues and PR comments for communication

## References

- [How to Write Better Git Commit Messages – A Step-By-Step Guide](https://www.freecodecamp.org/news/how-to-write-better-git-commit-messages/)