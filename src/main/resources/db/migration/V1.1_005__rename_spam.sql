ALTER TABLE message CHANGE COLUMN is_garbage is_spam  tinyint(1) NOT NULL DEFAULT 0 AFTER has_attachments;