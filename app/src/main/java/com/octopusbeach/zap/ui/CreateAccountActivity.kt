package com.octopusbeach.zap.ui

import android.app.ProgressDialog
import android.content.Intent
import android.support.v7.app.AppCompatActivity

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import com.firebase.client.AuthData
import com.firebase.client.Firebase
import com.firebase.client.FirebaseError
import com.octopusbeach.zap.R
import com.octopusbeach.zap.ZapApplication

class CreateAccountActivity : AppCompatActivity() {

    private lateinit var email: EditText
    private lateinit var pass: EditText
    private lateinit var pass_confirm: EditText
    private lateinit var createButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)
        email = findViewById(R.id.email_address) as EditText
        pass = findViewById(R.id.password) as EditText
        pass_confirm = findViewById(R.id.password_confirm) as EditText
        createButton = findViewById(R.id.create_account) as Button
        createButton.setOnClickListener { create_account() }
        findViewById(R.id.login)?.setOnClickListener { login() }
    }

    fun create_account() {
        if (!validate()) return

        createButton.isEnabled = false

        val dialog: ProgressDialog = ProgressDialog(this,
                R.style.AppTheme_Dark_Dialog);
        dialog.isIndeterminate = true
        dialog.setMessage("Creating Account...")
        dialog.show()

        val ref: Firebase = (application as ZapApplication).getRef()

        val authHandler:Firebase.AuthResultHandler = object: Firebase.AuthResultHandler {
            override fun onAuthenticated(authData: AuthData) {
                val map = mutableMapOf("provider" to authData.provider)
                ref.child("users").child(authData.uid).setValue(map)
                dialog.dismiss()
                setResult(RESULT_OK, null)
                finish()
            }

            override fun onAuthenticationError(firebaseError: FirebaseError) {
                dialog.dismiss()
                failure(firebaseError.message)
            }
        }
        val handler:Firebase.ResultHandler  = object:Firebase.ResultHandler {
            override fun onError(err: FirebaseError) {
                dialog.dismiss()
                failure(err.message)
            }

            override fun onSuccess() {
                dialog.setMessage("Logging in...")
                ref.authWithPassword(email.text.toString(), pass.text.toString(), authHandler)
            }
        }
        // Create account, then log in.
        ref.createUser(email.text.toString(), pass.text.toString(), handler)

    }

    fun login() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    private fun failure(message:String) {
        Snackbar.make(findViewById(R.id.linear) as View, message, Snackbar.LENGTH_SHORT).show()
        createButton.isEnabled = true
    }

    private fun validate():Boolean {
        val password = pass.text.toString()
        val password_confirm = pass_confirm.text.toString()
        val emailAddr = email.text.toString()

        var valid = true;

        if (emailAddr.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(emailAddr).matches()) {
            // email not correct.
            email.error = "enter a valid email"
            valid = false
        } else email.error = null // got cleared up.

        if(password.isEmpty() || password.length < 5 || password.length > 12) {
            pass.error = "must be between 5 and 12 characters"
            valid = false
        } else pass.error = null // got cleared up.

        if(!password_confirm.equals(password)) {
            pass_confirm.error = "passwords do not match"
            valid = false
        } else pass_confirm.error = null

        return valid
    }
}