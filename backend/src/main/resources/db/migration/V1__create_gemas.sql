create table if not exists games
(
    id             varchar(36)  not null
        constraint games_pkey
            primary key,
    external_id    varchar      not null,
    title_en       varchar(255) not null,
    title_ru       varchar(255) not null,
    description_ru varchar      not null,
    description_en varchar      not null,
    category       varchar(20)  not null
);

create table if not exists tags
(
    id      serial      not null
        constraint tags_pkey
            primary key,
    name_en varchar(64) not null,
    name_ru varchar(64) not null
);


create table if not exists game_tags
(
    tag_id  int references tags (id) on update cascade,
    game_id varchar(36) references games (id) on update cascade,
    created timestamp(0) default CURRENT_TIMESTAMP not null,
    removed timestamp(0)                           null,
    constraint tag_id_game_id_pkey PRIMARY KEY (tag_id, game_id)
);