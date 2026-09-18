import { Client, IMessage, StompSubscription } from '@stomp/stompjs';

import { WS_URL } from '@/services/api.service';
import {
  HostQuestion,
  RoomSnapshot,
  RoundResult,
  ServerError,
  VotingStarted,
} from '@/types/game';

interface RoomHandlers {
  onSnapshot: (snapshot: RoomSnapshot) => void;
  onVotingStarted: (voting: VotingStarted) => void;
  onRoundResult: (result: RoundResult) => void;
  onHostQuestion: (question: HostQuestion) => void;
  onError: (error: ServerError) => void;
}

class GameSocketService {
  private client: Client | null = null;
  private subscriptions: StompSubscription[] = [];
  private joinArgs: { code: string; handlers: RoomHandlers } | null = null;

  connect(token: string): Promise<void> {
    return new Promise((resolve, reject) => {
      let settled = false;
      const client = new Client({
        brokerURL: WS_URL,
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 3000,
        // React Native's WebSocket doesn't handle text frames the way stompjs expects by
        // default (incoming frames are silently dropped) — these two flags are the
        // documented workaround for RN.
        forceBinaryWSFrames: true,
        appendMissingNULLonIncoming: true,
        debug: __DEV__ ? (str) => console.log('[STOMP]', str) : undefined,
        onConnect: () => {
          console.log('[STOMP] connected, session established');
          if (this.joinArgs) {
            this.subscribeAndJoin(this.joinArgs.code, this.joinArgs.handlers);
          }
          if (!settled) {
            settled = true;
            resolve();
          }
        },
        onStompError: (frame) => {
          console.error('[STOMP] error frame', frame.headers, frame.body);
          if (!settled) {
            settled = true;
            reject(new Error(frame.headers.message ?? 'STOMP error'));
          }
        },
        onWebSocketError: (event) => {
          console.error('[STOMP] websocket error', event);
          if (!settled) {
            settled = true;
            reject(event instanceof Error ? event : new Error('WebSocket error'));
          }
        },
        onWebSocketClose: (event) => {
          console.error('[STOMP] websocket closed', event.code, event.reason);
        },
      });
      this.client = client;
      client.activate();
    });
  }

  joinRoom(code: string, handlers: RoomHandlers) {
    this.joinArgs = { code, handlers };
    this.subscribeAndJoin(code, handlers);
  }

  private subscribeAndJoin(code: string, handlers: RoomHandlers) {
    const client = this.requireClient();
    const parse = <T>(message: IMessage): T => JSON.parse(message.body) as T;

    this.subscriptions.forEach((sub) => sub.unsubscribe());
    this.subscriptions = [
      client.subscribe(`/topic/room/${code}`, (msg) => handlers.onSnapshot(parse<RoomSnapshot>(msg))),
      client.subscribe(`/topic/room/${code}/voting`, (msg) => handlers.onVotingStarted(parse<VotingStarted>(msg))),
      client.subscribe(`/topic/room/${code}/results`, (msg) => handlers.onRoundResult(parse<RoundResult>(msg))),
      client.subscribe('/user/queue/host-question', (msg) => handlers.onHostQuestion(parse<HostQuestion>(msg))),
      client.subscribe('/user/queue/errors', (msg) => handlers.onError(parse<ServerError>(msg))),
    ];

    client.publish({ destination: `/app/room/${code}/join`, body: '{}' });
  }

  start(code: string) {
    this.requireClient().publish({ destination: `/app/room/${code}/start`, body: '{}' });
  }

  submitTrap(code: string, text: string) {
    this.requireClient().publish({ destination: `/app/room/${code}/host-trap`, body: JSON.stringify({ text }) });
  }

  submitAnswer(code: string, text: string) {
    this.requireClient().publish({ destination: `/app/room/${code}/answer`, body: JSON.stringify({ text }) });
  }

  submitVote(code: string, optionId: string) {
    this.requireClient().publish({ destination: `/app/room/${code}/vote`, body: JSON.stringify({ optionId }) });
  }

  nextRound(code: string) {
    this.requireClient().publish({ destination: `/app/room/${code}/next-round`, body: '{}' });
  }

  disconnect() {
    this.subscriptions.forEach((sub) => sub.unsubscribe());
    this.subscriptions = [];
    this.joinArgs = null;
    this.client?.deactivate();
    this.client = null;
  }

  private requireClient(): Client {
    if (!this.client) {
      throw new Error('Socket is not connected');
    }
    return this.client;
  }
}

export const gameSocket = new GameSocketService();
