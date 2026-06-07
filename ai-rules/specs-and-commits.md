# Specs and Commit Rules

## Planning and Specs

- Match the task to the nearest real spec in `specs/` before changing code.
- If a current spec already covers the work, update that spec instead of creating a duplicate.
- Update `specs/steps.md` when the implementation path, tooling, or architecture guidance changes.

## Documentation-Only Changes

- For docs or agent-guidance changes, cite the most relevant existing spec file in `specs/` and keep the spec update minimal.
- Do not invent a new spec file unless the task genuinely introduces a new implementation direction.

## Commit Messages

- Use a short title, a quick summary, a real `Spec: specs/<file>.md` line, and a `Model: <model name>` line.
- Replace `<file>.md` with the actual spec or plan file that was updated for this change.
- Do not use a generic placeholder such as `implementation_plan.md` unless that exact file was the one changed.

## Git Guidelines

- Commit only the changes you made yourself.
- Keep commit messages concise, specific, and traceable to the relevant spec or plan.
- Use the repo’s existing commit structure and include the spec/model lines when preparing a commit.
