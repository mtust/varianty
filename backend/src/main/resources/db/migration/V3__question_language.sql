-- Existing questions are all Ukrainian; QuestionSeeder adds the English set on next startup.
alter table questions add column language varchar(2) not null default 'uk';

create index idx_questions_category_language on questions (category, language);
