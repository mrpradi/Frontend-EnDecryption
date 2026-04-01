from fastapi import FastAPI, HTTPException, UploadFile, File, Form
from fastapi.responses import FileResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, EmailStr, validator
import re
import random
import smtplib
import os
import json
import base64
import secrets
from email.mime.text import MIMEText
from connection import get_connection
import uuid

from cryptography.hazmat.primitives.asymmetric import rsa, padding
from cryptography.hazmat.primitives import hashes, hmac, serialization
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.backends import default_backend

app = FastAPI()

# ==========================================================
# CORS
# ==========================================================
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ==========================================================
# Upload Folder
# ==========================================================
BASE_UPLOAD_FOLDER = os.path.join(os.getcwd(),"uploads")
os.makedirs(BASE_UPLOAD_FOLDER,exist_ok=True)

# ==========================================================
# Persistent RSA Keys
# ==========================================================
PRIVATE_KEY_PATH="private_key.pem"
PUBLIC_KEY_PATH="public_key.pem"

if os.path.exists(PRIVATE_KEY_PATH):

    with open(PRIVATE_KEY_PATH,"rb") as f:
        private_key = serialization.load_pem_private_key(
            f.read(),
            password=None,
            backend=default_backend()
        )

    with open(PUBLIC_KEY_PATH,"rb") as f:
        public_key = serialization.load_pem_public_key(
            f.read(),
            backend=default_backend()
        )

else:

    private_key = rsa.generate_private_key(
        public_exponent=65537,
        key_size=4096,
        backend=default_backend()
    )

    public_key = private_key.public_key()

    with open(PRIVATE_KEY_PATH,"wb") as f:
        f.write(private_key.private_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PrivateFormat.PKCS8,
            encryption_algorithm=serialization.NoEncryption()
        ))

    with open(PUBLIC_KEY_PATH,"wb") as f:
        f.write(public_key.public_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PublicFormat.SubjectPublicKeyInfo
        ))

# ==========================================================
# EMAIL CONFIG
# ==========================================================
SENDER_EMAIL="pradeepkumar5031485@gmail.com"
SENDER_PASSWORD="tkhh eriu mvhl apmd"

def send_otp_email(receiver_email,otp):

    subject="Email Verification OTP"
    body=f"<h3>Your OTP:</h3><h1>{otp}</h1>"

    msg=MIMEText(body,"html")
    msg["Subject"]=subject
    msg["From"]=SENDER_EMAIL
    msg["To"]=receiver_email

    server=smtplib.SMTP("smtp.gmail.com",587)
    server.starttls()
    server.login(SENDER_EMAIL,SENDER_PASSWORD)
    server.sendmail(SENDER_EMAIL,receiver_email,msg.as_string())
    server.quit()

# ==========================================================
# ADD NOTIFICATION HELPER
# ==========================================================
def add_notification(user_id, message):

    connection = get_connection()
    cursor = connection.cursor()

    cursor.execute("""
        INSERT INTO notifications (user_id, message)
        VALUES (%s, %s)
    """, (user_id, message))

    connection.commit()
    cursor.close()
    connection.close()

# ==========================================================
# MODELS
# ==========================================================
class UserRegister(BaseModel):
    name:str
    email:EmailStr
    age:int
    password:str

    @validator("password")
    def validate_password(cls,value):
        if len(value)<8: raise ValueError("Password must be 8 characters")
        if not re.search(r"[A-Z]",value): raise ValueError("Need uppercase")
        if not re.search(r"[a-z]",value): raise ValueError("Need lowercase")
        if not re.search(r"[0-9]",value): raise ValueError("Need number")
        if not re.search(r"[!@#$%^&*]",value): raise ValueError("Need special")
        return value

class UserLogin(BaseModel):
    email:EmailStr
    password:str

class OTPVerify(BaseModel):
    email:EmailStr
    otp:str

class UpdateProfile(BaseModel):
    name:str
    email:EmailStr
    age:int

class ChangePassword(BaseModel):
    email:EmailStr
    current_password:str
    new_password:str
    confirm_password:str

class ForgotPassword(BaseModel):
    email:EmailStr

class ResetPassword(BaseModel):
    email:EmailStr
    otp:str
    new_password:str
    confirm_password:str

    @validator("new_password")
    def validate_password(cls,value):
        if len(value)<8: raise ValueError("Password must be 8 characters")
        if not re.search(r"[A-Z]",value): raise ValueError("Need uppercase")
        if not re.search(r"[a-z]",value): raise ValueError("Need lowercase")
        if not re.search(r"[0-9]",value): raise ValueError("Need number")
        if not re.search(r"[!@#$%^&*]",value): raise ValueError("Need special symbol")
        return value

# ==========================================================
# REGISTER
# ==========================================================
@app.post("/register")
def register_user(user:UserRegister):

    connection=get_connection()
    cursor=connection.cursor(dictionary=True)

    cursor.execute("SELECT * FROM users WHERE email=%s",(user.email,))
    existing_user = cursor.fetchone()
    
    otp=str(random.randint(100000,999999))

    if existing_user:
        if existing_user["otp_verified"]:
            cursor.close()
            connection.close()
            raise HTTPException(400,"Email already registered")
        else:
            # Replace old OTP with new one and update user info
            cursor.execute("UPDATE users SET name=%s, age=%s, password=%s, otp=%s WHERE email=%s",
                           (user.name, user.age, user.password, otp, user.email))
            connection.commit()
            user_id = existing_user["id"]
    else:
        cursor.execute("""
            INSERT INTO users(name,email,age,password,otp,otp_verified)
            VALUES(%s,%s,%s,%s,%s,%s)
        """,(user.name,user.email,user.age,user.password,otp,False))
        connection.commit()
        user_id = cursor.lastrowid
        add_notification(user_id, f"Welcome {user.name}! Your account has been created successfully.")

    cursor.close()
    connection.close()

    send_otp_email(user.email,otp)

    return {"message":"OTP sent"}

# ==========================================================
# VERIFY OTP
# ==========================================================
@app.post("/verify-otp")
def verify_otp(data:OTPVerify):

    connection=get_connection()
    cursor=connection.cursor(dictionary=True)

    cursor.execute("SELECT * FROM users WHERE email=%s",(data.email,))
    user=cursor.fetchone()

    if not user:
        raise HTTPException(400,"User not found")

    if user["otp"]!=data.otp:
        raise HTTPException(400,"Invalid OTP")

    cursor.execute("UPDATE users SET otp_verified=1 WHERE email=%s",(data.email,))
    connection.commit()

    cursor.close()
    connection.close()

    return {"message":"OTP verified"}

# ==========================================================
# LOGIN
# ==========================================================
@app.post("/login")
def login_user(user:UserLogin):

    connection=get_connection()
    cursor=connection.cursor(dictionary=True)

    cursor.execute("SELECT * FROM users WHERE email=%s",(user.email,))
    db_user=cursor.fetchone()

    if not db_user:
        raise HTTPException(400,"Invalid email")

    if not db_user["otp_verified"]:
        raise HTTPException(403,"Verify OTP first")

    if db_user["password"]!=user.password:
        raise HTTPException(400,"Invalid password")

    cursor.close()
    connection.close()

    return {"message":"Login successful","user_id":db_user["id"]}

# ==========================================================
# GET PROFILE
# ==========================================================
@app.get("/profile/{email}")
def get_user_profile(email:str):
    connection=get_connection()
    cursor=connection.cursor(dictionary=True)

    cursor.execute("SELECT name, email, age FROM users WHERE email=%s", (email,))
    user = cursor.fetchone()

    cursor.close()
    connection.close()

    if not user:
        raise HTTPException(404, "User not found")

    return user

# ==========================================================
# UPDATE PROFILE
# ==========================================================
@app.post("/update-profile")
def update_profile(data:UpdateProfile):

    connection=get_connection()
    cursor=connection.cursor(dictionary=True)

    cursor.execute("SELECT id FROM users WHERE email=%s",(data.email,))
    user=cursor.fetchone()

    if not user:
        cursor.close()
        connection.close()
        raise HTTPException(404,"User not found")

    cursor.execute("UPDATE users SET name=%s, age=%s WHERE email=%s",(data.name,data.age,data.email))
    connection.commit()

    add_notification(user["id"], "Your profile information has been updated successfully.")

    cursor.close()
    connection.close()

    return {"message":"Profile updated successfully"}

# ==========================================================
# WIPE ALL DATA
# ==========================================================
@app.delete("/wipe-data/{email}")
def wipe_user_data(email:str):
    connection = get_connection()
    cursor = connection.cursor(dictionary=True)

    # 1. Get user ID
    cursor.execute("SELECT id FROM users WHERE email=%s", (email,))
    user = cursor.fetchone()

    if not user:
        cursor.close()
        connection.close()
        raise HTTPException(404, "User not found")

    user_id = user["id"]

    # 2. Get all file paths and delete actual files from filesystem
    cursor.execute("SELECT file_path FROM files WHERE user_id=%s", (user_id,))
    user_files = cursor.fetchall()

    for f in user_files:
        path = f["file_path"]
        if os.path.exists(path):
            try:
                os.remove(path)
            except Exception as e:
                print(f"Error deleting file {path}: {e}")

    # 3. Delete database records
    cursor.execute("DELETE FROM files WHERE user_id=%s", (user_id,))
    cursor.execute("DELETE FROM notifications WHERE user_id=%s", (user_id,))

    connection.commit()
    cursor.close()
    connection.close()

    return {"message": "All user data wiped successfully"}

@app.delete("/delete-account/{email}")
def delete_account(email:str):
    connection = get_connection()
    cursor = connection.cursor(dictionary=True)
    cursor.execute("SELECT id FROM users WHERE email=%s", (email,))
    user = cursor.fetchone()
    if not user:
        cursor.close()
        connection.close()
        raise HTTPException(404, "User not found")
    user_id = user["id"]
    cursor.execute("SELECT file_path FROM files WHERE user_id=%s", (user_id,))
    for f in cursor.fetchall():
        path = f.get("file_path")
        if path and os.path.exists(path):
            try: os.remove(path)
            except: pass
    cursor.execute("DELETE FROM files WHERE user_id=%s", (user_id,))
    cursor.execute("DELETE FROM notifications WHERE user_id=%s", (user_id,))
    cursor.execute("DELETE FROM users WHERE email=%s", (email,))
    connection.commit()
    cursor.close()
    connection.close()
    return {"message": "Account deleted permanently"}

# ==========================================================
# CHANGE PASSWORD
# ==========================================================
@app.post("/change-password")
def change_password(data:ChangePassword):

    connection=get_connection()
    cursor=connection.cursor(dictionary=True)

    cursor.execute("SELECT id, password FROM users WHERE email=%s",(data.email,))
    user=cursor.fetchone()

    if not user:
        cursor.close()
        connection.close()
        raise HTTPException(404,"User not found")

    if user["password"]!=data.current_password:
        cursor.close()
        connection.close()
        raise HTTPException(400,"Incorrect current password")

    if data.new_password != data.confirm_password:
        cursor.close()
        connection.close()
        raise HTTPException(400,"Passwords do not match")

    cursor.execute("UPDATE users SET password=%s WHERE email=%s",(data.new_password,data.email))
    connection.commit()

    add_notification(user["id"], "Your account password has been updated successfully.")

    cursor.close()
    connection.close()

    return {"message":"Password changed successfully"}

# ==========================================================
# HYBRID ENCRYPTION
# ==========================================================
@app.post("/encrypt-file")
async def encrypt_file(email:str=Form(...),file:UploadFile=File(...)):

    data=await file.read()

    digest=hashes.Hash(hashes.SHA3_256())
    digest.update(data)
    file_hash=digest.finalize()

    aes_key=AESGCM.generate_key(bit_length=256)
    aes=AESGCM(aes_key)
    nonce=secrets.token_bytes(12)

    ciphertext=aes.encrypt(nonce,data,None)

    h=hmac.HMAC(aes_key,hashes.SHA256())
    h.update(ciphertext)
    mac=h.finalize()

    encrypted_key=public_key.encrypt(
        aes_key,
        padding.OAEP(
            mgf=padding.MGF1(hashes.SHA256()),
            algorithm=hashes.SHA256(),
            label=None
        )
    )

    package={
        "key":base64.b64encode(encrypted_key).decode(),
        "nonce":base64.b64encode(nonce).decode(),
        "ciphertext":base64.b64encode(ciphertext).decode(),
        "hmac":base64.b64encode(mac).decode(),
        "hash":base64.b64encode(file_hash).decode(),
        "original_filename": file.filename
    }

    filename="secure_"+file.filename+".json"
    path=os.path.join(BASE_UPLOAD_FOLDER,filename)

    with open(path,"w") as f:
        json.dump(package,f)

    # -------- ADDED LINES --------
    file_format = file.filename.split(".")[-1]
    decryption_key = base64.b64encode(aes_key).decode()
    # -----------------------------

    connection=get_connection()
    cursor=connection.cursor(dictionary=True)

    cursor.execute("SELECT id FROM users WHERE email=%s",(email,))
    user=cursor.fetchone()

    # -------- UPDATED INSERT --------
    cursor.execute("""
        INSERT INTO files(user_id,file_name,file_format,file_path,file_type,decryption_key)
        VALUES(%s,%s,%s,%s,%s,%s)
    """,(user["id"],filename,file_format,path,"encrypted",decryption_key))
    # --------------------------------

    connection.commit()

    file_id = cursor.lastrowid

    add_notification(user["id"], f"File encrypted successfully: {file.filename}")

    cursor.close()
    connection.close()

    return {
        "message":"Encryption successful",
        "file_id": file_id,
        "decryption_key":decryption_key,
        "encrypted_file":filename
    }

# ==========================================================
# FETCH ENCRYPTED FILES
# ==========================================================
@app.get("/encrypted-files/{email}")
def get_encrypted_files(email:str):

    connection=get_connection()
    cursor=connection.cursor(dictionary=True)

    cursor.execute("SELECT id FROM users WHERE email=%s",(email,))
    user=cursor.fetchone()

    cursor.execute("""
        SELECT id,file_name,file_path,created_at
        FROM files
        WHERE user_id=%s AND file_type='encrypted'
        ORDER BY created_at DESC
    """,(user["id"],))

    files=cursor.fetchall()

    cursor.close()
    connection.close()

    return {"encrypted_files":files}

# ==========================================================
# DOWNLOAD ENCRYPTED FILE
# ==========================================================
@app.get("/download-encrypted/{file_id}")
def download_encrypted_file(file_id:int):

    connection=get_connection()
    cursor=connection.cursor(dictionary=True)

    cursor.execute("SELECT * FROM files WHERE id=%s",(file_id,))
    file=cursor.fetchone()

    cursor.close()
    connection.close()

    return FileResponse(file["file_path"],filename=file["file_name"])

# ==========================================================
# HYBRID DECRYPTION
# ==========================================================
@app.post("/decrypt-file")
async def decrypt_file(file:UploadFile=File(...),decryption_key:str=Form(...), email:str=Form(None)):

    try:
        encrypted_content=await file.read()
        package=json.loads(encrypted_content)

        encrypted_key=base64.b64decode(package["key"])
        nonce=base64.b64decode(package["nonce"])
        ciphertext=base64.b64decode(package["ciphertext"])
        mac=base64.b64decode(package["hmac"])
        original_hash=base64.b64decode(package["hash"])

        # Try to use the user-provided key directly first for AES decryption
        # This is more robust as it doesn't depend on the RSA key being the same
        try:
            aes_key = base64.b64decode(decryption_key.strip())
        except Exception:
             raise HTTPException(400,"Invalid Decryption Key Format")

        # Verify integrity before decryption
        h=hmac.HMAC(aes_key,hashes.SHA256())
        h.update(ciphertext)
        try:
            h.verify(mac)
        except Exception:
            raise HTTPException(400,"Invalid Decryption Key or Tampered File")

        aes=AESGCM(aes_key)
        decrypted=aes.decrypt(nonce,ciphertext,None)

        digest=hashes.Hash(hashes.SHA3_256())
        digest.update(decrypted)
        new_hash=digest.finalize()

        if new_hash!=original_hash:
            raise HTTPException(400,"Tamper detected")

        original_filename = package.get("original_filename", "decrypted_file")
        unique_suffix = str(uuid.uuid4())[:8]
        temp_filename = f"dec_temp_{unique_suffix}_{original_filename}"
        path = os.path.join(BASE_UPLOAD_FOLDER, temp_filename)

        with open(path,"wb") as f:
            f.write(decrypted)

        # Decryption Notification
        if email:
            try:
                connection=get_connection()
                cursor=connection.cursor(dictionary=True)
                cursor.execute("SELECT id FROM users WHERE email=%s",(email,))
                user=cursor.fetchone()
                if user:
                    add_notification(user["id"], f"File decrypted successfully: {original_filename}")
                cursor.close()
                connection.close()
            except Exception as e:
                print(f"Notification error: {str(e)}")

        return FileResponse(path, filename=original_filename)
    except HTTPException as e:
        raise e
    except Exception as e:
        print(f"Decryption error: {str(e)}")
        raise HTTPException(500, f"Decryption failed: {str(e)}")

# ==========================================================
# USER HISTORY
# ==========================================================
@app.get("/history/{email}")
def get_user_history(email:str):

    connection=get_connection()
    cursor=connection.cursor(dictionary=True)

    cursor.execute("SELECT id FROM users WHERE email=%s",(email,))
    user=cursor.fetchone()

    cursor.execute("""
        SELECT file_name,file_type,file_path,created_at
        FROM files
        WHERE user_id=%s
        ORDER BY created_at DESC
    """,(user["id"],))

    history=cursor.fetchall()

    cursor.close()
    connection.close()

    return {"history":history}

# ==========================================================
# USER NOTIFICATIONS
# ==========================================================
@app.get("/notifications/{email}")
def get_notifications(email:str):

    connection=get_connection()
    cursor=connection.cursor(dictionary=True)

    cursor.execute("SELECT id FROM users WHERE email=%s",(email,))
    user=cursor.fetchone()

    cursor.execute("""
        SELECT message,created_at
        FROM notifications
        WHERE user_id=%s
        ORDER BY created_at DESC
    """,(user["id"],))

    notifications=cursor.fetchall()

    cursor.close()
    connection.close()

    return {"notifications":notifications}

# ==========================================================
# FORGOT PASSWORD
# ==========================================================
@app.post("/forgot-password")
def forgot_password(data:ForgotPassword):
    connection=get_connection()
    cursor=connection.cursor(dictionary=True)

    cursor.execute("SELECT id FROM users WHERE email=%s",(data.email,))
    user=cursor.fetchone()

    if not user:
        cursor.close()
        connection.close()
        raise HTTPException(404,"Email id not registered")

    otp=str(random.randint(100000,999999))
    # Replace the old otp with new otp always
    cursor.execute("UPDATE users SET otp=%s WHERE email=%s",(otp,data.email))
    connection.commit()

    cursor.close()
    connection.close()

    send_otp_email(data.email,otp)

    return {"message":"OTP sent to your email"}

# ==========================================================
# RESEND OTP
# ==========================================================
@app.post("/resend-otp")
def resend_otp(data:ForgotPassword):
    connection=get_connection()
    cursor=connection.cursor(dictionary=True)

    cursor.execute("SELECT * FROM users WHERE email=%s",(data.email,))
    user=cursor.fetchone()

    if not user:
        cursor.close()
        connection.close()
        raise HTTPException(404,"User not found")

    otp=str(random.randint(100000,999999))
    cursor.execute("UPDATE users SET otp=%s WHERE email=%s",(otp,data.email))
    connection.commit()

    cursor.close()
    connection.close()

    send_otp_email(data.email,otp)

    return {"message":"New OTP sent to your email"}

# ==========================================================
# RESET PASSWORD
# ==========================================================
@app.post("/reset-password")
def reset_password(data:ResetPassword):

    if data.new_password != data.confirm_password:
        raise HTTPException(400,"Passwords do not match")

    connection=get_connection()
    cursor=connection.cursor(dictionary=True)

    cursor.execute("SELECT * FROM users WHERE email=%s",(data.email,))
    user=cursor.fetchone()

    if not user:
        cursor.close()
        connection.close()
        raise HTTPException(404,"User not found")

    if user["otp"] != data.otp:
        cursor.close()
        connection.close()
        raise HTTPException(400,"Invalid OTP")

    cursor.execute("UPDATE users SET password=%s, otp_verified=1 WHERE email=%s",(data.new_password,data.email))
    connection.commit()

    add_notification(user["id"], "Your password has been reset successfully. You can now login with your new password.")

    cursor.close()
    connection.close()

    return {"message":"Password updated successfully"}