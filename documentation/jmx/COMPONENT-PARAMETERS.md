# Component parameters — every property of every catalogue component

Українською: [COMPONENT-PARAMETERS.uk.md](COMPONENT-PARAMETERS.uk.md).

Generated from `BE/src/main/resources/catalogue/electronics/components/**/*.jmx` — 69 files, 237 distinct parameters. ⚠️ Do not edit by hand: `perl tools/component-parameters/parameter-reference.pl BE/src/main/resources/catalogue/electronics/components tools/component-parameters/parameter-names-uk.tsv .` from `Parallax/`; a new parameter needs its Ukrainian line in `parameter-names-uk.tsv`.

## How to read this

- A **parameter** is a number in a component's `parameters` block. The value there is the default; a part on a sheet
  overrides any of them on its line: `Q1 2n2222 (b, c, e) beta_forward = 300`. A written quantity takes units
  (`100 kΩ`, `2 pF`); a value inside an equation does not (`1e-12`, never `1 pA`).
- A **family** is a component whose equations its members use: `2n2222` → `npn-transistor` → `bipolar-transistor`.
  A member inherits every parameter of its family and sets only what differs; the family table lists everything a
  member may set.
- `live` marks a parameter worked from the bench while a circuit runs (a slider, a switch); `in a .. b step s` is its
  range.
- Names follow one rule: a quantity's letter and what it is (`V_breakdown`, `R_series`, `C_junction_zero_bias`); a
  SPICE parameter carries its SPICE name in the meaning (`IS:`, `BF:`), so an imported `.model` maps by that name.
- **Figures on the sheet** (`display`) are computed on every sample and shown beside the part; `overvoltage` and
  `overcurrent` at 100 % or more are a failure cause on a realistic bench.
- **States** are unknowns the model carries: `heating` (kelvin above the bench) is solved with the circuit on a
  realistic bench and removed entirely on an ideal one.

## Families

### Bipolar transistor — `bipolar-transistor`

_Gummel–Poon bipolar transistor: gain falling at both ends, Early effect, stored charge, junction capacitances_ · Gummel–Poon model as in Berkeley SPICE3 / ngspice 47; defaults are that model's

`base/bipolar-transistor.jmx`

| Parameter | Default | Meaning |
|---|---|---|
| `polarity` | 1 | 1 for NPN, -1 for PNP |
| `I_saturation` | 0.1 fA | IS: transport saturation current |
| `beta_forward` | 100 | BF: ideal forward current gain |
| `beta_forward_spread` | 0 | how far BF spreads from part to part, as a share: a bench's tolerance corners use it |
| `emission_coefficient` | 1 | NF: forward emission coefficient |
| `V_early` | 1 MV | VAF: forward Early voltage (none is a megavolt here) |
| `I_knee` | 1000000000 A | IKF: forward high-injection knee (none is a gigaampere here) |
| `I_leakage_base_emitter` | 0 A | ISE: base–emitter leakage saturation current |
| `leakage_emission_base_emitter` | 1.5 | NE: its emission coefficient |
| `beta_reverse` | 1 | BR: ideal reverse current gain |
| `emission_coefficient_reverse` | 1 | NR: reverse emission coefficient |
| `V_early_reverse` | 1 MV | VAR: reverse Early voltage |
| `I_knee_reverse` | 1000000000 A | IKR: reverse high-injection knee |
| `I_leakage_base_collector` | 0 A | ISC: base–collector leakage saturation current |
| `leakage_emission_base_collector` | 2 | NC: its emission coefficient |
| `R_base` | 1 mOhm | RB: zero-bias base resistance |
| `R_base_minimum` | 0 Ohm | RBM: the base resistance at high current; 0 means R_base |
| `R_emitter` | 1 mOhm | RE: emitter resistance |
| `R_collector` | 1 mOhm | RC: collector resistance |
| `C_base_emitter` | 0 F | CJE: base–emitter zero-bias capacitance |
| `V_junction_base_emitter` | 0.75 V | VJE: its built-in potential |
| `grading_base_emitter` | 0.33 | MJE: its grading coefficient |
| `C_base_collector` | 0 F | CJC: base–collector zero-bias capacitance |
| `V_junction_base_collector` | 0.75 V | VJC: its built-in potential |
| `grading_base_collector` | 0.33 | MJC: its grading coefficient |
| `internal_base_collector_fraction` | 1 | XCJC: the share of CJC at the internal base; the rest is at the base pin |
| `forward_bias_coefficient` | 0.5 | FC: past FC·VJ a depletion capacitance goes on as a straight line |
| `transit_time_forward` | 0 s | TF: ideal forward transit time — sets fT |
| `transit_time_bias_dependence` | 0 | XTF: how much TF grows with current |
| `transit_time_bias_voltage` | 1 MV | VTF: the VBC that halves that growth |
| `transit_time_current` | 0 A | ITF: the current at which TF's growth sets in |
| `transit_time_reverse` | 0 s | TR: ideal reverse transit time — the storage time of a saturated switch |
| `beta_temperature_exponent` | 0 | XTB: how the gains follow the temperature |
| `saturation_temperature_exponent` | 3 | XTI: how the saturation current follows the temperature |
| `E_gap` | 1.11 V | EG: band gap, in volts |
| `T_nominal` | 300.15 K | TNOM: the temperature the figures above are measured at |
| `V_breakdown` | 1 kV | collector–emitter sustaining voltage: an inductive kick stops here |
| `I_breakdown` | 1 mA | avalanche current at V_breakdown |
| `power_rating` | 0.5 W | what it is rated for |
| `thermal_resistance` | 250 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 2 s | how fast it warms to that |
| `T_maximum` | 150 °C | past this it fails on a realistic bench |
| `V_collector_emitter_maximum` | 1e6 V | VCEO: the collector-emitter voltage it is rated for; none is a megavolt |
| `I_collector_maximum` | 1e6 A | IC: the collector current it is rated for; none is a megaampere |

**Members:** [`2n2222`](#2n2222a--2n2222), [`2n2907`](#2n2907a--2n2907), [`bc547`](#bc547b--bc547), [`bc557`](#bc557b--bc557), [`npn-transistor`](#npn-transistor--npn-transistor), [`pnp-transistor`](#pnp-transistor--pnp-transistor), [`tip31c`](#tip31c--tip31c), [`tip32c`](#tip32c--tip32c)

### Diode — `diode`

_Junction diode with every effect: recombination, high injection, depletion and diffusion charge, breakdown_ · Berkeley SPICE3 / ngspice 47 diode model, level 1; defaults are SPICE's

`base/diode.jmx`

| Parameter | Default | Meaning |
|---|---|---|
| `I_saturation` | 10 fA | IS: saturation current |
| `emission_coefficient` | 1 | N: emission coefficient |
| `R_series` | 1 mOhm | RS: series resistance (SPICE's 0 means none; here a milliohm) |
| `I_recombination` | 0 A | ISR: recombination saturation current — the low-current excess of the forward curve |
| `recombination_emission_coefficient` | 2 | NR: emission coefficient of the recombination current |
| `I_knee` | 1000000000 A | IKF: high-injection knee current (SPICE's none is a gigaampere here) |
| `C_junction_zero_bias` | 0 F | CJO: zero-bias junction capacitance |
| `V_junction` | 1 V | VJ: junction potential |
| `grading_coefficient` | 0.5 | M: grading coefficient |
| `forward_bias_coefficient` | 0.5 | FC: past FC·VJ the depletion capacitance goes on as a straight line |
| `transit_time` | 0 s | TT: transit time — the stored charge behind reverse recovery |
| `V_breakdown` | 1 kV | BV: reverse breakdown voltage (SPICE's none is a kilovolt here) |
| `I_breakdown` | 1 mA | IBV: current at V_breakdown |
| `T_nominal` | 300.15 K | TNOM: the temperature the figures above are measured at |
| `saturation_temperature_exponent` | 3 | XTI: how steeply I_saturation follows the temperature |
| `breakdown_temperature_coefficient` | 0 | TCV: BV moves by this share per kelvin |
| `resistance_temperature_coefficient` | 0 | TRS: RS moves by this share per kelvin |
| `E_gap` | 1.11 V | EG: band gap, in volts |
| `area` | 1 | area factor: multiplies the currents and the capacitance, divides RS |
| `power_rating` | 0.5 W | what it is rated for |
| `thermal_resistance` | 300 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 1 s | how fast it warms to that |
| `T_maximum` | 175 °C | past this it fails on a realistic bench |
| `V_reverse_maximum` | 1e6 V | VRRM: the reverse voltage it is rated to block; none is a megavolt |
| `I_forward_maximum` | 1e6 A | IF: the current it is rated to carry, either way; none is a megaampere |

**Members:** [`1n4007`](#1n4007--1n4007), [`1n4148`](#1n4148--1n4148), [`1n5819`](#1n5819--1n5819), [`bzx55c5v1`](#bzx55c5v1--bzx55c5v1), [`led-red`](#red-led--led-red)

### Junction FET — `junction-field-effect-transistor`

_JFET level 1: square-law channel, channel-length modulation, gate junctions and their capacitances_ · JFET level 1 as in Berkeley SPICE3 / ngspice 47; defaults are that model's

`base/junction-field-effect-transistor.jmx`

| Parameter | Default | Meaning |
|---|---|---|
| `polarity` | 1 | 1 for n-channel, -1 for p-channel |
| `V_threshold` | -2 V | VTO: pinch-off voltage, negative for a depletion part |
| `V_threshold_spread` | 0 V | how far the pinch-off spreads from part to part: a bench's tolerance corners use it |
| `transconductance_spread` | 0 | how far BETA spreads from part to part, as a share |
| `threshold_temperature_coefficient` | 0 | VTOTC: how VTO moves per kelvin, V/K |
| `transconductance` | 100 uA/V^2 | BETA: IDSS = BETA · VTO² |
| `channel_length_modulation` | 0 | LAMBDA: per volt |
| `R_drain` | 1 mOhm | RD: drain series resistance |
| `R_source` | 1 mOhm | RS: source series resistance |
| `I_saturation` | 10 fA | IS: gate junction saturation current |
| `emission_coefficient` | 1 | N: its emission coefficient |
| `C_gate_source` | 0 F | CGS: gate–source zero-bias capacitance |
| `C_gate_drain` | 0 F | CGD: gate–drain zero-bias capacitance |
| `V_junction` | 1 V | PB: gate junction potential |
| `forward_bias_coefficient` | 0.5 | FC |
| `T_nominal` | 300.15 K | TNOM |
| `E_gap` | 1.11 V | the band gap the gate junctions' saturation current follows |
| `power_rating` | 0.35 W | what it is rated for |
| `thermal_resistance` | 350 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 2 s | how fast it warms to that |
| `T_maximum` | 150 °C | past this it fails on a realistic bench |

**Members:** [`2n5457`](#2n5457--2n5457), [`j201`](#j201--j201), [`n-jfet`](#n-channel-jfet--n-jfet), [`p-jfet`](#p-channel-jfet--p-jfet)

### MOS transistor — `mos-transistor`

_Level-1 MOSFET: square-law channel, channel-length modulation, series resistances, body diode, gate and junction capacitances_ · MOS level 1 as in Berkeley SPICE3 / ngspice 47; defaults are that model's

`base/mos-transistor.jmx`

| Parameter | Default | Meaning |
|---|---|---|
| `polarity` | 1 | 1 for n-channel, -1 for p-channel |
| `V_threshold` | 0 V | VTO: zero-bias threshold voltage, SPICE's sign |
| `V_threshold_spread` | 0 V | how far the threshold's magnitude spreads from part to part: a bench's tolerance corners use it |
| `transconductance_spread` | 0 | how far KP spreads from part to part, as a share |
| `transconductance` | 20 uA/V^2 | KP: the channel's transconductance parameter, the whole device (W/L = 1) |
| `channel_length_modulation` | 0 | LAMBDA: per volt |
| `R_drain` | 1 mOhm | RD: drain series resistance |
| `R_source` | 1 mOhm | RS: source series resistance |
| `I_saturation` | 10 fA | IS: bulk junction saturation current — the body diode |
| `emission_coefficient` | 1 | N: its emission coefficient |
| `C_bulk_drain` | 0 F | CBD: drain–bulk zero-bias capacitance |
| `V_junction` | 0.8 V | PB: its built-in potential |
| `grading_coefficient` | 0.5 | MJ: its grading coefficient |
| `forward_bias_coefficient` | 0.5 | FC: past FC·PB the depletion capacitance goes on as a straight line |
| `C_gate_source` | 0 F | CGSO: gate–source overlap capacitance (W = 1) |
| `C_gate_drain` | 0 F | CGDO: gate–drain overlap capacitance (W = 1) |
| `R_gate` | 1 mOhm | RG: the gate's internal resistance, which with Ciss sets how fast it switches |
| `body_transit_time` | 0 s | the body diode's transit time: its stored charge, the reverse recovery of a freewheeling diode |
| `T_nominal` | 300.15 K | TNOM: the temperature the figures above are measured at |
| `V_threshold_coefficient` | 0 | not SPICE: how the threshold's magnitude drifts, V/K (negative: it opens earlier when hot) |
| `resistance_temperature_exponent` | 0 | not SPICE: RD and RS grow as (temperature / T_nominal)^this |
| `E_gap` | 1.11 V | the band gap the body diode's saturation current follows |
| `V_breakdown` | 1 kV | drain–source avalanche voltage |
| `I_breakdown` | 1 mA | avalanche current at V_breakdown |
| `power_rating` | 0.4 W | what it is rated for |
| `thermal_resistance` | 312 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 2 s | how fast it warms to that |
| `T_maximum` | 150 °C | past this it fails on a realistic bench |
| `V_drain_source_maximum` | 1e6 V | VDS: the drain-source voltage it is rated for; none is a megavolt |
| `V_gate_source_maximum` | 1e6 V | VGS: what the gate oxide is rated for, either way; none is a megavolt |
| `I_drain_maximum` | 1e6 A | ID: the drain current it is rated for; none is a megaampere |

**Members:** [`2n7000`](#2n7000--2n7000), [`bs250`](#bs250--bs250), [`irf9540`](#irf9540n--irf9540), [`irfz44n`](#irfz44n--irfz44n), [`n-mosfet`](#n-channel-mosfet--n-mosfet), [`p-mosfet`](#p-channel-mosfet--p-mosfet)

### Operational amplifier — `operational-amplifier`

_Op-amp macromodel: gain, two poles, slew rate, offset, bias currents, CMRR, PSRR, input range, output limits_ · behavioural macromodel; the figures a datasheet's electrical table gives

`base/operational-amplifier.jmx`

| Parameter | Default | Meaning |
|---|---|---|
| `open_loop_gain` | 100000 | DC open-loop gain, as a ratio (100 dB) |
| `gain_bandwidth` | 1 MHz | unity-gain bandwidth |
| `second_pole` | 0 Hz | the second pole; 0 means none |
| `slew_rate` | 1 V/us | slew rate |
| `V_input_offset` | 0 V per unit | input offset voltage |
| `I_input_bias` | 0 A | input bias current: positive flows out of the inputs (PNP stage), negative into them (NPN) |
| `V_offset_spread` | 0 V | the offset a part may have, either way: a bench's tolerance corners use it |
| `V_offset_drift` | 0 | how the offset moves per kelvin, V/K; none by default, its sign is the part's own |
| `bias_doubling` | 1e9 K | the input current doubles every this many kelvin: about 10 K for a JFET input; none is a gigakelvin |
| `T_nominal` | 298.15 K | the temperature the figures above are measured at |
| `I_input_offset` | 0 A | the difference between the two inputs' bias currents |
| `R_input` | 1 TOhm | differential input resistance |
| `C_input_differential` | 0 F | capacitance between the inputs |
| `C_input_common` | 0 F | capacitance from each input to the negative rail |
| `supply_current_coefficient` | 0 | the share per volt the supply current rises above V_supply_nominal |
| `common_mode_rejection` | 1e12 | CMRR, as a ratio; none by default — its error is an offset of unknown sign |
| `supply_rejection` | 1e12 | PSRR, as a ratio; none by default — its error is an offset of unknown sign |
| `V_supply_nominal` | 15 V | the supply span the offset is given at |
| `V_common_mode_low` | -0.3 V | the inputs work down to this far above the negative rail (negative: below it) |
| `V_common_mode_high` | 0 V | the inputs work up to this far below the positive rail |
| `phase_reversal` | 0 | 1: past the low limit the output swings the other way |
| `V_noise` | 0 | input voltage noise density, V/√Hz (white) |
| `I_noise` | 0 | input current noise density into each input, A/√Hz (white) |
| `noise_corner` | 0 Hz | below this frequency 1/f noise rises over the white; 0 for none |
| `I_quiescent` | 1 mA | supply current per unit |
| `R_output` | 50 Ohm | output resistance |
| `I_output_source` | 40 mA | output short-circuit current, sourcing |
| `I_output_sink` | 40 mA | output short-circuit current, sinking |
| `V_output_headroom` | 1.5 V | the output stays this far below the positive rail |
| `V_output_low` | 1.5 V | the output reaches this close to the negative rail |
| `recovery` | 1e6 1/s | how fast the gain stage comes back out of saturation |
| `thermal_resistance` | 120 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 5 s | how fast it warms to that |
| `T_maximum` | 150 °C | past this it fails on a realistic bench |
| `V_supply_maximum` | 1e6 V | the supply span it is rated for; none is a megavolt |

**Members:** [`lm358`](#lm358--lm358), [`lm393`](#lm393--lm393), [`ne5532`](#ne5532--ne5532), [`tl072`](#tl072--tl072)

## Components

### `base/`

#### N-channel JFET — `n-jfet`

_N-channel JFET, the level-1 model at its defaults_

`base/n-jfet.jmx` · type `jfet` · Family: `junction-field-effect-transistor` · fails: `open`

Inherits every parameter of [`junction-field-effect-transistor`](#junction-fet--junction-field-effect-transistor).

| Parameter | Value | Meaning |
|---|---|---|
| `polarity` | 1 | n-channel |

#### N-channel MOSFET — `n-mosfet`

_N-channel MOSFET, the level-1 model at its defaults_

`base/n-mosfet.jmx` · type `mosfet` · Family: `mos-transistor` · fails: `short`

Inherits every parameter of [`mos-transistor`](#mos-transistor--mos-transistor).

| Parameter | Value | Meaning |
|---|---|---|
| `polarity` | 1 | n-channel |
| `V_threshold` | 1 V | VTO: an enhancement part opens above it |
| `transconductance` | 0.1 A/V^2 | KP: the whole channel |

#### NPN transistor — `npn-transistor`

_NPN bipolar transistor, the Gummel–Poon model at its defaults_

`base/npn-transistor.jmx` · type `bjt` · Family: `bipolar-transistor` · fails: `open`

Inherits every parameter of [`bipolar-transistor`](#bipolar-transistor--bipolar-transistor).

| Parameter | Value | Meaning |
|---|---|---|
| `polarity` | 1 | NPN |

#### P-channel JFET — `p-jfet`

_P-channel JFET, the level-1 model at its defaults_

`base/p-jfet.jmx` · type `jfet` · Family: `junction-field-effect-transistor` · fails: `open`

Inherits every parameter of [`junction-field-effect-transistor`](#junction-fet--junction-field-effect-transistor).

| Parameter | Value | Meaning |
|---|---|---|
| `polarity` | -1 | p-channel |

#### P-channel MOSFET — `p-mosfet`

_P-channel MOSFET, the level-1 model at its defaults_

`base/p-mosfet.jmx` · type `mosfet` · Family: `mos-transistor` · fails: `short`

Inherits every parameter of [`mos-transistor`](#mos-transistor--mos-transistor).

| Parameter | Value | Meaning |
|---|---|---|
| `polarity` | -1 | p-channel |
| `V_threshold` | -1 V | VTO: an enhancement part opens when the gate is this far below the source |
| `transconductance` | 0.1 A/V^2 | KP: the whole channel |

#### PNP transistor — `pnp-transistor`

_PNP bipolar transistor, the Gummel–Poon model at its defaults_

`base/pnp-transistor.jmx` · type `bjt` · Family: `bipolar-transistor` · fails: `open`

Inherits every parameter of [`bipolar-transistor`](#bipolar-transistor--bipolar-transistor).

| Parameter | Value | Meaning |
|---|---|---|
| `polarity` | -1 | PNP |

### `electromechanical/`

#### Push button — `push-button`

_A momentary switch: closed while it is held, open when it is let go_

`electromechanical/push-button.jmx` · type `button`

| Parameter | Value | Meaning |
|---|---|---|
| `closed` | 0 in 0 .. 1 step 1 integer · _live_ | Pressed |
| `R_closed` | 10 mOhm | contact resistance when pressed |
| `R_open` | 100 MOhm | leakage resistance when released |

**Pins:** `a` (first contact), `b` (second contact)

#### Switch — `switch`

_A single-pole switch, opened and closed while the circuit runs_

`electromechanical/switch.jmx` · type `switch`

| Parameter | Value | Meaning |
|---|---|---|
| `closed` | 0 in 0 .. 1 step 1 integer · _live_ | Closed |
| `R_closed` | 10 mOhm | contact resistance when closed |
| `R_open` | 100 MOhm | leakage resistance when open |

**Pins:** `a` (first contact), `b` (second contact)

### `instruments/`

#### Ammeter — `ammeter`

_Reads the current through it, put in the break of a wire_

`instruments/ammeter.jmx` · type `meter`

| Parameter | Value | Meaning |
|---|---|---|
| `R_shunt` | 50 mOhm | the shunt it reads across: what it drops, on a realistic bench |

**Pins:** `in` (where the current enters), `out` (where it leaves)

**Readings:** `reading = in.current unit A signal`

#### Duty cycle meter — `duty-cycle-meter`

_Reads the share of each period its input spends above a level, from one rising edge to the next_

`instruments/duty-cycle-meter.jmx` · type `meter`

| Parameter | Value | Meaning |
|---|---|---|
| `threshold` | 2.5 V | the level the signal is read against |
| `R_input` | 1 MOhm | input resistance: what it draws through, on a realistic bench |

**Pins:** `input` (signal), `reference` (what the signal is read against)

**States:** `rose` (discrete) — when the input last rose through the level; -1 before the first; `fell` (discrete) — when it last fell through it; -1 before the first; `duty` (discrete) — the share of the last whole period spent above the level

**Readings:** `reading = 100 * duty unit %`

#### Frequency meter — `frequency-meter`

_Reads how often its input rises through a level: one over the time between two rising edges_

`instruments/frequency-meter.jmx` · type `meter`

| Parameter | Value | Meaning |
|---|---|---|
| `threshold` | 0 V | the level a rising edge crosses |
| `R_input` | 1 MOhm | input resistance: what it draws through, on a realistic bench |

**Pins:** `input` (signal), `reference` (what the signal is read against)

**States:** `last_edge` (discrete) — when the input last rose through the level; -1 before the first; `frequency` (discrete) — one over the time between the last two rising edges

**Readings:** `reading = frequency unit Hz`

#### Voltmeter — `voltmeter`

_Reads the voltage of its red lead against its black one_

`instruments/voltmeter.jmx` · type `meter`

| Parameter | Value | Meaning |
|---|---|---|
| `R_input` | 10 MOhm | input resistance: what it draws through, on a realistic bench |

**Pins:** `plus` (red lead), `minus` (black lead)

**Readings:** `reading = plus - minus unit V signal`

#### Wattmeter — `wattmeter`

_Reads the power a load takes: its current through in and out, its voltage across plus and minus_

`instruments/wattmeter.jmx` · type `meter`

| Parameter | Value | Meaning |
|---|---|---|
| `R_input` | 10 MOhm | voltage input resistance: what it draws through, on a realistic bench |

**Pins:** `in` (current in), `out` (current out), `plus` (voltage, red lead), `minus` (voltage, black lead)

**Readings:** `reading = (plus - minus) * in.current unit W signal`

### `integrated/amplifiers/`

#### LM358 — `lm358`

_Dual operational amplifier, single supply 3 V to 32 V, inputs and output to ground_

`integrated/amplifiers/lm358.jmx` · type `opamp` · Family: `operational-amplifier` · fails: `short` · TI LM358 datasheet (SLOS068)

Inherits every parameter of [`operational-amplifier`](#operational-amplifier--operational-amplifier).

| Parameter | Value | Meaning |
|---|---|---|
| `open_loop_gain` | 100000 | open-loop gain, 100 dB (min 25 V/mV) |
| `gain_bandwidth` | 0.7 MHz | unity-gain bandwidth |
| `slew_rate` | 0.3 V/us | slew rate |
| `V_input_offset` | 0 V per unit | input offset; typically 2 mV either way |
| `I_input_bias` | 20 nA | input bias current, out of the input pins (PNP inputs) |
| `V_common_mode_high` | 1.5 V | the inputs work up to V+ - 1.5 V |
| `V_noise` | 40 nV | input voltage noise density at 1 kHz, 40 nV/√Hz |
| `I_quiescent` | 0.35 mA | supply current per unit |
| `R_output` | 50 Ohm |  |
| `I_output_source` | 40 mA | output short-circuit current, sourcing |
| `I_output_sink` | 20 mA | output short-circuit current, sinking |
| `V_output_headroom` | 1.5 V | the output stays this far below V+ |
| `V_output_low` | 5 mV | the output reaches this close to ground |
| `recovery` | 1e6 1/s | how fast the gain stage comes back out of saturation |
| `thermal_resistance` | 120 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 5 s | how fast it warms to that |
| `T_maximum` | 150 °C | past this it fails on a realistic bench |
| `V_offset_spread` | 7 mV | VIO at most 7 mV |
| `V_supply_maximum` | 32 V | the supply span it is rated for |

**Pins:** `output`, `inverting_input` (inverting), `non_inverting_input` (non-inverting), `Vcc`, `GND`

#### NE5532 — `ne5532`

_Dual low-noise bipolar operational amplifier, ±22 V, 10 MHz, 9 V/µs, drives 600 Ω_

`integrated/amplifiers/ne5532.jmx` · type `opamp` · Family: `operational-amplifier` · fails: `short` · TI NE5532 datasheet (SLOS075), electrical characteristics at ±15 V

Inherits every parameter of [`operational-amplifier`](#operational-amplifier--operational-amplifier).

| Parameter | Value | Meaning |
|---|---|---|
| `open_loop_gain` | 100000 | AVD 100 V/mV |
| `gain_bandwidth` | 10 MHz | unity-gain bandwidth |
| `second_pole` | 20 MHz | unity-gain stable: a phase margin near 60° |
| `slew_rate` | 9 V/us | slew rate |
| `V_input_offset` | 0 V per unit | input offset; typically 0.5 mV either way |
| `I_input_bias` | -200 nA | input bias current, into the input pins (NPN inputs) |
| `R_input` | 300 kOhm | differential input resistance |
| `V_noise` | 5 nV | input voltage noise density at 1 kHz, 5 nV/√Hz |
| `I_noise` | 0.7 pA | input current noise density at 1 kHz, 0.7 pA/√Hz |
| `noise_corner` | 50 Hz | 8 nV/√Hz at 30 Hz against 5 at 1 kHz: a corner near 50 Hz |
| `V_common_mode_low` | 2 V | VICR ±13 V at ±15 V |
| `V_common_mode_high` | 2 V | VICR ±13 V at ±15 V |
| `I_quiescent` | 4 mA | supply current per unit |
| `R_output` | 10 Ohm | output resistance |
| `I_output_source` | 38 mA | output short-circuit current |
| `I_output_sink` | 38 mA | output short-circuit current |
| `V_output_headroom` | 2 V | VOM ±13 V into 600 Ω at ±15 V |
| `V_output_low` | 2 V | the same towards V- |
| `V_supply_nominal` | 30 V | the supply span the figures are given at |
| `V_supply_maximum` | 44 V | the supply span it is rated for |
| `V_offset_spread` | 4 mV | VIO at most 4 mV |

**Pins:** `output`, `inverting_input` (inverting), `non_inverting_input` (non-inverting), `Vcc` (V+), `GND` (V-)

#### TL072 — `tl072`

_Dual JFET-input operational amplifier, ±18 V, 3 MHz, 13 V/µs_

`integrated/amplifiers/tl072.jmx` · type `opamp` · Family: `operational-amplifier` · fails: `short` · TI TL07x datasheet (SLOS080), electrical characteristics at ±15 V

Inherits every parameter of [`operational-amplifier`](#operational-amplifier--operational-amplifier).

| Parameter | Value | Meaning |
|---|---|---|
| `open_loop_gain` | 200000 | AVD 200 V/mV |
| `gain_bandwidth` | 3 MHz | unity-gain bandwidth |
| `second_pole` | 4.5 MHz | a phase margin near 55° at unity gain |
| `slew_rate` | 13 V/us | slew rate |
| `V_input_offset` | 0 V per unit | input offset; typically 3 mV either way |
| `I_input_bias` | 65 pA | input bias current: a JFET gate's leakage |
| `R_input` | 1 TOhm | input resistance |
| `V_common_mode_low` | 3 V | the inputs work down to V- + 3 V (VICR -12 V at ±15 V) |
| `V_common_mode_high` | 0 V | and up to V+ |
| `phase_reversal` | 1 | past the low limit the output swings to the other rail |
| `V_noise` | 18 nV | input voltage noise density at 1 kHz, 18 nV/√Hz |
| `I_noise` | 10 fA | input current noise density, 0.01 pA/√Hz: a JFET gate |
| `noise_corner` | 100 Hz | a JFET input's 1/f corner, about |
| `bias_doubling` | 10 K | a JFET gate's leakage doubles every 10 K |
| `I_quiescent` | 1.4 mA | supply current per unit |
| `R_output` | 75 Ohm | output resistance |
| `I_output_source` | 40 mA | output short-circuit current, about |
| `I_output_sink` | 40 mA | output short-circuit current, about |
| `V_output_headroom` | 1.5 V | VOM ±13.5 V into 10 kΩ at ±15 V |
| `V_output_low` | 1.5 V | the same towards V- |
| `V_supply_nominal` | 30 V | the supply span the figures are given at |
| `V_supply_maximum` | 36 V | the supply span it is rated for |
| `V_offset_spread` | 10 mV | VIO at most 10 mV |

**Pins:** `output`, `inverting_input` (inverting), `non_inverting_input` (non-inverting), `Vcc` (V+), `GND` (V-)

### `integrated/comparators/`

#### LM393 — `lm393`

_Dual comparator, open-collector outputs, single supply 2 V to 36 V, inputs to ground_

`integrated/comparators/lm393.jmx` · type `opamp` · Family: `operational-amplifier` · fails: `short` · TI LM393 datasheet (SLCS005), electrical characteristics at 5 V

Inherits every parameter of [`operational-amplifier`](#operational-amplifier--operational-amplifier).

| Parameter | Value | Meaning |
|---|---|---|
| `open_loop_gain` | 200000 | AVD 200 V/mV |
| `gain_bandwidth` | 1 MHz | how fast the output starts to move: response time about 1.3 µs |
| `slew_rate` | 10 V/us | the output's swing rate once it moves |
| `V_input_offset` | 0 V per unit | input offset; typically 1 mV either way |
| `I_input_bias` | 25 nA | input bias current, out of the input pins (PNP inputs) |
| `V_common_mode_high` | 1.5 V | the inputs work up to V+ - 1.5 V |
| `I_quiescent` | 0.2 mA | supply current per unit |
| `R_output` | 37 Ohm | the output transistor on: VOL 150 mV at 4 mA |
| `I_output_source` | 0.1 nA | open collector: only its leakage flows out |
| `I_output_sink` | 16 mA | output sink current |
| `V_output_headroom` | -40 V | open collector: the stage aims far above V+, so it never pulls the output down while off |
| `V_output_low` | 0 V | on, the output is pulled to ground through R_output |
| `V_supply_nominal` | 5 V | the supply the figures are given at |
| `V_supply_maximum` | 36 V | the supply span it is rated for |
| `V_offset_spread` | 5 mV | VIO at most 5 mV |

**Pins:** `output`, `inverting_input` (inverting), `non_inverting_input` (non-inverting), `Vcc`, `GND`

### `integrated/logic/`

#### 74HC595 — `74hc595`

_Eight-bit serial-in, parallel-out shift register with output latch and three-state outputs_

`integrated/logic/74hc595.jmx` · type `shift-register` · fails: `short` · Nexperia 74HC_HCT595 datasheet, Rev. 12

| Parameter | Value | Meaning |
|---|---|---|
| `propagation_delay` | 19 ns | typical SHCP to Q7S at 4.5 V |
| `latch_delay` | 20 ns | typical STCP to Qn at 4.5 V |
| `thermal_resistance` | 120 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 5 s | how fast it warms to that |
| `T_maximum` | 150 °C | past this it fails on a realistic bench |

**Pins:** `output_0`, `output_1`, `output_2`, `output_3`, `output_4`, `output_5`, `output_6`, `output_7`, `GND`, `serial_output`, `not_master_reset` (master reset, active low), `shift_clock` (shift clock), `storage_clock` (storage clock), `not_output_enable` (output enable, active low), `serial_data` (serial data), `Vcc`

#### 74HC74 — `74hc74`

_Dual D-type flip-flop, positive edge, asynchronous set and reset_

`integrated/logic/74hc74.jmx` · type `flip-flop` · fails: `short` · Nexperia 74HC74 datasheet

| Parameter | Value | Meaning |
|---|---|---|
| `propagation_delay` | 16 ns | clock to Q at 4.5 V (max 44 ns) |
| `setup` | 20 ns | D stable before the clock rises |
| `hold` | 3 ns | D stable after the clock rises |
| `thermal_resistance` | 120 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 5 s | how fast it warms to that |
| `T_maximum` | 150 °C | past this it fails on a realistic bench |

**Pins:** `not_clear` (clear, active low), `data`, `clock`, `not_preset` (preset, active low), `output`, `not_output`, `GND`, `Vcc`

**States:** `stored` (discrete) — what the flip-flop holds; unknown at power-up

### `integrated/power/`

#### ICE3B0365 — `ice3b0365`

_Off-line SMPS current-mode controller with integrated 650 V CoolMOS, 67 kHz, 6.45 Ohm_

`integrated/power/ice3b0365.jmx` · type `pwm-controller` · fails: `open` · Infineon CoolSET-F3 datasheet v2.3, 2013-04-02

| Parameter | Value | Meaning |
|---|---|---|
| `V_reference` | 6.5 V | VREF, trimmed reference (6.37 .. 6.63 V) |
| `oscillator_frequency` | 67 kHz | fOSC, fixed oscillator frequency, ICE3Bxx65 (61 .. 73 kHz) |
| `maximum_duty_cycle` | 0.72 | maximum duty cycle (0.67 .. 0.77) |
| `voltage_gain` | 3.7 | PWM-OP gain applied to the CS voltage (3.5 .. 3.9) |
| `V_ramp` | 0.85 V | voltage ramp max level: slope compensation added to the amplified CS |
| `V_feedback_minimum` | 0.3 V | below this FB voltage the duty cycle is zero (Dmin) |
| `R_feedback` | 20 kOhm | FB pull-up resistor to VREF (16 .. 27 kOhm) |
| `R_soft_start` | 50 kOhm | SoftS pull-up resistor to VREF (39 .. 62 kOhm) |
| `V_supply_on` | 15.0 V | VCC turn-on threshold (14.2 .. 15.8 V) |
| `V_supply_off` | 8.5 V | VCC turn-off threshold (8.0 .. 9.0 V) |
| `I_charge` | 1.05 mA | VCC charge current from the startup cell at VCC = 0 (0.55 .. 1.60 mA) |
| `I_start` | 160 uA | start-up current before the controller is active (max 220 uA) |
| `I_supply` | 5.5 mA | supply current with active gate, ICE3B0365 (max 7.0 mA) |
| `I_restart` | 300 uA | supply current in auto restart mode with inactive gate |
| `V_soft_start_begin` | 4.0 V | soft start ends: above this SoftS voltage there is no duty limitation |
| `V_soft_start_clamp` | 4.4 V | clamped SoftS voltage in normal operation (4.23 .. 4.57 V) |
| `V_soft_start_window` | 5.4 V | C3: end of the blanking window, overload or burst is decided here (5.2 .. 5.6 V) |
| `V_feedback_overload` | 4.8 V | C4: overload and open-loop limit (4.62 .. 4.98 V) |
| `V_feedback_burst` | 1.30 V | C5: active burst mode entry level (1.23 .. 1.37 V) |
| `V_feedback_burst_on` | 4.0 V | C6a: in burst mode, switching resumes above this (3.85 .. 4.15 V) |
| `V_feedback_burst_off` | 3.4 V | C6b: in burst mode, switching stops below this (3.25 .. 3.55 V) |
| `V_current_sense_threshold` | 1.02 V | peak current limitation, incl. propagation delay (0.97 .. 1.07 V) |
| `V_current_sense_burst` | 0.257 V | VCS2, the limit during active burst mode (0.232 .. 0.282 V) |
| `leading_edge_blanking_time` | 220 ns | leading-edge blanking |
| `R_drain_source_on` | 6.45 Ohm | drain-source on-resistance at 25 C, 0.3 A (max 7.50 Ohm; 13.7 Ohm at 125 C) |
| `C_output_energy` | 3.65 pF | Co(er), effective output capacitance, energy related, 0 .. 480 V |
| `edge_time` | 50 ns | gate drive edge; the driver slows the rising slope to about 130 ns (figure 11) |
| `thermal_resistance` | 90 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 10 s | how fast it warms to that |
| `T_maximum` | 150 °C | past this it fails on a realistic bench |

**Pins:** `soft_start` (SoftS: soft start, and the blanking window before auto restart or burst), `feedback` (FB: feedback from the optocoupler; regulation, burst and overload), `current_sense` (CS: current sense, and the CoolMOS source), `drain` (Drain of the 650 V CoolMOS; pins 4 and 5 are shorted inside the DIP-8), `Vcc` (controller supply, 8.5 .. 21 V in operation), `GND` (controller ground)

**States:** `running` (discrete) — the controller is on: VCC passed V_supply_on and has not fallen below V_supply_off; `restarting` (discrete) — auto restart: switched off by a fault, waiting for VCC to fall below V_supply_off; `gate` (discrete) — the PWM latch FF1: the CoolMOS is commanded on; `started` (discrete) — when the present switching cycle began, for the slope ramp and the blanking; `clamped` (discrete) — S1 holds SoftS at 4.4 V; released while FB is out of the regulation range; `burst` (discrete) — active burst mode is entered; `bursting` (discrete) — inside burst mode, the switching packet is on; `shown` (continuous) — the gate as the CoolMOS sees it, following the latch over the driver edge

#### LM317 — `lm317`

_Adjustable positive regulator, 1.25 V reference, up to 1.5 A_

`integrated/power/lm317.jmx` · type `regulator` · fails: `open` · TI LM317 datasheet SLVS044Z, Rev. Z

| Parameter | Value | Meaning |
|---|---|---|
| `V_reference` | 1.25 V | reference voltage at 25 C |
| `I_adjustment` | 50 uA | adjustment pin current, typical |
| `I_maximum` | 1.5 A | rated maximum output current |
| `V_dropout` | 2 V | typical input-output dropout |
| `transconductance` | 10 A/V | regulation transconductance; a finite loop gain |
| `thermal_resistance` | 50 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 30 s | how fast it warms to that |
| `T_maximum` | 150 °C | past this it fails on a realistic bench |

**Pins:** `adjust` (adjustment), `output` (regulated output; tab is also output), `input` (unregulated input)

### `integrated/references/`

#### TL431 — `tl431`

_Adjustable precision shunt reference, 2.495 V to 36 V, 1 mA to 100 mA_

`integrated/references/tl431.jmx` · type `reference` · fails: `short` · TI TL431 datasheet (SLVS543), electrical characteristics

| Parameter | Value | Meaning |
|---|---|---|
| `V_reference` | 2.495 V | Vref: the ref pin above the anode when it regulates |
| `reference_drift` | 0 | how Vref moves per kelvin, V/K: the TL431's is a shallow bowl of a few millivolts, none here |
| `T_nominal` | 298.15 K | the temperature Vref is given at |
| `transconductance` | 10 A/V | how hard the cathode current follows ref above Vref: dynamic impedance about 0.2 Ω |
| `I_minimum` | 0.4 mA | the cathode current below which it does not regulate |
| `I_reference` | 2 uA | Iref: current into the ref pin |
| `R_off` | 10 MOhm | cathode to anode while it does not conduct |
| `power_rating` | 0.7 W | what it is rated for |
| `thermal_resistance` | 160 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 2 s | how fast it warms to that |
| `T_maximum` | 150 °C | past this it fails on a realistic bench |

**Pins:** `ref` (reference input), `anode` (anode), `cathode` (cathode)

**States:** `heating` (continuous) — how far its own heat has raised it above the bench, kelvin; 0 on an ideal bench

### `integrated/timers/`

#### NE555 — `ne555`

_Precision timer, 4.5 V to 16 V_

`integrated/timers/ne555.jmx` · type `timer` · fails: `short` · TI NE555 datasheet (SLFS022)

| Parameter | Value | Meaning |
|---|---|---|
| `R_divider` | 5 kOhm | each of the three divider resistors |
| `V_output_high_drop` | 1.7 V | output high sits this far below Vcc |
| `V_output_low` | 0.1 V | output low |
| `R_output` | 2 Ohm |  |
| `I_output_limit` | 200 mA | output sink and source |
| `switching_time` | 100 ns | output rise and fall time |
| `R_discharge` | 10 Ohm | discharge transistor, on |
| `I_discharge` | 50 mA | discharge transistor, saturation |
| `V_reset` | 0.7 V | reset threshold |
| `I_trigger` | 0.5 uA | out of TRIG |
| `I_threshold` | 0.1 uA | into THR |
| `I_quiescent` | 3 mA | supply current at 5 V |
| `thermal_resistance` | 120 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 5 s | how fast it warms to that |
| `T_maximum` | 150 °C | past this it fails on a realistic bench |

**Pins:** `GND`, `trigger` (trigger: sets below Vcc / 3), `output`, `reset` (active low), `control` (control voltage, the 2/3 Vcc tap), `threshold` (threshold: resets above 2/3 Vcc), `discharge` (discharge, open collector), `Vcc`

**States:** `latched` (discrete) — the internal flip-flop; written by the behaviour, read by the equations; `shown` (continuous) — the output stage following it with a finite edge

### `passive/`

#### Capacitor — `capacitor`

_Two-terminal capacitor_

`passive/capacitor.jmx` · type `capacitor`

| Parameter | Value | Meaning |
|---|---|---|
| `capacitance` | 100 nF | capacitance |
| `tolerance` | 10 % | how far a part may be from its value, ±; a bench's corners use it |
| `temperature_coefficient` | 200 ppm/K | how its value moves per kelvin away from 25 °C; a bench's temperature uses it |
| `voltage_rating` | 50 V | the most it is made to hold |

**Pins:** `terminal_one` (first terminal), `terminal_two` (second terminal)

**Figures on the sheet:** `overvoltage` [%] — of its rated voltage; `charge` [%] — of rating

#### Ceramic capacitor — `ceramic-capacitor`

_MLCC, X7R: capacitance falling with DC bias and temperature, ESR, ESL_

`passive/ceramic-capacitor.jmx` · type `capacitor` · fails: `short` · the DC-bias and temperature curves an X7R MLCC's datasheet gives

| Parameter | Value | Meaning |
|---|---|---|
| `capacitance` | 10 uF | nominal capacitance, at 0 V and 25 C |
| `V_half` | 8 V | the DC bias at which half the capacitance is left |
| `R_equivalent_series` | 5 mOhm | ESR |
| `L_equivalent_series` | 1 nH | ESL: package and mounting |
| `temperature_curvature` | 1.5e-5 | X7R: about -15 % at 100 K either side of 25 C, as a share per kelvin squared |
| `T_nominal` | 298.15 K | the temperature the capacitance is given at |
| `voltage_rating` | 25 V | the rated voltage |
| `power_rating` | 0.1 W | what it is rated for |
| `thermal_resistance` | 200 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 10 s | how fast it warms to that |
| `T_maximum` | 125 °C | past this it fails on a realistic bench |

**Pins:** `terminal_one` (first terminal), `terminal_two` (second terminal)

**Figures on the sheet:** `overvoltage` [%] — of its rated voltage; `charge` [%] — of rating

#### Electrolytic capacitor — `electrolytic-capacitor`

_Aluminium electrolytic capacitor: capacitance, ESR, leakage_

`passive/electrolytic-capacitor.jmx` · type `capacitor` · fails: `short` · the equivalent circuit an electrolytic's datasheet tabulates: C, ESR at 100 kHz, leakage current

| Parameter | Value | Meaning |
|---|---|---|
| `capacitance` | 1000 uF | capacitance |
| `R_equivalent_series` | 0.1 Ohm | ESR |
| `R_leakage` | 1 MOhm | leakage: the datasheet's I = 0.01 · C · V, as a resistance |
| `ESR_doubling` | 20 K | the ESR doubles for every this many kelvin colder: an electrolyte thickens in the cold |
| `T_nominal` | 298.15 K | the temperature the ESR is given at |
| `power_rating` | 0.5 W | what the ripple may heat it with |
| `thermal_resistance` | 40 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 60 s | how fast it warms to that |
| `T_maximum` | 105 °C | past this it fails on a realistic bench |
| `voltage_rating` | 25 V | the rated voltage |

**Pins:** `positive` (positive, the longer lead), `negative` (negative, the striped side)

**Figures on the sheet:** `overvoltage` [%] — of its rated voltage; `charge` [%] — of rating

**States:** `heating` (continuous) — how far its own heat has raised it above the bench, kelvin; 0 on an ideal bench

#### Inductor — `inductor`

_Two-terminal inductor_

`passive/inductor.jmx` · type `inductor`

| Parameter | Value | Meaning |
|---|---|---|
| `inductance` | 1 mH | inductance |

**Pins:** `terminal_one` (first terminal), `terminal_two` (second terminal)

#### Potentiometer — `potentiometer`

_A resistance with a wiper, moved while the circuit runs_

`passive/potentiometer.jmx` · type `potentiometer` · fails: `open`

| Parameter | Value | Meaning |
|---|---|---|
| `position` | 0.5 in 0 .. 1 step 0.01 · _live_ | Wiper position |
| `resistance` | 10 kOhm | end-to-end resistance |
| `R_wiper` | 1 Ohm | wiper contact resistance |
| `power_rating` | 0.5 W | what it is rated for |
| `thermal_resistance` | 100 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 20 s | how fast it warms to that |
| `T_maximum` | 125 °C | past this it fails on a realistic bench |

**Pins:** `a` (first end), `w` (wiper), `b` (second end)

#### Power inductor — `power-inductor`

_Power inductor: inductance, winding resistance, core saturation_

`passive/power-inductor.jmx` · type `inductor` · fails: `open` · the figures a power inductor's datasheet gives: L, DCR, ISAT

| Parameter | Value | Meaning |
|---|---|---|
| `inductance` | 100 uH | inductance at small currents |
| `I_saturation` | 2 A | ISAT: past it the core saturates and the inductance falls away |
| `R_winding` | 50 mOhm | DCR: the winding's resistance |
| `copper_temperature_coefficient` | 0.00393 | copper: the winding's resistance rises 0.393 % per kelvin |
| `saturation_temperature_coefficient` | -0.004 | a ferrite core saturates earlier when hot, about 0.4 % per kelvin |
| `T_nominal` | 298.15 K | the temperature the figures are given at |
| `power_rating` | 1 W | what it is rated for |
| `thermal_resistance` | 50 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 30 s | how fast it warms to that |
| `T_maximum` | 125 °C | past this it fails on a realistic bench |

**Pins:** `terminal_one` (first terminal), `terminal_two` (second terminal)

**States:** `heating` (continuous) — how far its own heat has raised it above the bench, kelvin; 0 on an ideal bench

#### Resistor — `resistor`

_Two-terminal resistor_

`passive/resistor.jmx` · type `resistor` · fails: `open`

| Parameter | Value | Meaning |
|---|---|---|
| `resistance` | 1 kOhm | resistance |
| `tolerance` | 1 % | how far a part may be from its value, ±; a bench's corners use it |
| `temperature_coefficient` | 100 ppm/K | how its value moves per kelvin away from 25 °C; a bench's temperature uses it |
| `power_rating` | 0.25 W | what it is rated for |
| `thermal_resistance` | 250 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 10 s | how fast it warms to that |
| `T_maximum` | 155 °C | past this it fails on a realistic bench |

**Pins:** `terminal_one` (first terminal), `terminal_two` (second terminal)

#### Transformer — `transformer`

_Two coupled windings: self and mutual inductance, leakage, winding resistance_

`passive/transformer.jmx` · type `transformer` · fails: `open` · coupled inductors as in Berkeley SPICE3 / ngspice 47 (L, L, K)

| Parameter | Value | Meaning |
|---|---|---|
| `L_primary` | 10 H | primary inductance: the magnetising inductance seen from the primary |
| `L_secondary` | 0.1 H | secondary inductance: turns ratio √(L1 / L2) = 10 |
| `coupling` | 0.999 | K: 1 is a perfect core; below it, leakage |
| `R_primary` | 10 Ohm | primary winding resistance |
| `R_secondary` | 0.1 Ohm | secondary winding resistance |
| `copper_temperature_coefficient` | 0.00393 | copper: the windings' resistance rises 0.393 % per kelvin |
| `T_nominal` | 298.15 K | the temperature the resistances are given at |
| `power_rating` | 10 W | what it is rated for |
| `thermal_resistance` | 10 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 300 s | how fast it warms to that |
| `T_maximum` | 120 °C | past this it fails on a realistic bench |

**Pins:** `primary_one` (primary, dotted end), `primary_two` (primary), `secondary_one` (secondary, dotted end), `secondary_two` (secondary)

**States:** `heating` (continuous) — how far its own heat has raised it above the bench, kelvin; 0 on an ideal bench

### `semiconductors/diodes/`

#### 1N4007 — `1n4007`

_General-purpose rectifier, 1000 V, 1 A_

`semiconductors/diodes/1n4007.jmx` · type `diode` · Family: `diode` · fails: `short` · onsemi / Vishay 1N4001–1N4007 datasheets; starting point: the common vendor SPICE model

Inherits every parameter of [`diode`](#diode--diode).

| Parameter | Value | Meaning |
|---|---|---|
| `I_saturation` | 7.03 nA | IS |
| `emission_coefficient` | 1.808 | N |
| `R_series` | 34 mOhm | RS |
| `C_junction_zero_bias` | 10 pF | CJO |
| `V_junction` | 0.7 V | VJ |
| `grading_coefficient` | 0.5 | M |
| `transit_time` | 4 us | TT: a standard-recovery rectifier stores microseconds of charge |
| `V_breakdown` | 1200 V | avalanche above the rated 1000 V |
| `I_breakdown` | 10 uA | current at V_breakdown |
| `E_gap` | 1.057 V | EG |
| `saturation_temperature_exponent` | 5 | XTI |
| `power_rating` | 1.25 W | what it is rated for |
| `thermal_resistance` | 100 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 2 s | how fast it warms to that |
| `T_maximum` | 150 °C | past this it fails on a realistic bench |
| `V_reverse_maximum` | 1000 V | VRRM |
| `I_forward_maximum` | 1 A | IF(AV) |

**Pins:** `anode` (anode), `cathode` (cathode, the banded end)

#### 1N4148 — `1n4148`

_Small-signal fast switching diode, 100 V_

`semiconductors/diodes/1n4148.jmx` · type `diode` · Family: `diode` · fails: `short` · onsemi 1N4148 datasheet; starting point: the common vendor SPICE model

Inherits every parameter of [`diode`](#diode--diode).

| Parameter | Value | Meaning |
|---|---|---|
| `I_saturation` | 2.52 nA | saturation current |
| `emission_coefficient` | 1.752 | emission coefficient |
| `R_series` | 0.568 Ohm | series resistance |
| `C_junction_zero_bias` | 4 pF | zero-bias junction capacitance |
| `V_junction` | 0.75 V | junction potential |
| `grading_coefficient` | 0.4 | grading coefficient |
| `transit_time` | 5 ns | transit time; the vendor model says 20 ns and then misses trr of 4 ns |
| `V_breakdown` | 100 V | reverse breakdown voltage |
| `I_breakdown` | 100 uA | current at V_breakdown |
| `power_rating` | 0.5 W | what it is rated for |
| `thermal_resistance` | 350 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 1 s | how fast it warms to that |
| `T_maximum` | 200 °C | past this it fails on a realistic bench |
| `V_reverse_maximum` | 100 V | VRRM |
| `I_forward_maximum` | 300 mA | IF, continuous |

**Pins:** `anode` (anode), `cathode` (cathode, the banded end)

#### 1N5819 — `1n5819`

_Schottky barrier rectifier, 40 V, 1 A_

`semiconductors/diodes/1n5819.jmx` · type `diode` · Family: `diode` · fails: `short` · onsemi 1N5817–1N5819 datasheet; starting point: the common vendor SPICE model

Inherits every parameter of [`diode`](#diode--diode).

| Parameter | Value | Meaning |
|---|---|---|
| `I_saturation` | 31.7 uA | IS: a Schottky barrier's large saturation current |
| `emission_coefficient` | 1.373 | N |
| `R_series` | 51 mOhm | RS |
| `C_junction_zero_bias` | 110 pF | CJO |
| `V_junction` | 0.35 V | VJ |
| `grading_coefficient` | 0.35 | M |
| `transit_time` | 0 s | a majority-carrier junction stores no charge |
| `V_breakdown` | 50 V | avalanche above the rated 40 V |
| `I_breakdown` | 1 mA | current at V_breakdown |
| `E_gap` | 0.69 V | EG: the barrier height |
| `saturation_temperature_exponent` | 2 | XTI |
| `power_rating` | 1.25 W | what it is rated for |
| `thermal_resistance` | 100 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 2 s | how fast it warms to that |
| `T_maximum` | 125 °C | past this it fails on a realistic bench |
| `V_reverse_maximum` | 40 V | VRRM |
| `I_forward_maximum` | 1 A | IF(AV) |

**Pins:** `anode` (anode), `cathode` (cathode, the banded end)

#### Bridge rectifier, 1 A 400 V — `bridge-rectifier`

_Four junctions in one package: + and − out of two alternating inputs, two forward drops in the path_

`semiconductors/diodes/bridge-rectifier.jmx` · type `rectifier-bridge` · fails: `short` · typical 1 A, 400 V glass-passivated bridge (W04M class): VF 1.0 V max at 1 A per element

| Parameter | Value | Meaning |
|---|---|---|
| `I_saturation` | 1.2 nA | each junction's saturation current |
| `emission_coefficient` | 1.8 | each junction's emission coefficient |
| `R_series` | 50 mOhm | each element's bulk resistance |
| `C_junction` | 15 pF | each junction's capacitance |
| `V_breakdown` | 400 V | each element's reverse breakdown |
| `I_breakdown` | 10 uA | current at V_breakdown |
| `T_nominal` | 298.15 K | the temperature the figures above are measured at |
| `saturation_temperature_exponent` | 3 | XTI: how steeply I_saturation follows the temperature |
| `E_gap` | 1.11 V | silicon's band gap, in volts |
| `thermal_resistance` | 40 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 5 s | how fast it warms to that |
| `T_maximum` | 150 °C | past this it fails on a realistic bench |

**Pins:** `ac1` (first alternating input), `ac2` (second alternating input), `plus` (rectified output, +), `minus` (rectified output, −)

#### BZX55C5V1 — `bzx55c5v1`

_Zener diode, 5.1 V ±5 %, 500 mW_

`semiconductors/diodes/bzx55c5v1.jmx` · type `diode` · Family: `diode` · fails: `short` · Vishay BZX55 series datasheet, electrical characteristics

Inherits every parameter of [`diode`](#diode--diode).

| Parameter | Value | Meaning |
|---|---|---|
| `I_saturation` | 10 fA | IS |
| `emission_coefficient` | 1 | N |
| `R_series` | 20 Ohm | most of the dynamic resistance, rzj 60 Ω at most at 5 mA |
| `C_junction_zero_bias` | 150 pF | CJO |
| `V_breakdown` | 5.0 V | BV: the Zener voltage, reached at IBV |
| `I_breakdown` | 5 mA | IBV: the test current IZT |
| `breakdown_temperature_coefficient` | 0.0001 | TCV: a 5.1 V Zener sits near zero tempco, about +0.5 mV/K |
| `power_rating` | 0.5 W | what it is rated for |
| `thermal_resistance` | 300 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 1 s | how fast it warms to that |
| `T_maximum` | 175 °C | past this it fails on a realistic bench |
| `I_forward_maximum` | 98 mA | IZM: 0.5 W at 5.1 V; it works reversed, so only its current is a limit |

**Pins:** `anode` (anode), `cathode` (cathode, the banded end)

### `semiconductors/optoelectronics/`

#### Red LED — `led-red`

_Red light-emitting diode, 5 mm, 20 mA_

`semiconductors/optoelectronics/led-red.jmx` · type `diode` · Family: `diode` · fails: `open` · a typical 5 mm red LED datasheet (VF 2.0 V typical, 2.5 V at most at 20 mA; VR 5 V)

Inherits every parameter of [`diode`](#diode--diode).

| Parameter | Value | Meaning |
|---|---|---|
| `I_saturation` | 0.00066 fA | IS: the wide band gap's tiny saturation current (jMX has no atto prefix) |
| `emission_coefficient` | 2 | N |
| `R_series` | 2.5 Ohm | RS |
| `C_junction_zero_bias` | 40 pF | CJO |
| `V_breakdown` | 10 V | reverse breakdown; the rating is 5 V |
| `I_breakdown` | 10 uA | current at V_breakdown |
| `E_gap` | 1.9 V | the band gap: red light |
| `power_rating` | 0.1 W | what it is rated for |
| `thermal_resistance` | 400 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 1 s | how fast it warms to that |
| `I_rated` | 20 mA | the current its brightness is given at |
| `light_temperature_coefficient` | -0.01 | red light falls by about 1 % per kelvin of junction temperature |
| `T_nominal` | 298.15 K | the temperature its brightness is given at |
| `T_maximum` | 100 °C | past this it fails on a realistic bench |
| `V_reverse_maximum` | 5 V | VR |
| `I_forward_maximum` | 30 mA | IF, continuous |

**Pins:** `anode` (anode, the longer lead), `cathode` (cathode, the flat side)

**Figures on the sheet:** `overvoltage` [%] — of VR; `overcurrent` [%] — of its rated current; `light` [%] — of its rated brightness

#### PC817 — `pc817`

_Optocoupler, phototransistor output, CTR 50 … 600 %, 5 kV isolation_

`semiconductors/optoelectronics/pc817.jmx` · type `optocoupler` · fails: `open` · Sharp PC817 datasheet, electrical characteristics

| Parameter | Value | Meaning |
|---|---|---|
| `I_saturation` | 4.2 fA | the LED's saturation current: VF 1.2 V at 20 mA |
| `emission_coefficient` | 1.6 | the LED's emission coefficient |
| `R_series` | 2 Ohm | the LED's series resistance |
| `current_transfer_ratio` | 1 | CTR: collector current over LED current, 0.5 … 6 by rank |
| `transfer_temperature_coefficient` | -0.004 | the CTR falls by this share per kelvin above 25 C: about 0.8 at 75 C |
| `T_nominal` | 298.15 K | the temperature the CTR is given at |
| `V_saturation` | 0.1 V | the VCE a saturated phototransistor does not go below |
| `V_saturation_knee` | 0.05 V | how sharply it comes out of saturation above that |
| `V_early` | 100 V | the collector current's rise with VCE |
| `I_dark` | 1 nA | collector current with the LED off |
| `power_rating` | 0.2 W | what it is rated for |
| `thermal_resistance` | 400 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 2 s | how fast it warms to that |
| `T_maximum` | 100 °C | past this it fails on a realistic bench |

**Pins:** `anode` (LED anode), `cathode` (LED cathode), `emitter` (phototransistor emitter), `collector` (phototransistor collector)

**States:** `heating` (continuous) — how far its own heat has raised it above the bench, kelvin; 0 on an ideal bench

### `semiconductors/thyristors/`

#### 2N5060 — `2n5060`

_Sensitive-gate thyristor, 30 V, 0.8 A, IGT 200 µA_

`semiconductors/thyristors/2n5060.jmx` · type `thyristor` · fails: `short` · onsemi 2N5060 series datasheet, electrical characteristics

| Parameter | Value | Meaning |
|---|---|---|
| `V_gate_trigger` | 0.6 V | VGT: the gate voltage that fires it (0.8 V at most) |
| `I_gate_saturation` | 14 fA | the gate–cathode junction: 200 µA at VGT |
| `V_on` | 0.9 V | the on-state voltage's knee |
| `R_on` | 0.5 Ohm | the on-state slope: VTM 1.7 V at 1.2 A at most |
| `I_holding` | 3 mA | IH: below this anode current it turns off (5 mA at most) |
| `R_off` | 100 MOhm | anode to cathode while it blocks |
| `power_rating` | 0.6 W | what it is rated for |
| `thermal_resistance` | 200 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 2 s | how fast it warms to that |
| `T_maximum` | 125 °C | past this it fails on a realistic bench |

**Pins:** `cathode` (cathode), `gate` (gate), `anode` (anode)

**States:** `heating` (continuous) — how far its own heat has raised it above the bench, kelvin; 0 on an ideal bench; `conducting` (discrete) — latched on: written by the behaviour, read by the equations

#### BT136 — `bt136`

_Triac, 600 V, 4 A, logic-level-sensitive gate_

`semiconductors/thyristors/bt136.jmx` · type `thyristor` · fails: `short` · NXP / WeEn BT136 series datasheet, electrical characteristics

| Parameter | Value | Meaning |
|---|---|---|
| `V_gate_trigger` | 0.7 V | VGT typical (1.5 V at most) |
| `I_gate_saturation` | 50 fA | the gate junction: a few milliamperes at VGT |
| `V_on` | 0.9 V | the on-state voltage's knee |
| `R_on` | 0.15 Ohm | the on-state slope: VT 1.7 V at 5 A at most |
| `I_holding` | 5 mA | IH: below this current it turns off (15 mA at most) |
| `R_off` | 100 MOhm | between the main terminals while it blocks |
| `power_rating` | 2 W | what it is rated for without a heatsink |
| `thermal_resistance` | 60 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 30 s | how fast it warms to that |
| `T_maximum` | 125 °C | past this it fails on a realistic bench |

**Pins:** `main_terminal_one` (MT1, the gate's reference), `main_terminal_two` (MT2, the tab), `gate` (gate)

**States:** `heating` (continuous) — how far its own heat has raised it above the bench, kelvin; 0 on an ideal bench; `conducting` (discrete) — latched on: written by the behaviour, read by the equations

### `semiconductors/transistors/`

#### 2N2222A — `2n2222`

_NPN small-signal switching transistor, 40 V, 600 mA_

`semiconductors/transistors/2n2222.jmx` · type `bjt` · Family: `npn-transistor` → `bipolar-transistor` · fails: `open` · onsemi P2N2222A datasheet P2N2222A/D, Rev. 7, and its SPICE model

Inherits every parameter of [`bipolar-transistor`](#bipolar-transistor--bipolar-transistor).

| Parameter | Value | Meaning |
|---|---|---|
| `I_saturation` | 14.34 fA | IS: transport saturation current |
| `beta_forward` | 255.9 | BF: ideal forward current gain |
| `V_early` | 74.03 V | VAF: forward Early voltage |
| `I_knee` | 0.2847 A | IKF: above it the gain falls |
| `I_leakage_base_emitter` | 14.34 fA | ISE: below a milliampere the gain falls |
| `leakage_emission_base_emitter` | 1.307 | NE |
| `beta_reverse` | 6.092 | BR: ideal reverse current gain |
| `R_base` | 10 Ohm | RB: base spreading resistance |
| `R_collector` | 0.3 Ohm | collector bulk resistance: most of VCE(sat) at high current |
| `R_emitter` | 0.1 Ohm | emitter resistance |
| `C_base_emitter` | 22.01 pF | CJE |
| `grading_base_emitter` | 0.377 | MJE |
| `C_base_collector` | 7.306 pF | CJC |
| `grading_base_collector` | 0.3416 | MJC |
| `transit_time_forward` | 411.1 ps | TF: fT about 300 MHz |
| `transit_time_bias_dependence` | 3 | XTF |
| `transit_time_bias_voltage` | 1.7 V | VTF |
| `transit_time_current` | 0.6 A | ITF |
| `transit_time_reverse` | 46.91 ns | TR: the storage time of a saturated switch |
| `beta_temperature_exponent` | 1.5 | XTB |
| `T_nominal` | 298.15 K | the temperature the figures above are measured at |
| `V_breakdown` | 60 V | avalanche onset: an approximation — VCEO is guaranteed to 40 V, a typical part holds more |
| `power_rating` | 0.625 W | what it is rated for |
| `thermal_resistance` | 200 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 2 s | how fast it warms to that |
| `T_maximum` | 150 °C | past this it fails on a realistic bench |
| `beta_forward_spread` | 0.5 | hFE 100 ... 300 at 150 mA: about half either way |
| `V_collector_emitter_maximum` | 40 V | VCEO |
| `I_collector_maximum` | 600 mA | IC, continuous |

**Pins:** `emitter` (emitter), `base` (base), `collector` (collector)

#### 2N2907A — `2n2907`

_PNP small-signal switching transistor, -60 V, -600 mA_

`semiconductors/transistors/2n2907.jmx` · type `bjt` · Family: `pnp-transistor` → `bipolar-transistor` · fails: `open` · onsemi PN2907A datasheet PN2907A/D; model: the standard 2N2907A Gummel–Poon parameters

Inherits every parameter of [`bipolar-transistor`](#bipolar-transistor--bipolar-transistor).

| Parameter | Value | Meaning |
|---|---|---|
| `I_saturation` | 0.6506 fA | IS: transport saturation current |
| `beta_forward` | 231.7 | BF: ideal forward current gain |
| `V_early` | 115.7 V | VAF: forward Early voltage |
| `I_knee` | 1.079 A | IKF: above it the gain falls |
| `I_leakage_base_emitter` | 54.81 fA | ISE: below a milliampere the gain falls |
| `leakage_emission_base_emitter` | 1.829 | NE |
| `beta_reverse` | 3.563 | BR: ideal reverse current gain |
| `R_base` | 10 Ohm | RB: base spreading resistance |
| `R_collector` | 0.715 Ohm | RC: collector resistance |
| `R_emitter` | 0.1 Ohm | RE: emitter resistance — not in the model; a bond wire's worth |
| `C_base_emitter` | 19.82 pF | CJE |
| `grading_base_emitter` | 0.3357 | MJE |
| `C_base_collector` | 14.76 pF | CJC |
| `grading_base_collector` | 0.5383 | MJC |
| `transit_time_forward` | 603.7 ps | TF: fT about 200 MHz |
| `transit_time_bias_dependence` | 1.7 | XTF |
| `transit_time_bias_voltage` | 5 V | VTF |
| `transit_time_current` | 0.65 A | ITF |
| `transit_time_reverse` | 111.3 ns | TR: the storage time of a saturated switch |
| `beta_temperature_exponent` | 1.5 | XTB |
| `T_nominal` | 298.15 K | the temperature the figures above are measured at |
| `V_breakdown` | 80 V | avalanche onset: an approximation — VCEO is guaranteed to 60 V |
| `power_rating` | 0.625 W | what it is rated for |
| `thermal_resistance` | 200 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 2 s | how fast it warms to that |
| `T_maximum` | 150 °C | past this it fails on a realistic bench |
| `beta_forward_spread` | 0.5 | hFE 100 ... 300 at 150 mA: about half either way |
| `V_collector_emitter_maximum` | 60 V | VCEO |
| `I_collector_maximum` | 600 mA | IC, continuous |

**Pins:** `emitter` (emitter), `base` (base), `collector` (collector)

#### 2N5457 — `2n5457`

_N-channel general-purpose JFET, 25 V, IDSS 1 … 5 mA_

`semiconductors/transistors/2n5457.jmx` · type `jfet` · Family: `n-jfet` → `junction-field-effect-transistor` · fails: `open` · onsemi 2N5457 datasheet, electrical characteristics

Inherits every parameter of [`junction-field-effect-transistor`](#junction-fet--junction-field-effect-transistor).

| Parameter | Value | Meaning |
|---|---|---|
| `V_threshold` | -1.372 V | VTO: inside VGS(off) -0.5 … -6 V |
| `transconductance` | 1.125 mA/V^2 | BETA: IDSS about 2.1 mA |
| `channel_length_modulation` | 0.0023 | LAMBDA |
| `R_drain` | 1 Ohm | RD |
| `R_source` | 1 Ohm | RS |
| `I_saturation` | 181.3 fA | IS |
| `C_gate_source` | 4.5 pF | CGS |
| `C_gate_drain` | 6 pF | CGD |
| `V_junction` | 0.5 V | PB |
| `T_nominal` | 298.15 K | the temperature the figures above are measured at |
| `threshold_temperature_coefficient` | 0.002 | VTO moves towards zero by about 2 mV per kelvin |
| `V_threshold_spread` | 1 V | VGS(off) -0.5 ... -6 V |
| `transconductance_spread` | 0.5 | IDSS 1 ... 5 mA |

**Pins:** `source` (source), `gate` (gate), `drain` (drain)

#### 2N7000 — `2n7000`

_N-channel enhancement MOSFET, 60 V, 200 mA continuous_

`semiconductors/transistors/2n7000.jmx` · type `mosfet` · Family: `n-mosfet` → `mos-transistor` · fails: `short` · onsemi 2N7000/2N7002/NDS7002A datasheet NDS7002A/D, Rev. 11

Inherits every parameter of [`mos-transistor`](#mos-transistor--mos-transistor).

| Parameter | Value | Meaning |
|---|---|---|
| `V_threshold` | 2.1 V | VTO: typical gate threshold at 1 mA |
| `transconductance` | 0.484 A/V^2 | KP: fits RDS(on) 1.2 Ω at VGS 10 V and 1.8 Ω at 4.5 V with RD + RS |
| `R_drain` | 0.84 Ohm | RD: most of what the gate cannot reduce |
| `R_source` | 0.1 Ohm | RS |
| `I_saturation` | 15 pA | IS: body diode saturation current |
| `emission_coefficient` | 1.5 | N: body diode emission factor |
| `C_bulk_drain` | 7 pF | CBD: Coss - Crss at zero bias |
| `C_gate_source` | 16 pF | CGSO: Ciss - Crss |
| `C_gate_drain` | 4 pF | CGDO: Crss |
| `T_nominal` | 298.15 K | the temperature the figures above are measured at |
| `V_threshold_coefficient` | -0.005 | the gate turns on earlier when hot, V/K |
| `resistance_temperature_exponent` | 1.5 | RDS(on) normalised to temperature: about 1.5x at 125 C |
| `V_breakdown` | 60 V | drain-source breakdown at 10 uA |
| `I_breakdown` | 10 uA | avalanche current at V_breakdown |
| `power_rating` | 0.4 W | what it is rated for |
| `thermal_resistance` | 312 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 2 s | how fast it warms to that |
| `T_maximum` | 150 °C | past this it fails on a realistic bench |
| `V_threshold_spread` | 0.9 V | VGS(th) 0.8 ... 3 V |
| `V_drain_source_maximum` | 60 V | VDS |
| `V_gate_source_maximum` | 20 V | VGS, either way |
| `I_drain_maximum` | 500 mA | ID 200 mA continuous, 500 mA pulsed |

**Pins:** `source` (source), `gate` (gate), `drain` (drain)

#### BC547B — `bc547`

_NPN general-purpose transistor, 45 V, 100 mA, gain group B_

`semiconductors/transistors/bc547.jmx` · type `bjt` · Family: `npn-transistor` → `bipolar-transistor` · fails: `open` · NXP BC547 datasheet and its SPICE model

Inherits every parameter of [`bipolar-transistor`](#bipolar-transistor--bipolar-transistor).

| Parameter | Value | Meaning |
|---|---|---|
| `I_saturation` | 23.9 fA | IS |
| `beta_forward` | 294.3 | BF |
| `emission_coefficient` | 1.008 | NF |
| `V_early` | 63.2 V | VAF |
| `I_knee` | 0.1357 A | IKF |
| `I_leakage_base_emitter` | 3.545 fA | ISE |
| `leakage_emission_base_emitter` | 1.541 | NE |
| `beta_reverse` | 7.946 | BR |
| `emission_coefficient_reverse` | 1.004 | NR |
| `V_early_reverse` | 25.9 V | VAR |
| `I_knee_reverse` | 0.1144 A | IKR |
| `I_leakage_base_collector` | 62.72 fA | ISC |
| `leakage_emission_base_collector` | 1.243 | NC |
| `R_base` | 1 Ohm | RB |
| `R_emitter` | 0.4683 Ohm | RE |
| `R_collector` | 0.85 Ohm | RC |
| `C_base_emitter` | 13.58 pF | CJE |
| `V_junction_base_emitter` | 0.65 V | VJE |
| `grading_base_emitter` | 0.3279 | MJE |
| `C_base_collector` | 3.728 pF | CJC |
| `V_junction_base_collector` | 0.3997 V | VJC |
| `grading_base_collector` | 0.2955 | MJC |
| `internal_base_collector_fraction` | 0.6193 | XCJC |
| `transit_time_forward` | 0.4391 ns | TF: fT about 300 MHz |
| `transit_time_bias_dependence` | 120 | XTF |
| `transit_time_bias_voltage` | 2.643 V | VTF |
| `transit_time_current` | 0.7495 A | ITF |
| `transit_time_reverse` | 10 ns | TR |
| `T_nominal` | 298.15 K | the temperature the figures above are measured at |
| `V_breakdown` | 65 V | avalanche onset: VCEO is guaranteed to 45 V |
| `power_rating` | 0.5 W | what it is rated for |
| `thermal_resistance` | 250 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 2 s | how fast it warms to that |
| `T_maximum` | 150 °C | past this it fails on a realistic bench |
| `beta_forward_spread` | 0.38 | gain group B, hFE 200 ... 450 |
| `V_collector_emitter_maximum` | 45 V | VCEO |
| `I_collector_maximum` | 100 mA | IC, continuous |

**Pins:** `emitter` (emitter), `base` (base), `collector` (collector)

#### BC557B — `bc557`

_PNP general-purpose transistor, -45 V, -100 mA, gain group B_

`semiconductors/transistors/bc557.jmx` · type `bjt` · Family: `pnp-transistor` → `bipolar-transistor` · fails: `open` · NXP BC557 datasheet and its SPICE model

Inherits every parameter of [`bipolar-transistor`](#bipolar-transistor--bipolar-transistor).

| Parameter | Value | Meaning |
|---|---|---|
| `I_saturation` | 38.3 fA | IS |
| `beta_forward` | 344.4 | BF |
| `emission_coefficient` | 1.008 | NF |
| `V_early` | 21.11 V | VAF |
| `I_knee` | 80.39 mA | IKF |
| `I_leakage_base_emitter` | 12.2 fA | ISE |
| `leakage_emission_base_emitter` | 1.528 | NE |
| `beta_reverse` | 14.84 | BR |
| `emission_coefficient_reverse` | 1.005 | NR |
| `V_early_reverse` | 32.02 V | VAR |
| `I_knee_reverse` | 47 mA | IKR |
| `I_leakage_base_collector` | 0.285 pA | ISC |
| `leakage_emission_base_collector` | 1.28 | NC |
| `R_base` | 1 Ohm | RB |
| `R_emitter` | 0.6202 Ohm | RE |
| `R_collector` | 0.5713 Ohm | RC |
| `C_base_emitter` | 12.3 pF | CJE |
| `V_junction_base_emitter` | 0.6106 V | VJE |
| `grading_base_emitter` | 0.378 | MJE |
| `C_base_collector` | 10.8 pF | CJC |
| `V_junction_base_collector` | 0.1022 V | VJC |
| `grading_base_collector` | 0.3563 | MJC |
| `internal_base_collector_fraction` | 0.6288 | XCJC |
| `transit_time_forward` | 0.56 ns | TF: fT about 150 MHz |
| `transit_time_bias_dependence` | 3.414 | XTF |
| `transit_time_bias_voltage` | 5.23 V | VTF |
| `transit_time_current` | 0.1483 A | ITF |
| `transit_time_reverse` | 10 ns | TR |
| `T_nominal` | 298.15 K | the temperature the figures above are measured at |
| `V_breakdown` | 65 V | avalanche onset: VCEO is guaranteed to 45 V |
| `power_rating` | 0.5 W | what it is rated for |
| `thermal_resistance` | 250 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 2 s | how fast it warms to that |
| `T_maximum` | 150 °C | past this it fails on a realistic bench |
| `beta_forward_spread` | 0.37 | gain group B, hFE 220 ... 475 |
| `V_collector_emitter_maximum` | 45 V | VCEO |
| `I_collector_maximum` | 100 mA | IC, continuous |

**Pins:** `emitter` (emitter), `base` (base), `collector` (collector)

#### BS250 — `bs250`

_P-channel enhancement MOSFET, -45 V, -230 mA continuous_

`semiconductors/transistors/bs250.jmx` · type `mosfet` · Family: `p-mosfet` → `mos-transistor` · fails: `short` · Diodes Inc. BS250P datasheet, electrical characteristics (limits only)

Inherits every parameter of [`mos-transistor`](#mos-transistor--mos-transistor).

| Parameter | Value | Meaning |
|---|---|---|
| `V_threshold` | -2.4 V | VTO: inside VGS(th) -1 .. -3.5 V at -1 mA |
| `transconductance` | 26 mA/V^2 | KP: with RD + RS, about 9 Ω at VGS -10 V — under the 14 Ω limit |
| `R_drain` | 3.5 Ohm | RD |
| `R_source` | 0.5 Ohm | RS |
| `I_saturation` | 10 pA | IS: body diode saturation current |
| `emission_coefficient` | 1.5 | N: body diode emission factor |
| `C_bulk_drain` | 20 pF | CBD: output capacitance at zero bias |
| `C_gate_source` | 25 pF | CGSO: inside Ciss at most 60 pF |
| `C_gate_drain` | 5 pF | CGDO: Crss |
| `T_nominal` | 298.15 K | the temperature the figures above are measured at |
| `V_threshold_coefficient` | -0.004 | the gate turns on earlier when hot, V/K |
| `resistance_temperature_exponent` | 1.5 | RDS(on) rises when hot |
| `V_breakdown` | 50 V | drain-source avalanche: BVDSS is at least 45 V |
| `I_breakdown` | 100 uA | avalanche current at V_breakdown |
| `V_drain_source_maximum` | 45 V | VDS |
| `V_gate_source_maximum` | 20 V | VGS, either way |
| `I_drain_maximum` | 230 mA | ID |
| `V_threshold_spread` | 1.1 V | VGS(th) -1 ... -3.5 V |

**Pins:** `source` (source), `gate` (gate), `drain` (drain)

#### IRF9540N — `irf9540`

_P-channel power MOSFET, -100 V, -23 A, 0.117 Ω_

`semiconductors/transistors/irf9540.jmx` · type `mosfet` · Family: `p-mosfet` → `mos-transistor` · fails: `short` · Infineon IRF9540N datasheet

Inherits every parameter of [`mos-transistor`](#mos-transistor--mos-transistor).

| Parameter | Value | Meaning |
|---|---|---|
| `V_threshold` | -3 V | VTO: gate threshold, -2 … -4 V at -250 uA |
| `transconductance` | 2.86 A/V^2 | KP: with RD + RS, 0.1 Ω at VGS -10 V; 0.117 Ω is the datasheet's maximum |
| `R_drain` | 40 mOhm | RD |
| `R_source` | 10 mOhm | RS |
| `I_saturation` | 10 pA | IS: body diode saturation current |
| `emission_coefficient` | 1.3 | N: body diode emission factor |
| `C_bulk_drain` | 160 pF | CBD: Coss - Crss |
| `C_gate_source` | 1060 pF | CGSO: Ciss - Crss |
| `C_gate_drain` | 240 pF | CGDO: Crss |
| `T_nominal` | 298.15 K | the temperature the figures above are measured at |
| `V_threshold_coefficient` | -0.006 | the gate turns on earlier when hot, V/K |
| `resistance_temperature_exponent` | 1.5 | RDS(on) rises when hot |
| `V_breakdown` | 100 V | drain-source breakdown |
| `I_breakdown` | 250 uA | avalanche current at V_breakdown |
| `power_rating` | 2 W | what it is rated for without a heatsink |
| `thermal_resistance` | 62 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 30 s | how fast it warms to that |
| `T_maximum` | 175 °C | past this it fails on a realistic bench |
| `V_threshold_spread` | 1 V | VGS(th) -2 ... -4 V |
| `V_drain_source_maximum` | 100 V | VDS |
| `V_gate_source_maximum` | 20 V | VGS, either way |
| `I_drain_maximum` | 23 A | ID at 25 C case |

**Pins:** `source` (source), `gate` (gate), `drain` (drain; the tab is drain too)

#### IRFZ44N — `irfz44n`

_N-channel power MOSFET, 55 V, 49 A, 17.5 mΩ_

`semiconductors/transistors/irfz44n.jmx` · type `mosfet` · Family: `n-mosfet` → `mos-transistor` · fails: `short` · Infineon IRFZ44N datasheet, PD-94053

Inherits every parameter of [`mos-transistor`](#mos-transistor--mos-transistor).

| Parameter | Value | Meaning |
|---|---|---|
| `V_threshold` | 3 V | VTO: gate threshold, 2 … 4 V at 250 uA |
| `transconductance` | 20.4 A/V^2 | KP: with RD + RS, 15 mΩ at VGS 10 V; 17.5 mΩ is the datasheet's maximum |
| `R_drain` | 6 mOhm | RD |
| `R_source` | 2 mOhm | RS |
| `I_saturation` | 10 pA | IS: body diode saturation current |
| `emission_coefficient` | 1.2 | N: body diode emission factor |
| `C_bulk_drain` | 272 pF | CBD: Coss - Crss |
| `C_gate_source` | 1382 pF | CGSO: Ciss - Crss |
| `C_gate_drain` | 88 pF | CGDO: Crss |
| `T_nominal` | 298.15 K | the temperature the figures above are measured at |
| `V_threshold_coefficient` | -0.006 | threshold change per kelvin, V/K |
| `resistance_temperature_exponent` | 1.5 | RDS(on) rises when hot |
| `V_breakdown` | 55 V | drain-source breakdown at 250 uA |
| `I_breakdown` | 250 uA | avalanche current at V_breakdown |
| `power_rating` | 2 W | what it is rated for without a heatsink |
| `thermal_resistance` | 62 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 30 s | how fast it warms to that |
| `T_maximum` | 175 °C | past this it fails on a realistic bench |
| `V_threshold_spread` | 1 V | VGS(th) 2 ... 4 V |
| `V_drain_source_maximum` | 55 V | VDS |
| `V_gate_source_maximum` | 20 V | VGS, either way |
| `I_drain_maximum` | 49 A | ID at 25 C case |

**Pins:** `source` (source), `gate` (gate), `drain` (drain; the tab is drain too)

#### J201 — `j201`

_N-channel JFET, 40 V, IDSS 0.2 … 1 mA_

`semiconductors/transistors/j201.jmx` · type `jfet` · Family: `n-jfet` → `junction-field-effect-transistor` · fails: `open` · onsemi J201/J202 datasheet, electrical characteristics

Inherits every parameter of [`junction-field-effect-transistor`](#junction-fet--junction-field-effect-transistor).

| Parameter | Value | Meaning |
|---|---|---|
| `V_threshold` | -0.6 V | VTO: inside VGS(off) -0.3 … -1.5 V |
| `transconductance` | 1.3 mA/V^2 | BETA: IDSS about 0.47 mA |
| `channel_length_modulation` | 0.00225 | LAMBDA |
| `R_drain` | 1 Ohm | RD |
| `R_source` | 1 Ohm | RS |
| `I_saturation` | 114.5 fA | IS |
| `C_gate_source` | 4.5 pF | CGS |
| `C_gate_drain` | 4.05 pF | CGD |
| `V_junction` | 0.5 V | PB |
| `T_nominal` | 298.15 K | the temperature the figures above are measured at |
| `threshold_temperature_coefficient` | 0.002 | VTO moves towards zero by about 2 mV per kelvin |
| `V_threshold_spread` | 0.3 V | VGS(off) -0.3 ... -1.5 V |
| `transconductance_spread` | 0.5 | IDSS 0.2 ... 1 mA |

**Pins:** `source` (source), `gate` (gate), `drain` (drain)

#### TIP31C — `tip31c`

_NPN power transistor, 100 V, 3 A_

`semiconductors/transistors/tip31c.jmx` · type `bjt` · Family: `npn-transistor` → `bipolar-transistor` · fails: `short` · onsemi TIP31 series datasheet TIP31A/D, Rev. 14

Inherits every parameter of [`bipolar-transistor`](#bipolar-transistor--bipolar-transistor).

| Parameter | Value | Meaning |
|---|---|---|
| `I_saturation` | 1 pA | IS: transport saturation current |
| `beta_forward` | 80 | BF: ideal forward current gain |
| `beta_reverse` | 5 | BR: ideal reverse current gain |
| `V_early` | 100 V | VAF: forward Early voltage |
| `I_knee` | 2 A | IKF: forward knee current: above it the gain falls |
| `R_base` | 3 Ohm | RB: part of VBE(on) at amperes |
| `R_collector` | 0.15 Ohm | RC: most of VCE(sat) at amperes |
| `R_emitter` | 0.05 Ohm | RE: emitter resistance and bond wire |
| `C_base_emitter` | 300 pF | CJE |
| `C_base_collector` | 80 pF | CJC |
| `transit_time_forward` | 50 ns | TF: from fT at least 3 MHz — an approximation, the datasheet gives no model |
| `V_breakdown` | 100 V | collector-emitter sustaining voltage: an inductive kick stops here |
| `I_breakdown` | 1 mA | avalanche current at V_breakdown |
| `T_nominal` | 298.15 K | the temperature the figures above are measured at |
| `power_rating` | 2 W | what it is rated for without a heatsink |
| `thermal_resistance` | 62.5 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 30 s | how fast it warms to that |
| `T_maximum` | 150 °C | past this it fails on a realistic bench |
| `V_collector_emitter_maximum` | 100 V | VCEO |
| `I_collector_maximum` | 3 A | IC, continuous |

**Pins:** `emitter` (emitter), `base` (base), `collector` (collector; the tab is collector too)

#### TIP32C — `tip32c`

_PNP power transistor, -100 V, -3 A_

`semiconductors/transistors/tip32c.jmx` · type `bjt` · Family: `pnp-transistor` → `bipolar-transistor` · fails: `short` · onsemi TIP32 series datasheet (the TIP31 complement)

Inherits every parameter of [`bipolar-transistor`](#bipolar-transistor--bipolar-transistor).

| Parameter | Value | Meaning |
|---|---|---|
| `I_saturation` | 1 pA | IS: transport saturation current |
| `beta_forward` | 80 | BF: ideal forward current gain |
| `beta_reverse` | 5 | BR: ideal reverse current gain |
| `V_early` | 100 V | VAF: forward Early voltage |
| `I_knee` | 2 A | IKF: forward knee current: above it the gain falls |
| `R_base` | 3 Ohm | RB: part of VBE(on) at amperes |
| `R_collector` | 0.15 Ohm | RC: most of VCE(sat) at amperes |
| `R_emitter` | 0.05 Ohm | RE: emitter resistance and bond wire |
| `C_base_emitter` | 300 pF | CJE |
| `C_base_collector` | 80 pF | CJC |
| `transit_time_forward` | 50 ns | TF: from fT at least 3 MHz — an approximation, the datasheet gives no model |
| `V_breakdown` | 100 V | collector-emitter sustaining voltage: an inductive kick stops here |
| `I_breakdown` | 1 mA | avalanche current at V_breakdown |
| `T_nominal` | 298.15 K | the temperature the figures above are measured at |
| `power_rating` | 2 W | what it is rated for without a heatsink |
| `thermal_resistance` | 62.5 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 30 s | how fast it warms to that |
| `T_maximum` | 150 °C | past this it fails on a realistic bench |
| `V_collector_emitter_maximum` | 100 V | VCEO |
| `I_collector_maximum` | 3 A | IC, continuous |

**Pins:** `emitter` (emitter), `base` (base), `collector` (collector; the tab is collector too)

### `sources/`

#### Battery — `battery`

_Battery: EMF by state of charge, internal resistance, polarisation sag and recovery_

`sources/battery.jmx` · type `source` · fails: `open` · the Thevenin battery model (EMF + R0 + one RC), a lithium-ion cell's figures

| Parameter | Value | Meaning |
|---|---|---|
| `V_full` | 4.2 V | EMF when full |
| `V_empty` | 3.0 V | EMF when empty |
| `state_of_charge` | 1 in 0 .. 1 step 0.05 · _live_ | how full it is |
| `R_internal` | 50 mOhm | R0: the drop under load, at once |
| `R_polarisation` | 30 mOhm | R1: the further sag, over the polarisation time |
| `C_polarisation` | 300 F | C1: with R1, a sag over about ten seconds |
| `power_rating` | 10 W | what it may deliver |
| `thermal_resistance` | 20 K/W | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 300 s | how fast it warms to that |
| `T_maximum` | 60 °C | past this it fails on a realistic bench |

**Pins:** `positive` (positive terminal), `negative` (negative terminal)

#### Bench power supply — `bench-supply`

_A DC supply with a current limit: constant voltage, then constant current when the load asks for more_

`sources/bench-supply.jmx` · type `power-supply`

| Parameter | Value | Meaning |
|---|---|---|
| `V_set` | 12 V in 0 .. 30 step 0.1 · _live_ | the voltage it holds while the current is below the limit |
| `I_limit` | 1 A in 0.01 .. 5 step 0.01 · _live_ | the most current it gives; at the limit the voltage sags |
| `R_output` | 10 mOhm | output resistance while it holds the voltage |

**Pins:** `positive` (positive output), `negative` (negative output)

#### Current source — `current-source`

_Two-terminal DC current source_

`sources/current-source.jmx` · type `current-source`

| Parameter | Value | Meaning |
|---|---|---|
| `I_output` | 1 mA | source current |

**Pins:** `positive` (positive terminal), `negative` (negative terminal)

#### Dual power supply — `dual-supply`

_A bipolar supply: +V and −V around a common 0 V, each rail with its own current limit_

`sources/dual-supply.jmx` · type `dual-power-supply`

| Parameter | Value | Meaning |
|---|---|---|
| `V_positive` | 15 V in 0 .. 30 step 0.1 · _live_ | the positive rail above the common |
| `V_negative` | 15 V in 0 .. 30 step 0.1 · _live_ | the negative rail below the common, as a magnitude |
| `I_limit_positive` | 1 A in 0.01 .. 5 step 0.01 · _live_ | the most current the positive rail gives |
| `I_limit_negative` | 1 A in 0.01 .. 5 step 0.01 · _live_ | the most current the negative rail gives |
| `R_output` | 10 mOhm | each rail's output resistance while it holds its voltage |

**Pins:** `positive` (positive rail), `common` (common, the circuit's 0 V), `negative` (negative rail)

#### Harmonic generator — `harmonic-generator`

_A fundamental and up to eight harmonics, summed: offset + Σ a_k · sin(2π · k · frequency · time + φ_k)_

`sources/harmonic-generator.jmx` · type `generator`

| Parameter | Value | Meaning |
|---|---|---|
| `frequency` | 100 Hz | the fundamental: harmonic k is k times it |
| `a1` | 1 V | amplitude of the fundamental |
| `a2` | 0 V | amplitude of the 2nd harmonic |
| `a3` | 0 V | amplitude of the 3rd harmonic |
| `a4` | 0 V | amplitude of the 4th harmonic |
| `a5` | 0 V | amplitude of the 5th harmonic |
| `a6` | 0 V | amplitude of the 6th harmonic |
| `a7` | 0 V | amplitude of the 7th harmonic |
| `a8` | 0 V | amplitude of the 8th harmonic |
| `p1` | 0 rad | phase of the fundamental |
| `p2` | 0 rad | phase of the 2nd harmonic |
| `p3` | 0 rad | phase of the 3rd harmonic |
| `p4` | 0 rad | phase of the 4th harmonic |
| `p5` | 0 rad | phase of the 5th harmonic |
| `p6` | 0 rad | phase of the 6th harmonic |
| `p7` | 0 rad | phase of the 7th harmonic |
| `p8` | 0 rad | phase of the 8th harmonic |
| `offset` | 0 V | the voltage it swings around |

**Pins:** `positive` (positive terminal), `negative` (negative terminal)

#### Noise generator — `noise-generator`

_Band-limited noise: white (flat to the bandwidth) or pink (equal power per octave), at an RMS level_

`sources/noise-generator.jmx` · type `generator`

| Parameter | Value | Meaning |
|---|---|---|
| `level` | 100 mV | RMS voltage of the noise |
| `bandwidth` | 20 kHz | the highest frequency in it |
| `lowest` | 20 Hz | where pink noise starts; white noise starts at zero |
| `pink` | 0 in 0 .. 1 step 1 · _live_ | 0: white — flat to the bandwidth; 1: pink — equal power in every octave |
| `offset` | 0 V | the voltage it wanders around |
| `seed` | 1 | another seed, another noise of the same kind |

**Pins:** `positive` (positive terminal), `negative` (negative terminal)

#### Sine generator — `sine-generator`

_An ideal sine voltage: offset + amplitude · sin(2π · frequency · time + phase)_

`sources/sine-generator.jmx` · type `generator`

| Parameter | Value | Meaning |
|---|---|---|
| `amplitude` | 1 V | peak voltage above the offset |
| `frequency` | 50 Hz | how many periods a second |
| `phase` | 0 rad | where the period stands at time 0 |
| `offset` | 0 V | the voltage it swings around |

**Pins:** `positive` (positive terminal), `negative` (negative terminal)

#### Square generator — `square-generator`

_An ideal square voltage: high for the duty's share of every period, low for the rest_

`sources/square-generator.jmx` · type `generator`

| Parameter | Value | Meaning |
|---|---|---|
| `high` | 5 V | the level while on |
| `low` | 0 V | the level while off |
| `frequency` | 1 kHz | how many periods a second |
| `duty` | 0.5 in 0.01 .. 0.99 step 0.01 · _live_ | share of each period spent high |

**Pins:** `positive` (positive terminal), `negative` (negative terminal)

#### Staircase generator — `staircase-generator`

_Equal voltage steps, each held for one period, cycling: offset + step · floor(mod(frequency · time, steps))_

`sources/staircase-generator.jmx` · type `generator`

| Parameter | Value | Meaning |
|---|---|---|
| `step` | 0.5 V | the height of one step |
| `steps` | 6 | how many steps before it starts again |
| `frequency` | 50 Hz | steps a second: each step lasts one period |
| `offset` | 0 V | the first step's level |

**Pins:** `positive` (positive terminal), `negative` (negative terminal)

#### Voltage-controlled current source — `voltage-controlled-current-source`

_Ideal voltage-controlled current source: I(positive → negative) = transconductance · V(control_positive, control_negative)_

`sources/voltage-controlled-current-source.jmx` · type `source` · fails: `open` · SPICE G element, linear form

| Parameter | Value | Meaning |
|---|---|---|
| `transconductance` | 1 mA/V | the output current over the controlling voltage |

**Pins:** `positive` (the current leaves the circuit here), `negative` (and comes back here), `control_positive` (controlling voltage, positive), `control_negative` (controlling voltage, negative)

#### Voltage-controlled voltage source — `voltage-controlled-voltage-source`

_Ideal voltage-controlled voltage source: V(positive, negative) = gain · V(control_positive, control_negative)_

`sources/voltage-controlled-voltage-source.jmx` · type `source` · fails: `open` · SPICE E element, linear form

| Parameter | Value | Meaning |
|---|---|---|
| `gain` | 1 | the output voltage over the controlling voltage |

**Pins:** `positive` (output, positive), `negative` (output, negative), `control_positive` (controlling voltage, positive), `control_negative` (controlling voltage, negative)

#### Voltage source — `voltage-source`

_Two-terminal voltage source: constant, or switched on at the start. A sine or a square is a generator — sine-generator, square-generator_

`sources/voltage-source.jmx` · type `voltage-source`

| Parameter | Value | Meaning |
|---|---|---|
| `U_output` | 5 V | source voltage |

**Pins:** `positive` (positive terminal), `negative` (negative terminal)

## The bench — properties every component reads

These are not component parameters: a component may not declare them. The bench sets them for every part at once.

| Name | Meaning |
|---|---|
| `temperature` | the bench's temperature in kelvin, 25 °C unless set (`bench for 1 ms at 85 °C`); every model writes its drift against its own `T_nominal` |
| `realistic` | 0 on an ideal bench, 1 on a realistic one (`bench for 1 ms realistic`): self-heating, an instrument's burden, a shunt's drop, burning out past `T_maximum` |
| `tolerance_corner` | −1, 0 or +1: every `tolerance` and every `*_spread` is taken at its low end, its value, or its high end |
| `time` | the circuit's time, for generators and behaviour |
| per-part conditions | a bench may give any part its own tolerance and ppm/°C, and any source an offset (`BenchConditions`) |

Every component may also carry:

| Line | Meaning |
|---|---|
| `meta failure open\|short` | what it becomes when it burns out |
| `power_rating`, `thermal_resistance`, `thermal_time_constant`, `T_maximum` | its heat: temperature = bench + P·Rθ over the time constant; past `T_maximum` on a realistic bench it fails |
| `state heating` | for a model: the junction's (body's, winding's) own temperature rise, fed back into every drift |
| `noise(density[, corner])` | in a current equation: a noise source for noise analysis and the transient noise of a realistic bench |

## Alphabetical index

| Parameter | Meaning | Used by |
|---|---|---|
| `a1` | amplitude of the fundamental | `harmonic-generator` |
| `a2` | amplitude of the 2nd harmonic | `harmonic-generator` |
| `a3` | amplitude of the 3rd harmonic | `harmonic-generator` |
| `a4` | amplitude of the 4th harmonic | `harmonic-generator` |
| `a5` | amplitude of the 5th harmonic | `harmonic-generator` |
| `a6` | amplitude of the 6th harmonic | `harmonic-generator` |
| `a7` | amplitude of the 7th harmonic | `harmonic-generator` |
| `a8` | amplitude of the 8th harmonic | `harmonic-generator` |
| `amplitude` | peak voltage above the offset | `sine-generator` |
| `area` | area factor: multiplies the currents and the capacitance, divides RS | `diode` |
| `bandwidth` | the highest frequency in it | `noise-generator` |
| `beta_forward` | BF: ideal forward current gain | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor`, `tip31c`, `tip32c` |
| `beta_forward_spread` | how far BF spreads from part to part, as a share: a bench's tolerance corners use it | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor` |
| `beta_reverse` | BR: ideal reverse current gain | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor`, `tip31c`, `tip32c` |
| `beta_temperature_exponent` | XTB: how the gains follow the temperature | `2n2222`, `2n2907`, `bipolar-transistor` |
| `bias_doubling` | the input current doubles every this many kelvin: about 10 K for a JFET input; none is a gigakelvin | `operational-amplifier`, `tl072` |
| `body_transit_time` | the body diode's transit time: its stored charge, the reverse recovery of a freewheeling diode | `mos-transistor` |
| `breakdown_temperature_coefficient` | TCV: BV moves by this share per kelvin | `bzx55c5v1`, `diode` |
| `C_base_collector` | CJC: base–collector zero-bias capacitance | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor`, `tip31c`, `tip32c` |
| `C_base_emitter` | CJE: base–emitter zero-bias capacitance | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor`, `tip31c`, `tip32c` |
| `C_bulk_drain` | CBD: drain–bulk zero-bias capacitance | `2n7000`, `bs250`, `irf9540`, `irfz44n`, `mos-transistor` |
| `C_gate_drain` | CGD: gate–drain zero-bias capacitance | `2n5457`, `2n7000`, `bs250`, `irf9540`, `irfz44n`, `j201`, `junction-field-effect-transistor`, `mos-transistor` |
| `C_gate_source` | CGS: gate–source zero-bias capacitance | `2n5457`, `2n7000`, `bs250`, `irf9540`, `irfz44n`, `j201`, `junction-field-effect-transistor`, `mos-transistor` |
| `C_input_common` | capacitance from each input to the negative rail | `operational-amplifier` |
| `C_input_differential` | capacitance between the inputs | `operational-amplifier` |
| `C_junction` | each junction's capacitance | `bridge-rectifier` |
| `C_junction_zero_bias` | CJO: zero-bias junction capacitance | `1n4007`, `1n4148`, `1n5819`, `bzx55c5v1`, `diode`, `led-red` |
| `C_output_energy` | Co(er), effective output capacitance, energy related, 0 .. 480 V | `ice3b0365` |
| `C_polarisation` | C1: with R1, a sag over about ten seconds | `battery` |
| `capacitance` | capacitance | `capacitor`, `ceramic-capacitor`, `electrolytic-capacitor` |
| `channel_length_modulation` | LAMBDA: per volt | `2n5457`, `j201`, `junction-field-effect-transistor`, `mos-transistor` |
| `closed` | Pressed | `push-button`, `switch` |
| `common_mode_rejection` | CMRR, as a ratio; none by default — its error is an offset of unknown sign | `operational-amplifier` |
| `copper_temperature_coefficient` | copper: the winding's resistance rises 0.393 % per kelvin | `power-inductor`, `transformer` |
| `coupling` | K: 1 is a perfect core; below it, leakage | `transformer` |
| `current_transfer_ratio` | CTR: collector current over LED current, 0.5 … 6 by rank | `pc817` |
| `duty` | share of each period spent high | `square-generator` |
| `E_gap` | EG: band gap, in volts | `1n4007`, `1n5819`, `bipolar-transistor`, `bridge-rectifier`, `diode`, `junction-field-effect-transistor`, `led-red`, `mos-transistor` |
| `edge_time` | gate drive edge; the driver slows the rising slope to about 130 ns (figure 11) | `ice3b0365` |
| `emission_coefficient` | NF: forward emission coefficient | `1n4007`, `1n4148`, `1n5819`, `2n7000`, `bc547`, `bc557`, `bipolar-transistor`, `bridge-rectifier`, `bs250`, `bzx55c5v1`, `diode`, `irf9540`, `irfz44n`, `junction-field-effect-transistor`, `led-red`, `mos-transistor`, `pc817` |
| `emission_coefficient_reverse` | NR: reverse emission coefficient | `bc547`, `bc557`, `bipolar-transistor` |
| `ESR_doubling` | the ESR doubles for every this many kelvin colder: an electrolyte thickens in the cold | `electrolytic-capacitor` |
| `forward_bias_coefficient` | FC: past FC·VJ a depletion capacitance goes on as a straight line | `bipolar-transistor`, `diode`, `junction-field-effect-transistor`, `mos-transistor` |
| `frequency` | the fundamental: harmonic k is k times it | `harmonic-generator`, `sine-generator`, `square-generator`, `staircase-generator` |
| `gain` | the output voltage over the controlling voltage | `voltage-controlled-voltage-source` |
| `gain_bandwidth` | unity-gain bandwidth | `lm358`, `lm393`, `ne5532`, `operational-amplifier`, `tl072` |
| `grading_base_collector` | MJC: its grading coefficient | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor` |
| `grading_base_emitter` | MJE: its grading coefficient | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor` |
| `grading_coefficient` | M: grading coefficient | `1n4007`, `1n4148`, `1n5819`, `diode`, `mos-transistor` |
| `high` | the level while on | `square-generator` |
| `hold` | D stable after the clock rises | `74hc74` |
| `I_adjustment` | adjustment pin current, typical | `lm317` |
| `I_breakdown` | avalanche current at V_breakdown | `1n4007`, `1n4148`, `1n5819`, `2n7000`, `bipolar-transistor`, `bridge-rectifier`, `bs250`, `bzx55c5v1`, `diode`, `irf9540`, `irfz44n`, `led-red`, `mos-transistor`, `tip31c`, `tip32c` |
| `I_charge` | VCC charge current from the startup cell at VCC = 0 (0.55 .. 1.60 mA) | `ice3b0365` |
| `I_collector_maximum` | IC: the collector current it is rated for; none is a megaampere | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor`, `tip31c`, `tip32c` |
| `I_dark` | collector current with the LED off | `pc817` |
| `I_discharge` | discharge transistor, saturation | `ne555` |
| `I_drain_maximum` | ID: the drain current it is rated for; none is a megaampere | `2n7000`, `bs250`, `irf9540`, `irfz44n`, `mos-transistor` |
| `I_forward_maximum` | IF: the current it is rated to carry, either way; none is a megaampere | `1n4007`, `1n4148`, `1n5819`, `bzx55c5v1`, `diode`, `led-red` |
| `I_gate_saturation` | the gate–cathode junction: 200 µA at VGT | `2n5060`, `bt136` |
| `I_holding` | IH: below this anode current it turns off (5 mA at most) | `2n5060`, `bt136` |
| `I_input_bias` | input bias current: positive flows out of the inputs (PNP stage), negative into them (NPN) | `lm358`, `lm393`, `ne5532`, `operational-amplifier`, `tl072` |
| `I_input_offset` | the difference between the two inputs' bias currents | `operational-amplifier` |
| `I_knee` | IKF: forward high-injection knee (none is a gigaampere here) | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor`, `diode`, `tip31c`, `tip32c` |
| `I_knee_reverse` | IKR: reverse high-injection knee | `bc547`, `bc557`, `bipolar-transistor` |
| `I_leakage_base_collector` | ISC: base–collector leakage saturation current | `bc547`, `bc557`, `bipolar-transistor` |
| `I_leakage_base_emitter` | ISE: base–emitter leakage saturation current | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor` |
| `I_limit` | the most current it gives; at the limit the voltage sags | `bench-supply` |
| `I_limit_negative` | the most current the negative rail gives | `dual-supply` |
| `I_limit_positive` | the most current the positive rail gives | `dual-supply` |
| `I_maximum` | rated maximum output current | `lm317` |
| `I_minimum` | the cathode current below which it does not regulate | `tl431` |
| `I_noise` | input current noise density into each input, A/√Hz (white) | `ne5532`, `operational-amplifier`, `tl072` |
| `I_output` | source current | `current-source` |
| `I_output_limit` | output sink and source | `ne555` |
| `I_output_sink` | output short-circuit current, sinking | `lm358`, `lm393`, `ne5532`, `operational-amplifier`, `tl072` |
| `I_output_source` | output short-circuit current, sourcing | `lm358`, `lm393`, `ne5532`, `operational-amplifier`, `tl072` |
| `I_quiescent` | supply current per unit | `lm358`, `lm393`, `ne5532`, `ne555`, `operational-amplifier`, `tl072` |
| `I_rated` | the current its brightness is given at | `led-red` |
| `I_recombination` | ISR: recombination saturation current — the low-current excess of the forward curve | `diode` |
| `I_reference` | Iref: current into the ref pin | `tl431` |
| `I_restart` | supply current in auto restart mode with inactive gate | `ice3b0365` |
| `I_saturation` | IS: transport saturation current | `1n4007`, `1n4148`, `1n5819`, `2n2222`, `2n2907`, `2n5457`, `2n7000`, `bc547`, `bc557`, `bipolar-transistor`, `bridge-rectifier`, `bs250`, `bzx55c5v1`, `diode`, `irf9540`, `irfz44n`, `j201`, `junction-field-effect-transistor`, `led-red`, `mos-transistor`, `pc817`, `power-inductor`, `tip31c`, `tip32c` |
| `I_start` | start-up current before the controller is active (max 220 uA) | `ice3b0365` |
| `I_supply` | supply current with active gate, ICE3B0365 (max 7.0 mA) | `ice3b0365` |
| `I_threshold` | into THR | `ne555` |
| `I_trigger` | out of TRIG | `ne555` |
| `inductance` | inductance | `inductor`, `power-inductor` |
| `internal_base_collector_fraction` | XCJC: the share of CJC at the internal base; the rest is at the base pin | `bc547`, `bc557`, `bipolar-transistor` |
| `L_equivalent_series` | ESL: package and mounting | `ceramic-capacitor` |
| `L_primary` | primary inductance: the magnetising inductance seen from the primary | `transformer` |
| `L_secondary` | secondary inductance: turns ratio √(L1 / L2) = 10 | `transformer` |
| `latch_delay` | typical STCP to Qn at 4.5 V | `74hc595` |
| `leading_edge_blanking_time` | leading-edge blanking | `ice3b0365` |
| `leakage_emission_base_collector` | NC: its emission coefficient | `bc547`, `bc557`, `bipolar-transistor` |
| `leakage_emission_base_emitter` | NE: its emission coefficient | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor` |
| `level` | RMS voltage of the noise | `noise-generator` |
| `light_temperature_coefficient` | red light falls by about 1 % per kelvin of junction temperature | `led-red` |
| `low` | the level while off | `square-generator` |
| `lowest` | where pink noise starts; white noise starts at zero | `noise-generator` |
| `maximum_duty_cycle` | maximum duty cycle (0.67 .. 0.77) | `ice3b0365` |
| `noise_corner` | below this frequency 1/f noise rises over the white; 0 for none | `ne5532`, `operational-amplifier`, `tl072` |
| `offset` | the voltage it swings around | `harmonic-generator`, `noise-generator`, `sine-generator`, `staircase-generator` |
| `open_loop_gain` | DC open-loop gain, as a ratio (100 dB) | `lm358`, `lm393`, `ne5532`, `operational-amplifier`, `tl072` |
| `oscillator_frequency` | fOSC, fixed oscillator frequency, ICE3Bxx65 (61 .. 73 kHz) | `ice3b0365` |
| `p1` | phase of the fundamental | `harmonic-generator` |
| `p2` | phase of the 2nd harmonic | `harmonic-generator` |
| `p3` | phase of the 3rd harmonic | `harmonic-generator` |
| `p4` | phase of the 4th harmonic | `harmonic-generator` |
| `p5` | phase of the 5th harmonic | `harmonic-generator` |
| `p6` | phase of the 6th harmonic | `harmonic-generator` |
| `p7` | phase of the 7th harmonic | `harmonic-generator` |
| `p8` | phase of the 8th harmonic | `harmonic-generator` |
| `phase` | where the period stands at time 0 | `sine-generator` |
| `phase_reversal` | 1: past the low limit the output swings the other way | `operational-amplifier`, `tl072` |
| `pink` | 0: white — flat to the bandwidth; 1: pink — equal power in every octave | `noise-generator` |
| `polarity` | 1 for NPN, -1 for PNP | `bipolar-transistor`, `junction-field-effect-transistor`, `mos-transistor`, `n-jfet`, `n-mosfet`, `npn-transistor`, `p-jfet`, `p-mosfet`, `pnp-transistor` |
| `position` | Wiper position | `potentiometer` |
| `power_rating` | what it is rated for | `1n4007`, `1n4148`, `1n5819`, `2n2222`, `2n2907`, `2n5060`, `2n7000`, `battery`, `bc547`, `bc557`, `bipolar-transistor`, `bt136`, `bzx55c5v1`, `ceramic-capacitor`, `diode`, `electrolytic-capacitor`, `irf9540`, `irfz44n`, `junction-field-effect-transistor`, `led-red`, `mos-transistor`, `pc817`, `potentiometer`, `power-inductor`, `resistor`, `tip31c`, `tip32c`, `tl431`, `transformer` |
| `propagation_delay` | typical SHCP to Q7S at 4.5 V | `74hc595`, `74hc74` |
| `R_base` | RB: zero-bias base resistance | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor`, `tip31c`, `tip32c` |
| `R_base_minimum` | RBM: the base resistance at high current; 0 means R_base | `bipolar-transistor` |
| `R_closed` | contact resistance when pressed | `push-button`, `switch` |
| `R_collector` | RC: collector resistance | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor`, `tip31c`, `tip32c` |
| `R_discharge` | discharge transistor, on | `ne555` |
| `R_divider` | each of the three divider resistors | `ne555` |
| `R_drain` | RD: drain series resistance | `2n5457`, `2n7000`, `bs250`, `irf9540`, `irfz44n`, `j201`, `junction-field-effect-transistor`, `mos-transistor` |
| `R_drain_source_on` | drain-source on-resistance at 25 C, 0.3 A (max 7.50 Ohm; 13.7 Ohm at 125 C) | `ice3b0365` |
| `R_emitter` | RE: emitter resistance | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor`, `tip31c`, `tip32c` |
| `R_equivalent_series` | ESR | `ceramic-capacitor`, `electrolytic-capacitor` |
| `R_feedback` | FB pull-up resistor to VREF (16 .. 27 kOhm) | `ice3b0365` |
| `R_gate` | RG: the gate's internal resistance, which with Ciss sets how fast it switches | `mos-transistor` |
| `R_input` | differential input resistance | `duty-cycle-meter`, `frequency-meter`, `ne5532`, `operational-amplifier`, `tl072`, `voltmeter`, `wattmeter` |
| `R_internal` | R0: the drop under load, at once | `battery` |
| `R_leakage` | leakage: the datasheet's I = 0.01 · C · V, as a resistance | `electrolytic-capacitor` |
| `R_off` | anode to cathode while it blocks | `2n5060`, `bt136`, `tl431` |
| `R_on` | the on-state slope: VTM 1.7 V at 1.2 A at most | `2n5060`, `bt136` |
| `R_open` | leakage resistance when released | `push-button`, `switch` |
| `R_output` | output resistance | `bench-supply`, `dual-supply`, `lm358`, `lm393`, `ne5532`, `ne555`, `operational-amplifier`, `tl072` |
| `R_polarisation` | R1: the further sag, over the polarisation time | `battery` |
| `R_primary` | primary winding resistance | `transformer` |
| `R_secondary` | secondary winding resistance | `transformer` |
| `R_series` | RS: series resistance (SPICE's 0 means none; here a milliohm) | `1n4007`, `1n4148`, `1n5819`, `bridge-rectifier`, `bzx55c5v1`, `diode`, `led-red`, `pc817` |
| `R_shunt` | the shunt it reads across: what it drops, on a realistic bench | `ammeter` |
| `R_soft_start` | SoftS pull-up resistor to VREF (39 .. 62 kOhm) | `ice3b0365` |
| `R_source` | RS: source series resistance | `2n5457`, `2n7000`, `bs250`, `irf9540`, `irfz44n`, `j201`, `junction-field-effect-transistor`, `mos-transistor` |
| `R_winding` | DCR: the winding's resistance | `power-inductor` |
| `R_wiper` | wiper contact resistance | `potentiometer` |
| `recombination_emission_coefficient` | NR: emission coefficient of the recombination current | `diode` |
| `recovery` | how fast the gain stage comes back out of saturation | `lm358`, `operational-amplifier` |
| `reference_drift` | how Vref moves per kelvin, V/K: the TL431's is a shallow bowl of a few millivolts, none here | `tl431` |
| `resistance` | end-to-end resistance | `potentiometer`, `resistor` |
| `resistance_temperature_coefficient` | TRS: RS moves by this share per kelvin | `diode` |
| `resistance_temperature_exponent` | not SPICE: RD and RS grow as (temperature / T_nominal)^this | `2n7000`, `bs250`, `irf9540`, `irfz44n`, `mos-transistor` |
| `saturation_temperature_coefficient` | a ferrite core saturates earlier when hot, about 0.4 % per kelvin | `power-inductor` |
| `saturation_temperature_exponent` | XTI: how the saturation current follows the temperature | `1n4007`, `1n5819`, `bipolar-transistor`, `bridge-rectifier`, `diode` |
| `second_pole` | the second pole; 0 means none | `ne5532`, `operational-amplifier`, `tl072` |
| `seed` | another seed, another noise of the same kind | `noise-generator` |
| `setup` | D stable before the clock rises | `74hc74` |
| `slew_rate` | slew rate | `lm358`, `lm393`, `ne5532`, `operational-amplifier`, `tl072` |
| `state_of_charge` | how full it is | `battery` |
| `step` | the height of one step | `staircase-generator` |
| `steps` | how many steps before it starts again | `staircase-generator` |
| `supply_current_coefficient` | the share per volt the supply current rises above V_supply_nominal | `operational-amplifier` |
| `supply_rejection` | PSRR, as a ratio; none by default — its error is an offset of unknown sign | `operational-amplifier` |
| `switching_time` | output rise and fall time | `ne555` |
| `T_maximum` | past this it fails on a realistic bench | `1n4007`, `1n4148`, `1n5819`, `2n2222`, `2n2907`, `2n5060`, `2n7000`, `74hc595`, `74hc74`, `battery`, `bc547`, `bc557`, `bipolar-transistor`, `bridge-rectifier`, `bt136`, `bzx55c5v1`, `ceramic-capacitor`, `diode`, `electrolytic-capacitor`, `ice3b0365`, `irf9540`, `irfz44n`, `junction-field-effect-transistor`, `led-red`, `lm317`, `lm358`, `mos-transistor`, `ne555`, `operational-amplifier`, `pc817`, `potentiometer`, `power-inductor`, `resistor`, `tip31c`, `tip32c`, `tl431`, `transformer` |
| `T_nominal` | TNOM: the temperature the figures above are measured at | `2n2222`, `2n2907`, `2n5457`, `2n7000`, `bc547`, `bc557`, `bipolar-transistor`, `bridge-rectifier`, `bs250`, `ceramic-capacitor`, `diode`, `electrolytic-capacitor`, `irf9540`, `irfz44n`, `j201`, `junction-field-effect-transistor`, `led-red`, `mos-transistor`, `operational-amplifier`, `pc817`, `power-inductor`, `tip31c`, `tip32c`, `tl431`, `transformer` |
| `temperature_coefficient` | how its value moves per kelvin away from 25 °C; a bench's temperature uses it | `capacitor`, `resistor` |
| `temperature_curvature` | X7R: about -15 % at 100 K either side of 25 C, as a share per kelvin squared | `ceramic-capacitor` |
| `thermal_resistance` | kelvin above the bench per watt it takes | `1n4007`, `1n4148`, `1n5819`, `2n2222`, `2n2907`, `2n5060`, `2n7000`, `74hc595`, `74hc74`, `battery`, `bc547`, `bc557`, `bipolar-transistor`, `bridge-rectifier`, `bt136`, `bzx55c5v1`, `ceramic-capacitor`, `diode`, `electrolytic-capacitor`, `ice3b0365`, `irf9540`, `irfz44n`, `junction-field-effect-transistor`, `led-red`, `lm317`, `lm358`, `mos-transistor`, `ne555`, `operational-amplifier`, `pc817`, `potentiometer`, `power-inductor`, `resistor`, `tip31c`, `tip32c`, `tl431`, `transformer` |
| `thermal_time_constant` | how fast it warms to that | `1n4007`, `1n4148`, `1n5819`, `2n2222`, `2n2907`, `2n5060`, `2n7000`, `74hc595`, `74hc74`, `battery`, `bc547`, `bc557`, `bipolar-transistor`, `bridge-rectifier`, `bt136`, `bzx55c5v1`, `ceramic-capacitor`, `diode`, `electrolytic-capacitor`, `ice3b0365`, `irf9540`, `irfz44n`, `junction-field-effect-transistor`, `led-red`, `lm317`, `lm358`, `mos-transistor`, `ne555`, `operational-amplifier`, `pc817`, `potentiometer`, `power-inductor`, `resistor`, `tip31c`, `tip32c`, `tl431`, `transformer` |
| `threshold` | the level the signal is read against | `duty-cycle-meter`, `frequency-meter` |
| `threshold_temperature_coefficient` | VTOTC: how VTO moves per kelvin, V/K | `2n5457`, `j201`, `junction-field-effect-transistor` |
| `tolerance` | how far a part may be from its value, ±; a bench's corners use it | `capacitor`, `resistor` |
| `transconductance` | BETA: IDSS = BETA · VTO² | `2n5457`, `2n7000`, `bs250`, `irf9540`, `irfz44n`, `j201`, `junction-field-effect-transistor`, `lm317`, `mos-transistor`, `n-mosfet`, `p-mosfet`, `tl431`, `voltage-controlled-current-source` |
| `transconductance_spread` | how far BETA spreads from part to part, as a share | `2n5457`, `j201`, `junction-field-effect-transistor`, `mos-transistor` |
| `transfer_temperature_coefficient` | the CTR falls by this share per kelvin above 25 C: about 0.8 at 75 C | `pc817` |
| `transit_time` | TT: transit time — the stored charge behind reverse recovery | `1n4007`, `1n4148`, `1n5819`, `diode` |
| `transit_time_bias_dependence` | XTF: how much TF grows with current | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor` |
| `transit_time_bias_voltage` | VTF: the VBC that halves that growth | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor` |
| `transit_time_current` | ITF: the current at which TF's growth sets in | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor` |
| `transit_time_forward` | TF: ideal forward transit time — sets fT | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor`, `tip31c`, `tip32c` |
| `transit_time_reverse` | TR: ideal reverse transit time — the storage time of a saturated switch | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor` |
| `U_output` | source voltage | `voltage-source` |
| `V_breakdown` | collector–emitter sustaining voltage: an inductive kick stops here | `1n4007`, `1n4148`, `1n5819`, `2n2222`, `2n2907`, `2n7000`, `bc547`, `bc557`, `bipolar-transistor`, `bridge-rectifier`, `bs250`, `bzx55c5v1`, `diode`, `irf9540`, `irfz44n`, `led-red`, `mos-transistor`, `tip31c`, `tip32c` |
| `V_collector_emitter_maximum` | VCEO: the collector-emitter voltage it is rated for; none is a megavolt | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor`, `tip31c`, `tip32c` |
| `V_common_mode_high` | the inputs work up to this far below the positive rail | `lm358`, `lm393`, `ne5532`, `operational-amplifier`, `tl072` |
| `V_common_mode_low` | the inputs work down to this far above the negative rail (negative: below it) | `ne5532`, `operational-amplifier`, `tl072` |
| `V_current_sense_burst` | VCS2, the limit during active burst mode (0.232 .. 0.282 V) | `ice3b0365` |
| `V_current_sense_threshold` | peak current limitation, incl. propagation delay (0.97 .. 1.07 V) | `ice3b0365` |
| `V_drain_source_maximum` | VDS: the drain-source voltage it is rated for; none is a megavolt | `2n7000`, `bs250`, `irf9540`, `irfz44n`, `mos-transistor` |
| `V_dropout` | typical input-output dropout | `lm317` |
| `V_early` | VAF: forward Early voltage (none is a megavolt here) | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor`, `pc817`, `tip31c`, `tip32c` |
| `V_early_reverse` | VAR: reverse Early voltage | `bc547`, `bc557`, `bipolar-transistor` |
| `V_empty` | EMF when empty | `battery` |
| `V_feedback_burst` | C5: active burst mode entry level (1.23 .. 1.37 V) | `ice3b0365` |
| `V_feedback_burst_off` | C6b: in burst mode, switching stops below this (3.25 .. 3.55 V) | `ice3b0365` |
| `V_feedback_burst_on` | C6a: in burst mode, switching resumes above this (3.85 .. 4.15 V) | `ice3b0365` |
| `V_feedback_minimum` | below this FB voltage the duty cycle is zero (Dmin) | `ice3b0365` |
| `V_feedback_overload` | C4: overload and open-loop limit (4.62 .. 4.98 V) | `ice3b0365` |
| `V_full` | EMF when full | `battery` |
| `V_gate_source_maximum` | VGS: what the gate oxide is rated for, either way; none is a megavolt | `2n7000`, `bs250`, `irf9540`, `irfz44n`, `mos-transistor` |
| `V_gate_trigger` | VGT: the gate voltage that fires it (0.8 V at most) | `2n5060`, `bt136` |
| `V_half` | the DC bias at which half the capacitance is left | `ceramic-capacitor` |
| `V_input_offset` | input offset voltage | `lm358`, `lm393`, `ne5532`, `operational-amplifier`, `tl072` |
| `V_junction` | VJ: junction potential | `1n4007`, `1n4148`, `1n5819`, `2n5457`, `diode`, `j201`, `junction-field-effect-transistor`, `mos-transistor` |
| `V_junction_base_collector` | VJC: its built-in potential | `bc547`, `bc557`, `bipolar-transistor` |
| `V_junction_base_emitter` | VJE: its built-in potential | `bc547`, `bc557`, `bipolar-transistor` |
| `V_negative` | the negative rail below the common, as a magnitude | `dual-supply` |
| `V_noise` | input voltage noise density, V/√Hz (white) | `lm358`, `ne5532`, `operational-amplifier`, `tl072` |
| `V_offset_drift` | how the offset moves per kelvin, V/K; none by default, its sign is the part's own | `operational-amplifier` |
| `V_offset_spread` | the offset a part may have, either way: a bench's tolerance corners use it | `lm358`, `lm393`, `ne5532`, `operational-amplifier`, `tl072` |
| `V_on` | the on-state voltage's knee | `2n5060`, `bt136` |
| `V_output_headroom` | the output stays this far below the positive rail | `lm358`, `lm393`, `ne5532`, `operational-amplifier`, `tl072` |
| `V_output_high_drop` | output high sits this far below Vcc | `ne555` |
| `V_output_low` | the output reaches this close to the negative rail | `lm358`, `lm393`, `ne5532`, `ne555`, `operational-amplifier`, `tl072` |
| `V_positive` | the positive rail above the common | `dual-supply` |
| `V_ramp` | voltage ramp max level: slope compensation added to the amplified CS | `ice3b0365` |
| `V_reference` | VREF, trimmed reference (6.37 .. 6.63 V) | `ice3b0365`, `lm317`, `tl431` |
| `V_reset` | reset threshold | `ne555` |
| `V_reverse_maximum` | VRRM: the reverse voltage it is rated to block; none is a megavolt | `1n4007`, `1n4148`, `1n5819`, `diode`, `led-red` |
| `V_saturation` | the VCE a saturated phototransistor does not go below | `pc817` |
| `V_saturation_knee` | how sharply it comes out of saturation above that | `pc817` |
| `V_set` | the voltage it holds while the current is below the limit | `bench-supply` |
| `V_soft_start_begin` | soft start ends: above this SoftS voltage there is no duty limitation | `ice3b0365` |
| `V_soft_start_clamp` | clamped SoftS voltage in normal operation (4.23 .. 4.57 V) | `ice3b0365` |
| `V_soft_start_window` | C3: end of the blanking window, overload or burst is decided here (5.2 .. 5.6 V) | `ice3b0365` |
| `V_supply_maximum` | the supply span it is rated for; none is a megavolt | `lm358`, `lm393`, `ne5532`, `operational-amplifier`, `tl072` |
| `V_supply_nominal` | the supply span the offset is given at | `lm393`, `ne5532`, `operational-amplifier`, `tl072` |
| `V_supply_off` | VCC turn-off threshold (8.0 .. 9.0 V) | `ice3b0365` |
| `V_supply_on` | VCC turn-on threshold (14.2 .. 15.8 V) | `ice3b0365` |
| `V_threshold` | VTO: pinch-off voltage, negative for a depletion part | `2n5457`, `2n7000`, `bs250`, `irf9540`, `irfz44n`, `j201`, `junction-field-effect-transistor`, `mos-transistor`, `n-mosfet`, `p-mosfet` |
| `V_threshold_coefficient` | not SPICE: how the threshold's magnitude drifts, V/K (negative: it opens earlier when hot) | `2n7000`, `bs250`, `irf9540`, `irfz44n`, `mos-transistor` |
| `V_threshold_spread` | how far the pinch-off spreads from part to part: a bench's tolerance corners use it | `2n5457`, `2n7000`, `bs250`, `irf9540`, `irfz44n`, `j201`, `junction-field-effect-transistor`, `mos-transistor` |
| `voltage_gain` | PWM-OP gain applied to the CS voltage (3.5 .. 3.9) | `ice3b0365` |
| `voltage_rating` | the most it is made to hold | `capacitor`, `ceramic-capacitor`, `electrolytic-capacitor` |
