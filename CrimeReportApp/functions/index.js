const functions = require('firebase-functions');
const admin = require('firebase-admin');
const nodemailer = require('nodemailer');

// Initialize Firebase Admin (if not already done)
admin.initializeApp();

// Create a transporter using SendGrid (free tier available)
const transporter = nodemailer.createTransport({
  service: 'SendGrid',
  auth: {
    user: 'apikey', // Your SendGrid username (usually 'apikey')
    pass: 'YOUR_SENDGRID_API_KEY' // Your SendGrid API key
  }
});

exports.sendOtpEmail = functions.https.onCall(async (data, context) => {
  const { email, otp } = data;

  const mailOptions = {
    from: 'your-email@example.com', // Your verified SendGrid sender email
    to: email,
    subject: 'Password Reset OTP',
    text: `Your OTP for password reset is: ${otp}

    This OTP will expire in 10 minutes.
    If you did not request a password reset, please ignore this email.`,
    html: `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
        <h2>Password Reset</h2>
        <p>Your One-Time Password (OTP) is:</p>
        <h1 style="background-color: #f0f0f0; padding: 10px; text-align: center;">${otp}</h1>
        <p>This OTP will expire in 10 minutes.</p>
        <p>If you did not request a password reset, please ignore this email.</p>
      </div>
    `
  };

  try {
    await transporter.sendMail(mailOptions);
    return { success: true };
  } catch (error) {
    console.error('Error sending email:', error);
    throw new functions.https.HttpsError('internal', 'Failed to send OTP email');
  }
});