#!/usr/bin/env sh
set -eu
repo_root="$(git rev-parse --show-toplevel)"
git -C "$repo_root" config core.hooksPath scripts
chmod +x "$repo_root/scripts/pre-push"
chmod +x "$repo_root/scripts/pre-commit"
printf '%s\n' 'Installed repository hooks path: scripts/'
