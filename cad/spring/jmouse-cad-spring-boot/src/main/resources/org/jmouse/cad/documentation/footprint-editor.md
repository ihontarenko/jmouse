---
subject: footprint-editor
title: The footprint editor
summary: Drawing a part's footprint — its pads, outlines and fields
---
## How the editor works

The editor is used as every editor is:
- **Select** is the tool it rests in;
- **Esc** drops what is in hand, then goes back to Select, then lets go of what is chosen;
- messages are said in the corner of the window.

**Save** keeps the footprint and stays in the editor. **Auto** saves it on its own, as in every editor. A new footprint is
created only by Save.

## Tools

| Tool | Key | What it does |
|---|---|---|
| Select | V | Chooses and moves; drag a pad's or an outline's corners |
| Pad | P | Places a pad; its shape, size and layers in the inspector |
| Custom pad | J | Draws a pad of any outline: click each corner |
| Field | T | Places where the designator or the value stands on the part |
| Line, Polyline, Arc | L, K, A | Outlines on the layer in hand — silkscreen, fabrication, courtyard |
| Box, Rounded, Ellipse, Circle, Polygon | B, O, E, C, G | Closed outlines |
| Distance, Angle | U, Y | Measures from pad centre to pad centre; adds nothing |
| Pan | H | Moves the view; the middle button does the same with any tool |

The **courtyard** is the room the part keeps round it. The **fabrication** outline is its body: on the PCB, a part is chosen by
its fabrication outline.

## The underlay

**View → Underlay** lays a datasheet's package drawing under the footprint to draw over. It is kept with the footprint.
