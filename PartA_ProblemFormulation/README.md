# Part A — Problem Formulation

Deliverable A for the **Accommodation Allocation Engine** (Track 1 — Mathematical Models for
Operations). Submit before the mid-semester exam.

## Contents

| File | Purpose |
|---|---|
| `slides.md` | The ≤10-slide presentation (Marp/Markdown). Problem, I/O, model, metrics, innovation, integration, Java features, scope. |
| `speaker-notes.md` | Per-slide narration so each group member can present a section. |
| `diagrams.md` | Mermaid data-flow, sequence, UML class, and GUI/Observer diagrams. |

## How to render the slides to PDF/PPTX

The slides are plain Markdown with a Marp front-matter header. Options:

**Marp CLI** (recommended):
```bash
npm install -g @marp-team/marp-cli
marp slides.md -o slides.pdf      # or: --pptx, --html
```

**VS Code**: install the *Marp for VS Code* extension, open `slides.md`, then
"Export slide deck…" → PDF / PPTX / HTML.

**No tooling**: the file is readable as-is, or paste each `---`-separated section into
PowerPoint / Google Slides.

## How to render the diagrams

`diagrams.md` uses Mermaid. View on GitHub, in VS Code (Markdown Preview Mermaid Support), or at
<https://mermaid.live> — export PNG/SVG to embed in the report and slides.

## Before submitting

- Fill in the group number and member names on slide 1 (and in the video file name
  `<GroupNo>_OOP_Summer_2026_Project`).
- The implementation, README, tests, and run instructions live in `../PartB_Solution/`.
