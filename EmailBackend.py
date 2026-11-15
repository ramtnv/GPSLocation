"""
EmailBackend.py – Simple Flask backend for silent email sending.
Run:  python EmailBackend.py
Then set BACKEND_URL in LocationEmailScreen.kt to http://<your-ip>:8080/send-email
"""

import os
import smtplib
from email.message import EmailMessage
from flask import Flask, request, jsonify

app = Flask(__name__)

# ── SMTP config – set these environment variables before running ────────────
SMTP_HOST = os.getenv("SMTP_HOST", "smtp.gmail.com")
SMTP_PORT = int(os.getenv("SMTP_PORT", 587))
SMTP_USER = os.getenv("SMTP_USER", "")   # your Gmail / SMTP address
SMTP_PASS = os.getenv("SMTP_PASS", "")   # Gmail App Password


@app.route("/send-email", methods=["POST"])
def send_email():
    data = request.get_json(silent=True)
    if not data:
        return jsonify({"error": "Invalid or missing JSON body"}), 400

    recipient = data.get("recipient", "").strip()
    subject   = data.get("subject",   "").strip()
    body      = data.get("body",      "").strip()

    if not all([recipient, subject, body]):
        return jsonify({"error": "Missing fields: recipient, subject, body"}), 400

    try:
        msg = EmailMessage()
        msg["From"]    = SMTP_USER
        msg["To"]      = recipient
        msg["Subject"] = subject
        msg.set_content(body)

        with smtplib.SMTP(SMTP_HOST, SMTP_PORT) as server:
            server.starttls()
            server.login(SMTP_USER, SMTP_PASS)
            server.send_message(msg)

        print(f"[OK] Email sent to {recipient}")
        return jsonify({"status": "sent"}), 200

    except Exception as exc:
        print(f"[ERR] {exc}")
        return jsonify({"error": str(exc)}), 500


if __name__ == "__main__":
    if not SMTP_USER or not SMTP_PASS:
        print("WARNING: SMTP_USER / SMTP_PASS env vars are not set!")
    print(f"Backend listening on http://0.0.0.0:8080  (SMTP: {SMTP_HOST}:{SMTP_PORT})")
    app.run(host="0.0.0.0", port=int(os.getenv("PORT", 8080)), debug=False)
