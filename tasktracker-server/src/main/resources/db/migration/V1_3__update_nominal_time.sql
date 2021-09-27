

UPDATE task_state
SET nominal_date = start_date
WHERE nominal_date IS NULL;