import mysql.connector

db_config = {
    "host": "localhost",
    "user": "root",
    "password": "your_password",
    "database": "your_db"
}

try:
    connection = mysql.connector.connect(**db_config)
    cursor = connection.cursor(dictionary=True)
    cursor.execute("SELECT email FROM users")
    users = cursor.fetchall()
    print("Emails in 'users' table:")
    for user in users:
        print(f"- {user['email']}")
    cursor.close()
    connection.close()
except Exception as e:
    print(f"Error: {e}")
