import { create } from 'zustand';

import { gameSocket } from '@/services/socket.service';
import { HostQuestion, RoomSnapshot, RoundResult, VotingStarted } from '@/types/game';

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

  connectAndJoin: (token: string, code: string, isHost: boolean) => Promise<void>;
  startGame: () => void;
  submitTrap: (text: string) => void;
  submitAnswer: (text: string) => void;
  submitVote: (optionId: string) => void;
  nextRound: () => void;
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

  connectAndJoin: async (token, code, isHost) => {
    await gameSocket.connect(token);
    set({ code, isHost });
    gameSocket.joinRoom(code, {
      onSnapshot: (snapshot) => {
        const previous = get().snapshot;
        // A hostless room skips the TRAP phase and goes straight to ANSWERING, so
        // detect a fresh round by roundNumber rather than by status.
        const newRound = !previous || previous.roundNumber !== snapshot.roundNumber;
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
      onError: (error) => {
        // A rejected vote/answer (e.g. voting for your own answer) must roll back
        // the optimistic flag, or the UI is stuck on the "waiting" screen with no
        // way to actually submit a valid choice.
        const status = get().snapshot?.status;
        set({
          error: error.message,
          ...(status === 'VOTING' ? { myVoted: false, myVoteOptionId: null } : {}),
          ...(status === 'ANSWERING' ? { mySubmittedAnswer: false } : {}),
        });
      },
    });
  },

  startGame: () => gameSocket.start(get().code!),

  submitTrap: (text) => gameSocket.submitTrap(get().code!, text),

  submitAnswer: (text) => {
    gameSocket.submitAnswer(get().code!, text);
    set({ mySubmittedAnswer: true });
  },

  submitVote: (optionId) => {
    gameSocket.submitVote(get().code!, optionId);
    set({ myVoted: true, myVoteOptionId: optionId });
  },

  nextRound: () => gameSocket.nextRound(get().code!),

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
    });
  },

  clearError: () => set({ error: null }),
}));
