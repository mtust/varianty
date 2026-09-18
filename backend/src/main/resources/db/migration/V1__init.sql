create table users (
    id varchar(36) primary key,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    display_name varchar(40) not null,
    created_at timestamp not null
);

create table questions (
    id varchar(36) primary key,
    fact text not null,
    correct_answer varchar(255) not null,
    category varchar(100)
);

create table game_history (
    id varchar(36) primary key,
    user_id varchar(36) not null,
    room_code varchar(10) not null,
    display_name varchar(40) not null,
    score integer not null,
    placement integer not null,
    player_count integer not null,
    played_at timestamp not null
);

create index idx_game_history_user_id on game_history (user_id);
