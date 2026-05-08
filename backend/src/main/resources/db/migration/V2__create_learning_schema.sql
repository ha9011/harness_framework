create table words (
	id bigserial primary key,
	user_id bigint not null,
	word varchar(200) not null,
	meaning varchar(500) not null,
	part_of_speech varchar(50),
	pronunciation varchar(200),
	synonyms varchar(500),
	tip varchar(500),
	is_important boolean not null default false,
	deleted boolean not null default false,
	created_at timestamp with time zone not null default now(),
	updated_at timestamp with time zone not null default now(),
	constraint fk_words_user foreign key (user_id) references users(id)
);

create index idx_words_user_id on words(user_id);
create index idx_words_deleted on words(deleted);

create table patterns (
	id bigserial primary key,
	user_id bigint not null,
	template varchar(255) not null,
	description varchar(500) not null,
	deleted boolean not null default false,
	created_at timestamp with time zone not null default now(),
	updated_at timestamp with time zone not null default now(),
	constraint fk_patterns_user foreign key (user_id) references users(id)
);

create index idx_patterns_user_id on patterns(user_id);
create index idx_patterns_deleted on patterns(deleted);

create table pattern_examples (
	id bigserial primary key,
	pattern_id bigint not null,
	sort_order integer not null,
	sentence varchar(500) not null,
	translation varchar(500) not null,
	constraint fk_pattern_examples_pattern foreign key (pattern_id) references patterns(id) on delete cascade,
	constraint ck_pattern_examples_sort_order check (sort_order > 0)
);

create index idx_pattern_examples_pattern_id on pattern_examples(pattern_id);

create table generation_history (
	id bigserial primary key,
	user_id bigint not null,
	level varchar(10) not null,
	requested_count integer not null,
	actual_count integer not null,
	word_id bigint,
	pattern_id bigint,
	created_at timestamp with time zone not null default now(),
	constraint fk_generation_history_user foreign key (user_id) references users(id),
	constraint fk_generation_history_word foreign key (word_id) references words(id),
	constraint fk_generation_history_pattern foreign key (pattern_id) references patterns(id),
	constraint ck_generation_history_requested_count check (requested_count > 0),
	constraint ck_generation_history_actual_count check (actual_count >= 0)
);

create index idx_generation_history_user_id on generation_history(user_id);
create index idx_generation_history_word_id on generation_history(word_id);
create index idx_generation_history_pattern_id on generation_history(pattern_id);

create table generated_sentences (
	id bigserial primary key,
	user_id bigint not null,
	generation_id bigint not null,
	pattern_id bigint,
	sentence text not null,
	translation text not null,
	level varchar(10) not null,
	deleted boolean not null default false,
	created_at timestamp with time zone not null default now(),
	constraint fk_generated_sentences_user foreign key (user_id) references users(id),
	constraint fk_generated_sentences_generation foreign key (generation_id) references generation_history(id),
	constraint fk_generated_sentences_pattern foreign key (pattern_id) references patterns(id)
);

create index idx_generated_sentences_user_id on generated_sentences(user_id);
create index idx_generated_sentences_generation_id on generated_sentences(generation_id);
create index idx_generated_sentences_pattern_id on generated_sentences(pattern_id);
create index idx_generated_sentences_deleted on generated_sentences(deleted);

create table generated_sentence_words (
	id bigserial primary key,
	sentence_id bigint not null,
	word_id bigint not null,
	constraint fk_generated_sentence_words_sentence foreign key (sentence_id) references generated_sentences(id) on delete cascade,
	constraint fk_generated_sentence_words_word foreign key (word_id) references words(id)
);

create index idx_generated_sentence_words_sentence_id on generated_sentence_words(sentence_id);
create index idx_generated_sentence_words_word_id on generated_sentence_words(word_id);

create table sentence_situations (
	id bigserial primary key,
	sentence_id bigint not null,
	situation text not null,
	constraint fk_sentence_situations_sentence foreign key (sentence_id) references generated_sentences(id) on delete cascade
);

create index idx_sentence_situations_sentence_id on sentence_situations(sentence_id);

create table study_records (
	id bigserial primary key,
	user_id bigint not null,
	study_date date not null,
	day_number integer not null,
	created_at timestamp with time zone not null default now(),
	constraint fk_study_records_user foreign key (user_id) references users(id),
	constraint uk_study_records_user_date unique (user_id, study_date),
	constraint ck_study_records_day_number check (day_number > 0)
);

create index idx_study_records_user_id on study_records(user_id);
create index idx_study_records_study_date on study_records(study_date);

create table study_record_items (
	id bigserial primary key,
	study_record_id bigint not null,
	item_type varchar(10) not null,
	item_id bigint not null,
	constraint fk_study_record_items_record foreign key (study_record_id) references study_records(id) on delete cascade,
	constraint uk_study_record_items_record_type_item unique (study_record_id, item_type, item_id),
	constraint ck_study_record_items_type check (item_type in ('WORD', 'PATTERN'))
);

create index idx_study_record_items_record_id on study_record_items(study_record_id);

create table review_items (
	id bigserial primary key,
	user_id bigint not null,
	item_type varchar(20) not null,
	item_id bigint not null,
	direction varchar(15) not null default 'RECOGNITION',
	deleted boolean not null default false,
	next_review_date date not null,
	interval_days integer not null default 1,
	ease_factor double precision not null default 2.5,
	review_count integer not null default 0,
	last_result varchar(10),
	last_reviewed_at timestamp with time zone,
	created_at timestamp with time zone not null default now(),
	constraint fk_review_items_user foreign key (user_id) references users(id),
	constraint ck_review_items_type check (item_type in ('WORD', 'PATTERN', 'SENTENCE')),
	constraint ck_review_items_direction check (direction in ('RECOGNITION', 'RECALL')),
	constraint ck_review_items_last_result check (last_result is null or last_result in ('EASY', 'MEDIUM', 'HARD')),
	constraint ck_review_items_interval_days check (interval_days > 0),
	constraint ck_review_items_ease_factor check (ease_factor >= 1.3),
	constraint ck_review_items_review_count check (review_count >= 0)
);

create index idx_review_items_user_id on review_items(user_id);
create index idx_review_items_deleted on review_items(deleted);
create index idx_review_items_next_review_date on review_items(next_review_date);
create index idx_review_items_item_type_last_result on review_items(item_type, last_result);

create table review_logs (
	id bigserial primary key,
	review_item_id bigint not null,
	result varchar(10) not null,
	reviewed_at timestamp with time zone not null default now(),
	constraint fk_review_logs_review_item foreign key (review_item_id) references review_items(id) on delete cascade,
	constraint ck_review_logs_result check (result in ('EASY', 'MEDIUM', 'HARD'))
);

create index idx_review_logs_review_item_id on review_logs(review_item_id);
create index idx_review_logs_reviewed_at on review_logs(reviewed_at);

create table user_settings (
	id bigserial primary key,
	user_id bigint not null,
	daily_review_count integer not null default 10,
	created_at timestamp with time zone not null default now(),
	updated_at timestamp with time zone not null default now(),
	constraint fk_user_settings_user foreign key (user_id) references users(id),
	constraint uk_user_settings_user unique (user_id),
	constraint ck_user_settings_daily_review_count check (daily_review_count in (10, 20, 30))
);

create index idx_user_settings_user_id on user_settings(user_id);
