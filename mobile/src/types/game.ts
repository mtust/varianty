export type RoomStatus = 'LOBBY' | 'TRAP' | 'ANSWERING' | 'VOTING' | 'RESULTS' | 'FINISHED';

export interface PlayerScore {
  id: string;
  displayName: string;
  score: number;
  connected: boolean;
}

export interface RoomSnapshot {
  code: string;
  status: RoomStatus;
  roundNumber: number;
  totalRounds: number;
  hostDisplayName: string;
  hostless: boolean;
  fact: string | null;
  players: PlayerScore[];
  answeredCount: number;
  votedCount: number;
  secondsRemaining: number;
}

export interface HostQuestion {
  fact: string;
  correctAnswer: string;
}

export interface AnswerOption {
  id: string;
  text: string;
}

export interface VotingStarted {
  options: AnswerOption[];
}

export interface RevealedOption {
  id: string;
  text: string;
  authorLabel: string;
  correct: boolean;
  hostTrap: boolean;
  votedByDisplayNames: string[];
}

export interface RoundResult {
  fact: string;
  options: RevealedOption[];
  scoreDeltas: Record<string, number>;
  scoreboard: PlayerScore[];
  gameFinished: boolean;
}

export interface ServerError {
  message: string;
}

export interface GameHistoryEntry {
  id: string;
  roomCode: string;
  displayName: string;
  score: number;
  placement: number;
  playerCount: number;
  playedAt: string;
}
