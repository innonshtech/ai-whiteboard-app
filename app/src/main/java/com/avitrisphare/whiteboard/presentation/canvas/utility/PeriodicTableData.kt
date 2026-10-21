package com.avitrisphare.whiteboard.presentation.canvas.utility

object PeriodicTableData {
    val elements: List<ChemicalElement> = listOf(
        // Period 1
        ChemicalElement(1, "H", "Hydrogen", 1.008, 1, 1, ElementCategory.REACTIVE_NONMETAL, "Gas", "1s¹", "The most abundant chemical substance in the universe."),
        ChemicalElement(2, "He", "Helium", 4.0026, 1, 18, ElementCategory.NOBLE_GAS, "Gas", "1s²", "Colorless, odorless noble gas used in cryogenics and balloons."),
        // Period 2
        ChemicalElement(3, "Li", "Lithium", 6.94, 2, 1, ElementCategory.ALKALI_METAL, "Solid", "[He] 2s¹", "Soft, silvery-white alkali metal widely used in rechargeable batteries."),
        ChemicalElement(4, "Be", "Beryllium", 9.0122, 2, 2, ElementCategory.ALKALINE_EARTH, "Solid", "[He] 2s²", "Relatively rare metal in the universe, used in aerospace alloys."),
        ChemicalElement(5, "B", "Boron", 10.81, 2, 13, ElementCategory.METALLOID, "Solid", "[He] 2s² 2p¹", "Metalloid used in fiberglass and semiconductor doping."),
        ChemicalElement(6, "C", "Carbon", 12.011, 2, 14, ElementCategory.REACTIVE_NONMETAL, "Solid", "[He] 2s² 2p²", "Basis of all known organic life, forms diamond and graphite."),
        ChemicalElement(7, "N", "Nitrogen", 14.007, 2, 15, ElementCategory.REACTIVE_NONMETAL, "Gas", "[He] 2s² 2p³", "Makes up about 78% of Earth's atmosphere; essential for fertilizers."),
        ChemicalElement(8, "O", "Oxygen", 15.999, 2, 16, ElementCategory.REACTIVE_NONMETAL, "Gas", "[He] 2s² 2p⁴", "Crucial for aerobic respiration and combustion."),
        ChemicalElement(9, "F", "Fluorine", 18.998, 2, 17, ElementCategory.REACTIVE_NONMETAL, "Gas", "[He] 2s² 2p⁵", "Extremely reactive halogen; used in toothpaste and Teflon."),
        ChemicalElement(10, "Ne", "Neon", 20.180, 2, 18, ElementCategory.NOBLE_GAS, "Gas", "[He] 2s² 2p⁶", "Glows reddish-orange in high-voltage electrical discharge signs."),
        // Period 3
        ChemicalElement(11, "Na", "Sodium", 22.990, 3, 1, ElementCategory.ALKALI_METAL, "Solid", "[Ne] 3s¹", "Highly reactive metal that forms table salt (NaCl) with chlorine."),
        ChemicalElement(12, "Mg", "Magnesium", 24.305, 3, 2, ElementCategory.ALKALINE_EARTH, "Solid", "[Ne] 3s²", "Lightweight structural metal and key element in chlorophyll."),
        ChemicalElement(13, "Al", "Aluminium", 26.982, 3, 13, ElementCategory.POST_TRANSITION, "Solid", "[Ne] 3s² 3p¹", "Abundant lightweight, corrosion-resistant metal used globally."),
        ChemicalElement(14, "Si", "Silicon", 28.085, 3, 14, ElementCategory.METALLOID, "Solid", "[Ne] 3s² 3p²", "Cornerstone of modern electronics and computer chips."),
        ChemicalElement(15, "P", "Phosphorus", 30.974, 3, 15, ElementCategory.REACTIVE_NONMETAL, "Solid", "[Ne] 3s² 3p³", "Essential for life in DNA, RNA, ATP, and cell membranes."),
        ChemicalElement(16, "S", "Sulfur", 32.06, 3, 16, ElementCategory.REACTIVE_NONMETAL, "Solid", "[Ne] 3s² 3p⁴", "Yellow crystalline solid used in sulfuric acid and vulcanization."),
        ChemicalElement(17, "Cl", "Chlorine", 35.45, 3, 17, ElementCategory.REACTIVE_NONMETAL, "Gas", "[Ne] 3s² 3p⁵", "Disinfectant in swimming pools and component of table salt."),
        ChemicalElement(18, "Ar", "Argon", 39.948, 3, 18, ElementCategory.NOBLE_GAS, "Gas", "[Ne] 3s² 3p⁶", "Third-most abundant gas in Earth's atmosphere, used in welding shields."),
        // Period 4
        ChemicalElement(19, "K", "Potassium", 39.098, 4, 1, ElementCategory.ALKALI_METAL, "Solid", "[Ar] 4s¹", "Vital electrolyte for nervous signal transmission and plant fertilizer."),
        ChemicalElement(20, "Ca", "Calcium", 40.078, 4, 2, ElementCategory.ALKALINE_EARTH, "Solid", "[Ar] 4s²", "Essential for bone and tooth structure and cellular signaling."),
        ChemicalElement(21, "Sc", "Scandium", 44.956, 4, 3, ElementCategory.TRANSITION_METAL, "Solid", "[Ar] 3d¹ 4s²", "Used in lightweight aluminum-scandium alloys for aerospace."),
        ChemicalElement(22, "Ti", "Titanium", 47.867, 4, 4, ElementCategory.TRANSITION_METAL, "Solid", "[Ar] 3d² 4s²", "Strong, low-density, corrosion-resistant metal used in implants and jets."),
        ChemicalElement(23, "V", "Vanadium", 50.942, 4, 5, ElementCategory.TRANSITION_METAL, "Solid", "[Ar] 3d³ 4s²", "Steel additive that dramatically increases strength and shock resistance."),
        ChemicalElement(24, "Cr", "Chromium", 51.996, 4, 6, ElementCategory.TRANSITION_METAL, "Solid", "[Ar] 3d⁵ 4s¹", "Provides corrosion resistance and a mirror shine in stainless steel."),
        ChemicalElement(25, "Mn", "Manganese", 54.938, 4, 7, ElementCategory.TRANSITION_METAL, "Solid", "[Ar] 3d⁵ 4s²", "Essential in iron and steel production and aluminum beverage cans."),
        ChemicalElement(26, "Fe", "Iron", 55.845, 4, 8, ElementCategory.TRANSITION_METAL, "Solid", "[Ar] 3d⁶ 4s²", "Most used metal by mass; core constituent of hemoglobin."),
        ChemicalElement(27, "Co", "Cobalt", 58.933, 4, 9, ElementCategory.TRANSITION_METAL, "Solid", "[Ar] 3d⁷ 4s²", "Used in superalloys and lithium-ion battery cathodes."),
        ChemicalElement(28, "Ni", "Nickel", 58.693, 4, 10, ElementCategory.TRANSITION_METAL, "Solid", "[Ar] 3d⁸ 4s²", "Resistant to corrosion; used in stainless steel and coins."),
        ChemicalElement(29, "Cu", "Copper", 63.546, 4, 11, ElementCategory.TRANSITION_METAL, "Solid", "[Ar] 3d¹⁰ 4s¹", "Superb electrical conductor used in wiring, plumbing, and bronze."),
        ChemicalElement(30, "Zn", "Zinc", 65.38, 4, 12, ElementCategory.TRANSITION_METAL, "Solid", "[Ar] 3d¹⁰ 4s²", "Used for galvanizing iron/steel against rust and in brass alloys."),
        ChemicalElement(31, "Ga", "Gallium", 69.723, 4, 13, ElementCategory.POST_TRANSITION, "Solid", "[Ar] 3d¹⁰ 4s² 4p¹", "Melts in your hand (29.76 °C); key in GaAs semiconductor chips."),
        ChemicalElement(32, "Ge", "Germanium", 72.630, 4, 14, ElementCategory.METALLOID, "Solid", "[Ar] 3d¹⁰ 4s² 4p²", "Important semiconductor used in fiber optics and infrared optics."),
        ChemicalElement(33, "As", "Arsenic", 74.922, 4, 15, ElementCategory.METALLOID, "Solid", "[Ar] 3d¹⁰ 4s² 4p³", "Toxic metalloid historically famous as poison, used in GaAs."),
        ChemicalElement(34, "Se", "Selenium", 78.971, 4, 16, ElementCategory.REACTIVE_NONMETAL, "Solid", "[Ar] 3d¹⁰ 4s² 4p⁴", "Photovoltaic and photoconductive; essential trace nutrient."),
        ChemicalElement(35, "Br", "Bromine", 79.904, 4, 17, ElementCategory.REACTIVE_NONMETAL, "Liquid", "[Ar] 3d¹⁰ 4s² 4p⁵", "Dense reddish-brown liquid halogen used in flame retardants."),
        ChemicalElement(36, "Kr", "Krypton", 83.798, 4, 18, ElementCategory.NOBLE_GAS, "Gas", "[Ar] 3d¹⁰ 4s² 4p⁶", "Used in fluorescent lighting and photographic flashes."),
        // Period 5
        ChemicalElement(37, "Rb", "Rubidium", 85.468, 5, 1, ElementCategory.ALKALI_METAL, "Solid", "[Kr] 5s¹", "Highly reactive alkali metal used in atomic clocks."),
        ChemicalElement(38, "Sr", "Strontium", 87.62, 5, 2, ElementCategory.ALKALINE_EARTH, "Solid", "[Kr] 5s²", "Gives fireworks their brilliant crimson red color."),
        ChemicalElement(39, "Y", "Yttrium", 88.906, 5, 3, ElementCategory.TRANSITION_METAL, "Solid", "[Kr] 4d¹ 5s²", "Used in LEDs, phosphors, and YBCO high-temperature superconductors."),
        ChemicalElement(40, "Zr", "Zirconium", 91.224, 5, 4, ElementCategory.TRANSITION_METAL, "Solid", "[Kr] 4d² 5s²", "Resistant to corrosion; used in nuclear reactor fuel rod cladding."),
        ChemicalElement(41, "Nb", "Niobium", 92.906, 5, 5, ElementCategory.TRANSITION_METAL, "Solid", "[Kr] 4d⁴ 5s¹", "Used in superconducting magnets for MRI scanners and LHC."),
        ChemicalElement(42, "Mo", "Molybdenum", 95.95, 5, 6, ElementCategory.TRANSITION_METAL, "Solid", "[Kr] 4d⁵ 5s¹", "High melting point metal used in steel alloys and enzymes."),
        ChemicalElement(43, "Tc", "Technetium", 98.0, 5, 7, ElementCategory.TRANSITION_METAL, "Solid", "[Kr] 4d⁵ 5s²", "First artificially produced element; Tc-99m is key in medical imaging."),
        ChemicalElement(44, "Ru", "Ruthenium", 101.07, 5, 8, ElementCategory.TRANSITION_METAL, "Solid", "[Kr] 4d⁷ 5s¹", "Platinum group metal used in wear-resistant electrical contacts."),
        ChemicalElement(45, "Rh", "Rhodium", 102.91, 5, 9, ElementCategory.TRANSITION_METAL, "Solid", "[Kr] 4d⁸ 5s¹", "Extremely rare, precious metal used in automotive catalytic converters."),
        ChemicalElement(46, "Pd", "Palladium", 106.42, 5, 10, ElementCategory.TRANSITION_METAL, "Solid", "[Kr] 4d¹⁰", "Absorbs up to 900 times its own volume of hydrogen gas."),
        ChemicalElement(47, "Ag", "Silver", 107.87, 5, 11, ElementCategory.TRANSITION_METAL, "Solid", "[Kr] 4d¹⁰ 5s¹", "Highest electrical conductivity, thermal conductivity, and reflectivity."),
        ChemicalElement(48, "Cd", "Cadmium", 112.41, 5, 12, ElementCategory.TRANSITION_METAL, "Solid", "[Kr] 4d¹⁰ 5s²", "Toxic heavy metal historically used in Ni-Cd rechargeable batteries."),
        ChemicalElement(49, "In", "Indium", 114.82, 5, 13, ElementCategory.POST_TRANSITION, "Solid", "[Kr] 4d¹⁰ 5s² 5p¹", "Key component of Indium Tin Oxide (ITO) for touchscreens."),
        ChemicalElement(50, "Sn", "Tin", 118.71, 5, 14, ElementCategory.POST_TRANSITION, "Solid", "[Kr] 4d¹⁰ 5s² 5p²", "Malleable metal used in solder, pewter, and bronze alloys."),
        ChemicalElement(51, "Sb", "Antimony", 121.76, 5, 15, ElementCategory.METALLOID, "Solid", "[Kr] 4d¹⁰ 5s² 5p³", "Used as a flame retardant synergist and in lead-acid batteries."),
        ChemicalElement(52, "Te", "Tellurium", 127.60, 5, 16, ElementCategory.METALLOID, "Solid", "[Kr] 4d¹⁰ 5s² 5p⁴", "Used in CdTe solar panels and thermoelectric devices."),
        ChemicalElement(53, "I", "Iodine", 126.90, 5, 17, ElementCategory.REACTIVE_NONMETAL, "Solid", "[Kr] 4d¹⁰ 5s² 5p⁵", "Sublimes into a violet gas; vital for human thyroid hormone."),
        ChemicalElement(54, "Xe", "Xenon", 131.29, 5, 18, ElementCategory.NOBLE_GAS, "Gas", "[Kr] 4d¹⁰ 5s² 5p⁶", "Used in high-intensity car headlights and ion thrusters for satellites."),
        // Period 6
        ChemicalElement(55, "Cs", "Caesium", 132.91, 6, 1, ElementCategory.ALKALI_METAL, "Solid", "[Xe] 6s¹", "Hyperfine transition of Cs-133 defines the international standard SI second."),
        ChemicalElement(56, "Ba", "Barium", 137.33, 6, 2, ElementCategory.ALKALINE_EARTH, "Solid", "[Xe] 6s²", "Used in drilling fluid for oil wells and gastrointestinal X-ray meals."),
        // Lanthanides (57-71)
        ChemicalElement(57, "La", "Lanthanum", 138.91, 6, null, ElementCategory.LANTHANIDE, "Solid", "[Xe] 5d¹ 6s²", "First of the rare earth lanthanides; used in camera lenses and hybrids."),
        ChemicalElement(58, "Ce", "Cerium", 140.12, 6, null, ElementCategory.LANTHANIDE, "Solid", "[Xe] 4f¹ 5d¹ 6s²", "Most abundant rare earth element; used in catalytic converters and lighters."),
        ChemicalElement(59, "Pr", "Praseodymium", 140.91, 6, null, ElementCategory.LANTHANIDE, "Solid", "[Xe] 4f³ 6s²", "Used in high-strength neodymium-praseodymium supermagnets."),
        ChemicalElement(60, "Nd", "Neodymium", 144.24, 6, null, ElementCategory.LANTHANIDE, "Solid", "[Xe] 4f⁴ 6s²", "Makes the strongest commercial permanent magnets (NdFeB)."),
        ChemicalElement(61, "Pm", "Promethium", 145.0, 6, null, ElementCategory.LANTHANIDE, "Solid", "[Xe] 4f⁵ 6s²", "Radioactive rare earth; used in luminous paint and atomic batteries."),
        ChemicalElement(62, "Sm", "Samarium", 150.36, 6, null, ElementCategory.LANTHANIDE, "Solid", "[Xe] 4f⁶ 6s²", "Used in Samarium-Cobalt magnets resistant to high demagnetizing temperatures."),
        ChemicalElement(63, "Eu", "Europium", 151.96, 6, null, ElementCategory.LANTHANIDE, "Solid", "[Xe] 4f⁷ 6s²", "Red phosphor in television screens and anti-counterfeiting euro banknotes."),
        ChemicalElement(64, "Gd", "Gadolinium", 157.25, 6, null, ElementCategory.LANTHANIDE, "Solid", "[Xe] 4f⁷ 5d¹ 6s²", "Paramagnetic at room temperature; used as an MRI contrast agent."),
        ChemicalElement(65, "Tb", "Terbium", 158.93, 6, null, ElementCategory.LANTHANIDE, "Solid", "[Xe] 4f⁹ 6s²", "Green phosphor in lighting and component of Terfenol-D magnetostrictive alloy."),
        ChemicalElement(66, "Dy", "Dysprosium", 162.50, 6, null, ElementCategory.LANTHANIDE, "Solid", "[Xe] 4f¹⁰ 6s²", "High thermal neutron absorption; improves EV motor magnet thermal stability."),
        ChemicalElement(67, "Ho", "Holmium", 164.93, 6, null, ElementCategory.LANTHANIDE, "Solid", "[Xe] 4f¹¹ 6s²", "Has the highest magnetic permeability of any chemical element."),
        ChemicalElement(68, "Er", "Erbium", 167.26, 6, null, ElementCategory.LANTHANIDE, "Solid", "[Xe] 4f¹² 6s²", "Used in erbium-doped fiber amplifiers (EDFAs) for the global internet."),
        ChemicalElement(69, "Tm", "Thulium", 168.93, 6, null, ElementCategory.LANTHANIDE, "Solid", "[Xe] 4f¹³ 6s²", "Second least abundant lanthanide; used in portable X-ray machines."),
        ChemicalElement(70, "Yb", "Ytterbium", 173.05, 6, null, ElementCategory.LANTHANIDE, "Solid", "[Xe] 4f¹⁴ 6s²", "Used in fiber laser amplifiers and next-generation atomic optical clocks."),
        ChemicalElement(71, "Lu", "Lutetium", 174.97, 6, null, ElementCategory.LANTHANIDE, "Solid", "[Xe] 4f¹⁴ 5d¹ 6s²", "Densest and hardest lanthanide; used in PET cancer scanners."),
        // Period 6 continued
        ChemicalElement(72, "Hf", "Hafnium", 178.49, 6, 4, ElementCategory.TRANSITION_METAL, "Solid", "[Xe] 4f¹⁴ 5d² 6s²", "Excellent neutron absorber for nuclear submarine reactor control rods."),
        ChemicalElement(73, "Ta", "Tantalum", 180.95, 6, 5, ElementCategory.TRANSITION_METAL, "Solid", "[Xe] 4f¹⁴ 5d³ 6s²", "Highly corrosion-resistant; used in compact tantalum capacitors for phones."),
        ChemicalElement(74, "W", "Tungsten", 183.84, 6, 6, ElementCategory.TRANSITION_METAL, "Solid", "[Xe] 4f¹⁴ 5d⁴ 6s²", "Highest melting point of all metals (3422 °C); used in incandescent filaments."),
        ChemicalElement(75, "Re", "Rhenium", 186.21, 6, 7, ElementCategory.TRANSITION_METAL, "Solid", "[Xe] 4f¹⁴ 5d⁵ 6s²", "Used in nickel-based superalloys for jet engine combustion chambers."),
        ChemicalElement(76, "Os", "Osmium", 190.23, 6, 8, ElementCategory.TRANSITION_METAL, "Solid", "[Xe] 4f¹⁴ 5d⁶ 6s²", "Densest naturally occurring element (22.59 g/cm³)."),
        ChemicalElement(77, "Ir", "Iridium", 192.22, 6, 9, ElementCategory.TRANSITION_METAL, "Solid", "[Xe] 4f¹⁴ 5d⁷ 6s²", "Most corrosion-resistant metal known; marked the dinosaur asteroid layer."),
        ChemicalElement(78, "Pt", "Platinum", 195.08, 6, 10, ElementCategory.TRANSITION_METAL, "Solid", "[Xe] 4f¹⁴ 5d⁹ 6s¹", "Dense, malleable, precious catalytic metal used in jewelry and chemistry."),
        ChemicalElement(79, "Au", "Gold", 196.97, 6, 11, ElementCategory.TRANSITION_METAL, "Solid", "[Xe] 4f¹⁴ 5d¹⁰ 6s¹", "Precious noble metal prized since antiquity; does not tarnish."),
        ChemicalElement(80, "Hg", "Mercury", 200.59, 6, 12, ElementCategory.TRANSITION_METAL, "Liquid", "[Xe] 4f¹⁴ 5d¹⁰ 6s²", "Only metal that is liquid at standard temperature and pressure."),
        ChemicalElement(81, "Tl", "Thallium", 204.38, 6, 13, ElementCategory.POST_TRANSITION, "Solid", "[Xe] 4f¹⁴ 5d¹⁰ 6s² 6p¹", "Soft, highly toxic post-transition metal historically called 'poisoner's poison'."),
        ChemicalElement(82, "Pb", "Lead", 207.2, 6, 14, ElementCategory.POST_TRANSITION, "Solid", "[Xe] 4f¹⁴ 5d¹⁰ 6s² 6p²", "Dense, soft metal used for radiation shielding and car batteries."),
        ChemicalElement(83, "Bi", "Bismuth", 208.98, 6, 15, ElementCategory.POST_TRANSITION, "Solid", "[Xe] 4f¹⁴ 5d¹⁰ 6s² 6p³", "Displays colorful iridescent oxide crystals; used in Pepto-Bismol."),
        ChemicalElement(84, "Po", "Polonium", 209.0, 6, 16, ElementCategory.POST_TRANSITION, "Solid", "[Xe] 4f¹⁴ 5d¹⁰ 6s² 6p⁴", "Intensely radioactive metal discovered by Marie and Pierre Curie."),
        ChemicalElement(85, "At", "Astatine", 210.0, 6, 17, ElementCategory.METALLOID, "Solid", "[Xe] 4f¹⁴ 5d¹⁰ 6s² 6p⁵", "Rarest naturally occurring element in Earth's crust (less than 1 gram)."),
        ChemicalElement(86, "Rn", "Radon", 222.0, 6, 18, ElementCategory.NOBLE_GAS, "Gas", "[Xe] 4f¹⁴ 5d¹⁰ 6s² 6p⁶", "Radioactive noble gas released from decay of uranium in soils/rocks."),
        // Period 7
        ChemicalElement(87, "Fr", "Francium", 223.0, 7, 1, ElementCategory.ALKALI_METAL, "Solid", "[Rn] 7s¹", "Second rarest element in Earth's crust; extremely radioactive."),
        ChemicalElement(88, "Ra", "Radium", 226.0, 7, 2, ElementCategory.ALKALINE_EARTH, "Solid", "[Rn] 7s²", "Historically used in glow-in-the-dark watch dials before radiation hazards were known."),
        // Actinides (89-103)
        ChemicalElement(89, "Ac", "Actinium", 227.0, 7, null, ElementCategory.ACTINIDE, "Solid", "[Rn] 6d¹ 7s²", "Glows with an eerie blue light due to its intense radioactivity."),
        ChemicalElement(90, "Th", "Thorium", 232.04, 7, null, ElementCategory.ACTINIDE, "Solid", "[Rn] 6d² 7s²", "Weakly radioactive metal proposed as an alternative, safer nuclear fuel."),
        ChemicalElement(91, "Pa", "Protactinium", 231.04, 7, null, ElementCategory.ACTINIDE, "Solid", "[Rn] 5f² 6d¹ 7s²", "Dense, radioactive actinide intermediate in uranium decay."),
        ChemicalElement(92, "U", "Uranium", 238.03, 7, null, ElementCategory.ACTINIDE, "Solid", "[Rn] 5f³ 6d¹ 7s²", "Primary fuel for commercial nuclear power reactors."),
        ChemicalElement(93, "Np", "Neptunium", 237.0, 7, null, ElementCategory.ACTINIDE, "Solid", "[Rn] 5f⁴ 6d¹ 7s²", "First transuranic element synthesized; named after the planet Neptune."),
        ChemicalElement(94, "Pu", "Plutonium", 244.0, 7, null, ElementCategory.ACTINIDE, "Solid", "[Rn] 5f⁶ 7s²", "Fissile actinide used in nuclear weapons and space probe RTG generators."),
        ChemicalElement(95, "Am", "Americium", 243.0, 7, null, ElementCategory.ACTINIDE, "Solid", "[Rn] 5f⁷ 7s²", "Used as the alpha radiation source in household ionization smoke detectors."),
        ChemicalElement(96, "Cm", "Curium", 247.0, 7, null, ElementCategory.ACTINIDE, "Solid", "[Rn] 5f⁷ 6d¹ 7s²", "Named in honor of Marie and Pierre Curie; used in X-ray alpha spectrometers on Mars."),
        ChemicalElement(97, "Bk", "Berkelium", 247.0, 7, null, ElementCategory.ACTINIDE, "Solid", "[Rn] 5f⁹ 7s²", "Synthesized at UC Berkeley in 1949 by bombarding americium with alpha particles."),
        ChemicalElement(98, "Cf", "Californium", 251.0, 7, null, ElementCategory.ACTINIDE, "Solid", "[Rn] 5f¹⁰ 7s²", "Powerful neutron emitter used in oil well logging and reactor startup."),
        ChemicalElement(99, "Es", "Einsteinium", 252.0, 7, null, ElementCategory.ACTINIDE, "Solid", "[Rn] 5f¹¹ 7s²", "Discovered in the debris of the first hydrogen bomb test ('Ivy Mike')."),
        ChemicalElement(100, "Fm", "Fermium", 257.0, 7, null, ElementCategory.ACTINIDE, "Solid", "[Rn] 5f¹² 7s²", "Heaviest element that can be prepared by neutron bombardment of lighter elements."),
        ChemicalElement(101, "Md", "Mendelevium", 258.0, 7, null, ElementCategory.ACTINIDE, "Solid", "[Rn] 5f¹³ 7s²", "Named after Dmitri Mendeleev, father of the Periodic Table."),
        ChemicalElement(102, "No", "Nobelium", 259.0, 7, null, ElementCategory.ACTINIDE, "Solid", "[Rn] 5f¹⁴ 7s²", "Named after Alfred Nobel, Swedish chemist and founder of the Nobel Prizes."),
        ChemicalElement(103, "Lr", "Lawrencium", 266.0, 7, null, ElementCategory.ACTINIDE, "Solid", "[Rn] 5f¹⁴ 7s² 7p¹", "Final actinide; named after Ernest Lawrence, inventor of the cyclotron."),
        // Period 7 continued (Transactinides 104-118)
        ChemicalElement(104, "Rf", "Rutherfordium", 267.0, 7, 4, ElementCategory.TRANSITION_METAL, "Solid", "[Rn] 5f¹⁴ 6d² 7s²", "Superheavy synthetic element named after Ernest Rutherford."),
        ChemicalElement(105, "Db", "Dubnium", 268.0, 7, 5, ElementCategory.TRANSITION_METAL, "Solid", "[Rn] 5f¹⁴ 6d³ 7s²", "Named after the Russian nuclear research town of Dubna."),
        ChemicalElement(106, "Sg", "Seaborgium", 269.0, 7, 6, ElementCategory.TRANSITION_METAL, "Solid", "[Rn] 5f¹⁴ 6d⁴ 7s²", "Named after Glenn T. Seaborg, pioneer of transuranium synthesis."),
        ChemicalElement(107, "Bh", "Bohrium", 270.0, 7, 7, ElementCategory.TRANSITION_METAL, "Solid", "[Rn] 5f¹⁴ 6d⁵ 7s²", "Named in honor of Danish quantum physicist Niels Bohr."),
        ChemicalElement(108, "Hs", "Hassium", 277.0, 7, 8, ElementCategory.TRANSITION_METAL, "Solid", "[Rn] 5f¹⁴ 6d⁶ 7s²", "Named after the German state of Hesse, where it was first synthesized at GSI."),
        ChemicalElement(109, "Mt", "Meitnerium", 278.0, 7, 9, ElementCategory.UNKNOWN, "Solid", "[Rn] 5f¹⁴ 6d⁷ 7s²", "Named in honor of Austrian-Swedish physicist Lise Meitner."),
        ChemicalElement(110, "Ds", "Darmstadtium", 281.0, 7, 10, ElementCategory.UNKNOWN, "Solid", "[Rn] 5f¹⁴ 6d⁸ 7s²", "Named after the German city of Darmstadt, home of GSI."),
        ChemicalElement(111, "Rg", "Roentgenium", 282.0, 7, 11, ElementCategory.UNKNOWN, "Solid", "[Rn] 5f¹⁴ 6d⁹ 7s²", "Named in honor of Wilhelm Röntgen, discoverer of X-rays."),
        ChemicalElement(112, "Cn", "Copernicium", 285.0, 7, 12, ElementCategory.TRANSITION_METAL, "Gas", "[Rn] 5f¹⁴ 6d¹⁰ 7s²", "Named after astronomer Nicolaus Copernicus; volatile metal."),
        ChemicalElement(113, "Nh", "Nihonium", 286.0, 7, 13, ElementCategory.UNKNOWN, "Solid", "[Rn] 5f¹⁴ 6d¹⁰ 7s² 7p¹", "First element discovered in Asia (at RIKEN in Japan; 'Nihon' = Japan)."),
        ChemicalElement(114, "Fl", "Flerovium", 289.0, 7, 14, ElementCategory.POST_TRANSITION, "Solid", "[Rn] 5f¹⁴ 6d¹⁰ 7s² 7p²", "Superheavy element named after Russian physicist Georgy Flyorov."),
        ChemicalElement(115, "Mc", "Moscovium", 290.0, 7, 15, ElementCategory.UNKNOWN, "Solid", "[Rn] 5f¹⁴ 6d¹⁰ 7s² 7p³", "Superheavy element named in honor of the Moscow Oblast."),
        ChemicalElement(116, "Lv", "Livermorium", 293.0, 7, 16, ElementCategory.UNKNOWN, "Solid", "[Rn] 5f¹⁴ 6d¹⁰ 7s² 7p⁴", "Named after the Lawrence Livermore National Laboratory in California."),
        ChemicalElement(117, "Ts", "Tennessine", 294.0, 7, 17, ElementCategory.UNKNOWN, "Solid", "[Rn] 5f¹⁴ 6d¹⁰ 7s² 7p⁵", "Named after the US state of Tennessee (Oak Ridge, Vanderbilt, UT)."),
        ChemicalElement(118, "Og", "Oganesson", 294.0, 7, 18, ElementCategory.UNKNOWN, "Solid", "[Rn] 5f¹⁴ 6d¹⁰ 7s² 7p⁶", "Heaviest element known to science; named after nuclear physicist Yuri Oganessian.")
    )

    fun search(query: String, category: ElementCategory?): List<ChemicalElement> {
        val q = query.trim().lowercase()
        return elements.filter { elem ->
            val matchesQuery = q.isEmpty() ||
                elem.symbol.lowercase().contains(q) ||
                elem.name.lowercase().contains(q) ||
                elem.atomicNumber.toString() == q
            val matchesCategory = category == null || elem.category == category
            matchesQuery && matchesCategory
        }
    }
}
