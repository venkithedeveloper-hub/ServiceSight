package com.ats.servicesight

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ats.servicesight.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private lateinit var logPage : ActivityLoginBinding
    private val fbAuth : FirebaseAuth = FirebaseAuth.getInstance()
    private var db : FirebaseFirestore = FirebaseFirestore.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        logPage = ActivityLoginBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(logPage.root)

        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = this.resources.getColor(R.color.heading)

        fbAuth.signOut()

        logPage.btnLog.setOnClickListener {
            if(logPage.logUsr.text.toString() == "demo@caa.cbe" && logPage.logPwd.text.toString() == "pitcrew") {
                loginUsr("demo@caa.cbe", "pitcrew")
            } else {
                signinWithFireStore(
                    logPage.logUsr.text.toString(),
                    logPage.logPwd.text.toString()
                )
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    @SuppressLint("HardwareIds")
    private fun signinWithFireStore(usrEml : String, usrPwd : String){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
            .setMessage("User has been checking & creating.")
            .setTitle("Please wait.......")
        var dialog: AlertDialog = builder.create()
        dialog = builder.create()
        dialog.show()
        var uid : String
        val cmp = usrEml.substring(usrEml.indexOf(".") + 1, usrEml.length)
        val usr = usrEml.substring(0,usrEml.indexOf("@"))
        db.collection(cmp)
            .document("masterdata")
            .collection("user")
            .whereEqualTo("username", usr)
            .whereEqualTo("company", cmp)
            .whereEqualTo("password", usrPwd)
            .whereEqualTo("sts", 1)
            .get().addOnSuccessListener { loginquery ->
                if (loginquery.isEmpty) {
                    Toast.makeText(applicationContext, "User does not exist.", Toast.LENGTH_SHORT).show()
                    logPage.btnLog.isEnabled = true
                    dialog.dismiss()
                } else {
                    val usrdoc = loginquery.documents[0]
                    //Log.d("user", usrdoc.get("nme") as String)
                    uid = usrdoc.get("uid") as String
                    when (uid) {
                        "" -> {
                            db.collection(cmp)
                                .document("masterdata")
                                .collection("user")
                                .whereEqualTo("uid", Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID))
                                .get().addOnSuccessListener { uidquery ->
                                    dialog.dismiss()
                                    if (!uidquery.isEmpty) {
                                        Toast.makeText(applicationContext, "User already connected with other mobile.", Toast.LENGTH_SHORT).show()
                                        logPage.btnLog.isEnabled = true
                                    } else {
                                        db.collection(cmp)
                                            .document("masterdata")
                                            .collection("user")
                                            .document((usrdoc.get("id") as Number).toString())
                                            .update(mapOf(
                                                "uid" to Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                                            )).addOnSuccessListener {
                                                //loginUsr((usrdoc.get("id") as Number).toInt(), (usrdoc.get("brnid") as Number).toInt(), usrdoc.get("cmp") as String)
                                                loginUsr(logPage.logUsr.text.toString(), logPage.logPwd.text.toString())
                                            }
                                    }
                                }
                        }
                        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) -> {
                            Log.d("TAG", "User already connected with other mobile.")
                            //loginUsr((usrdoc.get("id") as Number).toInt(), (usrdoc.get("brnid") as Number).toInt(), usrdoc.get("cmp") as String)
                            loginUsr(logPage.logUsr.text.toString(), logPage.logPwd.text.toString())
                        }
                        else -> {
                            dialog.dismiss()
                            Toast.makeText(applicationContext, "User already connected with other mobile.", Toast.LENGTH_SHORT).show()
                            logPage.btnLog.isEnabled = true
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w("TAG", "Error getting documents:", e)
            }
    }

    private fun loginUsr(usrId : String, pwd : String) {
        fbAuth.signInWithEmailAndPassword(usrId, pwd)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val hmeIntt = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(hmeIntt)
                    finish()
                } else {
                    createUsr(usrId, pwd)
                }
            }
    }


    private fun createUsr(usrId : String, pwd : String) {
        fbAuth.createUserWithEmailAndPassword(usrId, pwd).addOnCompleteListener { logtask ->
            if(logtask.isSuccessful){
                loginUsr(usrId, pwd)
            }else{
                Toast.makeText(applicationContext,"error on create user", Toast.LENGTH_SHORT).show()
                logPage.btnLog.isEnabled = true
            }
        }
    }
}