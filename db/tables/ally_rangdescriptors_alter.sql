ALTER TABLE ally_rangdescriptors ADD CONSTRAINT ally_rangdescriptors_fk_ally FOREIGN KEY (ally_id) REFERENCES ally(id);