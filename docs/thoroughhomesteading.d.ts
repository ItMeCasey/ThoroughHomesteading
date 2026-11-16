// Typed API of Thorough Homesteading 2026.11.1 for ModJS and KubeJS scripts.
// Matches HomesteadingApi, HomesteadingEvents and ScriptEvents.Event in the mod.

/** The five internal event names. */
type HomesteadingEventType = 'hearth_lit' | 'hearth_extinguished' | 'settler_arrived' | 'settling_blocked' | 'before_settle'

/** The tier ids. */
type HearthTierId = 'stone' | 'iron' | 'golden'

/** The reasons a home can be blocked (an empty string in a report means nothing blocks). */
type BlockedReason = 'beds' | 'workstations' | 'light' | 'food' | 'population'

/** The event a listener receives. Read values with get, string and number. */
interface HomesteadingEvent {
  /** The internal event name. */
  type(): HomesteadingEventType
  /** The raw value of a key, or null. */
  get(key: string): any
  /** The value as text, or an empty string. */
  string(key: string): string
  /** The value as a number, or 0. */
  number(key: string): number
  /** The keys this event carries. */
  keys(): string[]
  /** Stops the arrival. Only before_settle honors it. */
  cancel(): void
  cancelled(): boolean
}

/** Keys: x, y, z, dimension, tier. */
interface HearthStateEvent extends HomesteadingEvent {
  type(): 'hearth_lit' | 'hearth_extinguished'
}

/** Keys: settler, x, y, z, dimension, tier, profession, owner. */
interface SettlerArrivedEvent extends HomesteadingEvent {
  type(): 'settler_arrived'
}

/** Keys: x, y, z, dimension, tier, reason. */
interface SettlingBlockedEvent extends HomesteadingEvent {
  type(): 'settling_blocked'
}

/** Keys: x, y, z, dimension, tier, profession. */
interface BeforeSettleEvent extends HomesteadingEvent {
  type(): 'before_settle'
}

/** The HomesteadingEvents global. */
interface HomesteadingEventsApi {
  /** A Hearth was lit: x, y, z, dimension, tier. */
  hearthLit(listener: (event: HearthStateEvent) => void): void
  /** A Hearth went out: x, y, z, dimension, tier. */
  hearthExtinguished(listener: (event: HearthStateEvent) => void): void
  /** A settler walked in: settler, x, y, z, dimension, tier, profession, owner. */
  settlerArrived(listener: (event: SettlerArrivedEvent) => void): void
  /** A scan found the home incomplete; fires when the reason changes: x, y, z, dimension, tier, reason. */
  settlingBlocked(listener: (event: SettlingBlockedEvent) => void): void
  /** Before an arrival; call event.cancel() to stop it: x, y, z, dimension, tier, profession. */
  beforeSettle(listener: (event: BeforeSettleEvent) => void): void
}

/** One settling recipe as Homesteading.recipes() returns it. */
interface SettlingRecipeInfo {
  /** Workstation block id, such as minecraft:composter. */
  workstation: string
  /** Profession id, such as minecraft:farmer. */
  profession: string
  /** Lowest Hearth tier that may use it: 1 Stone, 2 Iron, 3 Golden. */
  minTier: number
  /** Nutrition of food it needs (the food_required setting when the recipe names none). */
  food: number
  /** Extra items spent with the food: item id to count. */
  extra: { [item: string]: number }
}

/** What Homesteading.report returns. */
interface HearthReport {
  tier: HearthTierId
  lit: boolean
  /** Owner UUID, or an empty string. */
  owner: string
  settlers: number
  population: number
  /** Free beds found. */
  beds: number
  /** Free beds with enough light at the head. */
  litBeds: number
  /** Best light level at a free bed. */
  light: number
  lightMin: number
  /** Free workstations with a settling recipe this tier may use. */
  workstations: number
  food: number
  foodRequired: number
  /** Ticks of the countdown done. */
  progress: number
  /** Ticks the countdown needs. */
  settleTicks: number
  /** The blocking reason, or an empty string. */
  blocked: BlockedReason | ''
  /** Profession of the workstation the next settler would take, or an empty string. */
  profession: string
  /** UUIDs of the settlers the Hearth counts. */
  settlerIds: string[]
}

/** The Homesteading global. */
interface HomesteadingApi {
  /** ["stone", "iron", "golden"]. */
  tiers(): HearthTierId[]
  /** The internal event names. */
  events(): HomesteadingEventType[]
  /** Every settling recipe the running server has; empty before a server runs. */
  recipes(): SettlingRecipeInfo[]
  /** The report of the Hearth at a position (overworld first, then every dimension), or null. */
  report(x: number, y: number, z: number): HearthReport | null
  /** The report of the Hearth in one dimension, such as "minecraft:overworld", or null. */
  report(dimension: string, x: number, y: number, z: number): HearthReport | null
  /** UUIDs of the settlers the Hearth has brought and still counts; empty when there is no Hearth. */
  settlersOf(x: number, y: number, z: number): string[]
  /**
   * Skips the countdown and brings a settler if the Hearth is lit and the home check passes.
   * profession may be empty to take the nearest workstation. Returns false when nothing happened.
   */
  settleNow(x: number, y: number, z: number, profession: string): boolean
  /** Every setting key, sorted. */
  settingKeys(): string[]
  /** A setting as text, such as "radius.stone". Unknown keys throw. */
  setting(key: string): string
  /** Changes a setting until the game closes (the config file is not touched). Unknown keys throw. */
  setSetting(key: string, value: string): void
}

declare const Homesteading: HomesteadingApi
declare const HomesteadingEvents: HomesteadingEventsApi
