-- Force QuestionSeeder to repopulate with the new facts/quotes content
-- (old questions had numeric answers, which the game no longer allows).
truncate table questions;
