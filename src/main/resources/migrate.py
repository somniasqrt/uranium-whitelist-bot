import json
import os
import psycopg2
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

# Database connection details from environment variables
db_host = os.getenv("DB_HOST", "localhost")
db_name = os.getenv("DB_NAME")
db_user = os.getenv("DB_USER")
db_password = os.getenv("DB_PASSWORD")
db_port = os.getenv("DB_PORT", "5432")

# Path to your JSON file
json_file_path = 'whitelist.json'

def migrate_whitelist():
    """
    Migrates whitelist data from a JSON file to the PostgreSQL database.
    """
    try:
        # Connect to the database
        conn = psycopg2.connect(
            dbname=db_name,
            user=db_user,
            password=db_password,
            host=db_host,
            port=db_port
        )
        cursor = conn.cursor()
        print("Successfully connected to the database.")

        # Load the JSON data
        with open(json_file_path, 'r') as f:
            whitelist_data = json.load(f)
        print(f"Loaded {len(whitelist_data)} records from {json_file_path}.")

        # Prepare the SQL statement
        insert_sql = """
            INSERT INTO whitelist (discord_id, minecraft_name, twin_name, on_server, paid, expires_on)
            VALUES (%s, %s, %s, %s, %s, %s)
            ON CONFLICT (discord_id) DO NOTHING;
        """

        # Iterate over the JSON data and insert into the database
        for minecraft_name, data in whitelist_data.items():
            discord_id = int(data.get("user_id"))
            on_server = data.get("on_server", True)
            twin_name = data.get("twin") if data.get("twin") else None
            
            # Assuming all migrated users are on a paid plan with no expiration
            paid = True
            expires_on = None

            try:
                cursor.execute(insert_sql, (discord_id, minecraft_name, twin_name, on_server, paid, expires_on))
                print(f"Migrated user: {minecraft_name}")
            except psycopg2.Error as e:
                print(f"Error inserting user {minecraft_name}: {e}")
                conn.rollback()

        # Commit the changes and close the connection
        conn.commit()
        cursor.close()
        conn.close()
        print("Migration complete. All records have been processed.")

    except FileNotFoundError:
        print(f"Error: The file {json_file_path} was not found.")
    except psycopg2.Error as e:
        print(f"Database connection error: {e}")
    except Exception as e:
        print(f"An unexpected error occurred: {e}")

if __name__ == "__main__":
    migrate_whitelist()
