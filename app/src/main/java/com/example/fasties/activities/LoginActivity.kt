package com.example.fasties.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.fasties.MainActivity
import com.example.fasties.R
import com.example.fasties.databinding.ActivityLoginBinding
import com.example.fasties.databinding.DialogOtpBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.TimeUnit


class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private var storedVerificationId: String? = null
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private var otpAlreadySent = false
    private var countdownTimer: CountDownTimer? = null
    private var currentPhoneNumber: String = ""
    private val GOOGLE_SIGN_IN_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://fasties-ec015-default-rtdb.asia-southeast1.firebasedatabase.app")
        database.setPersistenceEnabled(true)

        val prefs = getSharedPreferences("FastiesPrefs", Context.MODE_PRIVATE)

        if (prefs.getBoolean("rememberMe", false) && auth.currentUser != null) {
            val username = prefs.getString("username", "User")
            startMainActivity(username ?: "User")
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var isPasswordVisible = false
        binding.togglePasswordVisibility.setOnClickListener {
            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) {
                binding.passwordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.togglePasswordVisibility.setImageResource(R.drawable.ic_visibility)
            } else {
                binding.passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.togglePasswordVisibility.setImageResource(R.drawable.ic_visibility_off)
            }

            binding.passwordEditText.setSelection(binding.passwordEditText.text?.length ?: 0)
        }

        binding.loginButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val rememberMe = binding.rememberMeCheckbox.isChecked

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // First check username in database to get email
            val usersRef = database.getReference("users")
            usersRef.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val user = snapshot.children.first()
                            val email = user.child("email").value.toString()

                            // Sign in with email and password
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        // Save remember me preference
                                        prefs.edit().apply {
                                            putString("username", username)
                                            putBoolean("rememberMe", rememberMe)
                                            apply()
                                        }
                                        startMainActivity(username)
                                        finish()
                                    } else {
                                        Toast.makeText(
                                            this@LoginActivity,
                                            "Login failed: ${task.exception?.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        } else {
                            Toast.makeText(this@LoginActivity, "Username not found", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@LoginActivity, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        binding.signUpButton.setOnClickListener {
            showSignupDialog()
        }

        binding.forgotCredentialsTextView.setOnClickListener {
            showForgotDialog()
        }

        binding.loginButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val usersRef = database.getReference("users")
            usersRef.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val user = snapshot.children.first()
                            val email = user.child("email").value.toString()

                            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener {

                                    val prefs = getSharedPreferences("FastiesPrefs", MODE_PRIVATE)
                                    prefs.edit().putString("username", username).apply()

                                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this@LoginActivity, "Login failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(this@LoginActivity, "Username not found", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@LoginActivity, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        // Login via Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Make sure this is in strings.xml
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.googleSignInButton.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, GOOGLE_SIGN_IN_REQUEST_CODE)
            }
        }

        // Login via Phone as Guest
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                otpAlreadySent = false
                Toast.makeText(this@LoginActivity, "OTP failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                storedVerificationId = verificationId
                resendToken = token
                otpAlreadySent = true
                showOtpDialog()
                startCountdown()
                Toast.makeText(this@LoginActivity, "OTP Sent", Toast.LENGTH_SHORT).show()
            }
        }

        // Send OTP
        binding.sendOtpButton.setOnClickListener {
            if (otpAlreadySent) {
                Toast.makeText(this, "OTP already sent! Please wait.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val phone = binding.phoneEditText.text.toString().trim()
            val countryCode = binding.ccp.selectedCountryCodeWithPlus
            val fullPhone = countryCode + phone
            currentPhoneNumber = fullPhone

            if (phone.isEmpty()) {
                Toast.makeText(this, "Enter phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(fullPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        }
    }

    private fun showOtpDialog() {
        val dialogBinding = DialogOtpBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setTitle("Enter OTP")
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialogBinding.verifyOtpButton.setOnClickListener {
            val code = dialogBinding.otpEditText.text.toString().trim()
            if (code.isEmpty() || storedVerificationId == null) {
                Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, code)
            signInWithPhoneAuthCredential(credential)
            dialog.dismiss()
        }

        dialogBinding.resendOtpButton.setOnClickListener {
            if (currentPhoneNumber.isEmpty()) {
                Toast.makeText(this, "Phone number missing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(currentPhoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .setForceResendingToken(resendToken)
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
            startDialogCountdown(dialogBinding)
        }
        dialog.setOnShowListener {
            startDialogCountdown(dialogBinding)
        }
        dialog.show()
    }

    private fun startDialogCountdown(dialogBinding: DialogOtpBinding) {
        dialogBinding.resendOtpButton.isEnabled = false

        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                dialogBinding.otpTimerTextView.post {
                    dialogBinding.otpTimerTextView.text = "Resend available in ${secondsLeft}s"
                }

            }

            override fun onFinish() {
                dialogBinding.otpTimerTextView.post {
                    dialogBinding.otpTimerTextView.text = ""
                    dialogBinding.resendOtpButton.isEnabled = true
                }
            }
        }.start()
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
//                Toast.makeText(this, "Phone Login Successful", Toast.LENGTH_SHORT).show()
                startMainActivity("Sagnik")
            }
            .addOnFailureListener {
                Toast.makeText(this, "Verification Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                otpAlreadySent = false
            }
    }

    private fun startMainActivity(username: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("username", username)
        startActivity(intent)
        finish()
    }

    private fun startCountdown() {
        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.sendOtpButton.text = "Wait (${millisUntilFinished / 1000}s)"
                binding.sendOtpButton.isEnabled = false
            }

            override fun onFinish() {
                otpAlreadySent = false
                binding.sendOtpButton.text = "Send OTP"
                binding.sendOtpButton.isEnabled = true
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GOOGLE_SIGN_IN_REQUEST_CODE) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnSuccessListener {
                        val username = account.displayName ?: "Google User"
                        val uid = auth.currentUser?.uid ?: return@addOnSuccessListener
                        FirebaseDatabase.getInstance().getReference("users").child(uid)
                            .child("username").setValue(username)

                        val prefs = getSharedPreferences("FastiesPrefs", Context.MODE_PRIVATE)
                        prefs.edit().putString("username", username).putBoolean("remember", true).apply()
                        startMainActivity(username)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Google Sign-In Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun showSignupDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_signup, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.show()

        val emailEditText = dialogView.findViewById<EditText>(R.id.emailEditText)
        val usernameEditText = dialogView.findViewById<EditText>(R.id.usernameEditText)
        val passwordEditText = dialogView.findViewById<EditText>(R.id.passwordEditText)
        val confirmPasswordEditText = dialogView.findViewById<EditText>(R.id.confirmPasswordEditText)
        val togglePasswordVisibility = dialogView.findViewById<ImageView>(R.id.togglePasswordVisibility)
        val signupButton = dialogView.findViewById<Button>(R.id.signupButton)

        var isPasswordVisible = false

        togglePasswordVisibility.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            val inputType = if (isPasswordVisible) InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            passwordEditText.inputType = inputType
            confirmPasswordEditText.inputType = inputType
            togglePasswordVisibility.setImageResource(
                if (isPasswordVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
            )
        }

        signupButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    val uid = it.user?.uid ?: return@addOnSuccessListener
                    FirebaseDatabase.getInstance().getReference("users").child(uid).setValue(
                        mapOf("username" to username, "email" to email)
                    )
                    Toast.makeText(this, "Sign-up successful!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Signup failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showForgotDialog() {
        val options = arrayOf("Recover Username", "Reset Password")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Forgot Credentials?")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> recoverUsernameDialog()
                1 -> resetPasswordDialog()
            }
        }
        builder.show()
    }

    private fun recoverUsernameDialog() {
        val input = EditText(this).apply {
            hint = "Enter your email"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }

        AlertDialog.Builder(this)
            .setTitle("Recover Username")
            .setView(input)
            .setPositiveButton("Search") { dialog, _ ->
                val email = input.text.toString().trim()
                if (email.isEmpty()) {
                    Toast.makeText(this, "Email required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                searchUsername(email)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showUsernameResetDialog(email: String) {
        val input = EditText(this).apply {
            hint = "Enter new username"
            setSingleLine(true)
        }

        AlertDialog.Builder(this)
            .setTitle("Set New Username")
            .setMessage("No username found for this account. Please create a new one:")
            .setView(input)
            .setPositiveButton("Save") { dialog, _ ->
                val newUsername = input.text.toString().trim()
                if (newUsername.isEmpty()) {
                    Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                updateUsername(email, newUsername)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun searchUsername(email: String) {
        Log.d("Recovery", "Starting search for email: $email")
        val dbRef = FirebaseDatabase.getInstance().getReference("users")

        dbRef.orderByChild("email").equalTo(email.lowercase())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("Recovery", "Received snapshot with ${snapshot.childrenCount} children")

                    if (snapshot.exists()) {
                        Log.d("Recovery", "Email exists in database")
                        var found = false

                        snapshot.children.forEach { userSnapshot ->
                            Log.d("Recovery", "Checking user: ${userSnapshot.key}")
                            val username = userSnapshot.child("username").value?.toString()

                            if (username != null) {
                                Log.d("Recovery", "Found username: $username")
                                found = true
                                AlertDialog.Builder(this@LoginActivity)
                                    .setTitle("Username Found")
                                    .setMessage("Your username is:\n\n$username")
                                    .setPositiveButton("OK", null)
                                    .show()
                                return
                            }
                        }

                        if (!found) {
                            Log.d("Recovery", "Email exists but username is null")
                            showUsernameResetDialog(email)
                        }
                    } else {
                        Log.d("Recovery", "No account found with this email")
                        Toast.makeText(
                            this@LoginActivity,
                            "No account found with this email",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Recovery", "Database error: ${error.message}")
                    Toast.makeText(
                        this@LoginActivity,
                        "Error: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun updateUsername(email: String, newUsername: String) {
        val dbRef = FirebaseDatabase.getInstance().getReference("users")

        // First check if username already exists
        dbRef.orderByChild("username").equalTo(newUsername)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(usernameSnapshot: DataSnapshot) {
                    if (usernameSnapshot.exists()) {
                        Toast.makeText(
                            this@LoginActivity,
                            "Username already taken",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        // If username is available, proceed with update
                        dbRef.orderByChild("email").equalTo(email.lowercase())
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(emailSnapshot: DataSnapshot) {
                                    if (emailSnapshot.exists()) {
                                        val userKey = emailSnapshot.children.first().key
                                        userKey?.let {
                                            dbRef.child(it).child("username").setValue(newUsername)
                                                .addOnSuccessListener {
                                                    Toast.makeText(
                                                        this@LoginActivity,
                                                        "Username updated successfully!",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(
                                                        this@LoginActivity,
                                                        "Failed to update username: ${it.message}",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                        }
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "Error: ${error.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Error checking username: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun resetPasswordDialog() {
        val input = EditText(this)
        input.hint = "Enter your email"
        val builder = AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setView(input)
            .setPositiveButton("Send Reset Link") { dialog, _ ->
                val email = input.text.toString().trim()
                if (email.isEmpty()) {
                    Toast.makeText(this, "Email required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Reset link sent to $email", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

}



















//    private fun showForgotPasswordDialog() {
//        val builder = AlertDialog.Builder(this)
//        builder.setTitle("Reset Password")
//
//        val input = EditText(this)
//        input.hint = "Enter your registered email"
//        input.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
//        builder.setView(input)
//
//        builder.setPositiveButton("Send Reset Link") { dialog, _ ->
//            val email = input.text.toString().trim()
//            if (email.isNotEmpty()) {
//
//                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
//                    .addOnSuccessListener {
//                        Toast.makeText(this, "Password reset link sent to $email", Toast.LENGTH_LONG).show()
//                        dialog.dismiss()
//                    }
//                    .addOnFailureListener {
//                        Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
//                    }
//            }
//            else {
//                Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        builder.setNegativeButton("Cancel") { dialog, _ ->
//            dialog.cancel()
//        }
//
//        builder.show()
//    }

// cgpt code
//    private fun recoverUsernameDialog() {
//        val input = EditText(this)
//        input.hint = "Enter your email"
//        input.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
//
//        AlertDialog.Builder(this)
//            .setTitle("Recover Username")
//            .setView(input)
//            .setPositiveButton("Recover", null)
//            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
//            .create().apply {
//                setOnShowListener {
//                    val recoverButton = getButton(AlertDialog.BUTTON_POSITIVE)
//                    recoverButton.setOnClickListener {
//                        val email = input.text.toString().trim()
//                        if (email.isEmpty()) {
//                            Toast.makeText(this@LoginActivity, "Email required", Toast.LENGTH_SHORT).show()
//                            return@setOnClickListener
//                        }
//
//                        Toast.makeText(this@LoginActivity, "Searching for email...", Toast.LENGTH_SHORT).show()
//                        Log.d("RecoverUsername", "Searching for: $email")
//
//                        // 🔧 Use correct path — change "users" if your root is different
//                        val dbRef = FirebaseDatabase.getInstance().getReference("users")
//
//                        dbRef.get()
//                            .addOnSuccessListener { snapshot ->
//                                Log.d("RecoverUsername", "Snapshot received: $snapshot")
//
//                                var found = false
//                                for (child in snapshot.children) {
//                                    val userEmail = child.child("email").value?.toString()
//                                    if (userEmail.equals(email, ignoreCase = true)) {
//                                        val username = child.child("username").value?.toString() ?: "Unknown"
//                                        AlertDialog.Builder(this@LoginActivity)
//                                            .setTitle("Username Found")
//                                            .setMessage("Your username is: $username")
//                                            .setPositiveButton("OK", null)
//                                            .show()
//                                        found = true
//                                        break
//                                    }
//                                }
//
//                                if (!found) {
//                                    Toast.makeText(this@LoginActivity, "No username found for that email", Toast.LENGTH_SHORT).show()
//                                }
//                            }
//                            .addOnFailureListener {
//                                Log.e("RecoverUsername", "Failed to get data: ${it.message}", it)
//                                Toast.makeText(this@LoginActivity, "Error: ${it.message}", Toast.LENGTH_LONG).show()
//                            }
//                    }
//                }
//            }.show()
//    }

// Login via Username and Password
//        binding.loginButton.setOnClickListener {
//            val username = binding.usernameEditText.text.toString().trim()
//            val password = binding.passwordEditText.text.toString().trim()
//
//            if (username.isEmpty() || password.isEmpty()) {
//                Toast.makeText(this, "Enter username and password", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            val email = "$username@fasties.com" // Using fake email
//            auth.signInWithEmailAndPassword(email, password)
//                .addOnSuccessListener {
//                    val prefs = getSharedPreferences("FastiesPrefs", Context.MODE_PRIVATE)
//                    prefs.edit().putString("username", username)
//                        .putBoolean("remember", binding.rememberMeCheckbox.isChecked)
//                        .apply()
//
//                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
//                    startMainActivity(username)
//                }
//                .addOnFailureListener {
//                    Toast.makeText(this, "Login failed: ${it.message}", Toast.LENGTH_SHORT).show()
//                }
//        }

//        binding.loginButton.setOnClickListener {
//            val username = binding.usernameEditText.text.toString().trim()
//            val password = binding.passwordEditText.text.toString().trim()
//
//            if (username.isEmpty() || password.isEmpty()) {
//                Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            // Find user by username
//            val usersRef = FirebaseDatabase.getInstance().getReference("users")
//            usersRef.orderByChild("username").equalTo(username)
//                .addListenerForSingleValueEvent(object : ValueEventListener {
//                    override fun onDataChange(snapshot: DataSnapshot) {
//                        if (snapshot.exists()) {
//                            val user = snapshot.children.first()
//                            val email = user.child("email").value.toString()
//
//                            // Now log in using email and password
//                            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
//                                .addOnSuccessListener {
//                                    // Save username to SharedPreferences
//                                    val prefs = getSharedPreferences("FastiesPrefs", MODE_PRIVATE)
//                                    prefs.edit().putString("username", username).apply()
//
//                                    // Navigate to MainActivity
//                                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
//                                    finish()
//                                }
//                                .addOnFailureListener {
//                                    Toast.makeText(this@LoginActivity, "Login failed: ${it.message}", Toast.LENGTH_SHORT).show()
//                                }
//                        } else {
//                            Toast.makeText(this@LoginActivity, "Username not found", Toast.LENGTH_SHORT).show()
//                        }
//                    }
//
//                    override fun onCancelled(error: DatabaseError) {
//                        Toast.makeText(this@LoginActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
//                    }
//                })
//        }