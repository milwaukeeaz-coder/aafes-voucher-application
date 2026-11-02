#!/usr/bin/env bash
# Clean slate. Destroys uncommitted work and local branches other than main, demo-base and demo-run.
cd "$(git rev-parse --show-toplevel)" || exit 1
bash demo/stop-app.sh
git worktree list --porcelain | awk '/^worktree /{print $2}' | tail -n +2 | while read -r wt; do
  git worktree remove --force "$wt"
done
git worktree prune
git checkout -f -B demo-run demo-start
git clean -fdx -e target
for b in $(git for-each-ref --format='%(refname:short)' refs/heads); do
  case "$b" in main|demo-base|demo-run) ;; *) git branch -D "$b" ;; esac
done
git status --short; git worktree list; git branch
echo 'Clean. The database is in memory, so it resets on every app start.'
