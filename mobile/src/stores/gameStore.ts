import { create } from 'zustand';

import { localizeServerError, translate } from '@/i18n';
import { gameSocket } from '@/services/socket.service';
import { CustomQuestion, HostQuestion, NewGameSettings, RoomSnapshot, RoundResult, VotingStarted } from '@/types/game';

interface GameState {
  code: string | null;
  isHost: boolean;
  snapshot: RoomSnapshot | null;
  hostQuestion: HostQuestion | null;
  votingOptions: VotingStarted | null;
  roundResult: RoundResult | null;
  mySubmittedAnswer: boolean;
  myVoted: boolean;
  myVoteOptionId: string | null;
  error: string | null;
  /** Set when the host removed this player from the room; screens leave the room and go home. */
  kicked: boolean;
  /** Host only: their own questions that haven't been asked yet. */
  customQuestions: CustomQuestion[];

  connectAndJoin: (token: string, code: string, isHost: boolean) => Promise<void>;
  startGame: () => void;
  submitTrap: (text: string) => void;
  submitAnswer: (text: string) => void;
  submitVote: (optionId: string) => void;
  nextRound: () => void;
  startNewGame: (settings: NewGameSettings) => void;
  kickPlayer: (playerId: string) => void;
  addCustomQuestion: (fact: string, answer: string) => boolean;
  removeCustomQuestion: (id: string) => void;
  leaveRoom: () => void;
  clearError: () => void;
}

export const useGameStore = create<GameState>((set, get) => ({
  code: null,
  isHost: false,
  snapshot: null,
  hostQuestion: null,
  votingOptions: null,
  roundResult: null,
  mySubmittedAnswer: false,
  myVoted: false,
  myVoteOptionId: null,
  error: null,
  kicked: false,
  customQuestions: [],

  connectAndJoin: async (token, code, isHost) => {
    await gameSocket.connect(token);
    set({ code, isHost, kicked: false, customQuestions: [] });
    gameSocket.joinRoom(code, {
      onSnapshot: (snapshot) => {
        const previous = get().snapshot;
        // A hostless room skips the TRAP phase and goes straight to ANSWERING, so
        // detect a fresh round by roundNumber rather than by status.
        // A new game in the same room may start again at a round number the last game also had.
        const newRound =
          !previous || previous.roundNumber !== snapshot.roundNumber || previous.status === 'FINISHED';
        set({
          snapshot,
          ...(newRound
            ? {
                votingOptions: null,
                roundResult: null,
                mySubmittedAnswer: false,
                myVoted: false,
                myVoteOptionId: null,
              }
            : {}),
          ...(snapshot.status === 'LOBBY' ? { hostQuestion: null } : {}),
        });
      },
      onVotingStarted: (voting) => set({ votingOptions: voting }),
      onRoundResult: (result) => set({ roundResult: result }),
      onHostQuestion: (question) => set({ hostQuestion: question }),
      onCustomQuestions: (questions) => set({ customQuestions: questions }),
      onKicked: () => {
        // Stop the socket right away so a reconnect doesn't try to rejoin; the screens
        // see `kicked` and navigate home.
        gameSocket.disconnect();
        set({ kicked: true, error: translate('room.kicked') });
      },
      onError: (error) => {
        // A rejected vote/answer (e.g. voting for your own answer) must roll back
        // the optimistic flag, or the UI is stuck on the "waiting" screen with no
        // way to actually submit a valid choice.
        const status = get().snapshot?.status;
        set({
          error: localizeServerError(error.message),
          ...(status === 'VOTING' ? { myVoted: false, myVoteOptionId: null } : {}),
          ...(status === 'ANSWERING' ? { mySubmittedAnswer: false } : {}),
        });
      },
    });
  },

  startGame: () => sendSafely(set, () => gameSocket.start(get().code!)),

  submitTrap: (text) => sendSafely(set, () => gameSocket.submitTrap(get().code!, text)),

  submitAnswer: (text) => {
    if (sendSafely(set, () => gameSocket.submitAnswer(get().code!, text))) {
      set({ mySubmittedAnswer: true });
    }
  },

  submitVote: (optionId) => {
    if (sendSafely(set, () => gameSocket.submitVote(get().code!, optionId))) {
      set({ myVoted: true, myVoteOptionId: optionId });
    }
  },

  nextRound: () => sendSafely(set, () => gameSocket.nextRound(get().code!)),

  startNewGame: (settings) => sendSafely(set, () => gameSocket.startNewGame(get().code!, settings)),

  kickPlayer: (playerId) => sendSafely(set, () => gameSocket.kickPlayer(get().code!, playerId)),

  addCustomQuestion: (fact, answer) => sendSafely(set, () => gameSocket.addCustomQuestion(get().code!, fact, answer)),

  removeCustomQuestion: (id) => sendSafely(set, () => gameSocket.removeCustomQuestion(get().code!, id)),

  leaveRoom: () => {
    gameSocket.disconnect();
    set({
      code: null,
      isHost: false,
      snapshot: null,
      hostQuestion: null,
      votingOptions: null,
      roundResult: null,
      mySubmittedAnswer: false,
      myVoted: false,
      myVoteOptionId: null,
      error: null,
      kicked: false,
      customQuestions: [],
    });
  },

  clearError: () => set({ error: null }),
}));

/**
 * Runs a socket send; if the connection is down (it is being re-established in the
 * background) shows the error instead of crashing, so the player can simply retry.
 */
function sendSafely(set: (partial: Partial<GameState>) => void, send: () => void): boolean {
  try {
    send();
    return true;
  } catch (e) {
    set({ error: e instanceof Error ? e.message : translate('errors.connectRoom') });
    return false;
  }
}
