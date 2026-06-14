# Part A — Problem Formulation (ideation)

Deliverable A for the **Accommodation Allocation Engine** (Track 1 — Mathematical Models for
Operations). Submit before the mid-semester exam.

This deliverable presents the **idea and plan** — the problem, the proposed mathematical model
(Hungarian assignment), the planned integration and Java features, success metrics, and scope.
It is framed as a proposal ("we will…"); the working system is Part B.

## Contents

| File | Purpose |
|---|---|
| `slides.pdf` | The rendered presentation (ready to submit / present). |
| `slides.md` | Editable Marp/Markdown source of the deck (problem → idea → model → approach → integration → Java features → metrics → scope), with dedicated diagram slides. |
| `speaker-notes.md` | One-line intent per slide (the slides are self-explanatory). |
| `diagrams.md` | The conceptual diagrams (model idea, proposed pipeline, integration). |
| `diagrams/` | Diagram sources (`.mmd`) + rendered `.svg` (slides) and `.png` (report). |

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
