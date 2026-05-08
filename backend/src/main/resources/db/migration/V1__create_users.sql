create table users (
	id bigserial primary key,
	email varchar(320) not null,
	password varchar(255) not null,
	nickname varchar(50) not null,
	created_at timestamp with time zone not null default now(),
	constraint uk_users_email unique (email)
);
