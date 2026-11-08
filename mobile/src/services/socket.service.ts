import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { AppState, AppStateStatus, NativeEventSubscription } from 'react-native';

import { localizeServerError, translate } from '@/i18n';
import { WS_URL } from '@/services/api.service';
import {
  HostQuestion,
  KickedNotice,
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
  onKicked: (notice: KickedNotice) => void;
}

class GameSocketService {
  private client: Client | null = null;
  private subscriptions: StompSubscription[] = [];
  private joinArgs: { code: string; handlers: RoomHandlers } | null = null;
  private appStateSub: NativeEventSubscription | null = null;
  private backgroundedAt: number | null = null;
  private reconnecting = false;

  connect(token: string): Promise<void> {
    return new Promise((resolve, reject) => {
      let settled = false;
      const client = new Client({
        brokerURL: WS_URL,
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 3000,
        connectionTimeout: 10000,
        // Heartbeats let us notice a socket the OS silently killed while the app was in
        // the background, instead of sending into a dead connection.
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        // React Native's WebSocket doesn't handle text frames the way stompjs expects by
        // default (incoming frames are silently dropped) — these two flags are the
        // documented workaround for RN.
        forceBinaryWSFrames: true,
        appendMissingNULLonIncoming: true,
        // Must stay a function: stompjs calls it unconditionally, so `undefined` throws
        // inside activate() and the socket is never opened.
        debug: __DEV__ ? (str) => console.log('[STOMP]', str) : () => {},
        onConnect: () => {
          console.log('[STOMP] connected, session established');
          // Subscriptions from a previous connection died with it.
          this.subscriptions = [];
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
            reject(new Error(frame.headers.message ? localizeServerError(frame.headers.message) : translate('errors.connectRoom')));
          }
        },
        onWebSocketError: (event) => {
          console.error('[STOMP] websocket error', event);
          if (!settled) {
            settled = true;
            reject(event instanceof Error ? event : new Error(translate('errors.connectRoom')));
          }
        },
        onWebSocketClose: (event) => {
          console.error('[STOMP] websocket closed', event.code, event.reason);
          // this.client is cleared by disconnect(), so leaving the lobby isn't reported as a failure.
          if (!settled && this.client === client) {
            settled = true;
            reject(new Error(translate('errors.connectRoom')));
          }
        },
      });
      this.client = client;
      client.activate();
      this.watchAppState();
    });
  }

  /**
   * When the app comes back from the background its socket is often dead without stompjs
   * knowing yet, so the player would stay "offline" and their actions would be lost.
   * Reconnect right away instead; onConnect then resubscribes and rejoins the room.
   */
  private watchAppState() {
    if (this.appStateSub) return;
    this.appStateSub = AppState.addEventListener('change', (next: AppStateStatus) => {
      if (next === 'background' || next === 'inactive') {
        if (this.backgroundedAt === null) this.backgroundedAt = Date.now();
        return;
      }
      if (next !== 'active') return;
      const awayMs = this.backgroundedAt === null ? 0 : Date.now() - this.backgroundedAt;
      this.backgroundedAt = null;
      const client = this.client;
      if (!client) return;
      if (!client.connected || awayMs > 2000) {
        this.reconnect();
      }
    });
  }

  private reconnect() {
    const client = this.client;
    if (!client || this.reconnecting) return;
    this.reconnecting = true;
    client
      .deactivate()
      .catch(() => {})
      .finally(() => {
        this.reconnecting = false;
        // Left the room (or got kicked) while we were tearing down — don't come back.
        if (this.client === client) {
          client.activate();
        }
      });
  }

  joinRoom(code: string, handlers: RoomHandlers) {
    this.joinArgs = { code, handlers };
    this.subscribeAndJoin(code, handlers);
  }

  private subscribeAndJoin(code: string, handlers: RoomHandlers) {
    const client = this.requireClient();
    if (!client.connected) {
      // onConnect will subscribe and join once the connection is back.
      return;
    }
    const parse = <T>(message: IMessage): T => JSON.parse(message.body) as T;

    this.subscriptions.forEach((sub) => {
      try {
        sub.unsubscribe();
      } catch {
        // The connection these belonged to is already gone.
      }
    });
    this.subscriptions = [
      client.subscribe(`/topic/room/${code}`, (msg) => handlers.onSnapshot(parse<RoomSnapshot>(msg))),
      client.subscribe(`/topic/room/${code}/voting`, (msg) => handlers.onVotingStarted(parse<VotingStarted>(msg))),
      client.subscribe(`/topic/room/${code}/results`, (msg) => handlers.onRoundResult(parse<RoundResult>(msg))),
      client.subscribe('/user/queue/host-question', (msg) => handlers.onHostQuestion(parse<HostQuestion>(msg))),
      // Replayed privately by the server when we (re)join mid-round.
      client.subscribe('/user/queue/voting', (msg) => handlers.onVotingStarted(parse<VotingStarted>(msg))),
      client.subscribe('/user/queue/results', (msg) => handlers.onRoundResult(parse<RoundResult>(msg))),
      client.subscribe('/user/queue/errors', (msg) => handlers.onError(parse<ServerError>(msg))),
      client.subscribe('/user/queue/kicked', (msg) => handlers.onKicked(parse<KickedNotice>(msg))),
    ];

    client.publish({ destination: `/app/room/${code}/join`, body: '{}' });
  }

  start(code: string) {
    this.send({ destination: `/app/room/${code}/start`, body: '{}' });
  }

  submitTrap(code: string, text: string) {
    this.send({ destination: `/app/room/${code}/host-trap`, body: JSON.stringify({ text }) });
  }

  submitAnswer(code: string, text: string) {
    this.send({ destination: `/app/room/${code}/answer`, body: JSON.stringify({ text }) });
  }

  submitVote(code: string, optionId: string) {
    this.send({ destination: `/app/room/${code}/vote`, body: JSON.stringify({ optionId }) });
  }

  kickPlayer(code: string, playerId: string) {
    this.send({ destination: `/app/room/${code}/kick`, body: JSON.stringify({ playerId }) });
  }

  nextRound(code: string) {
    this.send({ destination: `/app/room/${code}/next-round`, body: '{}' });
  }

  disconnect() {
    const client = this.client;
    this.client = null;
    this.joinArgs = null;
    if (client?.connected) {
      this.subscriptions.forEach((sub) => sub.unsubscribe());
    }
    this.subscriptions = [];
    this.appStateSub?.remove();
    this.appStateSub = null;
    this.backgroundedAt = null;
    client?.deactivate();
  }

  /** Publishes to the server, reconnecting first if the socket dropped (e.g. after backgrounding). */
  private send(params: { destination: string; body: string }) {
    const client = this.requireClient();
    if (!client.connected) {
      this.reconnect();
      throw new Error(translate('errors.connectRoom'));
    }
    client.publish(params);
  }

  private requireClient(): Client {
    if (!this.client) {
      throw new Error('Socket is not connected');
    }
    return this.client;
  }
}

export const gameSocket = new GameSocketService();
