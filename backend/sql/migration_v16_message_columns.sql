-- Align legacy messages tables with the message service entity.
SET @schema_name = DATABASE();

SET @add_data_sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = @schema_name
          AND table_name = 'messages'
          AND column_name = 'data'
    ),
    'SELECT 1',
    'ALTER TABLE messages ADD COLUMN data TEXT NULL AFTER content'
);
PREPARE add_data_stmt FROM @add_data_sql;
EXECUTE add_data_stmt;
DEALLOCATE PREPARE add_data_stmt;

SET @add_read_time_sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = @schema_name
          AND table_name = 'messages'
          AND column_name = 'read_time'
    ),
    'SELECT 1',
    'ALTER TABLE messages ADD COLUMN read_time DATETIME NULL AFTER is_read'
);
PREPARE add_read_time_stmt FROM @add_read_time_sql;
EXECUTE add_read_time_stmt;
DEALLOCATE PREPARE add_read_time_stmt;
