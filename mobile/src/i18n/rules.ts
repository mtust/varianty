import type { Language } from '@/i18n';

export interface GameRules {
  button: string;
  title: string;
  goal: string;
  stepsTitle: string;
  steps: string[];
  scoringTitle: string;
  scoring: { points: string; text: string }[];
  modesTitle: string;
  modes: string[];
  close: string;
}

// Kept apart from translations.ts because the rules are structured text (lists), not single strings.
export const gameRules: Record<Language, GameRules> = {
  uk: {
    button: 'Правила гри',
    title: 'Як грати',
    goal: 'Вгадуйте правду й обманюйте друзів. Перемагає той, хто після останнього раунду має найбільше балів.',
    stepsTitle: 'Раунд',
    steps: [
      'На екрані з’являється дивний факт або цитата з пропуском «___».',
      'Кожен гравець вигадує правдоподібну, але хибну відповідь, тобто брехню. На це є 45 секунд.',
      'Якщо в кімнаті є ведучий, він бачить правильну відповідь і додає власну відповідь-пастку.',
      'Усі відповіді перемішуються разом із правильною. За 30 секунд оберіть ту, яку вважаєте правдою. За власну брехню голосувати не можна.',
      'Наприкінці раунду видно, хто що написав і хто за що проголосував.',
    ],
    scoringTitle: 'Бали',
    scoring: [
      { points: '+2', text: 'за правильну відповідь' },
      { points: '+1', text: 'за кожного гравця, який повірив вашій брехні' },
      { points: '−1', text: 'якщо ви повелися на пастку ведучого' },
    ],
    modesTitle: 'Кімнати',
    modes: [
      'З ведучим: ведучий не грає, а керує раундами та ставить пастки.',
      'Без ведучого: творець кімнати грає нарівні з усіма.',
      'Творець обирає категорію (факти чи цитати) і кількість раундів, а інші приєднуються за кодом кімнати.',
    ],
    close: 'Зрозуміло',
  },
  en: {
    button: 'How to play',
    title: 'How to play',
    goal: 'Spot the truth and fool your friends. Whoever has the most points after the last round wins.',
    stepsTitle: 'Each round',
    steps: [
      'A strange fact or a quote appears with a blank: “___”.',
      'Every player makes up a believable but wrong answer, a lie. You have 45 seconds.',
      'If the room has a host, the host sees the real answer and adds a trap answer of their own.',
      'All answers are shuffled together with the real one. You have 30 seconds to pick the one you think is true. You can’t vote for your own lie.',
      'At the end of the round you see who wrote what and who voted for what.',
    ],
    scoringTitle: 'Points',
    scoring: [
      { points: '+2', text: 'for picking the real answer' },
      { points: '+1', text: 'for every player who believed your lie' },
      { points: '−1', text: 'if you fell for the host’s trap' },
    ],
    modesTitle: 'Rooms',
    modes: [
      'With a host: the host doesn’t play, but runs the rounds and sets the traps.',
      'Without a host: the room creator plays like everyone else.',
      'The creator picks the category (facts or quotes) and the number of rounds, and others join with the room code.',
    ],
    close: 'Got it',
  },
};
