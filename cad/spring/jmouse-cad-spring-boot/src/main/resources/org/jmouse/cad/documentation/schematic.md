---
subject: schematic
title: The schematic
summary: Drawing a circuit's sheet — its parts, wires, labels and marks
---
## How the sheet works

A sheet always has one **tool in hand**, and the pointer shows which. **Select** is the tool the sheet rests in.

**Esc** works step by step:
1. The first press drops what is in hand — a wire or a bus being drawn, a part held to place, a note being written.
2. The next press goes back to Select.
3. The last press lets go of what is chosen.

Messages of the engine — a refusal, a failed check — are said in the corner of the window.

## Tools

| Tool | Key | What it does |
|---|---|---|
| Select | V | Chooses and moves; a box drawn on empty sheet chooses what it holds. A wire or a pin pressed lights its net |
| Wire | W | Draws wires between contacts and onto wires; a junction comes by itself where one wire meets another |
| Bus | B | Draws a bus: click its corners, and the last one again to end. A wire brought to it enters it on a label of its next net |
| Part | A | Adds a part, chosen from the components |
| Ground | G | The circuit's earth |
| 0 V common | O | The common of a bipolar supply, drawn as such rather than as an earth |
| Net label | L | Every label of one name on this sheet is one net, with no wire drawn between them |
| Global label | K | One name across every sheet of the project, as the rails are |
| Sheet port | J | A net this sheet hands to the sheet that places it: a pin of its block there |
| No-connect | Q | Marks a pin left open on purpose, so the checks take it as meant |
| Text | T | Words on the sheet |
| Line, Rectangle, Circle, Arc | I, E, C, U | Drawing for whoever reads the sheet; they join nothing |
| Cut | X | Cuts a wire into two open ends, to put an instrument into the break |
| Delete | D | Deletes what is clicked |
| Pan | H | Moves the view; the middle button does the same with any tool |

## Blocks and sheets

A part that is a sheet of its own is a **block**. Double click it to go down into its sheet, as KiCad enters a hierarchical
sheet. Its sheet ports are the block's pins.

## Saving

**Save** keeps the sheet, the circuit's fields, the bench's probes and meters, and the PCB together. The light in its corner, and
**Auto**, work as in every editor: see *The PCB editor → Saving*.
