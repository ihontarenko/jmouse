# Параметри компонентів — усі властивості кожного компонента каталогу

English: [COMPONENT-PARAMETERS.md](COMPONENT-PARAMETERS.md).

Згенеровано з `BE/src/main/resources/catalogue/electronics/components/**/*.jmx` — 69 файлів, 237 різних параметрів. ⚠️ Не редагувати вручну: `perl tools/component-parameters/parameter-reference.pl BE/src/main/resources/catalogue/electronics/components tools/component-parameters/parameter-names-uk.tsv .` з `Parallax/`; новий параметр потребує українського рядка в `parameter-names-uk.tsv`.

## Як це читати

- **Параметр** — число в блоці `parameters` компонента. Величина там — значення за замовчуванням; деталь на схемі
  перевизначає будь-який із них у своєму рядку: `Q1 2n2222 (b, c, e) beta_forward = 300`. Записана величина
  приймає одиниці (`100 kΩ`, `2 pF`); у рівняннях одиниць немає (`1e-12`, а не `1 pA`).
- **Сімейство** — компонент, рівняння якого використовують його члени: `2n2222` → `npn-transistor` →
  `bipolar-transistor`. Член успадковує всі параметри сімейства й задає лише відмінні; таблиця сімейства — це
  все, що член може задати.
- `live` (живий) — параметр, яким керують зі стенда під час роботи схеми (повзунок, перемикач); `in a .. b step s` —
  його діапазон.
- Імена за одним правилом: літера величини і що це (`V_breakdown`, `R_series`, `C_junction_zero_bias`); параметр
  SPICE має своє SPICE-ім'я на початку значення (`IS:`, `BF:`) — за ним зіставляється імпортована `.model`.
- **Показники на схемі** (`display`) обчислюються на кожному відліку й показуються біля деталі; `overvoltage` і
  `overcurrent` від 100 % — причина відмови на реалістичному стенді.
- **Стани** — невідомі, які несе модель (засувка, потік). Власне тепло деталі до них не належить: на реалістичному
  стенді рушій гріє кожну деталь потужністю, яку розсіюють її рівняння, через її `thermal_resistance` за її
  `thermal_time_constant`, і її рівняння читають це як `temperature`.
- Примітки з файлів (даташитні цифри й позначення) наведено мовою оригіналу; значення кожного параметра —
  українською.

## Сімейства

### Bipolar transistor — `bipolar-transistor`

_Gummel–Poon bipolar transistor: gain falling at both ends, Early effect, stored charge, junction capacitances_ · Gummel–Poon model as in Berkeley SPICE3 / ngspice 47; defaults are that model's

`base/bipolar-transistor.jmx`

| Параметр | За замовчуванням | Значення |
|---|---|---|
| `polarity` | 1 | 1 — NPN (n-канал), −1 — PNP (p-канал) |
| `I_saturation` | 0.1 fA | IS: струм насичення (транспортний для транзистора) |
| `beta_forward` | 100 | BF: ідеальний прямий коефіцієнт підсилення струму |
| `beta_forward_spread` | 0 | розкид BF між екземплярами, частка; використовують кути допуску стенда |
| `emission_coefficient` | 1 | NF / N: коефіцієнт емісії (неідеальності) прямого переходу |
| `V_early` | 1 MV | VAF: пряма напруга Ерлі (відсутня — мегавольт) |
| `I_knee` | 1000000000 A | IKF: струм коліна високої інжекції (відсутній — гігаампер) |
| `I_leakage_base_emitter` | 0 A | ISE: струм насичення витоку база–емітер |
| `leakage_emission_base_emitter` | 1.5 | NE: його коефіцієнт емісії |
| `beta_reverse` | 1 | BR: ідеальний зворотний коефіцієнт підсилення струму |
| `emission_coefficient_reverse` | 1 | NR: зворотний коефіцієнт емісії |
| `V_early_reverse` | 1 MV | VAR: зворотна напруга Ерлі |
| `I_knee_reverse` | 1000000000 A | IKR: зворотний струм коліна високої інжекції |
| `I_leakage_base_collector` | 0 A | ISC: струм насичення витоку база–колектор |
| `leakage_emission_base_collector` | 2 | NC: його коефіцієнт емісії |
| `R_base` | 1 mOhm | RB: опір бази при нульовому зміщенні |
| `R_base_minimum` | 0 Ohm | RBM: опір бази на великому струмі; 0 — дорівнює R_base |
| `R_emitter` | 1 mOhm | RE: опір емітера |
| `R_collector` | 1 mOhm | RC: опір колектора |
| `C_base_emitter` | 0 F | CJE: ємність переходу база–емітер при нульовому зміщенні |
| `V_junction_base_emitter` | 0.75 V | VJE: її контактна різниця потенціалів |
| `grading_base_emitter` | 0.33 | MJE: її коефіцієнт плавності переходу |
| `C_base_collector` | 0 F | CJC: ємність переходу база–колектор при нульовому зміщенні |
| `V_junction_base_collector` | 0.75 V | VJC: її контактна різниця потенціалів |
| `grading_base_collector` | 0.33 | MJC: її коефіцієнт плавності переходу |
| `internal_base_collector_fraction` | 1 | XCJC: частка CJC на внутрішній базі; решта — на виводі бази |
| `forward_bias_coefficient` | 0.5 | FC: після FC·VJ бар'єрна ємність продовжується прямою лінією |
| `transit_time_forward` | 0 s | TF: ідеальний прямий час прольоту — задає fT |
| `transit_time_bias_dependence` | 0 | XTF: наскільки TF зростає зі струмом |
| `transit_time_bias_voltage` | 1 MV | VTF: напруга VBC, що вдвічі зменшує це зростання |
| `transit_time_current` | 0 A | ITF: струм, з якого починається зростання TF |
| `transit_time_reverse` | 0 s | TR: ідеальний зворотний час прольоту — час розсмоктування насиченого ключа |
| `beta_temperature_exponent` | 0 | XTB: як коефіцієнти підсилення залежать від температури |
| `saturation_temperature_exponent` | 3 | XTI: як струм насичення залежить від температури |
| `E_gap` | 1.11 V | EG: ширина забороненої зони, у вольтах |
| `T_nominal` | 300.15 K | TNOM: температура, за якої виміряно наведені параметри |
| `V_breakdown` | 1 kV | напруга пробою (для транзистора — підтримувальна напруга колектор–емітер; на ній обмежується індуктивний викид) |
| `I_breakdown` | 1 mA | струм лавинного пробою при V_breakdown |
| `power_rating` | 0.5 W | номінальна потужність розсіювання |
| `thermal_resistance` | 250 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності |
| `thermal_time_constant` | 2 s | теплова стала часу: як швидко елемент прогрівається до цієї температури |
| `T_maximum` | 150 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу |
| `V_collector_emitter_maximum` | 1e6 V | VCEO: допустима напруга колектор–емітер; відсутня — мегавольт |
| `I_collector_maximum` | 1e6 A | IC: допустимий струм колектора; відсутній — мегаампер |

**Члени:** [`2n2222`](#2n2222a--2n2222), [`2n2907`](#2n2907a--2n2907), [`bc547`](#bc547b--bc547), [`bc557`](#bc557b--bc557), [`npn-transistor`](#npn-transistor--npn-transistor), [`pnp-transistor`](#pnp-transistor--pnp-transistor), [`tip31c`](#tip31c--tip31c), [`tip32c`](#tip32c--tip32c)

### Diode — `diode`

_Junction diode with every effect: recombination, high injection, depletion and diffusion charge, breakdown_ · Berkeley SPICE3 / ngspice 47 diode model, level 1; defaults are SPICE's

`base/diode.jmx`

| Параметр | За замовчуванням | Значення |
|---|---|---|
| `I_saturation` | 10 fA | IS: струм насичення (транспортний для транзистора) |
| `emission_coefficient` | 1 | NF / N: коефіцієнт емісії (неідеальності) прямого переходу |
| `R_series` | 1 mOhm | RS: послідовний опір (0 у SPICE — відсутній; тут міліом) |
| `I_recombination` | 0 A | ISR: струм насичення рекомбінації — надлишок прямої гілки на малих струмах |
| `recombination_emission_coefficient` | 2 | NR: коефіцієнт емісії струму рекомбінації |
| `I_knee` | 1000000000 A | IKF: струм коліна високої інжекції (відсутній — гігаампер) |
| `C_junction_zero_bias` | 0 F | CJO: ємність переходу при нульовому зміщенні |
| `V_junction` | 1 V | VJ: контактна різниця потенціалів переходу |
| `grading_coefficient` | 0.5 | M: коефіцієнт плавності переходу |
| `forward_bias_coefficient` | 0.5 | FC: після FC·VJ бар'єрна ємність продовжується прямою лінією |
| `transit_time` | 0 s | TT: час прольоту — накопичений заряд, що дає зворотне відновлення |
| `V_breakdown` | 1 kV | напруга пробою (для транзистора — підтримувальна напруга колектор–емітер; на ній обмежується індуктивний викид) |
| `I_breakdown` | 1 mA | струм лавинного пробою при V_breakdown |
| `T_nominal` | 300.15 K | TNOM: температура, за якої виміряно наведені параметри |
| `saturation_temperature_exponent` | 3 | XTI: як струм насичення залежить від температури |
| `breakdown_temperature_coefficient` | 0 | TCV: напруга пробою змінюється на цю частку на кельвін |
| `resistance_temperature_coefficient` | 0 | TRS: RS змінюється на цю частку на кельвін |
| `E_gap` | 1.11 V | EG: ширина забороненої зони, у вольтах |
| `area` | 1 | коефіцієнт площі: множить струми й ємність, ділить RS |
| `power_rating` | 0.5 W | номінальна потужність розсіювання |
| `thermal_resistance` | 300 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності |
| `thermal_time_constant` | 1 s | теплова стала часу: як швидко елемент прогрівається до цієї температури |
| `T_maximum` | 175 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу |
| `V_reverse_maximum` | 1e6 V | VRRM: допустима зворотна напруга; відсутня — мегавольт |
| `I_forward_maximum` | 1e6 A | IF: допустимий струм в обидва боки; відсутній — мегаампер |

**Члени:** [`1n4007`](#1n4007--1n4007), [`1n4148`](#1n4148--1n4148), [`1n5819`](#1n5819--1n5819), [`bzx55c5v1`](#bzx55c5v1--bzx55c5v1), [`led-red`](#red-led--led-red)

### Junction FET — `junction-field-effect-transistor`

_JFET level 1: square-law channel, channel-length modulation, gate junctions and their capacitances_ · JFET level 1 as in Berkeley SPICE3 / ngspice 47; defaults are that model's

`base/junction-field-effect-transistor.jmx`

| Параметр | За замовчуванням | Значення |
|---|---|---|
| `polarity` | 1 | 1 — NPN (n-канал), −1 — PNP (p-канал) |
| `V_threshold` | -2 V | VTO: порогова напруга (для JFET — напруга відсічки, від'ємна для збідненого типу) |
| `V_threshold_spread` | 0 V | розкид порогу між екземплярами; використовують кути допуску стенда |
| `transconductance_spread` | 0 | розкид крутизни (BETA / KP) між екземплярами, частка |
| `threshold_temperature_coefficient` | 0 | VTOTC: зміна VTO на кельвін, В/К |
| `transconductance` | 100 uA/V^2 | крутизна: BETA для JFET (IDSS = BETA · VTO²), KP для MOSFET |
| `channel_length_modulation` | 0 | LAMBDA: модуляція довжини каналу, на вольт |
| `R_drain` | 1 mOhm | RD: послідовний опір стоку |
| `R_source` | 1 mOhm | RS: послідовний опір витоку |
| `I_saturation` | 10 fA | IS: струм насичення (транспортний для транзистора) |
| `emission_coefficient` | 1 | NF / N: коефіцієнт емісії (неідеальності) прямого переходу |
| `C_gate_source` | 0 F | CGS: ємність затвор–витік |
| `C_gate_drain` | 0 F | CGD: ємність затвор–стік |
| `V_junction` | 1 V | VJ: контактна різниця потенціалів переходу |
| `forward_bias_coefficient` | 0.5 | FC: після FC·VJ бар'єрна ємність продовжується прямою лінією |
| `T_nominal` | 300.15 K | TNOM: температура, за якої виміряно наведені параметри |
| `E_gap` | 1.11 V | EG: ширина забороненої зони, у вольтах |
| `power_rating` | 0.35 W | номінальна потужність розсіювання |
| `thermal_resistance` | 350 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності |
| `thermal_time_constant` | 2 s | теплова стала часу: як швидко елемент прогрівається до цієї температури |
| `T_maximum` | 150 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу |

**Члени:** [`2n5457`](#2n5457--2n5457), [`j201`](#j201--j201), [`n-jfet`](#n-channel-jfet--n-jfet), [`p-jfet`](#p-channel-jfet--p-jfet)

### MOS transistor — `mos-transistor`

_Level-1 MOSFET: square-law channel, channel-length modulation, series resistances, body diode, gate and junction capacitances_ · MOS level 1 as in Berkeley SPICE3 / ngspice 47; defaults are that model's

`base/mos-transistor.jmx`

| Параметр | За замовчуванням | Значення |
|---|---|---|
| `polarity` | 1 | 1 — NPN (n-канал), −1 — PNP (p-канал) |
| `V_threshold` | 0 V | VTO: порогова напруга (для JFET — напруга відсічки, від'ємна для збідненого типу) |
| `V_threshold_spread` | 0 V | розкид порогу між екземплярами; використовують кути допуску стенда |
| `transconductance_spread` | 0 | розкид крутизни (BETA / KP) між екземплярами, частка |
| `transconductance` | 20 uA/V^2 | крутизна: BETA для JFET (IDSS = BETA · VTO²), KP для MOSFET |
| `channel_length_modulation` | 0 | LAMBDA: модуляція довжини каналу, на вольт |
| `R_drain` | 1 mOhm | RD: послідовний опір стоку |
| `R_source` | 1 mOhm | RS: послідовний опір витоку |
| `I_saturation` | 10 fA | IS: струм насичення (транспортний для транзистора) |
| `emission_coefficient` | 1 | NF / N: коефіцієнт емісії (неідеальності) прямого переходу |
| `C_bulk_drain` | 0 F | CBD: ємність стік–підкладка (вихідна ємність, тіло діода) |
| `V_junction` | 0.8 V | VJ: контактна різниця потенціалів переходу |
| `grading_coefficient` | 0.5 | M: коефіцієнт плавності переходу |
| `forward_bias_coefficient` | 0.5 | FC: після FC·VJ бар'єрна ємність продовжується прямою лінією |
| `C_gate_source` | 0 F | CGS: ємність затвор–витік |
| `C_gate_drain` | 0 F | CGD: ємність затвор–стік |
| `R_gate` | 1 mOhm | RG: внутрішній опір затвора, що разом із Ciss задає швидкість перемикання |
| `body_transit_time` | 0 s | час прольоту діода тіла: накопичений заряд, зворотне відновлення діода вільного ходу |
| `T_nominal` | 300.15 K | TNOM: температура, за якої виміряно наведені параметри |
| `V_threshold_coefficient` | 0 | не SPICE: дрейф модуля порогу, В/К (від'ємний — гарячий відкривається раніше) |
| `resistance_temperature_exponent` | 0 | не SPICE: RD і RS ростуть як (температура / T_nominal)^цей показник |
| `E_gap` | 1.11 V | EG: ширина забороненої зони, у вольтах |
| `V_breakdown` | 1 kV | напруга пробою (для транзистора — підтримувальна напруга колектор–емітер; на ній обмежується індуктивний викид) |
| `I_breakdown` | 1 mA | струм лавинного пробою при V_breakdown |
| `power_rating` | 0.4 W | номінальна потужність розсіювання |
| `thermal_resistance` | 312 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності |
| `thermal_time_constant` | 2 s | теплова стала часу: як швидко елемент прогрівається до цієї температури |
| `T_maximum` | 150 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу |
| `V_drain_source_maximum` | 1e6 V | VDS: допустима напруга стік–витік; відсутня — мегавольт |
| `V_gate_source_maximum` | 1e6 V | VGS: допустима напруга на оксиді затвора в обидва боки; відсутня — мегавольт |
| `I_drain_maximum` | 1e6 A | ID: допустимий струм стоку; відсутній — мегаампер |

**Члени:** [`2n7000`](#2n7000--2n7000), [`bs250`](#bs250--bs250), [`irf9540`](#irf9540n--irf9540), [`irfz44n`](#irfz44n--irfz44n), [`n-mosfet`](#n-channel-mosfet--n-mosfet), [`p-mosfet`](#p-channel-mosfet--p-mosfet)

### Operational amplifier — `operational-amplifier`

_Op-amp macromodel: gain, two poles, slew rate, offset, bias currents, CMRR, PSRR, input range, output limits_ · behavioural macromodel; the figures a datasheet's electrical table gives

`base/operational-amplifier.jmx`

| Параметр | За замовчуванням | Значення |
|---|---|---|
| `open_loop_gain` | 100000 | коефіцієнт підсилення без зворотного зв'язку на постійному струмі, як відношення |
| `gain_bandwidth` | 1 MHz | частота одиничного підсилення |
| `second_pole` | 0 Hz | другий полюс; 0 — відсутній |
| `slew_rate` | 1 V/us | швидкість наростання вихідної напруги |
| `V_input_offset` | 0 V per unit | вхідна напруга зміщення нуля |
| `I_input_bias` | 0 A | вхідний струм зміщення: додатний витікає з входів (PNP-каскад), від'ємний втікає (NPN) |
| `V_offset_spread` | 0 V | можливе зміщення нуля екземпляра в будь-який бік; використовують кути допуску стенда |
| `V_offset_drift` | 0 | дрейф зміщення нуля на кельвін, В/К; за замовчуванням відсутній, знак — властивість екземпляра |
| `bias_doubling` | 1e9 K | вхідний струм подвоюється кожні стільки кельвінів: близько 10 К для входу на JFET; відсутній — гігакельвін |
| `T_nominal` | 298.15 K | TNOM: температура, за якої виміряно наведені параметри |
| `I_input_offset` | 0 A | різниця вхідних струмів двох входів |
| `R_input` | 1 TOhm | диференційний вхідний опір |
| `C_input_differential` | 0 F | ємність між входами |
| `C_input_common` | 0 F | ємність від кожного входу до від'ємної шини |
| `supply_current_coefficient` | 0 | на яку частку на вольт зростає струм споживання понад V_supply_nominal |
| `common_mode_rejection` | 1e12 | КОСС (CMRR), як відношення; за замовчуванням відсутній — його похибка є зміщенням невідомого знака |
| `supply_rejection` | 1e12 | КПНЖ (PSRR), як відношення; за замовчуванням відсутній — похибка є зміщенням невідомого знака |
| `V_supply_nominal` | 15 V | напруга живлення, за якої задано зміщення нуля |
| `V_common_mode_low` | -0.3 V | входи працюють до стількох вольт над від'ємною шиною (від'ємне — нижче неї) |
| `V_common_mode_high` | 0 V | входи працюють до стількох вольт нижче додатної шини |
| `phase_reversal` | 0 | 1: нижче нижньої межі вихід переходить у протилежний бік (інверсія фази) |
| `V_noise` | 0 | спектральна густина вхідної напруги шуму, В/√Гц (білий шум) |
| `I_noise` | 0 | спектральна густина струму шуму в кожен вхід, А/√Гц (білий шум) |
| `noise_corner` | 0 Hz | нижче цієї частоти шум 1/f перевищує білий; 0 — відсутній |
| `I_quiescent` | 1 mA | струм споживання на одиницю (канал) |
| `R_output` | 50 Ohm | вихідний опір |
| `I_output_source` | 40 mA | струм короткого замикання виходу на витікання |
| `I_output_sink` | 40 mA | струм короткого замикання виходу на втікання |
| `V_output_headroom` | 1.5 V | вихід не підходить до додатної шини ближче цієї напруги |
| `V_output_low` | 1.5 V | наскільки близько вихід підходить до від'ємної шини |
| `recovery` | 1e6 1/s | як швидко каскад підсилення виходить із насичення |
| `thermal_resistance` | 120 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності |
| `thermal_time_constant` | 5 s | теплова стала часу: як швидко елемент прогрівається до цієї температури |
| `T_maximum` | 150 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу |
| `V_supply_maximum` | 1e6 V | допустима напруга живлення; відсутня — мегавольт |

**Члени:** [`lm358`](#lm358--lm358), [`lm393`](#lm393--lm393), [`ne5532`](#ne5532--ne5532), [`tl072`](#tl072--tl072)

## Компоненти

### `base/`

#### N-channel JFET — `n-jfet`

_N-channel JFET, the level-1 model at its defaults_

`base/n-jfet.jmx` · type `jfet` · Сімейство: `junction-field-effect-transistor` · відмова: `open`

Успадковує всі параметри [`junction-field-effect-transistor`](#junction-fet--junction-field-effect-transistor).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `polarity` | 1 | 1 — NPN (n-канал), −1 — PNP (p-канал) | n-channel |

#### N-channel MOSFET — `n-mosfet`

_N-channel MOSFET, the level-1 model at its defaults_

`base/n-mosfet.jmx` · type `mosfet` · Сімейство: `mos-transistor` · відмова: `short`

Успадковує всі параметри [`mos-transistor`](#mos-transistor--mos-transistor).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `polarity` | 1 | 1 — NPN (n-канал), −1 — PNP (p-канал) | n-channel |
| `V_threshold` | 1 V | VTO: порогова напруга (для JFET — напруга відсічки, від'ємна для збідненого типу) | VTO: an enhancement part opens above it |
| `transconductance` | 0.1 A/V^2 | крутизна: BETA для JFET (IDSS = BETA · VTO²), KP для MOSFET | KP: the whole channel |

#### NPN transistor — `npn-transistor`

_NPN bipolar transistor, the Gummel–Poon model at its defaults_

`base/npn-transistor.jmx` · type `bjt` · Сімейство: `bipolar-transistor` · відмова: `open`

Успадковує всі параметри [`bipolar-transistor`](#bipolar-transistor--bipolar-transistor).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `polarity` | 1 | 1 — NPN (n-канал), −1 — PNP (p-канал) | NPN |

#### P-channel JFET — `p-jfet`

_P-channel JFET, the level-1 model at its defaults_

`base/p-jfet.jmx` · type `jfet` · Сімейство: `junction-field-effect-transistor` · відмова: `open`

Успадковує всі параметри [`junction-field-effect-transistor`](#junction-fet--junction-field-effect-transistor).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `polarity` | -1 | 1 — NPN (n-канал), −1 — PNP (p-канал) | p-channel |

#### P-channel MOSFET — `p-mosfet`

_P-channel MOSFET, the level-1 model at its defaults_

`base/p-mosfet.jmx` · type `mosfet` · Сімейство: `mos-transistor` · відмова: `short`

Успадковує всі параметри [`mos-transistor`](#mos-transistor--mos-transistor).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `polarity` | -1 | 1 — NPN (n-канал), −1 — PNP (p-канал) | p-channel |
| `V_threshold` | -1 V | VTO: порогова напруга (для JFET — напруга відсічки, від'ємна для збідненого типу) | VTO: an enhancement part opens when the gate is this far below the source |
| `transconductance` | 0.1 A/V^2 | крутизна: BETA для JFET (IDSS = BETA · VTO²), KP для MOSFET | KP: the whole channel |

#### PNP transistor — `pnp-transistor`

_PNP bipolar transistor, the Gummel–Poon model at its defaults_

`base/pnp-transistor.jmx` · type `bjt` · Сімейство: `bipolar-transistor` · відмова: `open`

Успадковує всі параметри [`bipolar-transistor`](#bipolar-transistor--bipolar-transistor).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `polarity` | -1 | 1 — NPN (n-канал), −1 — PNP (p-канал) | PNP |

### `electromechanical/`

#### Push button — `push-button`

_A momentary switch: closed while it is held, open when it is let go_

`electromechanical/push-button.jmx` · type `button`

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `closed` | 0 in 0 .. 1 step 1 integer · _живий_ | натиснуто / замкнуто | Pressed |
| `R_closed` | 10 mOhm | опір контакту в замкненому стані | contact resistance when pressed |
| `R_open` | 100 MOhm | опір витоку в розімкненому стані | leakage resistance when released |

**Виводи:** `a`, `b`

#### Switch — `switch`

_A single-pole switch, opened and closed while the circuit runs_

`electromechanical/switch.jmx` · type `switch`

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `closed` | 0 in 0 .. 1 step 1 integer · _живий_ | натиснуто / замкнуто | Closed |
| `R_closed` | 10 mOhm | опір контакту в замкненому стані | contact resistance when closed |
| `R_open` | 100 MOhm | опір витоку в розімкненому стані | leakage resistance when open |

**Виводи:** `a`, `b`

### `instruments/`

#### Ammeter — `ammeter`

_Reads the current through it, put in the break of a wire_

`instruments/ammeter.jmx` · type `meter`

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `R_shunt` | 50 mOhm | шунт, на якому вимірюється струм: його падіння на реалістичному стенді | the shunt it reads across: what it drops, on a realistic bench |

**Виводи:** `in`, `out`

**Показ приладу:** `reading = in.current unit A signal`

#### Duty cycle meter — `duty-cycle-meter`

_Reads the share of each period its input spends above a level, from one rising edge to the next_

`instruments/duty-cycle-meter.jmx` · type `meter`

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `threshold` | 2.5 V | рівень, відносно якого зчитується сигнал | the level the signal is read against |
| `R_input` | 1 MOhm | диференційний вхідний опір | input resistance: what it draws through, on a realistic bench |

**Виводи:** `input`, `reference`

**Стани:** `rose` (discrete) — коли вхід востаннє перетнув рівень угору; −1 до першого разу; `fell` (discrete) — коли вхід востаннє перетнув рівень униз; `duty` (discrete) — частка останнього повного періоду над рівнем

**Показ приладу:** `reading = 100 * duty unit %`

#### Frequency meter — `frequency-meter`

_Reads how often its input rises through a level: one over the time between two rising edges_

`instruments/frequency-meter.jmx` · type `meter`

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `threshold` | 0 V | рівень, відносно якого зчитується сигнал | the level a rising edge crosses |
| `R_input` | 1 MOhm | диференційний вхідний опір | input resistance: what it draws through, on a realistic bench |

**Виводи:** `input`, `reference`

**Стани:** `last_edge` (discrete) — коли вхід востаннє перетнув рівень угору; `frequency` (discrete) — одиниця, поділена на час між двома останніми фронтами

**Показ приладу:** `reading = frequency unit Hz`

#### Voltmeter — `voltmeter`

_Reads the voltage of its red lead against its black one_

`instruments/voltmeter.jmx` · type `meter`

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `R_input` | 10 MOhm | диференційний вхідний опір | input resistance: what it draws through, on a realistic bench |

**Виводи:** `plus`, `minus`

**Показ приладу:** `reading = plus - minus unit V signal`

#### Wattmeter — `wattmeter`

_Reads the power a load takes: its current through in and out, its voltage across plus and minus_

`instruments/wattmeter.jmx` · type `meter`

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `R_input` | 10 MOhm | диференційний вхідний опір | voltage input resistance: what it draws through, on a realistic bench |

**Виводи:** `in`, `out`, `plus`, `minus`

**Показ приладу:** `reading = (plus - minus) * in.current unit W signal`

### `integrated/amplifiers/`

#### LM358 — `lm358`

_Dual operational amplifier, single supply 3 V to 32 V, inputs and output to ground_

`integrated/amplifiers/lm358.jmx` · type `opamp` · Сімейство: `operational-amplifier` · відмова: `short` · TI LM358 datasheet (SLOS068)

Успадковує всі параметри [`operational-amplifier`](#operational-amplifier--operational-amplifier).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `open_loop_gain` | 100000 | коефіцієнт підсилення без зворотного зв'язку на постійному струмі, як відношення | open-loop gain, 100 dB (min 25 V/mV) |
| `gain_bandwidth` | 0.7 MHz | частота одиничного підсилення | unity-gain bandwidth |
| `slew_rate` | 0.3 V/us | швидкість наростання вихідної напруги | slew rate |
| `V_input_offset` | 0 V per unit | вхідна напруга зміщення нуля | input offset; typically 2 mV either way |
| `I_input_bias` | 20 nA | вхідний струм зміщення: додатний витікає з входів (PNP-каскад), від'ємний втікає (NPN) | input bias current, out of the input pins (PNP inputs) |
| `V_common_mode_high` | 1.5 V | входи працюють до стількох вольт нижче додатної шини | the inputs work up to V+ - 1.5 V |
| `V_noise` | 40 nV | спектральна густина вхідної напруги шуму, В/√Гц (білий шум) | input voltage noise density at 1 kHz, 40 nV/√Hz |
| `I_quiescent` | 0.35 mA | струм споживання на одиницю (канал) | supply current per unit |
| `R_output` | 50 Ohm | вихідний опір |  |
| `I_output_source` | 40 mA | струм короткого замикання виходу на витікання | output short-circuit current, sourcing |
| `I_output_sink` | 20 mA | струм короткого замикання виходу на втікання | output short-circuit current, sinking |
| `V_output_headroom` | 1.5 V | вихід не підходить до додатної шини ближче цієї напруги | the output stays this far below V+ |
| `V_output_low` | 5 mV | наскільки близько вихід підходить до від'ємної шини | the output reaches this close to ground |
| `recovery` | 1e6 1/s | як швидко каскад підсилення виходить із насичення | how fast the gain stage comes back out of saturation |
| `thermal_resistance` | 120 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 5 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 150 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |
| `V_offset_spread` | 7 mV | можливе зміщення нуля екземпляра в будь-який бік; використовують кути допуску стенда | VIO at most 7 mV |
| `V_supply_maximum` | 32 V | допустима напруга живлення; відсутня — мегавольт | the supply span it is rated for |

**Виводи:** `output`, `inverting_input`, `non_inverting_input`, `Vcc`, `GND`

#### NE5532 — `ne5532`

_Dual low-noise bipolar operational amplifier, ±22 V, 10 MHz, 9 V/µs, drives 600 Ω_

`integrated/amplifiers/ne5532.jmx` · type `opamp` · Сімейство: `operational-amplifier` · відмова: `short` · TI NE5532 datasheet (SLOS075), electrical characteristics at ±15 V

Успадковує всі параметри [`operational-amplifier`](#operational-amplifier--operational-amplifier).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `open_loop_gain` | 100000 | коефіцієнт підсилення без зворотного зв'язку на постійному струмі, як відношення | AVD 100 V/mV |
| `gain_bandwidth` | 10 MHz | частота одиничного підсилення | unity-gain bandwidth |
| `second_pole` | 20 MHz | другий полюс; 0 — відсутній | unity-gain stable: a phase margin near 60° |
| `slew_rate` | 9 V/us | швидкість наростання вихідної напруги | slew rate |
| `V_input_offset` | 0 V per unit | вхідна напруга зміщення нуля | input offset; typically 0.5 mV either way |
| `I_input_bias` | -200 nA | вхідний струм зміщення: додатний витікає з входів (PNP-каскад), від'ємний втікає (NPN) | input bias current, into the input pins (NPN inputs) |
| `R_input` | 300 kOhm | диференційний вхідний опір | differential input resistance |
| `V_noise` | 5 nV | спектральна густина вхідної напруги шуму, В/√Гц (білий шум) | input voltage noise density at 1 kHz, 5 nV/√Hz |
| `I_noise` | 0.7 pA | спектральна густина струму шуму в кожен вхід, А/√Гц (білий шум) | input current noise density at 1 kHz, 0.7 pA/√Hz |
| `noise_corner` | 50 Hz | нижче цієї частоти шум 1/f перевищує білий; 0 — відсутній | 8 nV/√Hz at 30 Hz against 5 at 1 kHz: a corner near 50 Hz |
| `V_common_mode_low` | 2 V | входи працюють до стількох вольт над від'ємною шиною (від'ємне — нижче неї) | VICR ±13 V at ±15 V |
| `V_common_mode_high` | 2 V | входи працюють до стількох вольт нижче додатної шини | VICR ±13 V at ±15 V |
| `I_quiescent` | 4 mA | струм споживання на одиницю (канал) | supply current per unit |
| `R_output` | 10 Ohm | вихідний опір | output resistance |
| `I_output_source` | 38 mA | струм короткого замикання виходу на витікання | output short-circuit current |
| `I_output_sink` | 38 mA | струм короткого замикання виходу на втікання | output short-circuit current |
| `V_output_headroom` | 2 V | вихід не підходить до додатної шини ближче цієї напруги | VOM ±13 V into 600 Ω at ±15 V |
| `V_output_low` | 2 V | наскільки близько вихід підходить до від'ємної шини | the same towards V- |
| `V_supply_nominal` | 30 V | напруга живлення, за якої задано зміщення нуля | the supply span the figures are given at |
| `V_supply_maximum` | 44 V | допустима напруга живлення; відсутня — мегавольт | the supply span it is rated for |
| `V_offset_spread` | 4 mV | можливе зміщення нуля екземпляра в будь-який бік; використовують кути допуску стенда | VIO at most 4 mV |

**Виводи:** `output`, `inverting_input`, `non_inverting_input`, `Vcc`, `GND`

#### TL072 — `tl072`

_Dual JFET-input operational amplifier, ±18 V, 3 MHz, 13 V/µs_

`integrated/amplifiers/tl072.jmx` · type `opamp` · Сімейство: `operational-amplifier` · відмова: `short` · TI TL07x datasheet (SLOS080), electrical characteristics at ±15 V

Успадковує всі параметри [`operational-amplifier`](#operational-amplifier--operational-amplifier).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `open_loop_gain` | 200000 | коефіцієнт підсилення без зворотного зв'язку на постійному струмі, як відношення | AVD 200 V/mV |
| `gain_bandwidth` | 3 MHz | частота одиничного підсилення | unity-gain bandwidth |
| `second_pole` | 4.5 MHz | другий полюс; 0 — відсутній | a phase margin near 55° at unity gain |
| `slew_rate` | 13 V/us | швидкість наростання вихідної напруги | slew rate |
| `V_input_offset` | 0 V per unit | вхідна напруга зміщення нуля | input offset; typically 3 mV either way |
| `I_input_bias` | 65 pA | вхідний струм зміщення: додатний витікає з входів (PNP-каскад), від'ємний втікає (NPN) | input bias current: a JFET gate's leakage |
| `R_input` | 1 TOhm | диференційний вхідний опір | input resistance |
| `V_common_mode_low` | 3 V | входи працюють до стількох вольт над від'ємною шиною (від'ємне — нижче неї) | the inputs work down to V- + 3 V (VICR -12 V at ±15 V) |
| `V_common_mode_high` | 0 V | входи працюють до стількох вольт нижче додатної шини | and up to V+ |
| `phase_reversal` | 1 | 1: нижче нижньої межі вихід переходить у протилежний бік (інверсія фази) | past the low limit the output swings to the other rail |
| `V_noise` | 18 nV | спектральна густина вхідної напруги шуму, В/√Гц (білий шум) | input voltage noise density at 1 kHz, 18 nV/√Hz |
| `I_noise` | 10 fA | спектральна густина струму шуму в кожен вхід, А/√Гц (білий шум) | input current noise density, 0.01 pA/√Hz: a JFET gate |
| `noise_corner` | 100 Hz | нижче цієї частоти шум 1/f перевищує білий; 0 — відсутній | a JFET input's 1/f corner, about |
| `bias_doubling` | 10 K | вхідний струм подвоюється кожні стільки кельвінів: близько 10 К для входу на JFET; відсутній — гігакельвін | a JFET gate's leakage doubles every 10 K |
| `I_quiescent` | 1.4 mA | струм споживання на одиницю (канал) | supply current per unit |
| `R_output` | 75 Ohm | вихідний опір | output resistance |
| `I_output_source` | 40 mA | струм короткого замикання виходу на витікання | output short-circuit current, about |
| `I_output_sink` | 40 mA | струм короткого замикання виходу на втікання | output short-circuit current, about |
| `V_output_headroom` | 1.5 V | вихід не підходить до додатної шини ближче цієї напруги | VOM ±13.5 V into 10 kΩ at ±15 V |
| `V_output_low` | 1.5 V | наскільки близько вихід підходить до від'ємної шини | the same towards V- |
| `V_supply_nominal` | 30 V | напруга живлення, за якої задано зміщення нуля | the supply span the figures are given at |
| `V_supply_maximum` | 36 V | допустима напруга живлення; відсутня — мегавольт | the supply span it is rated for |
| `V_offset_spread` | 10 mV | можливе зміщення нуля екземпляра в будь-який бік; використовують кути допуску стенда | VIO at most 10 mV |

**Виводи:** `output`, `inverting_input`, `non_inverting_input`, `Vcc`, `GND`

### `integrated/comparators/`

#### LM393 — `lm393`

_Dual comparator, open-collector outputs, single supply 2 V to 36 V, inputs to ground_

`integrated/comparators/lm393.jmx` · type `opamp` · Сімейство: `operational-amplifier` · відмова: `short` · TI LM393 datasheet (SLCS005), electrical characteristics at 5 V

Успадковує всі параметри [`operational-amplifier`](#operational-amplifier--operational-amplifier).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `open_loop_gain` | 200000 | коефіцієнт підсилення без зворотного зв'язку на постійному струмі, як відношення | AVD 200 V/mV |
| `gain_bandwidth` | 1 MHz | частота одиничного підсилення | how fast the output starts to move: response time about 1.3 µs |
| `slew_rate` | 10 V/us | швидкість наростання вихідної напруги | the output's swing rate once it moves |
| `V_input_offset` | 0 V per unit | вхідна напруга зміщення нуля | input offset; typically 1 mV either way |
| `I_input_bias` | 25 nA | вхідний струм зміщення: додатний витікає з входів (PNP-каскад), від'ємний втікає (NPN) | input bias current, out of the input pins (PNP inputs) |
| `V_common_mode_high` | 1.5 V | входи працюють до стількох вольт нижче додатної шини | the inputs work up to V+ - 1.5 V |
| `I_quiescent` | 0.2 mA | струм споживання на одиницю (канал) | supply current per unit |
| `R_output` | 37 Ohm | вихідний опір | the output transistor on: VOL 150 mV at 4 mA |
| `I_output_source` | 0.1 nA | струм короткого замикання виходу на витікання | open collector: only its leakage flows out |
| `I_output_sink` | 16 mA | струм короткого замикання виходу на втікання | output sink current |
| `V_output_headroom` | -40 V | вихід не підходить до додатної шини ближче цієї напруги | open collector: the stage aims far above V+, so it never pulls the output down while off |
| `V_output_low` | 0 V | наскільки близько вихід підходить до від'ємної шини | on, the output is pulled to ground through R_output |
| `V_supply_nominal` | 5 V | напруга живлення, за якої задано зміщення нуля | the supply the figures are given at |
| `V_supply_maximum` | 36 V | допустима напруга живлення; відсутня — мегавольт | the supply span it is rated for |
| `V_offset_spread` | 5 mV | можливе зміщення нуля екземпляра в будь-який бік; використовують кути допуску стенда | VIO at most 5 mV |

**Виводи:** `output`, `inverting_input`, `non_inverting_input`, `Vcc`, `GND`

### `integrated/logic/`

#### 74HC595 — `74hc595`

_Eight-bit serial-in, parallel-out shift register with output latch and three-state outputs_

`integrated/logic/74hc595.jmx` · type `shift-register` · відмова: `short` · Nexperia 74HC_HCT595 datasheet, Rev. 12

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `propagation_delay` | 19 ns | затримка поширення (SHCP → Q7S при 4,5 В) | typical SHCP to Q7S at 4.5 V |
| `latch_delay` | 20 ns | затримка засувки (STCP → Qn при 4,5 В) | typical STCP to Qn at 4.5 V |
| `thermal_resistance` | 120 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 5 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 150 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |

**Виводи:** `output_0`, `output_1`, `output_2`, `output_3`, `output_4`, `output_5`, `output_6`, `output_7`, `GND`, `serial_output`, `not_master_reset`, `shift_clock`, `storage_clock`, `not_output_enable`, `serial_data`, `Vcc`

#### 74HC74 — `74hc74`

_Dual D-type flip-flop, positive edge, asynchronous set and reset_

`integrated/logic/74hc74.jmx` · type `flip-flop` · відмова: `short` · Nexperia 74HC74 datasheet

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `propagation_delay` | 16 ns | затримка поширення (SHCP → Q7S при 4,5 В) | clock to Q at 4.5 V (max 44 ns) |
| `setup` | 20 ns | час встановлення: D стабільний до фронту такту | D stable before the clock rises |
| `hold` | 3 ns | час утримання: D стабільний після фронту такту | D stable after the clock rises |
| `thermal_resistance` | 120 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 5 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 150 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |

**Виводи:** `not_clear`, `data`, `clock`, `not_preset`, `output`, `not_output`, `GND`, `Vcc`

**Стани:** `stored` (discrete) — що зберігає тригер; невідомо при ввімкненні

### `integrated/power/`

#### ICE3B0365 — `ice3b0365`

_Off-line SMPS current-mode controller with integrated 650 V CoolMOS, 67 kHz, 6.45 Ohm_

`integrated/power/ice3b0365.jmx` · type `pwm-controller` · відмова: `open` · Infineon CoolSET-F3 datasheet v2.3, 2013-04-02

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `V_reference` | 6.5 V | VREF, підігнана опорна напруга | VREF, trimmed reference (6.37 .. 6.63 V) |
| `oscillator_frequency` | 67 kHz | fOSC, фіксована частота генератора | fOSC, fixed oscillator frequency, ICE3Bxx65 (61 .. 73 kHz) |
| `maximum_duty_cycle` | 0.72 | максимальний коефіцієнт заповнення | maximum duty cycle (0.67 .. 0.77) |
| `voltage_gain` | 3.7 | коефіцієнт підсилення PWM-OP для напруги CS | PWM-OP gain applied to the CS voltage (3.5 .. 3.9) |
| `V_ramp` | 0.85 V | максимальний рівень пилки: компенсація нахилу, додана до підсиленого CS | voltage ramp max level: slope compensation added to the amplified CS |
| `V_feedback_minimum` | 0.3 V | нижче цієї напруги FB заповнення нульове (Dmin) | below this FB voltage the duty cycle is zero (Dmin) |
| `R_feedback` | 20 kOhm | підтягувальний резистор FB до VREF | FB pull-up resistor to VREF (16 .. 27 kOhm) |
| `R_soft_start` | 50 kOhm | підтягувальний резистор SoftS до VREF | SoftS pull-up resistor to VREF (39 .. 62 kOhm) |
| `V_supply_on` | 15.0 V | поріг увімкнення VCC | VCC turn-on threshold (14.2 .. 15.8 V) |
| `V_supply_off` | 8.5 V | поріг вимкнення VCC | VCC turn-off threshold (8.0 .. 9.0 V) |
| `I_charge` | 1.05 mA | струм заряду VCC від пускової комірки при VCC = 0 | VCC charge current from the startup cell at VCC = 0 (0.55 .. 1.60 mA) |
| `I_start` | 160 uA | пусковий струм до активації контролера | start-up current before the controller is active (max 220 uA) |
| `I_supply` | 5.5 mA | струм живлення з активним затвором | supply current with active gate, ICE3B0365 (max 7.0 mA) |
| `I_restart` | 300 uA | струм живлення в режимі автоперезапуску з неактивним затвором | supply current in auto restart mode with inactive gate |
| `V_soft_start_begin` | 4.0 V | кінець м'якого старту: вище цієї напруги SoftS заповнення не обмежене | soft start ends: above this SoftS voltage there is no duty limitation |
| `V_soft_start_clamp` | 4.4 V | обмежена напруга SoftS у нормальному режимі | clamped SoftS voltage in normal operation (4.23 .. 4.57 V) |
| `V_soft_start_window` | 5.4 V | C3: кінець вікна гасіння, тут вирішується перевантаження чи пакетний режим | C3: end of the blanking window, overload or burst is decided here (5.2 .. 5.6 V) |
| `V_feedback_overload` | 4.8 V | C4: межа перевантаження та розімкненого контуру | C4: overload and open-loop limit (4.62 .. 4.98 V) |
| `V_feedback_burst` | 1.30 V | C5: рівень входу в активний пакетний режим | C5: active burst mode entry level (1.23 .. 1.37 V) |
| `V_feedback_burst_on` | 4.0 V | C6a: у пакетному режимі перемикання відновлюється вище цього рівня | C6a: in burst mode, switching resumes above this (3.85 .. 4.15 V) |
| `V_feedback_burst_off` | 3.4 V | C6b: у пакетному режимі перемикання зупиняється нижче цього рівня | C6b: in burst mode, switching stops below this (3.25 .. 3.55 V) |
| `V_current_sense_threshold` | 1.02 V | поріг обмеження пікового струму, з урахуванням затримки | peak current limitation, incl. propagation delay (0.97 .. 1.07 V) |
| `V_current_sense_burst` | 0.257 V | VCS2, межа під час активного пакетного режиму | VCS2, the limit during active burst mode (0.232 .. 0.282 V) |
| `leading_edge_blanking_time` | 220 ns | час гасіння переднього фронту | leading-edge blanking |
| `R_drain_source_on` | 6.45 Ohm | опір стік–витік у відкритому стані | drain-source on-resistance at 25 C, 0.3 A (max 7.50 Ohm; 13.7 Ohm at 125 C) |
| `C_output_energy` | 3.65 pF | Co(er), ефективна вихідна ємність за енергією | Co(er), effective output capacitance, energy related, 0 .. 480 V |
| `edge_time` | 50 ns | тривалість фронту драйвера затвора | gate drive edge; the driver slows the rising slope to about 130 ns (figure 11) |
| `thermal_resistance` | 90 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 10 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 150 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |

**Виводи:** `soft_start`, `feedback`, `current_sense`, `drain`, `Vcc`, `GND`

**Стани:** `running` (discrete) — контролер увімкнено: VCC пройшла V_supply_on і не впала нижче V_supply_off; `restarting` (discrete) — автоперезапуск: вимкнено через несправність, чекає, доки VCC впаде нижче V_supply_off; `gate` (discrete) — засувка ШІМ FF1: силовий ключ має бути відкритий; `started` (discrete) — коли почався поточний цикл перемикання — для пилки й гасіння; `clamped` (discrete) — S1 тримає SoftS на 4,4 В; відпускає, коли FB поза діапазоном регулювання; `burst` (discrete) — увійдено в активний пакетний режим; `bursting` (discrete) — у пакетному режимі пакет перемикань триває; `shown` (continuous) — затвор, як його бачить силовий ключ, — засувка з фронтом драйвера

#### LM317 — `lm317`

_Adjustable positive regulator, 1.25 V reference, up to 1.5 A_

`integrated/power/lm317.jmx` · type `regulator` · відмова: `open` · TI LM317 datasheet SLVS044Z, Rev. Z

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `V_reference` | 1.25 V | VREF, підігнана опорна напруга | reference voltage at 25 C |
| `I_adjustment` | 50 uA | струм виводу регулювання, типовий | adjustment pin current, typical |
| `I_maximum` | 1.5 A | допустимий вихідний струм | rated maximum output current |
| `V_dropout` | 2 V | типове падіння вхід–вихід (мінімальна різниця) | typical input-output dropout |
| `transconductance` | 10 A/V | крутизна: BETA для JFET (IDSS = BETA · VTO²), KP для MOSFET | regulation transconductance; a finite loop gain |
| `thermal_resistance` | 50 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 30 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 150 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |

**Виводи:** `adjust`, `output`, `input`

### `integrated/references/`

#### TL431 — `tl431`

_Adjustable precision shunt reference, 2.495 V to 36 V, 1 mA to 100 mA_

`integrated/references/tl431.jmx` · type `reference` · відмова: `short` · TI TL431 datasheet (SLVS543), electrical characteristics

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `V_reference` | 2.495 V | VREF, підігнана опорна напруга | Vref: the ref pin above the anode when it regulates |
| `reference_drift` | 0 | дрейф опорної напруги на кельвін, В/К | how Vref moves per kelvin, V/K: the TL431's is a shallow bowl of a few millivolts, none here |
| `T_nominal` | 298.15 K | TNOM: температура, за якої виміряно наведені параметри | the temperature Vref is given at |
| `transconductance` | 10 A/V | крутизна: BETA для JFET (IDSS = BETA · VTO²), KP для MOSFET | how hard the cathode current follows ref above Vref: dynamic impedance about 0.2 Ω |
| `I_minimum` | 0.4 mA | струм катода, нижче якого стабілізації немає | the cathode current below which it does not regulate |
| `I_reference` | 2 uA | Iref: струм у вивід REF | Iref: current into the ref pin |
| `R_off` | 10 MOhm | опір катод–анод у закритому стані | cathode to anode while it does not conduct |
| `power_rating` | 0.7 W | номінальна потужність розсіювання | what it is rated for |
| `thermal_resistance` | 160 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 2 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 150 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |

**Виводи:** `ref`, `anode`, `cathode`


### `integrated/timers/`

#### NE555 — `ne555`

_Precision timer, 4.5 V to 16 V_

`integrated/timers/ne555.jmx` · type `timer` · відмова: `short` · TI NE555 datasheet (SLFS022)

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `R_divider` | 5 kOhm | кожен із трьох резисторів дільника | each of the three divider resistors |
| `V_output_high_drop` | 1.7 V | високий рівень виходу нижче Vcc на цю величину | output high sits this far below Vcc |
| `V_output_low` | 0.1 V | наскільки близько вихід підходить до від'ємної шини | output low |
| `R_output` | 2 Ohm | вихідний опір |  |
| `I_output_limit` | 200 mA | обмеження вихідного струму (втікання й витікання) | output sink and source |
| `switching_time` | 100 ns | час наростання й спаду виходу | output rise and fall time |
| `R_discharge` | 10 Ohm | опір відкритого розрядного транзистора | discharge transistor, on |
| `I_discharge` | 50 mA | струм насичення розрядного транзистора | discharge transistor, saturation |
| `V_reset` | 0.7 V | поріг скидання | reset threshold |
| `I_trigger` | 0.5 uA | струм із виводу TRIG | out of TRIG |
| `I_threshold` | 0.1 uA | струм у вивід THR | into THR |
| `I_quiescent` | 3 mA | струм споживання на одиницю (канал) | supply current at 5 V |
| `thermal_resistance` | 120 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 5 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 150 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |

**Виводи:** `GND`, `trigger`, `output`, `reset`, `control`, `threshold`, `discharge`, `Vcc`

**Стани:** `latched` (discrete) — внутрішній тригер: пише поведінка, читають рівняння; `shown` (continuous) — затвор, як його бачить силовий ключ, — засувка з фронтом драйвера

### `passive/`

#### Capacitor — `capacitor`

_Two-terminal capacitor_

`passive/capacitor.jmx` · type `capacitor`

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `capacitance` | 100 nF | ємність | capacitance |
| `tolerance` | 10 % | допуск: наскільки екземпляр може відрізнятися від номіналу, ±; використовують кути стенда | how far a part may be from its value, ±; a bench's corners use it |
| `temperature_coefficient` | 200 ppm/K | температурний коефіцієнт: зміна номіналу на кельвін від 25 °C; використовує температура стенда | how its value moves per kelvin away from 25 °C; a bench's temperature uses it |
| `voltage_rating` | 50 V | номінальна напруга, яку розраховано витримувати | the most it is made to hold |

**Виводи:** `terminal_one`, `terminal_two`

**Показники на схемі:** `overvoltage` [%] — відсоток допустимої напруги; `charge` [%] — заряд у відсотках від номінальної напруги

#### Ceramic capacitor — `ceramic-capacitor`

_MLCC, X7R: capacitance falling with DC bias and temperature, ESR, ESL_

`passive/ceramic-capacitor.jmx` · type `capacitor` · відмова: `short` · the DC-bias and temperature curves an X7R MLCC's datasheet gives

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `capacitance` | 10 uF | ємність | nominal capacitance, at 0 V and 25 C |
| `V_half` | 8 V | постійна напруга зміщення, за якої лишається половина ємності | the DC bias at which half the capacitance is left |
| `R_equivalent_series` | 5 mOhm | ESR: еквівалентний послідовний опір | ESR |
| `L_equivalent_series` | 1 nH | ESL: еквівалентна послідовна індуктивність корпусу й монтажу | ESL: package and mounting |
| `temperature_curvature` | 1.5e-5 | X7R: близько −15 % за 100 К в обидва боки від 25 °C, частка на кельвін² | X7R: about -15 % at 100 K either side of 25 C, as a share per kelvin squared |
| `T_nominal` | 298.15 K | TNOM: температура, за якої виміряно наведені параметри | the temperature the capacitance is given at |
| `voltage_rating` | 25 V | номінальна напруга, яку розраховано витримувати | the rated voltage |
| `power_rating` | 0.1 W | номінальна потужність розсіювання | what it is rated for |
| `thermal_resistance` | 200 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 10 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 125 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |

**Виводи:** `terminal_one`, `terminal_two`

**Показники на схемі:** `overvoltage` [%] — відсоток допустимої напруги; `charge` [%] — заряд у відсотках від номінальної напруги

#### Electrolytic capacitor — `electrolytic-capacitor`

_Aluminium electrolytic capacitor: capacitance, ESR, leakage_

`passive/electrolytic-capacitor.jmx` · type `capacitor` · відмова: `short` · the equivalent circuit an electrolytic's datasheet tabulates: C, ESR at 100 kHz, leakage current

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `capacitance` | 1000 uF | ємність | capacitance |
| `R_equivalent_series` | 0.1 Ohm | ESR: еквівалентний послідовний опір | ESR |
| `R_leakage` | 1 MOhm | витік: даташитне I = 0,01 · C · V, як опір | leakage: the datasheet's I = 0.01 · C · V, as a resistance |
| `ESR_doubling` | 20 K | ESR подвоюється на кожні стільки кельвінів холоднішання: електроліт густішає на холоді | the ESR doubles for every this many kelvin colder: an electrolyte thickens in the cold |
| `T_nominal` | 298.15 K | TNOM: температура, за якої виміряно наведені параметри | the temperature the ESR is given at |
| `power_rating` | 0.5 W | номінальна потужність розсіювання | what the ripple may heat it with |
| `thermal_resistance` | 40 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 60 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 105 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |
| `voltage_rating` | 25 V | номінальна напруга, яку розраховано витримувати | the rated voltage |

**Виводи:** `positive`, `negative`

**Показники на схемі:** `overvoltage` [%] — відсоток допустимої напруги; `charge` [%] — заряд у відсотках від номінальної напруги


#### Inductor — `inductor`

_Two-terminal inductor_

`passive/inductor.jmx` · type `inductor`

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `inductance` | 1 mH | індуктивність | inductance |

**Виводи:** `terminal_one`, `terminal_two`

#### Potentiometer — `potentiometer`

_A resistance with a wiper, moved while the circuit runs_

`passive/potentiometer.jmx` · type `potentiometer` · відмова: `open`

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `position` | 0.5 in 0 .. 1 step 0.01 · _живий_ | положення повзунка | Wiper position |
| `resistance` | 10 kOhm | опір (повний, між крайніми виводами) | end-to-end resistance |
| `R_wiper` | 1 Ohm | опір контакту повзунка | wiper contact resistance |
| `power_rating` | 0.5 W | номінальна потужність розсіювання | what it is rated for |
| `thermal_resistance` | 100 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 20 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 125 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |

**Виводи:** `a`, `w`, `b`

#### Power inductor — `power-inductor`

_Power inductor: inductance, winding resistance, core saturation_

`passive/power-inductor.jmx` · type `inductor` · відмова: `open` · the figures a power inductor's datasheet gives: L, DCR, ISAT

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `inductance` | 100 uH | індуктивність | inductance at small currents |
| `I_saturation` | 2 A | IS: струм насичення (транспортний для транзистора) | ISAT: past it the core saturates and the inductance falls away |
| `R_winding` | 50 mOhm | DCR: опір обмотки | DCR: the winding's resistance |
| `copper_temperature_coefficient` | 0.00393 | мідь: опір обмотки зростає на 0,393 % на кельвін | copper: the winding's resistance rises 0.393 % per kelvin |
| `saturation_temperature_coefficient` | -0.004 | феритове осердя насичується раніше в нагрітому стані, близько 0,4 % на кельвін | a ferrite core saturates earlier when hot, about 0.4 % per kelvin |
| `T_nominal` | 298.15 K | TNOM: температура, за якої виміряно наведені параметри | the temperature the figures are given at |
| `power_rating` | 1 W | номінальна потужність розсіювання | what it is rated for |
| `thermal_resistance` | 50 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 30 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 125 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |

**Виводи:** `terminal_one`, `terminal_two`


#### Resistor — `resistor`

_Two-terminal resistor_

`passive/resistor.jmx` · type `resistor` · відмова: `open`

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `resistance` | 1 kOhm | опір (повний, між крайніми виводами) | resistance |
| `tolerance` | 1 % | допуск: наскільки екземпляр може відрізнятися від номіналу, ±; використовують кути стенда | how far a part may be from its value, ±; a bench's corners use it |
| `temperature_coefficient` | 100 ppm/K | температурний коефіцієнт: зміна номіналу на кельвін від 25 °C; використовує температура стенда | how its value moves per kelvin away from 25 °C; a bench's temperature uses it |
| `power_rating` | 0.25 W | номінальна потужність розсіювання | what it is rated for |
| `thermal_resistance` | 250 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 10 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 155 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |

**Виводи:** `terminal_one`, `terminal_two`

#### Transformer — `transformer`

_Two coupled windings: self and mutual inductance, leakage, winding resistance_

`passive/transformer.jmx` · type `transformer` · відмова: `open` · coupled inductors as in Berkeley SPICE3 / ngspice 47 (L, L, K)

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `L_primary` | 10 H | індуктивність первинної обмотки: індуктивність намагнічування з боку первинки | primary inductance: the magnetising inductance seen from the primary |
| `L_secondary` | 0.1 H | індуктивність вторинної обмотки: коефіцієнт трансформації √(L1 / L2) | secondary inductance: turns ratio √(L1 / L2) = 10 |
| `coupling` | 0.999 | K: коефіцієнт зв'язку; 1 — ідеальне осердя, менше — розсіювання | K: 1 is a perfect core; below it, leakage |
| `R_primary` | 10 Ohm | опір первинної обмотки | primary winding resistance |
| `R_secondary` | 0.1 Ohm | опір вторинної обмотки | secondary winding resistance |
| `copper_temperature_coefficient` | 0.00393 | мідь: опір обмотки зростає на 0,393 % на кельвін | copper: the windings' resistance rises 0.393 % per kelvin |
| `T_nominal` | 298.15 K | TNOM: температура, за якої виміряно наведені параметри | the temperature the resistances are given at |
| `power_rating` | 10 W | номінальна потужність розсіювання | what it is rated for |
| `thermal_resistance` | 10 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 300 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 120 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |

**Виводи:** `primary_one`, `primary_two`, `secondary_one`, `secondary_two`


### `semiconductors/diodes/`

#### 1N4007 — `1n4007`

_General-purpose rectifier, 1000 V, 1 A_

`semiconductors/diodes/1n4007.jmx` · type `diode` · Сімейство: `diode` · відмова: `short` · onsemi / Vishay 1N4001–1N4007 datasheets; starting point: the common vendor SPICE model

Успадковує всі параметри [`diode`](#diode--diode).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `I_saturation` | 7.03 nA | IS: струм насичення (транспортний для транзистора) | IS |
| `emission_coefficient` | 1.808 | NF / N: коефіцієнт емісії (неідеальності) прямого переходу | N |
| `R_series` | 34 mOhm | RS: послідовний опір (0 у SPICE — відсутній; тут міліом) | RS |
| `C_junction_zero_bias` | 10 pF | CJO: ємність переходу при нульовому зміщенні | CJO |
| `V_junction` | 0.7 V | VJ: контактна різниця потенціалів переходу | VJ |
| `grading_coefficient` | 0.5 | M: коефіцієнт плавності переходу | M |
| `transit_time` | 4 us | TT: час прольоту — накопичений заряд, що дає зворотне відновлення | TT: a standard-recovery rectifier stores microseconds of charge |
| `V_breakdown` | 1200 V | напруга пробою (для транзистора — підтримувальна напруга колектор–емітер; на ній обмежується індуктивний викид) | avalanche above the rated 1000 V |
| `I_breakdown` | 10 uA | струм лавинного пробою при V_breakdown | current at V_breakdown |
| `E_gap` | 1.057 V | EG: ширина забороненої зони, у вольтах | EG |
| `saturation_temperature_exponent` | 5 | XTI: як струм насичення залежить від температури | XTI |
| `power_rating` | 1.25 W | номінальна потужність розсіювання | what it is rated for |
| `thermal_resistance` | 100 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 2 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 150 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |
| `V_reverse_maximum` | 1000 V | VRRM: допустима зворотна напруга; відсутня — мегавольт | VRRM |
| `I_forward_maximum` | 1 A | IF: допустимий струм в обидва боки; відсутній — мегаампер | IF(AV) |

**Виводи:** `anode`, `cathode`

#### 1N4148 — `1n4148`

_Small-signal fast switching diode, 100 V_

`semiconductors/diodes/1n4148.jmx` · type `diode` · Сімейство: `diode` · відмова: `short` · onsemi 1N4148 datasheet; starting point: the common vendor SPICE model

Успадковує всі параметри [`diode`](#diode--diode).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `I_saturation` | 2.52 nA | IS: струм насичення (транспортний для транзистора) | saturation current |
| `emission_coefficient` | 1.752 | NF / N: коефіцієнт емісії (неідеальності) прямого переходу | emission coefficient |
| `R_series` | 0.568 Ohm | RS: послідовний опір (0 у SPICE — відсутній; тут міліом) | series resistance |
| `C_junction_zero_bias` | 4 pF | CJO: ємність переходу при нульовому зміщенні | zero-bias junction capacitance |
| `V_junction` | 0.75 V | VJ: контактна різниця потенціалів переходу | junction potential |
| `grading_coefficient` | 0.4 | M: коефіцієнт плавності переходу | grading coefficient |
| `transit_time` | 5 ns | TT: час прольоту — накопичений заряд, що дає зворотне відновлення | transit time; the vendor model says 20 ns and then misses trr of 4 ns |
| `V_breakdown` | 100 V | напруга пробою (для транзистора — підтримувальна напруга колектор–емітер; на ній обмежується індуктивний викид) | reverse breakdown voltage |
| `I_breakdown` | 100 uA | струм лавинного пробою при V_breakdown | current at V_breakdown |
| `power_rating` | 0.5 W | номінальна потужність розсіювання | what it is rated for |
| `thermal_resistance` | 350 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 1 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 200 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |
| `V_reverse_maximum` | 100 V | VRRM: допустима зворотна напруга; відсутня — мегавольт | VRRM |
| `I_forward_maximum` | 300 mA | IF: допустимий струм в обидва боки; відсутній — мегаампер | IF, continuous |

**Виводи:** `anode`, `cathode`

#### 1N5819 — `1n5819`

_Schottky barrier rectifier, 40 V, 1 A_

`semiconductors/diodes/1n5819.jmx` · type `diode` · Сімейство: `diode` · відмова: `short` · onsemi 1N5817–1N5819 datasheet; starting point: the common vendor SPICE model

Успадковує всі параметри [`diode`](#diode--diode).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `I_saturation` | 31.7 uA | IS: струм насичення (транспортний для транзистора) | IS: a Schottky barrier's large saturation current |
| `emission_coefficient` | 1.373 | NF / N: коефіцієнт емісії (неідеальності) прямого переходу | N |
| `R_series` | 51 mOhm | RS: послідовний опір (0 у SPICE — відсутній; тут міліом) | RS |
| `C_junction_zero_bias` | 110 pF | CJO: ємність переходу при нульовому зміщенні | CJO |
| `V_junction` | 0.35 V | VJ: контактна різниця потенціалів переходу | VJ |
| `grading_coefficient` | 0.35 | M: коефіцієнт плавності переходу | M |
| `transit_time` | 0 s | TT: час прольоту — накопичений заряд, що дає зворотне відновлення | a majority-carrier junction stores no charge |
| `V_breakdown` | 50 V | напруга пробою (для транзистора — підтримувальна напруга колектор–емітер; на ній обмежується індуктивний викид) | avalanche above the rated 40 V |
| `I_breakdown` | 1 mA | струм лавинного пробою при V_breakdown | current at V_breakdown |
| `E_gap` | 0.69 V | EG: ширина забороненої зони, у вольтах | EG: the barrier height |
| `saturation_temperature_exponent` | 2 | XTI: як струм насичення залежить від температури | XTI |
| `power_rating` | 1.25 W | номінальна потужність розсіювання | what it is rated for |
| `thermal_resistance` | 100 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 2 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 125 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |
| `V_reverse_maximum` | 40 V | VRRM: допустима зворотна напруга; відсутня — мегавольт | VRRM |
| `I_forward_maximum` | 1 A | IF: допустимий струм в обидва боки; відсутній — мегаампер | IF(AV) |

**Виводи:** `anode`, `cathode`

#### Bridge rectifier, 1 A 400 V — `bridge-rectifier`

_Four junctions in one package: + and − out of two alternating inputs, two forward drops in the path_

`semiconductors/diodes/bridge-rectifier.jmx` · type `rectifier-bridge` · відмова: `short` · typical 1 A, 400 V glass-passivated bridge (W04M class): VF 1.0 V max at 1 A per element

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `I_saturation` | 1.2 nA | IS: струм насичення (транспортний для транзистора) | each junction's saturation current |
| `emission_coefficient` | 1.8 | NF / N: коефіцієнт емісії (неідеальності) прямого переходу | each junction's emission coefficient |
| `R_series` | 50 mOhm | RS: послідовний опір (0 у SPICE — відсутній; тут міліом) | each element's bulk resistance |
| `C_junction` | 15 pF | ємність кожного переходу | each junction's capacitance |
| `V_breakdown` | 400 V | напруга пробою (для транзистора — підтримувальна напруга колектор–емітер; на ній обмежується індуктивний викид) | each element's reverse breakdown |
| `I_breakdown` | 10 uA | струм лавинного пробою при V_breakdown | current at V_breakdown |
| `T_nominal` | 298.15 K | TNOM: температура, за якої виміряно наведені параметри | the temperature the figures above are measured at |
| `saturation_temperature_exponent` | 3 | XTI: як струм насичення залежить від температури | XTI: how steeply I_saturation follows the temperature |
| `E_gap` | 1.11 V | EG: ширина забороненої зони, у вольтах | silicon's band gap, in volts |
| `thermal_resistance` | 40 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 5 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 150 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |

**Виводи:** `ac1`, `ac2`, `plus`, `minus`

#### BZX55C5V1 — `bzx55c5v1`

_Zener diode, 5.1 V ±5 %, 500 mW_

`semiconductors/diodes/bzx55c5v1.jmx` · type `diode` · Сімейство: `diode` · відмова: `short` · Vishay BZX55 series datasheet, electrical characteristics

Успадковує всі параметри [`diode`](#diode--diode).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `I_saturation` | 10 fA | IS: струм насичення (транспортний для транзистора) | IS |
| `emission_coefficient` | 1 | NF / N: коефіцієнт емісії (неідеальності) прямого переходу | N |
| `R_series` | 20 Ohm | RS: послідовний опір (0 у SPICE — відсутній; тут міліом) | most of the dynamic resistance, rzj 60 Ω at most at 5 mA |
| `C_junction_zero_bias` | 150 pF | CJO: ємність переходу при нульовому зміщенні | CJO |
| `V_breakdown` | 5.0 V | напруга пробою (для транзистора — підтримувальна напруга колектор–емітер; на ній обмежується індуктивний викид) | BV: the Zener voltage, reached at IBV |
| `I_breakdown` | 5 mA | струм лавинного пробою при V_breakdown | IBV: the test current IZT |
| `breakdown_temperature_coefficient` | 0.0001 | TCV: напруга пробою змінюється на цю частку на кельвін | TCV: a 5.1 V Zener sits near zero tempco, about +0.5 mV/K |
| `power_rating` | 0.5 W | номінальна потужність розсіювання | what it is rated for |
| `thermal_resistance` | 300 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 1 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 175 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |
| `I_forward_maximum` | 98 mA | IF: допустимий струм в обидва боки; відсутній — мегаампер | IZM: 0.5 W at 5.1 V; it works reversed, so only its current is a limit |

**Виводи:** `anode`, `cathode`

### `semiconductors/optoelectronics/`

#### Red LED — `led-red`

_Red light-emitting diode, 5 mm, 20 mA_

`semiconductors/optoelectronics/led-red.jmx` · type `diode` · Сімейство: `diode` · відмова: `open` · a typical 5 mm red LED datasheet (VF 2.0 V typical, 2.5 V at most at 20 mA; VR 5 V)

Успадковує всі параметри [`diode`](#diode--diode).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `I_saturation` | 0.00066 fA | IS: струм насичення (транспортний для транзистора) | IS: the wide band gap's tiny saturation current (jMX has no atto prefix) |
| `emission_coefficient` | 2 | NF / N: коефіцієнт емісії (неідеальності) прямого переходу | N |
| `R_series` | 2.5 Ohm | RS: послідовний опір (0 у SPICE — відсутній; тут міліом) | RS |
| `C_junction_zero_bias` | 40 pF | CJO: ємність переходу при нульовому зміщенні | CJO |
| `V_breakdown` | 10 V | напруга пробою (для транзистора — підтримувальна напруга колектор–емітер; на ній обмежується індуктивний викид) | reverse breakdown; the rating is 5 V |
| `I_breakdown` | 10 uA | струм лавинного пробою при V_breakdown | current at V_breakdown |
| `E_gap` | 1.9 V | EG: ширина забороненої зони, у вольтах | the band gap: red light |
| `power_rating` | 0.1 W | номінальна потужність розсіювання | what it is rated for |
| `thermal_resistance` | 400 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 1 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `I_rated` | 20 mA | струм, за якого задано яскравість | the current its brightness is given at |
| `light_temperature_coefficient` | -0.01 | світловий потік червоного світлодіода падає приблизно на 1 % на кельвін температури переходу | red light falls by about 1 % per kelvin of junction temperature |
| `T_nominal` | 298.15 K | TNOM: температура, за якої виміряно наведені параметри | the temperature its brightness is given at |
| `T_maximum` | 100 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |
| `V_reverse_maximum` | 5 V | VRRM: допустима зворотна напруга; відсутня — мегавольт | VR |
| `I_forward_maximum` | 30 mA | IF: допустимий струм в обидва боки; відсутній — мегаампер | IF, continuous |

**Виводи:** `anode`, `cathode`

**Показники на схемі:** `overvoltage` [%] — відсоток допустимої напруги; `overcurrent` [%] — відсоток допустимого струму; `light` [%] — яскравість у відсотках від номінальної

#### PC817 — `pc817`

_Optocoupler, phototransistor output, CTR 50 … 600 %, 5 kV isolation_

`semiconductors/optoelectronics/pc817.jmx` · type `optocoupler` · відмова: `open` · Sharp PC817 datasheet, electrical characteristics

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `I_saturation` | 4.2 fA | IS: струм насичення (транспортний для транзистора) | the LED's saturation current: VF 1.2 V at 20 mA |
| `emission_coefficient` | 1.6 | NF / N: коефіцієнт емісії (неідеальності) прямого переходу | the LED's emission coefficient |
| `R_series` | 2 Ohm | RS: послідовний опір (0 у SPICE — відсутній; тут міліом) | the LED's series resistance |
| `current_transfer_ratio` | 1 | CTR: коефіцієнт передачі струму — струм колектора до струму світлодіода | CTR: collector current over LED current, 0.5 … 6 by rank |
| `transfer_temperature_coefficient` | -0.004 | CTR падає на цю частку на кельвін понад 25 °C | the CTR falls by this share per kelvin above 25 C: about 0.8 at 75 C |
| `T_nominal` | 298.15 K | TNOM: температура, за якої виміряно наведені параметри | the temperature the CTR is given at |
| `V_saturation` | 0.1 V | напруга VCE, нижче якої насичений фототранзистор не опускається | the VCE a saturated phototransistor does not go below |
| `V_saturation_knee` | 0.05 V | наскільки різко він виходить із насичення вище неї | how sharply it comes out of saturation above that |
| `V_early` | 100 V | VAF: пряма напруга Ерлі (відсутня — мегавольт) | the collector current's rise with VCE |
| `I_dark` | 1 nA | темновий струм колектора при вимкненому світлодіоді | collector current with the LED off |
| `power_rating` | 0.2 W | номінальна потужність розсіювання | what it is rated for |
| `thermal_resistance` | 400 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 2 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 100 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |

**Виводи:** `anode`, `cathode`, `emitter`, `collector`


### `semiconductors/thyristors/`

#### 2N5060 — `2n5060`

_Sensitive-gate thyristor, 30 V, 0.8 A, IGT 200 µA_

`semiconductors/thyristors/2n5060.jmx` · type `thyristor` · відмова: `short` · onsemi 2N5060 series datasheet, electrical characteristics

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `V_gate_trigger` | 0.6 V | VGT: напруга керуючого електрода, що відкриває тиристор | VGT: the gate voltage that fires it (0.8 V at most) |
| `I_gate_saturation` | 14 fA | перехід керуючий електрод–катод: струм насичення | the gate–cathode junction: 200 µA at VGT |
| `V_on` | 0.9 V | коліно напруги у відкритому стані | the on-state voltage's knee |
| `R_on` | 0.5 Ohm | нахил у відкритому стані (динамічний опір) | the on-state slope: VTM 1.7 V at 1.2 A at most |
| `I_holding` | 3 mA | IH: струм утримання — нижче нього тиристор закривається | IH: below this anode current it turns off (5 mA at most) |
| `R_off` | 100 MOhm | опір катод–анод у закритому стані | anode to cathode while it blocks |
| `power_rating` | 0.6 W | номінальна потужність розсіювання | what it is rated for |
| `thermal_resistance` | 200 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 2 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 125 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |

**Виводи:** `cathode`, `gate`, `anode`

**Стани:** `conducting` (discrete) — увімкнено й засувкою утримано: пише поведінка, читають рівняння

#### BT136 — `bt136`

_Triac, 600 V, 4 A, logic-level-sensitive gate_

`semiconductors/thyristors/bt136.jmx` · type `thyristor` · відмова: `short` · NXP / WeEn BT136 series datasheet, electrical characteristics

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `V_gate_trigger` | 0.7 V | VGT: напруга керуючого електрода, що відкриває тиристор | VGT typical (1.5 V at most) |
| `I_gate_saturation` | 50 fA | перехід керуючий електрод–катод: струм насичення | the gate junction: a few milliamperes at VGT |
| `V_on` | 0.9 V | коліно напруги у відкритому стані | the on-state voltage's knee |
| `R_on` | 0.15 Ohm | нахил у відкритому стані (динамічний опір) | the on-state slope: VT 1.7 V at 5 A at most |
| `I_holding` | 5 mA | IH: струм утримання — нижче нього тиристор закривається | IH: below this current it turns off (15 mA at most) |
| `R_off` | 100 MOhm | опір катод–анод у закритому стані | between the main terminals while it blocks |
| `power_rating` | 2 W | номінальна потужність розсіювання | what it is rated for without a heatsink |
| `thermal_resistance` | 60 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 30 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 125 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |

**Виводи:** `main_terminal_one`, `main_terminal_two`, `gate`

**Стани:** `conducting` (discrete) — увімкнено й засувкою утримано: пише поведінка, читають рівняння

### `semiconductors/transistors/`

#### 2N2222A — `2n2222`

_NPN small-signal switching transistor, 40 V, 600 mA_

`semiconductors/transistors/2n2222.jmx` · type `bjt` · Сімейство: `npn-transistor` → `bipolar-transistor` · відмова: `open` · onsemi P2N2222A datasheet P2N2222A/D, Rev. 7, and its SPICE model

Успадковує всі параметри [`bipolar-transistor`](#bipolar-transistor--bipolar-transistor).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `I_saturation` | 14.34 fA | IS: струм насичення (транспортний для транзистора) | IS: transport saturation current |
| `beta_forward` | 255.9 | BF: ідеальний прямий коефіцієнт підсилення струму | BF: ideal forward current gain |
| `V_early` | 74.03 V | VAF: пряма напруга Ерлі (відсутня — мегавольт) | VAF: forward Early voltage |
| `I_knee` | 0.2847 A | IKF: струм коліна високої інжекції (відсутній — гігаампер) | IKF: above it the gain falls |
| `I_leakage_base_emitter` | 14.34 fA | ISE: струм насичення витоку база–емітер | ISE: below a milliampere the gain falls |
| `leakage_emission_base_emitter` | 1.307 | NE: його коефіцієнт емісії | NE |
| `beta_reverse` | 6.092 | BR: ідеальний зворотний коефіцієнт підсилення струму | BR: ideal reverse current gain |
| `R_base` | 10 Ohm | RB: опір бази при нульовому зміщенні | RB: base spreading resistance |
| `R_collector` | 0.3 Ohm | RC: опір колектора | collector bulk resistance: most of VCE(sat) at high current |
| `R_emitter` | 0.1 Ohm | RE: опір емітера | emitter resistance |
| `C_base_emitter` | 22.01 pF | CJE: ємність переходу база–емітер при нульовому зміщенні | CJE |
| `grading_base_emitter` | 0.377 | MJE: її коефіцієнт плавності переходу | MJE |
| `C_base_collector` | 7.306 pF | CJC: ємність переходу база–колектор при нульовому зміщенні | CJC |
| `grading_base_collector` | 0.3416 | MJC: її коефіцієнт плавності переходу | MJC |
| `transit_time_forward` | 411.1 ps | TF: ідеальний прямий час прольоту — задає fT | TF: fT about 300 MHz |
| `transit_time_bias_dependence` | 3 | XTF: наскільки TF зростає зі струмом | XTF |
| `transit_time_bias_voltage` | 1.7 V | VTF: напруга VBC, що вдвічі зменшує це зростання | VTF |
| `transit_time_current` | 0.6 A | ITF: струм, з якого починається зростання TF | ITF |
| `transit_time_reverse` | 46.91 ns | TR: ідеальний зворотний час прольоту — час розсмоктування насиченого ключа | TR: the storage time of a saturated switch |
| `beta_temperature_exponent` | 1.5 | XTB: як коефіцієнти підсилення залежать від температури | XTB |
| `T_nominal` | 298.15 K | TNOM: температура, за якої виміряно наведені параметри | the temperature the figures above are measured at |
| `V_breakdown` | 60 V | напруга пробою (для транзистора — підтримувальна напруга колектор–емітер; на ній обмежується індуктивний викид) | avalanche onset: an approximation — VCEO is guaranteed to 40 V, a typical part holds more |
| `power_rating` | 0.625 W | номінальна потужність розсіювання | what it is rated for |
| `thermal_resistance` | 200 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 2 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 150 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |
| `beta_forward_spread` | 0.5 | розкид BF між екземплярами, частка; використовують кути допуску стенда | hFE 100 ... 300 at 150 mA: about half either way |
| `V_collector_emitter_maximum` | 40 V | VCEO: допустима напруга колектор–емітер; відсутня — мегавольт | VCEO |
| `I_collector_maximum` | 600 mA | IC: допустимий струм колектора; відсутній — мегаампер | IC, continuous |

**Виводи:** `emitter`, `base`, `collector`

#### 2N2907A — `2n2907`

_PNP small-signal switching transistor, -60 V, -600 mA_

`semiconductors/transistors/2n2907.jmx` · type `bjt` · Сімейство: `pnp-transistor` → `bipolar-transistor` · відмова: `open` · onsemi PN2907A datasheet PN2907A/D; model: the standard 2N2907A Gummel–Poon parameters

Успадковує всі параметри [`bipolar-transistor`](#bipolar-transistor--bipolar-transistor).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `I_saturation` | 0.6506 fA | IS: струм насичення (транспортний для транзистора) | IS: transport saturation current |
| `beta_forward` | 231.7 | BF: ідеальний прямий коефіцієнт підсилення струму | BF: ideal forward current gain |
| `V_early` | 115.7 V | VAF: пряма напруга Ерлі (відсутня — мегавольт) | VAF: forward Early voltage |
| `I_knee` | 1.079 A | IKF: струм коліна високої інжекції (відсутній — гігаампер) | IKF: above it the gain falls |
| `I_leakage_base_emitter` | 54.81 fA | ISE: струм насичення витоку база–емітер | ISE: below a milliampere the gain falls |
| `leakage_emission_base_emitter` | 1.829 | NE: його коефіцієнт емісії | NE |
| `beta_reverse` | 3.563 | BR: ідеальний зворотний коефіцієнт підсилення струму | BR: ideal reverse current gain |
| `R_base` | 10 Ohm | RB: опір бази при нульовому зміщенні | RB: base spreading resistance |
| `R_collector` | 0.715 Ohm | RC: опір колектора | RC: collector resistance |
| `R_emitter` | 0.1 Ohm | RE: опір емітера | RE: emitter resistance — not in the model; a bond wire's worth |
| `C_base_emitter` | 19.82 pF | CJE: ємність переходу база–емітер при нульовому зміщенні | CJE |
| `grading_base_emitter` | 0.3357 | MJE: її коефіцієнт плавності переходу | MJE |
| `C_base_collector` | 14.76 pF | CJC: ємність переходу база–колектор при нульовому зміщенні | CJC |
| `grading_base_collector` | 0.5383 | MJC: її коефіцієнт плавності переходу | MJC |
| `transit_time_forward` | 603.7 ps | TF: ідеальний прямий час прольоту — задає fT | TF: fT about 200 MHz |
| `transit_time_bias_dependence` | 1.7 | XTF: наскільки TF зростає зі струмом | XTF |
| `transit_time_bias_voltage` | 5 V | VTF: напруга VBC, що вдвічі зменшує це зростання | VTF |
| `transit_time_current` | 0.65 A | ITF: струм, з якого починається зростання TF | ITF |
| `transit_time_reverse` | 111.3 ns | TR: ідеальний зворотний час прольоту — час розсмоктування насиченого ключа | TR: the storage time of a saturated switch |
| `beta_temperature_exponent` | 1.5 | XTB: як коефіцієнти підсилення залежать від температури | XTB |
| `T_nominal` | 298.15 K | TNOM: температура, за якої виміряно наведені параметри | the temperature the figures above are measured at |
| `V_breakdown` | 80 V | напруга пробою (для транзистора — підтримувальна напруга колектор–емітер; на ній обмежується індуктивний викид) | avalanche onset: an approximation — VCEO is guaranteed to 60 V |
| `power_rating` | 0.625 W | номінальна потужність розсіювання | what it is rated for |
| `thermal_resistance` | 200 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 2 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 150 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |
| `beta_forward_spread` | 0.5 | розкид BF між екземплярами, частка; використовують кути допуску стенда | hFE 100 ... 300 at 150 mA: about half either way |
| `V_collector_emitter_maximum` | 60 V | VCEO: допустима напруга колектор–емітер; відсутня — мегавольт | VCEO |
| `I_collector_maximum` | 600 mA | IC: допустимий струм колектора; відсутній — мегаампер | IC, continuous |

**Виводи:** `emitter`, `base`, `collector`

#### 2N5457 — `2n5457`

_N-channel general-purpose JFET, 25 V, IDSS 1 … 5 mA_

`semiconductors/transistors/2n5457.jmx` · type `jfet` · Сімейство: `n-jfet` → `junction-field-effect-transistor` · відмова: `open` · onsemi 2N5457 datasheet, electrical characteristics

Успадковує всі параметри [`junction-field-effect-transistor`](#junction-fet--junction-field-effect-transistor).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `V_threshold` | -1.372 V | VTO: порогова напруга (для JFET — напруга відсічки, від'ємна для збідненого типу) | VTO: inside VGS(off) -0.5 … -6 V |
| `transconductance` | 1.125 mA/V^2 | крутизна: BETA для JFET (IDSS = BETA · VTO²), KP для MOSFET | BETA: IDSS about 2.1 mA |
| `channel_length_modulation` | 0.0023 | LAMBDA: модуляція довжини каналу, на вольт | LAMBDA |
| `R_drain` | 1 Ohm | RD: послідовний опір стоку | RD |
| `R_source` | 1 Ohm | RS: послідовний опір витоку | RS |
| `I_saturation` | 181.3 fA | IS: струм насичення (транспортний для транзистора) | IS |
| `C_gate_source` | 4.5 pF | CGS: ємність затвор–витік | CGS |
| `C_gate_drain` | 6 pF | CGD: ємність затвор–стік | CGD |
| `V_junction` | 0.5 V | VJ: контактна різниця потенціалів переходу | PB |
| `T_nominal` | 298.15 K | TNOM: температура, за якої виміряно наведені параметри | the temperature the figures above are measured at |
| `threshold_temperature_coefficient` | 0.002 | VTOTC: зміна VTO на кельвін, В/К | VTO moves towards zero by about 2 mV per kelvin |
| `V_threshold_spread` | 1 V | розкид порогу між екземплярами; використовують кути допуску стенда | VGS(off) -0.5 ... -6 V |
| `transconductance_spread` | 0.5 | розкид крутизни (BETA / KP) між екземплярами, частка | IDSS 1 ... 5 mA |

**Виводи:** `source`, `gate`, `drain`

#### 2N7000 — `2n7000`

_N-channel enhancement MOSFET, 60 V, 200 mA continuous_

`semiconductors/transistors/2n7000.jmx` · type `mosfet` · Сімейство: `n-mosfet` → `mos-transistor` · відмова: `short` · onsemi 2N7000/2N7002/NDS7002A datasheet NDS7002A/D, Rev. 11

Успадковує всі параметри [`mos-transistor`](#mos-transistor--mos-transistor).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `V_threshold` | 2.1 V | VTO: порогова напруга (для JFET — напруга відсічки, від'ємна для збідненого типу) | VTO: typical gate threshold at 1 mA |
| `transconductance` | 0.484 A/V^2 | крутизна: BETA для JFET (IDSS = BETA · VTO²), KP для MOSFET | KP: fits RDS(on) 1.2 Ω at VGS 10 V and 1.8 Ω at 4.5 V with RD + RS |
| `R_drain` | 0.84 Ohm | RD: послідовний опір стоку | RD: most of what the gate cannot reduce |
| `R_source` | 0.1 Ohm | RS: послідовний опір витоку | RS |
| `I_saturation` | 15 pA | IS: струм насичення (транспортний для транзистора) | IS: body diode saturation current |
| `emission_coefficient` | 1.5 | NF / N: коефіцієнт емісії (неідеальності) прямого переходу | N: body diode emission factor |
| `C_bulk_drain` | 7 pF | CBD: ємність стік–підкладка (вихідна ємність, тіло діода) | CBD: Coss - Crss at zero bias |
| `C_gate_source` | 16 pF | CGS: ємність затвор–витік | CGSO: Ciss - Crss |
| `C_gate_drain` | 4 pF | CGD: ємність затвор–стік | CGDO: Crss |
| `T_nominal` | 298.15 K | TNOM: температура, за якої виміряно наведені параметри | the temperature the figures above are measured at |
| `V_threshold_coefficient` | -0.005 | не SPICE: дрейф модуля порогу, В/К (від'ємний — гарячий відкривається раніше) | the gate turns on earlier when hot, V/K |
| `resistance_temperature_exponent` | 1.5 | не SPICE: RD і RS ростуть як (температура / T_nominal)^цей показник | RDS(on) normalised to temperature: about 1.5x at 125 C |
| `V_breakdown` | 60 V | напруга пробою (для транзистора — підтримувальна напруга колектор–емітер; на ній обмежується індуктивний викид) | drain-source breakdown at 10 uA |
| `I_breakdown` | 10 uA | струм лавинного пробою при V_breakdown | avalanche current at V_breakdown |
| `power_rating` | 0.4 W | номінальна потужність розсіювання | what it is rated for |
| `thermal_resistance` | 312 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 2 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 150 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |
| `V_threshold_spread` | 0.9 V | розкид порогу між екземплярами; використовують кути допуску стенда | VGS(th) 0.8 ... 3 V |
| `V_drain_source_maximum` | 60 V | VDS: допустима напруга стік–витік; відсутня — мегавольт | VDS |
| `V_gate_source_maximum` | 20 V | VGS: допустима напруга на оксиді затвора в обидва боки; відсутня — мегавольт | VGS, either way |
| `I_drain_maximum` | 500 mA | ID: допустимий струм стоку; відсутній — мегаампер | ID 200 mA continuous, 500 mA pulsed |

**Виводи:** `source`, `gate`, `drain`

#### BC547B — `bc547`

_NPN general-purpose transistor, 45 V, 100 mA, gain group B_

`semiconductors/transistors/bc547.jmx` · type `bjt` · Сімейство: `npn-transistor` → `bipolar-transistor` · відмова: `open` · NXP BC547 datasheet and its SPICE model

Успадковує всі параметри [`bipolar-transistor`](#bipolar-transistor--bipolar-transistor).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `I_saturation` | 23.9 fA | IS: струм насичення (транспортний для транзистора) | IS |
| `beta_forward` | 294.3 | BF: ідеальний прямий коефіцієнт підсилення струму | BF |
| `emission_coefficient` | 1.008 | NF / N: коефіцієнт емісії (неідеальності) прямого переходу | NF |
| `V_early` | 63.2 V | VAF: пряма напруга Ерлі (відсутня — мегавольт) | VAF |
| `I_knee` | 0.1357 A | IKF: струм коліна високої інжекції (відсутній — гігаампер) | IKF |
| `I_leakage_base_emitter` | 3.545 fA | ISE: струм насичення витоку база–емітер | ISE |
| `leakage_emission_base_emitter` | 1.541 | NE: його коефіцієнт емісії | NE |
| `beta_reverse` | 7.946 | BR: ідеальний зворотний коефіцієнт підсилення струму | BR |
| `emission_coefficient_reverse` | 1.004 | NR: зворотний коефіцієнт емісії | NR |
| `V_early_reverse` | 25.9 V | VAR: зворотна напруга Ерлі | VAR |
| `I_knee_reverse` | 0.1144 A | IKR: зворотний струм коліна високої інжекції | IKR |
| `I_leakage_base_collector` | 62.72 fA | ISC: струм насичення витоку база–колектор | ISC |
| `leakage_emission_base_collector` | 1.243 | NC: його коефіцієнт емісії | NC |
| `R_base` | 1 Ohm | RB: опір бази при нульовому зміщенні | RB |
| `R_emitter` | 0.4683 Ohm | RE: опір емітера | RE |
| `R_collector` | 0.85 Ohm | RC: опір колектора | RC |
| `C_base_emitter` | 13.58 pF | CJE: ємність переходу база–емітер при нульовому зміщенні | CJE |
| `V_junction_base_emitter` | 0.65 V | VJE: її контактна різниця потенціалів | VJE |
| `grading_base_emitter` | 0.3279 | MJE: її коефіцієнт плавності переходу | MJE |
| `C_base_collector` | 3.728 pF | CJC: ємність переходу база–колектор при нульовому зміщенні | CJC |
| `V_junction_base_collector` | 0.3997 V | VJC: її контактна різниця потенціалів | VJC |
| `grading_base_collector` | 0.2955 | MJC: її коефіцієнт плавності переходу | MJC |
| `internal_base_collector_fraction` | 0.6193 | XCJC: частка CJC на внутрішній базі; решта — на виводі бази | XCJC |
| `transit_time_forward` | 0.4391 ns | TF: ідеальний прямий час прольоту — задає fT | TF: fT about 300 MHz |
| `transit_time_bias_dependence` | 120 | XTF: наскільки TF зростає зі струмом | XTF |
| `transit_time_bias_voltage` | 2.643 V | VTF: напруга VBC, що вдвічі зменшує це зростання | VTF |
| `transit_time_current` | 0.7495 A | ITF: струм, з якого починається зростання TF | ITF |
| `transit_time_reverse` | 10 ns | TR: ідеальний зворотний час прольоту — час розсмоктування насиченого ключа | TR |
| `T_nominal` | 298.15 K | TNOM: температура, за якої виміряно наведені параметри | the temperature the figures above are measured at |
| `V_breakdown` | 65 V | напруга пробою (для транзистора — підтримувальна напруга колектор–емітер; на ній обмежується індуктивний викид) | avalanche onset: VCEO is guaranteed to 45 V |
| `power_rating` | 0.5 W | номінальна потужність розсіювання | what it is rated for |
| `thermal_resistance` | 250 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 2 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 150 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |
| `beta_forward_spread` | 0.38 | розкид BF між екземплярами, частка; використовують кути допуску стенда | gain group B, hFE 200 ... 450 |
| `V_collector_emitter_maximum` | 45 V | VCEO: допустима напруга колектор–емітер; відсутня — мегавольт | VCEO |
| `I_collector_maximum` | 100 mA | IC: допустимий струм колектора; відсутній — мегаампер | IC, continuous |

**Виводи:** `emitter`, `base`, `collector`

#### BC557B — `bc557`

_PNP general-purpose transistor, -45 V, -100 mA, gain group B_

`semiconductors/transistors/bc557.jmx` · type `bjt` · Сімейство: `pnp-transistor` → `bipolar-transistor` · відмова: `open` · NXP BC557 datasheet and its SPICE model

Успадковує всі параметри [`bipolar-transistor`](#bipolar-transistor--bipolar-transistor).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `I_saturation` | 38.3 fA | IS: струм насичення (транспортний для транзистора) | IS |
| `beta_forward` | 344.4 | BF: ідеальний прямий коефіцієнт підсилення струму | BF |
| `emission_coefficient` | 1.008 | NF / N: коефіцієнт емісії (неідеальності) прямого переходу | NF |
| `V_early` | 21.11 V | VAF: пряма напруга Ерлі (відсутня — мегавольт) | VAF |
| `I_knee` | 80.39 mA | IKF: струм коліна високої інжекції (відсутній — гігаампер) | IKF |
| `I_leakage_base_emitter` | 12.2 fA | ISE: струм насичення витоку база–емітер | ISE |
| `leakage_emission_base_emitter` | 1.528 | NE: його коефіцієнт емісії | NE |
| `beta_reverse` | 14.84 | BR: ідеальний зворотний коефіцієнт підсилення струму | BR |
| `emission_coefficient_reverse` | 1.005 | NR: зворотний коефіцієнт емісії | NR |
| `V_early_reverse` | 32.02 V | VAR: зворотна напруга Ерлі | VAR |
| `I_knee_reverse` | 47 mA | IKR: зворотний струм коліна високої інжекції | IKR |
| `I_leakage_base_collector` | 0.285 pA | ISC: струм насичення витоку база–колектор | ISC |
| `leakage_emission_base_collector` | 1.28 | NC: його коефіцієнт емісії | NC |
| `R_base` | 1 Ohm | RB: опір бази при нульовому зміщенні | RB |
| `R_emitter` | 0.6202 Ohm | RE: опір емітера | RE |
| `R_collector` | 0.5713 Ohm | RC: опір колектора | RC |
| `C_base_emitter` | 12.3 pF | CJE: ємність переходу база–емітер при нульовому зміщенні | CJE |
| `V_junction_base_emitter` | 0.6106 V | VJE: її контактна різниця потенціалів | VJE |
| `grading_base_emitter` | 0.378 | MJE: її коефіцієнт плавності переходу | MJE |
| `C_base_collector` | 10.8 pF | CJC: ємність переходу база–колектор при нульовому зміщенні | CJC |
| `V_junction_base_collector` | 0.1022 V | VJC: її контактна різниця потенціалів | VJC |
| `grading_base_collector` | 0.3563 | MJC: її коефіцієнт плавності переходу | MJC |
| `internal_base_collector_fraction` | 0.6288 | XCJC: частка CJC на внутрішній базі; решта — на виводі бази | XCJC |
| `transit_time_forward` | 0.56 ns | TF: ідеальний прямий час прольоту — задає fT | TF: fT about 150 MHz |
| `transit_time_bias_dependence` | 3.414 | XTF: наскільки TF зростає зі струмом | XTF |
| `transit_time_bias_voltage` | 5.23 V | VTF: напруга VBC, що вдвічі зменшує це зростання | VTF |
| `transit_time_current` | 0.1483 A | ITF: струм, з якого починається зростання TF | ITF |
| `transit_time_reverse` | 10 ns | TR: ідеальний зворотний час прольоту — час розсмоктування насиченого ключа | TR |
| `T_nominal` | 298.15 K | TNOM: температура, за якої виміряно наведені параметри | the temperature the figures above are measured at |
| `V_breakdown` | 65 V | напруга пробою (для транзистора — підтримувальна напруга колектор–емітер; на ній обмежується індуктивний викид) | avalanche onset: VCEO is guaranteed to 45 V |
| `power_rating` | 0.5 W | номінальна потужність розсіювання | what it is rated for |
| `thermal_resistance` | 250 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 2 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 150 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |
| `beta_forward_spread` | 0.37 | розкид BF між екземплярами, частка; використовують кути допуску стенда | gain group B, hFE 220 ... 475 |
| `V_collector_emitter_maximum` | 45 V | VCEO: допустима напруга колектор–емітер; відсутня — мегавольт | VCEO |
| `I_collector_maximum` | 100 mA | IC: допустимий струм колектора; відсутній — мегаампер | IC, continuous |

**Виводи:** `emitter`, `base`, `collector`

#### BS250 — `bs250`

_P-channel enhancement MOSFET, -45 V, -230 mA continuous_

`semiconductors/transistors/bs250.jmx` · type `mosfet` · Сімейство: `p-mosfet` → `mos-transistor` · відмова: `short` · Diodes Inc. BS250P datasheet, electrical characteristics (limits only)

Успадковує всі параметри [`mos-transistor`](#mos-transistor--mos-transistor).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `V_threshold` | -2.4 V | VTO: порогова напруга (для JFET — напруга відсічки, від'ємна для збідненого типу) | VTO: inside VGS(th) -1 .. -3.5 V at -1 mA |
| `transconductance` | 26 mA/V^2 | крутизна: BETA для JFET (IDSS = BETA · VTO²), KP для MOSFET | KP: with RD + RS, about 9 Ω at VGS -10 V — under the 14 Ω limit |
| `R_drain` | 3.5 Ohm | RD: послідовний опір стоку | RD |
| `R_source` | 0.5 Ohm | RS: послідовний опір витоку | RS |
| `I_saturation` | 10 pA | IS: струм насичення (транспортний для транзистора) | IS: body diode saturation current |
| `emission_coefficient` | 1.5 | NF / N: коефіцієнт емісії (неідеальності) прямого переходу | N: body diode emission factor |
| `C_bulk_drain` | 20 pF | CBD: ємність стік–підкладка (вихідна ємність, тіло діода) | CBD: output capacitance at zero bias |
| `C_gate_source` | 25 pF | CGS: ємність затвор–витік | CGSO: inside Ciss at most 60 pF |
| `C_gate_drain` | 5 pF | CGD: ємність затвор–стік | CGDO: Crss |
| `T_nominal` | 298.15 K | TNOM: температура, за якої виміряно наведені параметри | the temperature the figures above are measured at |
| `V_threshold_coefficient` | -0.004 | не SPICE: дрейф модуля порогу, В/К (від'ємний — гарячий відкривається раніше) | the gate turns on earlier when hot, V/K |
| `resistance_temperature_exponent` | 1.5 | не SPICE: RD і RS ростуть як (температура / T_nominal)^цей показник | RDS(on) rises when hot |
| `V_breakdown` | 50 V | напруга пробою (для транзистора — підтримувальна напруга колектор–емітер; на ній обмежується індуктивний викид) | drain-source avalanche: BVDSS is at least 45 V |
| `I_breakdown` | 100 uA | струм лавинного пробою при V_breakdown | avalanche current at V_breakdown |
| `V_drain_source_maximum` | 45 V | VDS: допустима напруга стік–витік; відсутня — мегавольт | VDS |
| `V_gate_source_maximum` | 20 V | VGS: допустима напруга на оксиді затвора в обидва боки; відсутня — мегавольт | VGS, either way |
| `I_drain_maximum` | 230 mA | ID: допустимий струм стоку; відсутній — мегаампер | ID |
| `V_threshold_spread` | 1.1 V | розкид порогу між екземплярами; використовують кути допуску стенда | VGS(th) -1 ... -3.5 V |

**Виводи:** `source`, `gate`, `drain`

#### IRF9540N — `irf9540`

_P-channel power MOSFET, -100 V, -23 A, 0.117 Ω_

`semiconductors/transistors/irf9540.jmx` · type `mosfet` · Сімейство: `p-mosfet` → `mos-transistor` · відмова: `short` · Infineon IRF9540N datasheet

Успадковує всі параметри [`mos-transistor`](#mos-transistor--mos-transistor).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `V_threshold` | -3 V | VTO: порогова напруга (для JFET — напруга відсічки, від'ємна для збідненого типу) | VTO: gate threshold, -2 … -4 V at -250 uA |
| `transconductance` | 2.86 A/V^2 | крутизна: BETA для JFET (IDSS = BETA · VTO²), KP для MOSFET | KP: with RD + RS, 0.1 Ω at VGS -10 V; 0.117 Ω is the datasheet's maximum |
| `R_drain` | 40 mOhm | RD: послідовний опір стоку | RD |
| `R_source` | 10 mOhm | RS: послідовний опір витоку | RS |
| `I_saturation` | 10 pA | IS: струм насичення (транспортний для транзистора) | IS: body diode saturation current |
| `emission_coefficient` | 1.3 | NF / N: коефіцієнт емісії (неідеальності) прямого переходу | N: body diode emission factor |
| `C_bulk_drain` | 160 pF | CBD: ємність стік–підкладка (вихідна ємність, тіло діода) | CBD: Coss - Crss |
| `C_gate_source` | 1060 pF | CGS: ємність затвор–витік | CGSO: Ciss - Crss |
| `C_gate_drain` | 240 pF | CGD: ємність затвор–стік | CGDO: Crss |
| `T_nominal` | 298.15 K | TNOM: температура, за якої виміряно наведені параметри | the temperature the figures above are measured at |
| `V_threshold_coefficient` | -0.006 | не SPICE: дрейф модуля порогу, В/К (від'ємний — гарячий відкривається раніше) | the gate turns on earlier when hot, V/K |
| `resistance_temperature_exponent` | 1.5 | не SPICE: RD і RS ростуть як (температура / T_nominal)^цей показник | RDS(on) rises when hot |
| `V_breakdown` | 100 V | напруга пробою (для транзистора — підтримувальна напруга колектор–емітер; на ній обмежується індуктивний викид) | drain-source breakdown |
| `I_breakdown` | 250 uA | струм лавинного пробою при V_breakdown | avalanche current at V_breakdown |
| `power_rating` | 2 W | номінальна потужність розсіювання | what it is rated for without a heatsink |
| `thermal_resistance` | 62 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 30 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 175 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |
| `V_threshold_spread` | 1 V | розкид порогу між екземплярами; використовують кути допуску стенда | VGS(th) -2 ... -4 V |
| `V_drain_source_maximum` | 100 V | VDS: допустима напруга стік–витік; відсутня — мегавольт | VDS |
| `V_gate_source_maximum` | 20 V | VGS: допустима напруга на оксиді затвора в обидва боки; відсутня — мегавольт | VGS, either way |
| `I_drain_maximum` | 23 A | ID: допустимий струм стоку; відсутній — мегаампер | ID at 25 C case |

**Виводи:** `source`, `gate`, `drain`

#### IRFZ44N — `irfz44n`

_N-channel power MOSFET, 55 V, 49 A, 17.5 mΩ_

`semiconductors/transistors/irfz44n.jmx` · type `mosfet` · Сімейство: `n-mosfet` → `mos-transistor` · відмова: `short` · Infineon IRFZ44N datasheet, PD-94053

Успадковує всі параметри [`mos-transistor`](#mos-transistor--mos-transistor).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `V_threshold` | 3 V | VTO: порогова напруга (для JFET — напруга відсічки, від'ємна для збідненого типу) | VTO: gate threshold, 2 … 4 V at 250 uA |
| `transconductance` | 20.4 A/V^2 | крутизна: BETA для JFET (IDSS = BETA · VTO²), KP для MOSFET | KP: with RD + RS, 15 mΩ at VGS 10 V; 17.5 mΩ is the datasheet's maximum |
| `R_drain` | 6 mOhm | RD: послідовний опір стоку | RD |
| `R_source` | 2 mOhm | RS: послідовний опір витоку | RS |
| `I_saturation` | 10 pA | IS: струм насичення (транспортний для транзистора) | IS: body diode saturation current |
| `emission_coefficient` | 1.2 | NF / N: коефіцієнт емісії (неідеальності) прямого переходу | N: body diode emission factor |
| `C_bulk_drain` | 272 pF | CBD: ємність стік–підкладка (вихідна ємність, тіло діода) | CBD: Coss - Crss |
| `C_gate_source` | 1382 pF | CGS: ємність затвор–витік | CGSO: Ciss - Crss |
| `C_gate_drain` | 88 pF | CGD: ємність затвор–стік | CGDO: Crss |
| `T_nominal` | 298.15 K | TNOM: температура, за якої виміряно наведені параметри | the temperature the figures above are measured at |
| `V_threshold_coefficient` | -0.006 | не SPICE: дрейф модуля порогу, В/К (від'ємний — гарячий відкривається раніше) | threshold change per kelvin, V/K |
| `resistance_temperature_exponent` | 1.5 | не SPICE: RD і RS ростуть як (температура / T_nominal)^цей показник | RDS(on) rises when hot |
| `V_breakdown` | 55 V | напруга пробою (для транзистора — підтримувальна напруга колектор–емітер; на ній обмежується індуктивний викид) | drain-source breakdown at 250 uA |
| `I_breakdown` | 250 uA | струм лавинного пробою при V_breakdown | avalanche current at V_breakdown |
| `power_rating` | 2 W | номінальна потужність розсіювання | what it is rated for without a heatsink |
| `thermal_resistance` | 62 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 30 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 175 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |
| `V_threshold_spread` | 1 V | розкид порогу між екземплярами; використовують кути допуску стенда | VGS(th) 2 ... 4 V |
| `V_drain_source_maximum` | 55 V | VDS: допустима напруга стік–витік; відсутня — мегавольт | VDS |
| `V_gate_source_maximum` | 20 V | VGS: допустима напруга на оксиді затвора в обидва боки; відсутня — мегавольт | VGS, either way |
| `I_drain_maximum` | 49 A | ID: допустимий струм стоку; відсутній — мегаампер | ID at 25 C case |

**Виводи:** `source`, `gate`, `drain`

#### J201 — `j201`

_N-channel JFET, 40 V, IDSS 0.2 … 1 mA_

`semiconductors/transistors/j201.jmx` · type `jfet` · Сімейство: `n-jfet` → `junction-field-effect-transistor` · відмова: `open` · onsemi J201/J202 datasheet, electrical characteristics

Успадковує всі параметри [`junction-field-effect-transistor`](#junction-fet--junction-field-effect-transistor).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `V_threshold` | -0.6 V | VTO: порогова напруга (для JFET — напруга відсічки, від'ємна для збідненого типу) | VTO: inside VGS(off) -0.3 … -1.5 V |
| `transconductance` | 1.3 mA/V^2 | крутизна: BETA для JFET (IDSS = BETA · VTO²), KP для MOSFET | BETA: IDSS about 0.47 mA |
| `channel_length_modulation` | 0.00225 | LAMBDA: модуляція довжини каналу, на вольт | LAMBDA |
| `R_drain` | 1 Ohm | RD: послідовний опір стоку | RD |
| `R_source` | 1 Ohm | RS: послідовний опір витоку | RS |
| `I_saturation` | 114.5 fA | IS: струм насичення (транспортний для транзистора) | IS |
| `C_gate_source` | 4.5 pF | CGS: ємність затвор–витік | CGS |
| `C_gate_drain` | 4.05 pF | CGD: ємність затвор–стік | CGD |
| `V_junction` | 0.5 V | VJ: контактна різниця потенціалів переходу | PB |
| `T_nominal` | 298.15 K | TNOM: температура, за якої виміряно наведені параметри | the temperature the figures above are measured at |
| `threshold_temperature_coefficient` | 0.002 | VTOTC: зміна VTO на кельвін, В/К | VTO moves towards zero by about 2 mV per kelvin |
| `V_threshold_spread` | 0.3 V | розкид порогу між екземплярами; використовують кути допуску стенда | VGS(off) -0.3 ... -1.5 V |
| `transconductance_spread` | 0.5 | розкид крутизни (BETA / KP) між екземплярами, частка | IDSS 0.2 ... 1 mA |

**Виводи:** `source`, `gate`, `drain`

#### TIP31C — `tip31c`

_NPN power transistor, 100 V, 3 A_

`semiconductors/transistors/tip31c.jmx` · type `bjt` · Сімейство: `npn-transistor` → `bipolar-transistor` · відмова: `short` · onsemi TIP31 series datasheet TIP31A/D, Rev. 14

Успадковує всі параметри [`bipolar-transistor`](#bipolar-transistor--bipolar-transistor).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `I_saturation` | 1 pA | IS: струм насичення (транспортний для транзистора) | IS: transport saturation current |
| `beta_forward` | 80 | BF: ідеальний прямий коефіцієнт підсилення струму | BF: ideal forward current gain |
| `beta_reverse` | 5 | BR: ідеальний зворотний коефіцієнт підсилення струму | BR: ideal reverse current gain |
| `V_early` | 100 V | VAF: пряма напруга Ерлі (відсутня — мегавольт) | VAF: forward Early voltage |
| `I_knee` | 2 A | IKF: струм коліна високої інжекції (відсутній — гігаампер) | IKF: forward knee current: above it the gain falls |
| `R_base` | 3 Ohm | RB: опір бази при нульовому зміщенні | RB: part of VBE(on) at amperes |
| `R_collector` | 0.15 Ohm | RC: опір колектора | RC: most of VCE(sat) at amperes |
| `R_emitter` | 0.05 Ohm | RE: опір емітера | RE: emitter resistance and bond wire |
| `C_base_emitter` | 300 pF | CJE: ємність переходу база–емітер при нульовому зміщенні | CJE |
| `C_base_collector` | 80 pF | CJC: ємність переходу база–колектор при нульовому зміщенні | CJC |
| `transit_time_forward` | 50 ns | TF: ідеальний прямий час прольоту — задає fT | TF: from fT at least 3 MHz — an approximation, the datasheet gives no model |
| `V_breakdown` | 100 V | напруга пробою (для транзистора — підтримувальна напруга колектор–емітер; на ній обмежується індуктивний викид) | collector-emitter sustaining voltage: an inductive kick stops here |
| `I_breakdown` | 1 mA | струм лавинного пробою при V_breakdown | avalanche current at V_breakdown |
| `T_nominal` | 298.15 K | TNOM: температура, за якої виміряно наведені параметри | the temperature the figures above are measured at |
| `power_rating` | 2 W | номінальна потужність розсіювання | what it is rated for without a heatsink |
| `thermal_resistance` | 62.5 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 30 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 150 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |
| `V_collector_emitter_maximum` | 100 V | VCEO: допустима напруга колектор–емітер; відсутня — мегавольт | VCEO |
| `I_collector_maximum` | 3 A | IC: допустимий струм колектора; відсутній — мегаампер | IC, continuous |

**Виводи:** `emitter`, `base`, `collector`

#### TIP32C — `tip32c`

_PNP power transistor, -100 V, -3 A_

`semiconductors/transistors/tip32c.jmx` · type `bjt` · Сімейство: `pnp-transistor` → `bipolar-transistor` · відмова: `short` · onsemi TIP32 series datasheet (the TIP31 complement)

Успадковує всі параметри [`bipolar-transistor`](#bipolar-transistor--bipolar-transistor).

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `I_saturation` | 1 pA | IS: струм насичення (транспортний для транзистора) | IS: transport saturation current |
| `beta_forward` | 80 | BF: ідеальний прямий коефіцієнт підсилення струму | BF: ideal forward current gain |
| `beta_reverse` | 5 | BR: ідеальний зворотний коефіцієнт підсилення струму | BR: ideal reverse current gain |
| `V_early` | 100 V | VAF: пряма напруга Ерлі (відсутня — мегавольт) | VAF: forward Early voltage |
| `I_knee` | 2 A | IKF: струм коліна високої інжекції (відсутній — гігаампер) | IKF: forward knee current: above it the gain falls |
| `R_base` | 3 Ohm | RB: опір бази при нульовому зміщенні | RB: part of VBE(on) at amperes |
| `R_collector` | 0.15 Ohm | RC: опір колектора | RC: most of VCE(sat) at amperes |
| `R_emitter` | 0.05 Ohm | RE: опір емітера | RE: emitter resistance and bond wire |
| `C_base_emitter` | 300 pF | CJE: ємність переходу база–емітер при нульовому зміщенні | CJE |
| `C_base_collector` | 80 pF | CJC: ємність переходу база–колектор при нульовому зміщенні | CJC |
| `transit_time_forward` | 50 ns | TF: ідеальний прямий час прольоту — задає fT | TF: from fT at least 3 MHz — an approximation, the datasheet gives no model |
| `V_breakdown` | 100 V | напруга пробою (для транзистора — підтримувальна напруга колектор–емітер; на ній обмежується індуктивний викид) | collector-emitter sustaining voltage: an inductive kick stops here |
| `I_breakdown` | 1 mA | струм лавинного пробою при V_breakdown | avalanche current at V_breakdown |
| `T_nominal` | 298.15 K | TNOM: температура, за якої виміряно наведені параметри | the temperature the figures above are measured at |
| `power_rating` | 2 W | номінальна потужність розсіювання | what it is rated for without a heatsink |
| `thermal_resistance` | 62.5 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 30 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 150 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |
| `V_collector_emitter_maximum` | 100 V | VCEO: допустима напруга колектор–емітер; відсутня — мегавольт | VCEO |
| `I_collector_maximum` | 3 A | IC: допустимий струм колектора; відсутній — мегаампер | IC, continuous |

**Виводи:** `emitter`, `base`, `collector`

### `sources/`

#### Battery — `battery`

_Battery: EMF by state of charge, internal resistance, polarisation sag and recovery_

`sources/battery.jmx` · type `source` · відмова: `open` · the Thevenin battery model (EMF + R0 + one RC), a lithium-ion cell's figures

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `V_full` | 4.2 V | ЕРС повністю зарядженого | EMF when full |
| `V_empty` | 3.0 V | ЕРС розрядженого | EMF when empty |
| `state_of_charge` | 1 in 0 .. 1 step 0.05 · _живий_ | рівень заряду (0…1) | how full it is |
| `R_internal` | 50 mOhm | R0: миттєве падіння під навантаженням (внутрішній опір) | R0: the drop under load, at once |
| `R_polarisation` | 30 mOhm | R1: подальше просідання за час поляризації | R1: the further sag, over the polarisation time |
| `C_polarisation` | 300 F | C1: разом із R1 — просідання протягом приблизно десяти секунд | C1: with R1, a sag over about ten seconds |
| `power_rating` | 10 W | номінальна потужність розсіювання | what it may deliver |
| `thermal_resistance` | 20 K/W | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | kelvin above the bench per watt it takes |
| `thermal_time_constant` | 300 s | теплова стала часу: як швидко елемент прогрівається до цієї температури | how fast it warms to that |
| `T_maximum` | 60 °C | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | past this it fails on a realistic bench |

**Виводи:** `positive`, `negative`

#### Bench power supply — `bench-supply`

_A DC supply with a current limit: constant voltage, then constant current when the load asks for more_

`sources/bench-supply.jmx` · type `power-supply`

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `V_set` | 12 V in 0 .. 30 step 0.1 · _живий_ | напруга, яку тримає джерело, доки струм нижче межі | the voltage it holds while the current is below the limit |
| `I_limit` | 1 A in 0.01 .. 5 step 0.01 · _живий_ | максимальний струм; на межі напруга просідає | the most current it gives; at the limit the voltage sags |
| `R_output` | 10 mOhm | вихідний опір | output resistance while it holds the voltage |

**Виводи:** `positive`, `negative`

#### Current source — `current-source`

_Two-terminal DC current source_

`sources/current-source.jmx` · type `current-source`

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `I_output` | 1 mA | струм джерела | source current |

**Виводи:** `positive`, `negative`

#### Dual power supply — `dual-supply`

_A bipolar supply: +V and −V around a common 0 V, each rail with its own current limit_

`sources/dual-supply.jmx` · type `dual-power-supply`

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `V_positive` | 15 V in 0 .. 30 step 0.1 · _живий_ | додатна шина відносно спільного | the positive rail above the common |
| `V_negative` | 15 V in 0 .. 30 step 0.1 · _живий_ | від'ємна шина нижче спільного, за модулем | the negative rail below the common, as a magnitude |
| `I_limit_positive` | 1 A in 0.01 .. 5 step 0.01 · _живий_ | максимальний струм додатної шини | the most current the positive rail gives |
| `I_limit_negative` | 1 A in 0.01 .. 5 step 0.01 · _живий_ | максимальний струм від'ємної шини | the most current the negative rail gives |
| `R_output` | 10 mOhm | вихідний опір | each rail's output resistance while it holds its voltage |

**Виводи:** `positive`, `common`, `negative`

#### Harmonic generator — `harmonic-generator`

_A fundamental and up to eight harmonics, summed: offset + Σ a_k · sin(2π · k · frequency · time + φ_k)_

`sources/harmonic-generator.jmx` · type `generator`

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `frequency` | 100 Hz | частота (для гармонійного генератора — основна; гармоніка k має частоту k·f) | the fundamental: harmonic k is k times it |
| `a1` | 1 V | амплітуда основної гармоніки | amplitude of the fundamental |
| `a2` | 0 V | амплітуда 2-ї гармоніки | amplitude of the 2nd harmonic |
| `a3` | 0 V | амплітуда 3-ї гармоніки | amplitude of the 3rd harmonic |
| `a4` | 0 V | амплітуда 4-ї гармоніки | amplitude of the 4th harmonic |
| `a5` | 0 V | амплітуда 5-ї гармоніки | amplitude of the 5th harmonic |
| `a6` | 0 V | амплітуда 6-ї гармоніки | amplitude of the 6th harmonic |
| `a7` | 0 V | амплітуда 7-ї гармоніки | amplitude of the 7th harmonic |
| `a8` | 0 V | амплітуда 8-ї гармоніки | amplitude of the 8th harmonic |
| `p1` | 0 rad | фаза основної гармоніки | phase of the fundamental |
| `p2` | 0 rad | фаза 2-ї гармоніки | phase of the 2nd harmonic |
| `p3` | 0 rad | фаза 3-ї гармоніки | phase of the 3rd harmonic |
| `p4` | 0 rad | фаза 4-ї гармоніки | phase of the 4th harmonic |
| `p5` | 0 rad | фаза 5-ї гармоніки | phase of the 5th harmonic |
| `p6` | 0 rad | фаза 6-ї гармоніки | phase of the 6th harmonic |
| `p7` | 0 rad | фаза 7-ї гармоніки | phase of the 7th harmonic |
| `p8` | 0 rad | фаза 8-ї гармоніки | phase of the 8th harmonic |
| `offset` | 0 V | постійна складова, навколо якої коливається сигнал | the voltage it swings around |

**Виводи:** `positive`, `negative`

#### Noise generator — `noise-generator`

_Band-limited noise: white (flat to the bandwidth) or pink (equal power per octave), at an RMS level_

`sources/noise-generator.jmx` · type `generator`

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `level` | 100 mV | СКЗ (RMS) напруги шуму | RMS voltage of the noise |
| `bandwidth` | 20 kHz | найвища частота в шумі (смуга) | the highest frequency in it |
| `lowest` | 20 Hz | нижня частота рожевого шуму; білий починається з нуля | where pink noise starts; white noise starts at zero |
| `pink` | 0 in 0 .. 1 step 1 · _живий_ | 0 — білий (рівний до смуги), 1 — рожевий (рівна потужність у кожній октаві) | 0: white — flat to the bandwidth; 1: pink — equal power in every octave |
| `offset` | 0 V | постійна складова, навколо якої коливається сигнал | the voltage it wanders around |
| `seed` | 1 | зерно генератора: інше зерно — інший шум того ж типу | another seed, another noise of the same kind |

**Виводи:** `positive`, `negative`

#### Sine generator — `sine-generator`

_An ideal sine voltage: offset + amplitude · sin(2π · frequency · time + phase)_

`sources/sine-generator.jmx` · type `generator`

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `amplitude` | 1 V | пікова напруга над постійною складовою | peak voltage above the offset |
| `frequency` | 50 Hz | частота (для гармонійного генератора — основна; гармоніка k має частоту k·f) | how many periods a second |
| `phase` | 0 rad | початкова фаза: де стоїть період у момент 0 | where the period stands at time 0 |
| `offset` | 0 V | постійна складова, навколо якої коливається сигнал | the voltage it swings around |

**Виводи:** `positive`, `negative`

#### Square generator — `square-generator`

_An ideal square voltage: high for the duty's share of every period, low for the rest_

`sources/square-generator.jmx` · type `generator`

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `high` | 5 V | рівень у стані «увімкнено» | the level while on |
| `low` | 0 V | рівень у стані «вимкнено» | the level while off |
| `frequency` | 1 kHz | частота (для гармонійного генератора — основна; гармоніка k має частоту k·f) | how many periods a second |
| `duty` | 0.5 in 0.01 .. 0.99 step 0.01 · _живий_ | коефіцієнт заповнення: частка періоду у високому стані | share of each period spent high |

**Виводи:** `positive`, `negative`

#### Staircase generator — `staircase-generator`

_Equal voltage steps, each held for one period, cycling: offset + step · floor(mod(frequency · time, steps))_

`sources/staircase-generator.jmx` · type `generator`

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `step` | 0.5 V | висота однієї сходинки | the height of one step |
| `steps` | 6 | кількість сходинок до повторення | how many steps before it starts again |
| `frequency` | 50 Hz | частота (для гармонійного генератора — основна; гармоніка k має частоту k·f) | steps a second: each step lasts one period |
| `offset` | 0 V | постійна складова, навколо якої коливається сигнал | the first step's level |

**Виводи:** `positive`, `negative`

#### Voltage-controlled current source — `voltage-controlled-current-source`

_Ideal voltage-controlled current source: I(positive → negative) = transconductance · V(control_positive, control_negative)_

`sources/voltage-controlled-current-source.jmx` · type `source` · відмова: `open` · SPICE G element, linear form

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `transconductance` | 1 mA/V | крутизна: BETA для JFET (IDSS = BETA · VTO²), KP для MOSFET | the output current over the controlling voltage |

**Виводи:** `positive`, `negative`, `control_positive`, `control_negative`

#### Voltage-controlled voltage source — `voltage-controlled-voltage-source`

_Ideal voltage-controlled voltage source: V(positive, negative) = gain · V(control_positive, control_negative)_

`sources/voltage-controlled-voltage-source.jmx` · type `source` · відмова: `open` · SPICE E element, linear form

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `gain` | 1 | коефіцієнт передачі: вихідна напруга (або струм) на керуючу напругу | the output voltage over the controlling voltage |

**Виводи:** `positive`, `negative`, `control_positive`, `control_negative`

#### Voltage source — `voltage-source`

_Two-terminal voltage source: constant, or switched on at the start. A sine or a square is a generator — sine-generator, square-generator_

`sources/voltage-source.jmx` · type `voltage-source`

| Параметр | Величина | Значення | Примітка з файлу (англ.) |
|---|---|---|---|
| `U_output` | 5 V | напруга джерела | source voltage |

**Виводи:** `positive`, `negative`

## Стенд — властивості, які читає кожен компонент

Це не параметри компонентів: компонент не може їх оголосити. Стенд задає їх одразу для всіх деталей.

| Ім'я | Значення |
|---|---|
| `temperature` | власна температура деталі в кельвінах: стенда, 25 °C якщо не задано (`bench for 1 ms at 85 °C`), а на реалістичному стенді ще й піднята її власним теплом; кожна модель пише свій дрейф відносно власної `T_nominal` |
| `realistic` | 0 на ідеальному стенді, 1 на реалістичному (`bench for 1 ms realistic`): саморозігрів, навантаження приладу, падіння на шунті, вигорання вище `T_maximum` |
| `tolerance_corner` | −1, 0 або +1: кожен `tolerance` і кожен `*_spread` береться на нижній межі, на номіналі або на верхній межі |
| `detail` | рівень деталізації стенда — 0 чернетка, 1 стандарт (усі перевірки), 2 точно; бази з переходами множать свій накопичений заряд на `min(detail, 1)`, тож у чернетці перемикання миттєве |
| `time` | час схеми — для генераторів і поведінки |
| умови окремих деталей | стенд може задати будь-якій деталі власний допуск і ppm/°C, а будь-якому джерелу — зміщення (`BenchConditions`) |

Кожен компонент може також мати:

| Рядок | Значення |
|---|---|
| `meta failure open\|short` | чим стає після вигорання: обрив чи коротке замикання |
| `power_rating`, `thermal_resistance`, `thermal_time_constant`, `T_maximum` | його тепло: температура = стенд + P·Rθ із теплою сталою часу; вище `T_maximum` на реалістичному стенді — відмова |
| `noise(density[, corner])` | у рівнянні струму: джерело шуму для аналізу шуму й перехідного шуму реалістичного стенда |

## Абетковий покажчик

| Параметр | Значення | Де використовується |
|---|---|---|
| `a1` | амплітуда основної гармоніки | `harmonic-generator` |
| `a2` | амплітуда 2-ї гармоніки | `harmonic-generator` |
| `a3` | амплітуда 3-ї гармоніки | `harmonic-generator` |
| `a4` | амплітуда 4-ї гармоніки | `harmonic-generator` |
| `a5` | амплітуда 5-ї гармоніки | `harmonic-generator` |
| `a6` | амплітуда 6-ї гармоніки | `harmonic-generator` |
| `a7` | амплітуда 7-ї гармоніки | `harmonic-generator` |
| `a8` | амплітуда 8-ї гармоніки | `harmonic-generator` |
| `amplitude` | пікова напруга над постійною складовою | `sine-generator` |
| `area` | коефіцієнт площі: множить струми й ємність, ділить RS | `diode` |
| `bandwidth` | найвища частота в шумі (смуга) | `noise-generator` |
| `beta_forward` | BF: ідеальний прямий коефіцієнт підсилення струму | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor`, `tip31c`, `tip32c` |
| `beta_forward_spread` | розкид BF між екземплярами, частка; використовують кути допуску стенда | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor` |
| `beta_reverse` | BR: ідеальний зворотний коефіцієнт підсилення струму | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor`, `tip31c`, `tip32c` |
| `beta_temperature_exponent` | XTB: як коефіцієнти підсилення залежать від температури | `2n2222`, `2n2907`, `bipolar-transistor` |
| `bias_doubling` | вхідний струм подвоюється кожні стільки кельвінів: близько 10 К для входу на JFET; відсутній — гігакельвін | `operational-amplifier`, `tl072` |
| `body_transit_time` | час прольоту діода тіла: накопичений заряд, зворотне відновлення діода вільного ходу | `mos-transistor` |
| `breakdown_temperature_coefficient` | TCV: напруга пробою змінюється на цю частку на кельвін | `bzx55c5v1`, `diode` |
| `C_base_collector` | CJC: ємність переходу база–колектор при нульовому зміщенні | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor`, `tip31c`, `tip32c` |
| `C_base_emitter` | CJE: ємність переходу база–емітер при нульовому зміщенні | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor`, `tip31c`, `tip32c` |
| `C_bulk_drain` | CBD: ємність стік–підкладка (вихідна ємність, тіло діода) | `2n7000`, `bs250`, `irf9540`, `irfz44n`, `mos-transistor` |
| `C_gate_drain` | CGD: ємність затвор–стік | `2n5457`, `2n7000`, `bs250`, `irf9540`, `irfz44n`, `j201`, `junction-field-effect-transistor`, `mos-transistor` |
| `C_gate_source` | CGS: ємність затвор–витік | `2n5457`, `2n7000`, `bs250`, `irf9540`, `irfz44n`, `j201`, `junction-field-effect-transistor`, `mos-transistor` |
| `C_input_common` | ємність від кожного входу до від'ємної шини | `operational-amplifier` |
| `C_input_differential` | ємність між входами | `operational-amplifier` |
| `C_junction` | ємність кожного переходу | `bridge-rectifier` |
| `C_junction_zero_bias` | CJO: ємність переходу при нульовому зміщенні | `1n4007`, `1n4148`, `1n5819`, `bzx55c5v1`, `diode`, `led-red` |
| `C_output_energy` | Co(er), ефективна вихідна ємність за енергією | `ice3b0365` |
| `C_polarisation` | C1: разом із R1 — просідання протягом приблизно десяти секунд | `battery` |
| `capacitance` | ємність | `capacitor`, `ceramic-capacitor`, `electrolytic-capacitor` |
| `channel_length_modulation` | LAMBDA: модуляція довжини каналу, на вольт | `2n5457`, `j201`, `junction-field-effect-transistor`, `mos-transistor` |
| `closed` | натиснуто / замкнуто | `push-button`, `switch` |
| `common_mode_rejection` | КОСС (CMRR), як відношення; за замовчуванням відсутній — його похибка є зміщенням невідомого знака | `operational-amplifier` |
| `copper_temperature_coefficient` | мідь: опір обмотки зростає на 0,393 % на кельвін | `power-inductor`, `transformer` |
| `coupling` | K: коефіцієнт зв'язку; 1 — ідеальне осердя, менше — розсіювання | `transformer` |
| `current_transfer_ratio` | CTR: коефіцієнт передачі струму — струм колектора до струму світлодіода | `pc817` |
| `duty` | коефіцієнт заповнення: частка періоду у високому стані | `square-generator` |
| `E_gap` | EG: ширина забороненої зони, у вольтах | `1n4007`, `1n5819`, `bipolar-transistor`, `bridge-rectifier`, `diode`, `junction-field-effect-transistor`, `led-red`, `mos-transistor` |
| `edge_time` | тривалість фронту драйвера затвора | `ice3b0365` |
| `emission_coefficient` | NF / N: коефіцієнт емісії (неідеальності) прямого переходу | `1n4007`, `1n4148`, `1n5819`, `2n7000`, `bc547`, `bc557`, `bipolar-transistor`, `bridge-rectifier`, `bs250`, `bzx55c5v1`, `diode`, `irf9540`, `irfz44n`, `junction-field-effect-transistor`, `led-red`, `mos-transistor`, `pc817` |
| `emission_coefficient_reverse` | NR: зворотний коефіцієнт емісії | `bc547`, `bc557`, `bipolar-transistor` |
| `ESR_doubling` | ESR подвоюється на кожні стільки кельвінів холоднішання: електроліт густішає на холоді | `electrolytic-capacitor` |
| `forward_bias_coefficient` | FC: після FC·VJ бар'єрна ємність продовжується прямою лінією | `bipolar-transistor`, `diode`, `junction-field-effect-transistor`, `mos-transistor` |
| `frequency` | частота (для гармонійного генератора — основна; гармоніка k має частоту k·f) | `harmonic-generator`, `sine-generator`, `square-generator`, `staircase-generator` |
| `gain` | коефіцієнт передачі: вихідна напруга (або струм) на керуючу напругу | `voltage-controlled-voltage-source` |
| `gain_bandwidth` | частота одиничного підсилення | `lm358`, `lm393`, `ne5532`, `operational-amplifier`, `tl072` |
| `grading_base_collector` | MJC: її коефіцієнт плавності переходу | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor` |
| `grading_base_emitter` | MJE: її коефіцієнт плавності переходу | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor` |
| `grading_coefficient` | M: коефіцієнт плавності переходу | `1n4007`, `1n4148`, `1n5819`, `diode`, `mos-transistor` |
| `high` | рівень у стані «увімкнено» | `square-generator` |
| `hold` | час утримання: D стабільний після фронту такту | `74hc74` |
| `I_adjustment` | струм виводу регулювання, типовий | `lm317` |
| `I_breakdown` | струм лавинного пробою при V_breakdown | `1n4007`, `1n4148`, `1n5819`, `2n7000`, `bipolar-transistor`, `bridge-rectifier`, `bs250`, `bzx55c5v1`, `diode`, `irf9540`, `irfz44n`, `led-red`, `mos-transistor`, `tip31c`, `tip32c` |
| `I_charge` | струм заряду VCC від пускової комірки при VCC = 0 | `ice3b0365` |
| `I_collector_maximum` | IC: допустимий струм колектора; відсутній — мегаампер | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor`, `tip31c`, `tip32c` |
| `I_dark` | темновий струм колектора при вимкненому світлодіоді | `pc817` |
| `I_discharge` | струм насичення розрядного транзистора | `ne555` |
| `I_drain_maximum` | ID: допустимий струм стоку; відсутній — мегаампер | `2n7000`, `bs250`, `irf9540`, `irfz44n`, `mos-transistor` |
| `I_forward_maximum` | IF: допустимий струм в обидва боки; відсутній — мегаампер | `1n4007`, `1n4148`, `1n5819`, `bzx55c5v1`, `diode`, `led-red` |
| `I_gate_saturation` | перехід керуючий електрод–катод: струм насичення | `2n5060`, `bt136` |
| `I_holding` | IH: струм утримання — нижче нього тиристор закривається | `2n5060`, `bt136` |
| `I_input_bias` | вхідний струм зміщення: додатний витікає з входів (PNP-каскад), від'ємний втікає (NPN) | `lm358`, `lm393`, `ne5532`, `operational-amplifier`, `tl072` |
| `I_input_offset` | різниця вхідних струмів двох входів | `operational-amplifier` |
| `I_knee` | IKF: струм коліна високої інжекції (відсутній — гігаампер) | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor`, `diode`, `tip31c`, `tip32c` |
| `I_knee_reverse` | IKR: зворотний струм коліна високої інжекції | `bc547`, `bc557`, `bipolar-transistor` |
| `I_leakage_base_collector` | ISC: струм насичення витоку база–колектор | `bc547`, `bc557`, `bipolar-transistor` |
| `I_leakage_base_emitter` | ISE: струм насичення витоку база–емітер | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor` |
| `I_limit` | максимальний струм; на межі напруга просідає | `bench-supply` |
| `I_limit_negative` | максимальний струм від'ємної шини | `dual-supply` |
| `I_limit_positive` | максимальний струм додатної шини | `dual-supply` |
| `I_maximum` | допустимий вихідний струм | `lm317` |
| `I_minimum` | струм катода, нижче якого стабілізації немає | `tl431` |
| `I_noise` | спектральна густина струму шуму в кожен вхід, А/√Гц (білий шум) | `ne5532`, `operational-amplifier`, `tl072` |
| `I_output` | струм джерела | `current-source` |
| `I_output_limit` | обмеження вихідного струму (втікання й витікання) | `ne555` |
| `I_output_sink` | струм короткого замикання виходу на втікання | `lm358`, `lm393`, `ne5532`, `operational-amplifier`, `tl072` |
| `I_output_source` | струм короткого замикання виходу на витікання | `lm358`, `lm393`, `ne5532`, `operational-amplifier`, `tl072` |
| `I_quiescent` | струм споживання на одиницю (канал) | `lm358`, `lm393`, `ne5532`, `ne555`, `operational-amplifier`, `tl072` |
| `I_rated` | струм, за якого задано яскравість | `led-red` |
| `I_recombination` | ISR: струм насичення рекомбінації — надлишок прямої гілки на малих струмах | `diode` |
| `I_reference` | Iref: струм у вивід REF | `tl431` |
| `I_restart` | струм живлення в режимі автоперезапуску з неактивним затвором | `ice3b0365` |
| `I_saturation` | IS: струм насичення (транспортний для транзистора) | `1n4007`, `1n4148`, `1n5819`, `2n2222`, `2n2907`, `2n5457`, `2n7000`, `bc547`, `bc557`, `bipolar-transistor`, `bridge-rectifier`, `bs250`, `bzx55c5v1`, `diode`, `irf9540`, `irfz44n`, `j201`, `junction-field-effect-transistor`, `led-red`, `mos-transistor`, `pc817`, `power-inductor`, `tip31c`, `tip32c` |
| `I_start` | пусковий струм до активації контролера | `ice3b0365` |
| `I_supply` | струм живлення з активним затвором | `ice3b0365` |
| `I_threshold` | струм у вивід THR | `ne555` |
| `I_trigger` | струм із виводу TRIG | `ne555` |
| `inductance` | індуктивність | `inductor`, `power-inductor` |
| `internal_base_collector_fraction` | XCJC: частка CJC на внутрішній базі; решта — на виводі бази | `bc547`, `bc557`, `bipolar-transistor` |
| `L_equivalent_series` | ESL: еквівалентна послідовна індуктивність корпусу й монтажу | `ceramic-capacitor` |
| `L_primary` | індуктивність первинної обмотки: індуктивність намагнічування з боку первинки | `transformer` |
| `L_secondary` | індуктивність вторинної обмотки: коефіцієнт трансформації √(L1 / L2) | `transformer` |
| `latch_delay` | затримка засувки (STCP → Qn при 4,5 В) | `74hc595` |
| `leading_edge_blanking_time` | час гасіння переднього фронту | `ice3b0365` |
| `leakage_emission_base_collector` | NC: його коефіцієнт емісії | `bc547`, `bc557`, `bipolar-transistor` |
| `leakage_emission_base_emitter` | NE: його коефіцієнт емісії | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor` |
| `level` | СКЗ (RMS) напруги шуму | `noise-generator` |
| `light_temperature_coefficient` | світловий потік червоного світлодіода падає приблизно на 1 % на кельвін температури переходу | `led-red` |
| `low` | рівень у стані «вимкнено» | `square-generator` |
| `lowest` | нижня частота рожевого шуму; білий починається з нуля | `noise-generator` |
| `maximum_duty_cycle` | максимальний коефіцієнт заповнення | `ice3b0365` |
| `noise_corner` | нижче цієї частоти шум 1/f перевищує білий; 0 — відсутній | `ne5532`, `operational-amplifier`, `tl072` |
| `offset` | постійна складова, навколо якої коливається сигнал | `harmonic-generator`, `noise-generator`, `sine-generator`, `staircase-generator` |
| `open_loop_gain` | коефіцієнт підсилення без зворотного зв'язку на постійному струмі, як відношення | `lm358`, `lm393`, `ne5532`, `operational-amplifier`, `tl072` |
| `oscillator_frequency` | fOSC, фіксована частота генератора | `ice3b0365` |
| `p1` | фаза основної гармоніки | `harmonic-generator` |
| `p2` | фаза 2-ї гармоніки | `harmonic-generator` |
| `p3` | фаза 3-ї гармоніки | `harmonic-generator` |
| `p4` | фаза 4-ї гармоніки | `harmonic-generator` |
| `p5` | фаза 5-ї гармоніки | `harmonic-generator` |
| `p6` | фаза 6-ї гармоніки | `harmonic-generator` |
| `p7` | фаза 7-ї гармоніки | `harmonic-generator` |
| `p8` | фаза 8-ї гармоніки | `harmonic-generator` |
| `phase` | початкова фаза: де стоїть період у момент 0 | `sine-generator` |
| `phase_reversal` | 1: нижче нижньої межі вихід переходить у протилежний бік (інверсія фази) | `operational-amplifier`, `tl072` |
| `pink` | 0 — білий (рівний до смуги), 1 — рожевий (рівна потужність у кожній октаві) | `noise-generator` |
| `polarity` | 1 — NPN (n-канал), −1 — PNP (p-канал) | `bipolar-transistor`, `junction-field-effect-transistor`, `mos-transistor`, `n-jfet`, `n-mosfet`, `npn-transistor`, `p-jfet`, `p-mosfet`, `pnp-transistor` |
| `position` | положення повзунка | `potentiometer` |
| `power_rating` | номінальна потужність розсіювання | `1n4007`, `1n4148`, `1n5819`, `2n2222`, `2n2907`, `2n5060`, `2n7000`, `battery`, `bc547`, `bc557`, `bipolar-transistor`, `bt136`, `bzx55c5v1`, `ceramic-capacitor`, `diode`, `electrolytic-capacitor`, `irf9540`, `irfz44n`, `junction-field-effect-transistor`, `led-red`, `mos-transistor`, `pc817`, `potentiometer`, `power-inductor`, `resistor`, `tip31c`, `tip32c`, `tl431`, `transformer` |
| `propagation_delay` | затримка поширення (SHCP → Q7S при 4,5 В) | `74hc595`, `74hc74` |
| `R_base` | RB: опір бази при нульовому зміщенні | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor`, `tip31c`, `tip32c` |
| `R_base_minimum` | RBM: опір бази на великому струмі; 0 — дорівнює R_base | `bipolar-transistor` |
| `R_closed` | опір контакту в замкненому стані | `push-button`, `switch` |
| `R_collector` | RC: опір колектора | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor`, `tip31c`, `tip32c` |
| `R_discharge` | опір відкритого розрядного транзистора | `ne555` |
| `R_divider` | кожен із трьох резисторів дільника | `ne555` |
| `R_drain` | RD: послідовний опір стоку | `2n5457`, `2n7000`, `bs250`, `irf9540`, `irfz44n`, `j201`, `junction-field-effect-transistor`, `mos-transistor` |
| `R_drain_source_on` | опір стік–витік у відкритому стані | `ice3b0365` |
| `R_emitter` | RE: опір емітера | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor`, `tip31c`, `tip32c` |
| `R_equivalent_series` | ESR: еквівалентний послідовний опір | `ceramic-capacitor`, `electrolytic-capacitor` |
| `R_feedback` | підтягувальний резистор FB до VREF | `ice3b0365` |
| `R_gate` | RG: внутрішній опір затвора, що разом із Ciss задає швидкість перемикання | `mos-transistor` |
| `R_input` | диференційний вхідний опір | `duty-cycle-meter`, `frequency-meter`, `ne5532`, `operational-amplifier`, `tl072`, `voltmeter`, `wattmeter` |
| `R_internal` | R0: миттєве падіння під навантаженням (внутрішній опір) | `battery` |
| `R_leakage` | витік: даташитне I = 0,01 · C · V, як опір | `electrolytic-capacitor` |
| `R_off` | опір катод–анод у закритому стані | `2n5060`, `bt136`, `tl431` |
| `R_on` | нахил у відкритому стані (динамічний опір) | `2n5060`, `bt136` |
| `R_open` | опір витоку в розімкненому стані | `push-button`, `switch` |
| `R_output` | вихідний опір | `bench-supply`, `dual-supply`, `lm358`, `lm393`, `ne5532`, `ne555`, `operational-amplifier`, `tl072` |
| `R_polarisation` | R1: подальше просідання за час поляризації | `battery` |
| `R_primary` | опір первинної обмотки | `transformer` |
| `R_secondary` | опір вторинної обмотки | `transformer` |
| `R_series` | RS: послідовний опір (0 у SPICE — відсутній; тут міліом) | `1n4007`, `1n4148`, `1n5819`, `bridge-rectifier`, `bzx55c5v1`, `diode`, `led-red`, `pc817` |
| `R_shunt` | шунт, на якому вимірюється струм: його падіння на реалістичному стенді | `ammeter` |
| `R_soft_start` | підтягувальний резистор SoftS до VREF | `ice3b0365` |
| `R_source` | RS: послідовний опір витоку | `2n5457`, `2n7000`, `bs250`, `irf9540`, `irfz44n`, `j201`, `junction-field-effect-transistor`, `mos-transistor` |
| `R_winding` | DCR: опір обмотки | `power-inductor` |
| `R_wiper` | опір контакту повзунка | `potentiometer` |
| `recombination_emission_coefficient` | NR: коефіцієнт емісії струму рекомбінації | `diode` |
| `recovery` | як швидко каскад підсилення виходить із насичення | `lm358`, `operational-amplifier` |
| `reference_drift` | дрейф опорної напруги на кельвін, В/К | `tl431` |
| `resistance` | опір (повний, між крайніми виводами) | `potentiometer`, `resistor` |
| `resistance_temperature_coefficient` | TRS: RS змінюється на цю частку на кельвін | `diode` |
| `resistance_temperature_exponent` | не SPICE: RD і RS ростуть як (температура / T_nominal)^цей показник | `2n7000`, `bs250`, `irf9540`, `irfz44n`, `mos-transistor` |
| `saturation_temperature_coefficient` | феритове осердя насичується раніше в нагрітому стані, близько 0,4 % на кельвін | `power-inductor` |
| `saturation_temperature_exponent` | XTI: як струм насичення залежить від температури | `1n4007`, `1n5819`, `bipolar-transistor`, `bridge-rectifier`, `diode` |
| `second_pole` | другий полюс; 0 — відсутній | `ne5532`, `operational-amplifier`, `tl072` |
| `seed` | зерно генератора: інше зерно — інший шум того ж типу | `noise-generator` |
| `setup` | час встановлення: D стабільний до фронту такту | `74hc74` |
| `slew_rate` | швидкість наростання вихідної напруги | `lm358`, `lm393`, `ne5532`, `operational-amplifier`, `tl072` |
| `state_of_charge` | рівень заряду (0…1) | `battery` |
| `step` | висота однієї сходинки | `staircase-generator` |
| `steps` | кількість сходинок до повторення | `staircase-generator` |
| `supply_current_coefficient` | на яку частку на вольт зростає струм споживання понад V_supply_nominal | `operational-amplifier` |
| `supply_rejection` | КПНЖ (PSRR), як відношення; за замовчуванням відсутній — похибка є зміщенням невідомого знака | `operational-amplifier` |
| `switching_time` | час наростання й спаду виходу | `ne555` |
| `T_maximum` | максимальна температура: вище неї на реалістичному стенді елемент виходить з ладу | `1n4007`, `1n4148`, `1n5819`, `2n2222`, `2n2907`, `2n5060`, `2n7000`, `74hc595`, `74hc74`, `battery`, `bc547`, `bc557`, `bipolar-transistor`, `bridge-rectifier`, `bt136`, `bzx55c5v1`, `ceramic-capacitor`, `diode`, `electrolytic-capacitor`, `ice3b0365`, `irf9540`, `irfz44n`, `junction-field-effect-transistor`, `led-red`, `lm317`, `lm358`, `mos-transistor`, `ne555`, `operational-amplifier`, `pc817`, `potentiometer`, `power-inductor`, `resistor`, `tip31c`, `tip32c`, `tl431`, `transformer` |
| `T_nominal` | TNOM: температура, за якої виміряно наведені параметри | `2n2222`, `2n2907`, `2n5457`, `2n7000`, `bc547`, `bc557`, `bipolar-transistor`, `bridge-rectifier`, `bs250`, `ceramic-capacitor`, `diode`, `electrolytic-capacitor`, `irf9540`, `irfz44n`, `j201`, `junction-field-effect-transistor`, `led-red`, `mos-transistor`, `operational-amplifier`, `pc817`, `power-inductor`, `tip31c`, `tip32c`, `tl431`, `transformer` |
| `temperature_coefficient` | температурний коефіцієнт: зміна номіналу на кельвін від 25 °C; використовує температура стенда | `capacitor`, `resistor` |
| `temperature_curvature` | X7R: близько −15 % за 100 К в обидва боки від 25 °C, частка на кельвін² | `ceramic-capacitor` |
| `thermal_resistance` | тепловий опір: на скільки кельвінів вище стенда на кожен ват розсіяної потужності | `1n4007`, `1n4148`, `1n5819`, `2n2222`, `2n2907`, `2n5060`, `2n7000`, `74hc595`, `74hc74`, `battery`, `bc547`, `bc557`, `bipolar-transistor`, `bridge-rectifier`, `bt136`, `bzx55c5v1`, `ceramic-capacitor`, `diode`, `electrolytic-capacitor`, `ice3b0365`, `irf9540`, `irfz44n`, `junction-field-effect-transistor`, `led-red`, `lm317`, `lm358`, `mos-transistor`, `ne555`, `operational-amplifier`, `pc817`, `potentiometer`, `power-inductor`, `resistor`, `tip31c`, `tip32c`, `tl431`, `transformer` |
| `thermal_time_constant` | теплова стала часу: як швидко елемент прогрівається до цієї температури | `1n4007`, `1n4148`, `1n5819`, `2n2222`, `2n2907`, `2n5060`, `2n7000`, `74hc595`, `74hc74`, `battery`, `bc547`, `bc557`, `bipolar-transistor`, `bridge-rectifier`, `bt136`, `bzx55c5v1`, `ceramic-capacitor`, `diode`, `electrolytic-capacitor`, `ice3b0365`, `irf9540`, `irfz44n`, `junction-field-effect-transistor`, `led-red`, `lm317`, `lm358`, `mos-transistor`, `ne555`, `operational-amplifier`, `pc817`, `potentiometer`, `power-inductor`, `resistor`, `tip31c`, `tip32c`, `tl431`, `transformer` |
| `threshold` | рівень, відносно якого зчитується сигнал | `duty-cycle-meter`, `frequency-meter` |
| `threshold_temperature_coefficient` | VTOTC: зміна VTO на кельвін, В/К | `2n5457`, `j201`, `junction-field-effect-transistor` |
| `tolerance` | допуск: наскільки екземпляр може відрізнятися від номіналу, ±; використовують кути стенда | `capacitor`, `resistor` |
| `transconductance` | крутизна: BETA для JFET (IDSS = BETA · VTO²), KP для MOSFET | `2n5457`, `2n7000`, `bs250`, `irf9540`, `irfz44n`, `j201`, `junction-field-effect-transistor`, `lm317`, `mos-transistor`, `n-mosfet`, `p-mosfet`, `tl431`, `voltage-controlled-current-source` |
| `transconductance_spread` | розкид крутизни (BETA / KP) між екземплярами, частка | `2n5457`, `j201`, `junction-field-effect-transistor`, `mos-transistor` |
| `transfer_temperature_coefficient` | CTR падає на цю частку на кельвін понад 25 °C | `pc817` |
| `transit_time` | TT: час прольоту — накопичений заряд, що дає зворотне відновлення | `1n4007`, `1n4148`, `1n5819`, `diode` |
| `transit_time_bias_dependence` | XTF: наскільки TF зростає зі струмом | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor` |
| `transit_time_bias_voltage` | VTF: напруга VBC, що вдвічі зменшує це зростання | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor` |
| `transit_time_current` | ITF: струм, з якого починається зростання TF | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor` |
| `transit_time_forward` | TF: ідеальний прямий час прольоту — задає fT | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor`, `tip31c`, `tip32c` |
| `transit_time_reverse` | TR: ідеальний зворотний час прольоту — час розсмоктування насиченого ключа | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor` |
| `U_output` | напруга джерела | `voltage-source` |
| `V_breakdown` | напруга пробою (для транзистора — підтримувальна напруга колектор–емітер; на ній обмежується індуктивний викид) | `1n4007`, `1n4148`, `1n5819`, `2n2222`, `2n2907`, `2n7000`, `bc547`, `bc557`, `bipolar-transistor`, `bridge-rectifier`, `bs250`, `bzx55c5v1`, `diode`, `irf9540`, `irfz44n`, `led-red`, `mos-transistor`, `tip31c`, `tip32c` |
| `V_collector_emitter_maximum` | VCEO: допустима напруга колектор–емітер; відсутня — мегавольт | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor`, `tip31c`, `tip32c` |
| `V_common_mode_high` | входи працюють до стількох вольт нижче додатної шини | `lm358`, `lm393`, `ne5532`, `operational-amplifier`, `tl072` |
| `V_common_mode_low` | входи працюють до стількох вольт над від'ємною шиною (від'ємне — нижче неї) | `ne5532`, `operational-amplifier`, `tl072` |
| `V_current_sense_burst` | VCS2, межа під час активного пакетного режиму | `ice3b0365` |
| `V_current_sense_threshold` | поріг обмеження пікового струму, з урахуванням затримки | `ice3b0365` |
| `V_drain_source_maximum` | VDS: допустима напруга стік–витік; відсутня — мегавольт | `2n7000`, `bs250`, `irf9540`, `irfz44n`, `mos-transistor` |
| `V_dropout` | типове падіння вхід–вихід (мінімальна різниця) | `lm317` |
| `V_early` | VAF: пряма напруга Ерлі (відсутня — мегавольт) | `2n2222`, `2n2907`, `bc547`, `bc557`, `bipolar-transistor`, `pc817`, `tip31c`, `tip32c` |
| `V_early_reverse` | VAR: зворотна напруга Ерлі | `bc547`, `bc557`, `bipolar-transistor` |
| `V_empty` | ЕРС розрядженого | `battery` |
| `V_feedback_burst` | C5: рівень входу в активний пакетний режим | `ice3b0365` |
| `V_feedback_burst_off` | C6b: у пакетному режимі перемикання зупиняється нижче цього рівня | `ice3b0365` |
| `V_feedback_burst_on` | C6a: у пакетному режимі перемикання відновлюється вище цього рівня | `ice3b0365` |
| `V_feedback_minimum` | нижче цієї напруги FB заповнення нульове (Dmin) | `ice3b0365` |
| `V_feedback_overload` | C4: межа перевантаження та розімкненого контуру | `ice3b0365` |
| `V_full` | ЕРС повністю зарядженого | `battery` |
| `V_gate_source_maximum` | VGS: допустима напруга на оксиді затвора в обидва боки; відсутня — мегавольт | `2n7000`, `bs250`, `irf9540`, `irfz44n`, `mos-transistor` |
| `V_gate_trigger` | VGT: напруга керуючого електрода, що відкриває тиристор | `2n5060`, `bt136` |
| `V_half` | постійна напруга зміщення, за якої лишається половина ємності | `ceramic-capacitor` |
| `V_input_offset` | вхідна напруга зміщення нуля | `lm358`, `lm393`, `ne5532`, `operational-amplifier`, `tl072` |
| `V_junction` | VJ: контактна різниця потенціалів переходу | `1n4007`, `1n4148`, `1n5819`, `2n5457`, `diode`, `j201`, `junction-field-effect-transistor`, `mos-transistor` |
| `V_junction_base_collector` | VJC: її контактна різниця потенціалів | `bc547`, `bc557`, `bipolar-transistor` |
| `V_junction_base_emitter` | VJE: її контактна різниця потенціалів | `bc547`, `bc557`, `bipolar-transistor` |
| `V_negative` | від'ємна шина нижче спільного, за модулем | `dual-supply` |
| `V_noise` | спектральна густина вхідної напруги шуму, В/√Гц (білий шум) | `lm358`, `ne5532`, `operational-amplifier`, `tl072` |
| `V_offset_drift` | дрейф зміщення нуля на кельвін, В/К; за замовчуванням відсутній, знак — властивість екземпляра | `operational-amplifier` |
| `V_offset_spread` | можливе зміщення нуля екземпляра в будь-який бік; використовують кути допуску стенда | `lm358`, `lm393`, `ne5532`, `operational-amplifier`, `tl072` |
| `V_on` | коліно напруги у відкритому стані | `2n5060`, `bt136` |
| `V_output_headroom` | вихід не підходить до додатної шини ближче цієї напруги | `lm358`, `lm393`, `ne5532`, `operational-amplifier`, `tl072` |
| `V_output_high_drop` | високий рівень виходу нижче Vcc на цю величину | `ne555` |
| `V_output_low` | наскільки близько вихід підходить до від'ємної шини | `lm358`, `lm393`, `ne5532`, `ne555`, `operational-amplifier`, `tl072` |
| `V_positive` | додатна шина відносно спільного | `dual-supply` |
| `V_ramp` | максимальний рівень пилки: компенсація нахилу, додана до підсиленого CS | `ice3b0365` |
| `V_reference` | VREF, підігнана опорна напруга | `ice3b0365`, `lm317`, `tl431` |
| `V_reset` | поріг скидання | `ne555` |
| `V_reverse_maximum` | VRRM: допустима зворотна напруга; відсутня — мегавольт | `1n4007`, `1n4148`, `1n5819`, `diode`, `led-red` |
| `V_saturation` | напруга VCE, нижче якої насичений фототранзистор не опускається | `pc817` |
| `V_saturation_knee` | наскільки різко він виходить із насичення вище неї | `pc817` |
| `V_set` | напруга, яку тримає джерело, доки струм нижче межі | `bench-supply` |
| `V_soft_start_begin` | кінець м'якого старту: вище цієї напруги SoftS заповнення не обмежене | `ice3b0365` |
| `V_soft_start_clamp` | обмежена напруга SoftS у нормальному режимі | `ice3b0365` |
| `V_soft_start_window` | C3: кінець вікна гасіння, тут вирішується перевантаження чи пакетний режим | `ice3b0365` |
| `V_supply_maximum` | допустима напруга живлення; відсутня — мегавольт | `lm358`, `lm393`, `ne5532`, `operational-amplifier`, `tl072` |
| `V_supply_nominal` | напруга живлення, за якої задано зміщення нуля | `lm393`, `ne5532`, `operational-amplifier`, `tl072` |
| `V_supply_off` | поріг вимкнення VCC | `ice3b0365` |
| `V_supply_on` | поріг увімкнення VCC | `ice3b0365` |
| `V_threshold` | VTO: порогова напруга (для JFET — напруга відсічки, від'ємна для збідненого типу) | `2n5457`, `2n7000`, `bs250`, `irf9540`, `irfz44n`, `j201`, `junction-field-effect-transistor`, `mos-transistor`, `n-mosfet`, `p-mosfet` |
| `V_threshold_coefficient` | не SPICE: дрейф модуля порогу, В/К (від'ємний — гарячий відкривається раніше) | `2n7000`, `bs250`, `irf9540`, `irfz44n`, `mos-transistor` |
| `V_threshold_spread` | розкид порогу між екземплярами; використовують кути допуску стенда | `2n5457`, `2n7000`, `bs250`, `irf9540`, `irfz44n`, `j201`, `junction-field-effect-transistor`, `mos-transistor` |
| `voltage_gain` | коефіцієнт підсилення PWM-OP для напруги CS | `ice3b0365` |
| `voltage_rating` | номінальна напруга, яку розраховано витримувати | `capacitor`, `ceramic-capacitor`, `electrolytic-capacitor` |
