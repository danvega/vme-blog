DROP TABLE IF EXISTS Post;

CREATE TABLE IF NOT EXISTS Post (
  id SERIAL NOT NULL,
  title varchar(255) NOT NULL,
  summary text,
  url varchar(255) NOT NULL,
  date_published timestamp NOT NULL,
  version INT,
  PRIMARY KEY (id)
);