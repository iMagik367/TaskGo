#!/bin/bash

# Export data from SQLite
echo ".mode insert" > export.sql
echo "SELECT * FROM users;" | sqlite3 taskgo.db >> export.sql
echo "SELECT * FROM services;" | sqlite3 taskgo.db >> export.sql
echo "SELECT * FROM tasks;" | sqlite3 taskgo.db >> export.sql
echo "SELECT * FROM reviews;" | sqlite3 taskgo.db >> export.sql

# Convert SQLite export to PostgreSQL format
sed -i 's/INSERT INTO/INSERT INTO public./g' export.sql

# Import data to PostgreSQL
PGPASSWORD=$DATABASE_PASSWORD psql -h $DATABASE_HOST -U $DATABASE_USER -d $DATABASE_NAME -f export.sql