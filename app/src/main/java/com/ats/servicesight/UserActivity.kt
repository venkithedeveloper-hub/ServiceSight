package com.ats.servicesight

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ats.servicesight.databinding.ActivityUserBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class UserActivity : AppCompatActivity() {
    private var db : FirebaseFirestore = FirebaseFirestore.getInstance()
    private val fbAuth : FirebaseAuth = FirebaseAuth.getInstance()
    private var currentUser = fbAuth.currentUser
    private var usr : String = ""
    private var cmp : String = ""
    private var entId : String = "0"
    private var brnNList = ArrayList<String>()
    private var brnIList = ArrayList<String>()
    private var brnId : String = "0"
    private var brn : String = ""
    private var typNList = ArrayList<String>()
    private var typIList = ArrayList<String>()
    private var typId : String = "0"
    private lateinit var usrPage : ActivityUserBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        usrPage = ActivityUserBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(usrPage.root)

        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = this.resources.getColor(R.color.heading)
        val hmeIntt = Intent(this@UserActivity, RecyclerViewActivity::class.java)
        entId = intent.getStringExtra("entNo").toString()

        currentUser = fbAuth.currentUser
        usr = fbAuth.currentUser?.email.toString()
        //Log.d("user", usr)
        cmp = if (usr != "" && usr != "null") {
            usr.substring(usr.indexOf(".") + 1, usr.length)
        } else {
            ""
        }

        if (entId == "0") {
            loadAll(cmp)
        } else {
            loadDatas(cmp)
            loadData(entId.toInt())
        }

        usrPage.btnBack.setOnClickListener {
            hmeIntt.putExtra("type", "user")
            startActivity(hmeIntt)
            finish()
        }

        usrPage.spnrType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>,
                                        view: View, position: Int, id: Long) {
                typId = typIList[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                typId = "0"
            }
        }

        usrPage.spnrBranch.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>,
                                        view: View, position: Int, id: Long) {
                brnId = brnIList[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                brnId = "0"
            }
        }

        usrPage.btnUpd.setOnClickListener {
            if (typId == "0" || brnId == "0" || usrPage.txtName.text.toString() == "" || usrPage.txtUserName.text.toString() == "" || usrPage.txtPwd.text.toString() == "") {
                Toast.makeText(this, "Please check the details", Toast.LENGTH_SHORT).show()
            } else {
                usrPage.btnUpd.isEnabled = true
                addUser()
            }
        }

        val onBackPressedCallback = object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                hmeIntt.putExtra("type", "user")
                startActivity(hmeIntt)
                finish()
            }
        }
        this.onBackPressedDispatcher.addCallback(onBackPressedCallback)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadAll(cmp : String) {
        loadDatas(cmp)
        brnId = "0"
        typId = "0"
        usrPage.txtName.setText("")
        usrPage.txtUserName.setText("")
        usrPage.txtPwd.setText("")
    }

    private fun loadDatas(cmp : String) {
        loadType(brnNList, brnIList, usrPage.spnrBranch, cmp)
        typNList.clear()
        typNList.add("Select User type")
        typNList.add("Service Advisor")
        typNList.add("Team Lead")
        typIList.clear()
        typIList.add("0")
        typIList.add("3")
        typIList.add("2")
        val adapter = ArrayAdapter(this@UserActivity, R.layout.spinner_style, typNList)
        adapter.setDropDownViewResource(R.layout.spinner_style)
        usrPage.spnrType.adapter = adapter
    }

    private fun loadData(entId : Int) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
            .setMessage("Your data is fetching")
            .setTitle("Please wait.......")

        val dialog: AlertDialog = builder.create()
        dialog.show()
        runAfterDelay(2000) {
            db.collection(cmp)
                .document("masterdata")
                .collection("user")
                .whereEqualTo("id", entId).get()
                .addOnSuccessListener { qrySnapshot ->
                    val document = qrySnapshot.documents[0]
                    usrPage.txtName.setText((document.get("name") as String).toString())
                    usrPage.txtMobile.setText((document.get("mobile") as String).toString())
                    usrPage.txtUserName.setText((document.get("username") as String).toString())
                    usrPage.txtPwd.setText((document.get("password") as String).toString())
                    brnId = (document.get("branch") as Number).toString()
                    typId = (document.get("type") as Number).toString()
                    setSpinnerData(usrPage.spnrBranch, brnIList, brnId)
                    setSpinnerData(usrPage.spnrType, typIList, typId)
                    dialog.dismiss()
                }
        }
    }

    private fun loadType(nameList : ArrayList<String>, idList : ArrayList<String>, spnr : Spinner, cmp : String) {
        nameList.clear()
        nameList.add("Select a Branch")
        idList.clear()
        idList.add("0")
        db.collection(cmp)
            .document("masterdata")
            .collection("branch")
            .whereEqualTo("sts", 1)
            .orderBy("name", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val documents = task.result?.documents ?: emptyList()
                    for (document in documents) {
                        val data = document.data ?: continue
                        nameList.add(data["name"].toString().uppercase(Locale.getDefault()))
                        idList.add(data["id"].toString())
                    }
                } else {
                    Log.d("error", "Error getting documents: ", task.exception)
                }
                val adapter = ArrayAdapter(this@UserActivity, R.layout.spinner_style, nameList)
                adapter.setDropDownViewResource(R.layout.spinner_style)
                spnr.adapter = adapter
            }
    }

    private fun addUser() {
        brn = usrPage.txtUserName.text.toString().lowercase(Locale.getDefault()) + "@" +
                usrPage.spnrBranch.selectedItem.toString().lowercase(Locale.getDefault()) + "." +
                cmp.lowercase(Locale.getDefault())
        db.collection(cmp)
            .document("masterdata")
            .collection("user")
            .whereEqualTo("username", usrPage.txtUserName.text.toString())
            .whereEqualTo("sts", 1)
            .get().addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    if (entId == "0") {
                        db.collection(cmp)
                            .document("masterdata")
                            .collection("user")
                            .orderBy("id", com.google.firebase.firestore.Query.Direction.ASCENDING)
                            .limitToLast(1).get()
                            .addOnSuccessListener { qrySnapshot ->
                                if (qrySnapshot.isEmpty) {
                                    addData(1)
                                } else {
                                    val document = qrySnapshot.documents[0]
                                    //cmnCls.tmp = document.get("id") as Number
                                    val aId : Number = document.get("id") as Number
                                    val aid = aId.toInt() + 1
                                    Log.d("name", aid.toString())
                                    addData(aid)
                                }
                            }
                    } else {
                        updateData(entId.toInt())
                    }

                } else {
                    if (entId == "0") {
                        Toast.makeText(applicationContext,"Details already exist.", Toast.LENGTH_SHORT).show()
                    } else {
                        updateData(entId.toInt())
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.d("nme failure", e.message.toString())
            }
    }

    private fun addData(tid : Number){
        brn = usrPage.txtUserName.text.toString().lowercase(Locale.getDefault()) + "@" +
                usrPage.spnrBranch.selectedItem.toString().lowercase(Locale.getDefault()) + "." +
                cmp.lowercase(Locale.getDefault())
        val data = hashMapOf(
            "branch"   to brnId.toInt(),
            "company"  to cmp,
            "id"       to tid.toInt(),
            "name"     to usrPage.txtName.text.toString(),
            "mobile"   to usrPage.txtMobile.text.toString(),
            "password" to usrPage.txtPwd.text.toString(),
            "sts"      to 1,
            "type"     to typId.toInt(),
            "uid"      to "",
            "user"     to brn,
            "username" to usrPage.txtUserName.text.toString()
        )
        db.collection(cmp)
            .document("masterdata")
            .collection("user")
            .document(tid.toString())
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(applicationContext, "Details successfully added", Toast.LENGTH_SHORT).show()
                val mainIntt = Intent(this@UserActivity, RecyclerViewActivity::class.java)
                mainIntt.putExtra("type", "user")
                startActivity(mainIntt)
                finish()
            }
            .addOnFailureListener { e ->
                Log.w("TAG", "Error deleting document", e)
                Toast.makeText(applicationContext, "Please check the details", Toast.LENGTH_SHORT).show()
                usrPage.btnUpd.isEnabled = true
            }
    }

    private fun updateData(tid : Number){
        brn = usrPage.txtUserName.text.toString().lowercase(Locale.getDefault()) + "@" +
                usrPage.spnrBranch.selectedItem.toString().lowercase(Locale.getDefault()) + "." +
                cmp.lowercase(Locale.getDefault())
        db.collection(cmp)
            .document("masterdata")
            .collection("user")
            .document(tid.toString())
            .update(mapOf(
                "branch"   to brnId.toInt(),
                "company"  to cmp,
                "id"       to tid.toInt(),
                "name"     to usrPage.txtName.text.toString(),
                "mobile"   to usrPage.txtMobile.text.toString(),
                "password" to usrPage.txtPwd.text.toString(),
                "sts"      to 1,
                "type"     to typId.toInt(),
                "uid"      to "",
                "user"     to brn,
                "username" to usrPage.txtUserName.text.toString()
            )).addOnSuccessListener {
                Toast.makeText(applicationContext, "Details successfully updated", Toast.LENGTH_SHORT).show()
                val mainIntt = Intent(this@UserActivity, RecyclerViewActivity::class.java)
                mainIntt.putExtra("type", "user")
                startActivity(mainIntt)
                finish()
            }
            .addOnFailureListener { e ->
                Log.w("TAG", "Error deleting document", e)
                Toast.makeText(applicationContext, "Please check the details", Toast.LENGTH_SHORT).show()
                usrPage.btnUpd.isEnabled = true
            }
    }

    private fun setSpinnerData(spinner : Spinner, idAry : ArrayList<String>, value : String) {
        for (position in 0 until idAry.count()) {
            if (idAry[position] == value) {
                spinner.setSelection(position)
                return
            }
        }
    }

    private fun runAfterDelay(delayMillis: Long, action: () -> Unit) {
        Handler(Looper.getMainLooper()).postDelayed(action, delayMillis)
    }
}