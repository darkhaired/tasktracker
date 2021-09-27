CREATE TABLE configuration (
  id SERIAL,
  key text UNIQUE NOT NULL ,
  value text NOT NULL ,
  PRIMARY KEY(id)
);


INSERT INTO configuration (key, value)
VALUES
('jira.server.uri', 'https://test.com'),
('jira.username', 'test'),
('jira.password', 'test'),
('jira.project.name', 'TEST'),
('jira.issue.type.id', '1'),
('jira.component.name', 'TEST'),
('jira.priority.id', '1'),
('jira.assignee.name', 'alvilitvinov@test.ru'),
('jira.issue.url', 'https://test.com/browse/'),
('jira.issue.summary.prefix', 'Ошибка расчета: ');

