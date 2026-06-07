# Git Guidelines

Use these rules when the task involves commit preparation, review, or git hygiene.

## Commit Scope

- Commit only files that were intentionally changed by the current task.
- Keep messages focused on the actual change and avoid generic wording.

## Commit Structure

- Use the repository’s existing commit structure: short title, quick summary, real `Spec: specs/<file>.md`, and `Model: <model name>`.
- Replace `<file>.md` with the exact spec or plan file that was updated for this change.

## Traceability

- Prefer a real spec or plan file over a placeholder.
- If the work is documentation-only or agent-guidance-only, point to the most relevant existing spec in `specs/` instead of inventing a generic reference.
