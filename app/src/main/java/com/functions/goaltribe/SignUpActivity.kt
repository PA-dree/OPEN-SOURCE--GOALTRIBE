package com.functions.goaltribe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton // This button executes the primary action (Log In or Sign Up)
    private lateinit var btnSignUp: MaterialButton // This button acts as the toggle
    private lateinit var btnGoogle: MaterialButton

    private var isLoginMode = true // Track login/signup state. Start in Login Mode.

    // Constant for Log Tag
    private val TAG = "SignUpActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        FirebaseApp.initializeApp(this)

        // Firebase Auth
        auth = FirebaseAuth.getInstance()

        // View references
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        btnSignUp = findViewById(R.id.btn_sign_up) // Used as a toggle button
        btnGoogle = findViewById(R.id.btn_google_sign_in)

        // Google Sign-In setup
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initial UI state setup - Reflects 'isLoginMode = true'
        updateUiForMode()

        // Email login/signup logic
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isLoginMode) {
                loginUser(email, password)
            } else {
                registerUser(email, password)
            }
        }

        // Toggle between Login and Signup modes
        btnSignUp.setOnClickListener {
            isLoginMode = !isLoginMode
            updateUiForMode()
            Toast.makeText(this, if (isLoginMode) "Switched to Login Mode" else "Switched to Sign Up Mode", Toast.LENGTH_SHORT).show()
        }

        // Google sign-in button
        btnGoogle.setOnClickListener {
            signInWithGoogle()
        }
    }

    // Helper function to update the button texts based on the current mode
    private fun updateUiForMode() {
        if (isLoginMode) {
            btnLogin.text = "Log In"
            btnSignUp.text = "Don't have an account? Sign Up"
        } else {
            btnLogin.text = "Create Account"
            btnSignUp.text = "Already have an account? Log In"
        }
    }

    // Firebase Email/Password Login
    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Login success for ${auth.currentUser?.email}")
                    Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                    goToHomeScreen()
                } else {
                    Log.e(TAG, "Login failed: ${task.exception?.message}")
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Firebase Email/Password Sign-Up
    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Sign Up success for ${auth.currentUser?.email}")
                    Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
                    goToHomeScreen()
                } else {
                    Log.e(TAG, "Sign up failed: ${task.exception?.message}")
                    Toast.makeText(this, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Start Google Sign-In
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    // Handle Google Sign-In result
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            Log.d(TAG, "Google sign-in succeeded, token: ${account.idToken}")
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            // FIX: Using the defined TAG constant
            Log.w(TAG, "Google sign-in failed", e)
            Toast.makeText(this, "Google sign-in failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    // Authenticate Google account with Firebase
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Firebase auth with Google success for ${auth.currentUser?.email}")
                    Toast.makeText(this, "Signed in with Google", Toast.LENGTH_SHORT).show()
                    goToHomeScreen()
                } else {
                    Log.e(TAG, "Google authentication failed: ${task.exception?.message}")
                    Toast.makeText(this, "Google authentication failed", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Go to home screen (after login/signup)
    private fun goToHomeScreen() {
        // You should verify that MainActivity is the correct next screen.
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}