---
subject: pcb-editor
title: The PCB editor
summary: Laying out a printed circuit board — its parts, copper, areas and checks
---
## How the editor works

The editor always has one **tool in hand**. The tool shapes what a click does, and the pointer shows which tool it is:
- **Select** is the tool the editor rests in: an arrow, and a move cursor over something chosen.
- A drawing tool shows a crosshair.
- **Pan** shows a hand.

**Esc** works step by step:
1. The first press drops what is in hand — a track being laid, the corners of a shape, a box being drawn.
2. The next press goes back to Select.
3. The last press lets go of what is chosen.

When the editor refuses something — a zone with no net, a track onto another net — it says why in the corner of the window.
Every message of the engine is said there.

## Choosing

- **Click** something to choose it.
- **Shift+click** adds to what is chosen.
- A **box drawn left to right** takes what it holds whole.
- A **box drawn right to left** takes whatever its lines cross. A zone, a keepout or the board's outline is taken only when the
  box crosses its edge, never because the box lies inside it.
- A part is taken by its **body**: its fabrication outline, else its courtyard, else its silkscreen, and its pads. A click in the
  empty corner of a round part's square takes nothing.
- What is **locked** is not taken on the canvas. Unlock it from the Parts panel.

## Tools

| Tool | Key | What it does | How |
|---|---|---|---|
| Select | V | Chooses and moves | click, drag, box |
| Pick net | N | Takes a net from copper | click a pad, a track or a via |
| Track | W | Lays a track the engine routes round what is in the way | click a pad, click each corner, double click or Enter to end |
| Pair | X | Lays a differential pair | start on a pad of a net that is in a pair (Setup → Differential pairs) |
| Arc | A | Lays an arc of copper | its start, a point it passes, its end |
| Tune length | 7 | Lengthens a track with a meander | choose the length and the meander on the stage, then click the track |
| Line, Polyline | L, K | Lines on the layer in hand | click the ends; for a polyline, double click to end |
| Box, Rounded | S, O | A rectangle | two corners |
| Ellipse, Circle, Polygon | E, C, G | A closed shape | the box, or the centre and a point, or each corner |
| Via | I | A via joining the layers | click where it stands |
| Hole | Q | A mounting hole | click where it stands; its sizes in the inspector |
| Text | T | Words on the silkscreen | click, then write them in the inspector |
| Dimension | D | A measured length drawn on the board | the two ends, then where it stands |
| Distance, Angle | U, Y | Measures; adds nothing | two points, or three for an angle |
| Resistance | J | The copper's resistance between two points | two points |
| Pan | H | Moves the view | drag; the middle button does the same with any tool |

On a PCB the key **B** pours the copper, as in KiCad, so the Box is **S**.

## What a drawn shape becomes

With a shape tool in hand, **Becomes** on the stage says what a closed shape turns into:

- **Zone** — copper poured for a net, kept off every other net. It needs a net: start the shape on copper of that net, or choose
  the net beside Becomes.
- **Filled** — a copper area of its own.
- **Bare copper** — an opening in the solder mask of that side. The copper under it is left bare.
- **Keepout** — no copper of any layer enters it.
- **Lines** — the shape's lines on the layer, as drawn.

A shape already drawn becomes another kind from the inspector. Choose it, then pick what it becomes under **Becomes**, with its
net. A polyline drawn round some copper becomes a zone or bare copper this way; its path is closed for it.

## Lengthening a track

Take **Tune length** (7). On the stage, set **Tune to** — a length in millimetres, or empty for the length of the track's pair
partner — and choose the **Meander**:

- **Square** — square bumps to one side, as KiCad lays them.
- **Accordion** — square bumps to either side in turn: half as tall for the same length.
- **Chamfered** — corners cut at 45°, for fast edges.
- **Rounded** — half-circle tops, with no corner at all.
- **Zigzag** — teeth up and down: a little length in little room.

Then click the track where the meander should go. A track's rounded corners are kept only as far as the meander's bumps bear
them: a zigzag is laid sharp.

## Saving

**Save** keeps the board, its sheet and the bench together. The light in the corner of Save tells where the work stands:
- green — saved;
- amber — changed since;
- blue — saving;
- red — the last save was refused.

Its title says when the work was last saved. **Auto** saves the work on its own once it has rested for a moment. It is one
choice for every editor.

## The underlay

**View → Underlay** lays a picture under the board to draw over: a photograph of a board, or a datasheet's drawing. It is kept
with the document and is there again whenever the board is opened. **Replace** and **Remove** change it.
