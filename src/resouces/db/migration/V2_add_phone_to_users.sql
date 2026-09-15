-- V2__add_phone_to_users.sql
ALTER TABLE users
    ADD COLUMN phone_number VARCHAR(15) NOT NULL DEFAULT '' ;

-- drop the default once existing rows (if any) are backfilled in real usage;
-- kept here so the column is safely non-null for new inserts during dev
CREATE UNIQUE INDEX idx_users_phone_number ON users(phone_number) WHERE phone_number <> '';