
package com.example.fire1

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    private lateinit var loginContainer: LinearLayout
    private lateinit var profileContainer: LinearLayout
    private lateinit var btnGoogleSignIn: Button
    private lateinit var btnLogout: Button
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvUid: TextView
    private lateinit var database: FirebaseDatabase
    private lateinit var btnReadProfile: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //FIREBASE AUTHENTICATION INSTANCE
        auth = FirebaseAuth.getInstance()

        //FIREBASE DATABASE INSTANCE
        database = FirebaseDatabase.getInstance(
            "https://fire1-c8839-default-rtdb.firebaseio.com/"
        )

        //CREDENTIAL MANAGER INSTANCE
        credentialManager = CredentialManager.create(this)

        //INITIALIZE VIEWS
        btnReadProfile = findViewById(R.id.btnReadProfile)
        loginContainer = findViewById(R.id.loginContainer)
        profileContainer = findViewById(R.id.profileContainer)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        btnLogout = findViewById(R.id.btnLogout)
        tvName = findViewById(R.id.tvName)
        tvEmail = findViewById(R.id.tvEmail)
        tvUid = findViewById(R.id.tvUid)

        //SET LOGIN CLICK LISTENERS
        btnGoogleSignIn.setOnClickListener {
            launchGoogleSignIn()
        }

        //SET LOGOUT CLICK LISTENERS
        btnLogout.setOnClickListener {
            auth.signOut()
            showLogin()
        }

        //SET READ PROFILE CLICK LISTENERS
        btnReadProfile.setOnClickListener {
            readUserProfile()
        }

        if (auth.currentUser != null) {
            showProfile()
        } else {
            showLogin()
        }
    }

    private fun launchGoogleSignIn() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@MainActivity,
                    request = request
                )

                val credential = result.credential

                if (
                    credential is CustomCredential &&
                    credential.type ==
                    GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)

                    firebaseAuthWithGoogle(googleCredential.idToken)
                } else {
                    showMessage("Google account credential not received.")
                }

            } catch (e: Exception) {
                showMessage(
                    "Google Sign-In failed: " +
                            (e.localizedMessage ?: "Unknown error")
                )
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val firebaseCredential =
            GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(firebaseCredential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    appendLoginHistory()
                    showProfile()
                    showMessage("Google Sign-In successful!")
                } else {
                    showMessage(
                        "Firebase sign-in failed: " +
                                (task.exception?.localizedMessage
                                    ?: "Unknown error")
                    )
                }
            }
    }

    private fun showLogin() {
        loginContainer.visibility = View.VISIBLE
        profileContainer.visibility = View.GONE
    }

    private fun showProfile() {
        val user = auth.currentUser

        if (user == null) {
            showLogin()
            return
        }

        loginContainer.visibility = View.GONE
        profileContainer.visibility = View.VISIBLE

        tvName.text = user.displayName ?: "Name unavailable"
        tvEmail.text = user.email ?: "Email unavailable"
        tvUid.text = user.uid
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }


    private fun appendLoginHistory() {
        val user = auth.currentUser ?: return

        val loginEvent = mapOf(
            "event" to "login",
            "uid" to user.uid,
            "email" to (user.email ?: ""),
            "timestamp" to System.currentTimeMillis()
        )

        database.reference
            .child("users")
            .child(user.uid)
            .child("loginHistory")
            .push()
            .setValue(loginEvent)
            .addOnSuccessListener {
                showMessage("Login event appended!")
            }
            .addOnFailureListener { exception ->
                showMessage(
                    "Append failed: ${exception.localizedMessage}"
                )
            }
    }



    private fun readUserProfile() {
        val user = auth.currentUser

        if (user == null) {
            showMessage("Please sign in first.")
            return
        }

        database.reference
            .child("users")
            .child(user.uid)
            .get()
            .addOnSuccessListener { snapshot ->

                if (!snapshot.exists()) {
                    showMessage("No saved profile found.")
                    return@addOnSuccessListener
                }

                val name = snapshot.child("name")
                    .getValue(String::class.java) ?: "Name unavailable"

                val email = snapshot.child("email")
                    .getValue(String::class.java) ?: "Email unavailable"

                val uid = snapshot.child("uid")
                    .getValue(String::class.java) ?: user.uid

                tvName.text = name
                tvEmail.text = email
                tvUid.text = uid

                val loginHistorySnapshot = snapshot.child("loginHistory")
                val loginCount = loginHistorySnapshot.childrenCount

                showMessage(
                    "Profile read successfully! Total login attempts: $loginCount"
                )
            }
            .addOnFailureListener { exception ->
                android.util.Log.e(
                    "DB_READ",
                    "Profile read failed",
                    exception
                )

                showMessage(
                    "Profile read failed: ${exception.localizedMessage}"
                )
            }
    }

}