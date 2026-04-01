import mysql.connector

def get_connection():
    db_config = {
        "host": "localhost",
        "user": "root",
        "password": "", # Default for local dev often blank
        "database": "endecryption" # Guessed from previous user command
    }
    return mysql.connector.connect(**db_config)
