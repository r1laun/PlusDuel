/**
 * Chess-style Elo rating for ranked (Quick play) matches.
 * Provisional players (first PROVISIONAL_GAMES) move faster with a higher K.
 */

export const START_RATING = 1000;
export const PROVISIONAL_GAMES = 10;
export const K_PROVISIONAL = 40;
export const K_REGULAR = 32;

export type MatchScore = 1 | 0.5 | 0;

export function kFor(gamesPlayed: number): number {
  return gamesPlayed < PROVISIONAL_GAMES ? K_PROVISIONAL : K_REGULAR;
}

function expectedScore(ra: number, rb: number): number {
  return 1 / (1 + Math.pow(10, (rb - ra) / 400));
}

/** New rating for player A after scoring `scoreA` against player B. */
export function eloUpdate(ra: number, rb: number, scoreA: MatchScore, k: number): number {
  return Math.round(ra + k * (scoreA - expectedScore(ra, rb)));
}

export interface TitleBand {
  min: number;
  title: string;
}

export const TITLE_BANDS: TitleBand[] = [
  { min: 1800, title: 'Grandmaster' },
  { min: 1600, title: 'Master' },
  { min: 1400, title: 'Diamond' },
  { min: 1200, title: 'Platinum' },
  { min: 1000, title: 'Gold' },
  { min: 800, title: 'Club' },
  { min: -Infinity, title: 'Novice' },
];

export function titleFor(rating: number): string {
  return TITLE_BANDS.find((b) => rating >= b.min)?.title ?? 'Novice';
}

export interface LeaderboardEntry {
  playerId: string;
  name: string;
  rating: number;
  title: string;
  games: number;
  wins: number;
  losses: number;
  draws: number;
}
