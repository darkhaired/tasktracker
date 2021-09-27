
ALTER TABLE task_state 
ADD COLUMN IF NOT EXISTS nominal_date timestamp without time zone;

UPDATE task_state
SET nominal_date = start_date
WHERE nominal_date IS NULL;