# Conventional Commits & Semantic Versioning Utility for Git Repositories

A small utility that allows computing the next semantic version number for a git repository based on git tags and commit messages.

## How To Use

Download the [latest release](https://github.com/fruiture/git-ccs/releases/latest) and run:

```sh
git-ccs --help
```

## Backlog

`(!)` = priority features, `(?)` = maybe

* (!) support version tags with prefixes/suffixes like "v1.2.3"
  * required for replacement of [git-semver](https://github.com/PSanetra/git-semver)
* (?) add `next pre-release -r 2.0.0` for proper releases in pre-release strategy (auto-bumping)
  * using release branches is still more elegant