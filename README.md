# Conventional Commits & Semantic Versioning Utility for Git Repositories

A small utility that allows computing the next semantic version number for a git repository based on git tags and commit messages.

## How To Use

It's a CLI tool with `--help` option.

## Backlog

`(!)` = priority features, `(?)` = maybe

* (!) support version tags with prefixes/suffixes like "v1.2.3"
  * required for replacement of [git-semver](https://github.com/PSanetra/git-semver)
* add `--version` option (not so easy to do in mpp/native)
* (?) add `next pre-release -r 2.0.0` for proper releases in pre-release strategy (auto-bumping)
  * using release branches is still more elegant