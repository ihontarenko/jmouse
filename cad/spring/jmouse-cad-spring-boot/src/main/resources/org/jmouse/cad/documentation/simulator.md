---
subject: simulator
title: The simulator
summary: Running a circuit live on the bench, with its probes, meters and instruments
---
## Running

The bench runs the circuit as its sheet draws it. **Run** starts it from zero, **Pause** holds it, and the speed says how fast
its time passes against the clock's. While it runs, the sheet shows what flows: currents along the wires, the readings of the
parts on their plates.

When a run stops on its own, the reason is said in the corner of the window and on the bench. That happens when the circuit
cannot be solved, or a part is given a value it cannot take.

## Probes and instruments

- A **probe** clipped onto a node is a channel of the oscilloscope. Clip it by dragging it onto a wire or a contact; click the
  same node again, or press Esc, to leave it on ground.
- A **meter** reads what it is wired across. Cut a wire (**Cut**, X) to put a meter into the break.
- The **oscilloscope** shows the channels over time. Its readout folds to a button; its cursors measure between two moments.

## Changing values

A part's values are changed while it runs, from its plate or the inspector: the run goes on with the new value. A parameter of
the circuit changes every part that uses it.

## Replays

What the run went through is kept: a stretch of it plays again through the same instruments, without running the circuit again.
