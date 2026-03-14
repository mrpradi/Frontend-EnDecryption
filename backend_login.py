from fastapi import FastAPI, HTTPException, Depends
from pydantic import BaseModel
from typing import Optional
import mysql.connector

app = FastAPI()

# Database configuration
db_config = {
    "host": "localhost",
    "user": "root",
    "password": "your_password",
    "database": "your_db"
}

def get_connection():
    return mysql.connector.connect(**db_config)

class UserLogin(BaseModel):
    email: str
    password: str

@app.post("/login")
def login_user(user: UserLogin):
    try:
        connection = get_connection()
        cursor = connection.cursor(dictionary=True)

        cursor.execute("SELECT * FROM users WHERE email=%s", (user.email,))
        db_user = cursor.fetchone()

        if not db_user:
            raise HTTPException(status_code=400, detail="Invalid email")

        if not db_user.get("otp_verified", False):
            raise HTTPException(status_code=403, detail="Verify OTP first")

        if db_user["password"] != user.password:
            raise HTTPException(status_code=400, detail="Invalid password")

        return {"message": "Login successful", "user_id": db_user["id"]}
    
    except mysql.connector.Error as err:
        raise HTTPException(status_code=500, detail=f"Database error: {err}")
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'connection' in locals():
            connection.close()

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
