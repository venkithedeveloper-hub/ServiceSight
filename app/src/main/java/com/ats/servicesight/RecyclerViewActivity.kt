package com.ats.servicesight

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ats.servicesight.adaptor.CustomerAdaptor
import com.ats.servicesight.adaptor.UserAdaptor
import com.ats.servicesight.classes.CustomerClass
import com.ats.servicesight.classes.UserClass
import com.ats.servicesight.databinding.ActivityRecyclerViewBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class RecyclerViewActivity : AppCompatActivity() {
    private var db : FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var rvewPage : ActivityRecyclerViewBinding
    private val fbAuth : FirebaseAuth = FirebaseAuth.getInstance()
    private var currentUser = fbAuth.currentUser
    private var usr : String = ""
    private var cmp : String = ""
    private val cusRpt = ArrayList<CustomerClass>()
    private lateinit var cusAdp : CustomerAdaptor
    private val usrRpt = ArrayList<UserClass>()
    private lateinit var usrAdp : UserAdaptor
    private var typ : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        rvewPage = ActivityRecyclerViewBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(rvewPage.root)

        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = this.resources.getColor(R.color.heading)
        val hmeIntt = Intent(this@RecyclerViewActivity, MainActivity::class.java)

        typ = intent.getStringExtra("type").toString()

        currentUser = fbAuth.currentUser
        usr = fbAuth.currentUser?.email.toString()
        //Log.d("user", usr)
        cmp = if (usr != "" && usr != "null") {
            usr.substring(usr.indexOf(".") + 1, usr.length)
        } else {
            ""
        }

        rvewPage.rcyVew.layoutManager = LinearLayoutManager(
            this@RecyclerViewActivity,
            LinearLayoutManager.VERTICAL,
            false
        )

        rvewPage.rcyVew.setHasFixedSize(true)

        if (typ == "customer") {
            rvewPage.txtVno.isEnabled = true
            rvewPage.btnVehicleSrc.isEnabled = true
            loadCustomer()
        } else if (typ == "user") {
            rvewPage.txtVno.hint = ""
            rvewPage.txtVno.isEnabled = false
            rvewPage.btnVehicleSrc.isEnabled = false
            loadUser()
        }

        rvewPage.btnVehicleSrc.setOnClickListener {
            loadCustomer()
        }

        val pulltoRefresh = findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        pulltoRefresh.setOnRefreshListener {
            if (typ == "customer") {
                loadCustomer()
            } else if (typ == "user") {
                loadUser()
            }
            pulltoRefresh.isRefreshing = false
        }

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT){
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean {
                TODO("Not yet implemented")
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.LEFT){
                    when (typ) {
                        "customer" -> {
                            deleteData(cusAdp.getUsrId(viewHolder.adapterPosition), typ, "Customer")
                        }
                        "user" -> {
                            deleteData(usrAdp.getUsrId(viewHolder.adapterPosition), typ, "User")
                        }
                    }
                } else if (direction == ItemTouchHelper.RIGHT) {
                    when (typ) {
                        "customer" -> {
                            sendAddScreen(cusAdp.getUsrId(viewHolder.adapterPosition))
                        }
                        "user" -> {
                            sendAddScreen(usrAdp.getUsrId(viewHolder.adapterPosition))
                        }
                    }
                }
                if (typ == "customer") {
                    loadCustomer()
                } else if (typ == "user") {
                    loadUser()
                }
            }
            @SuppressLint("UseCompatLoadingForDrawables")
            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val p = Paint()
                    val itemView = viewHolder.itemView
                    if (dX > 0) {
                        //p.color = Color.parseColor("#74E291")
                        p.color = "#EEC759".toColorInt()
                        val background = RectF(itemView.left.toFloat(), itemView.top.toFloat(), dX, itemView.bottom.toFloat())
                        c.drawRect(background, p)
                    } else {
                        p.color = "#FF6868".toColorInt()
                        val background = RectF(itemView.right.toFloat() + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
                        c.drawRect(background, p)
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }).attachToRecyclerView(rvewPage.rcyVew)

        rvewPage.btnBack.setOnClickListener {
            startActivity(hmeIntt)
            finish()
        }

        rvewPage.fltAdd.setOnClickListener {
            sendAddScreen(0)
        }

        val onBackPressedCallback = object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
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

    private fun loadCustomer() {
        rvewPage.txtHeading.setText(R.string.customer)
        cusRpt.clear()
        cusAdp = CustomerAdaptor(cusRpt)
        rvewPage.rcyVew.adapter = cusAdp

        val str = rvewPage.txtVno.text.toString().replace("\\s".toRegex(), "")
        if (str != "") {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder
                .setMessage("Fetching you data, Please wait.")
                .setTitle("Loading.......")
            val dialog: AlertDialog = builder.create()
            dialog.show()
            db.collection(cmp)
                .document("masterdata")
                .collection("vehicle")
                .whereEqualTo("sts", 1)
                .get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val documents = task.result?.documents ?: emptyList()
                        for (document in documents) {
                            val data = document.data ?: continue
                            //data["name"].toString()
                            if (data["vehNo"].toString().uppercase(Locale.getDefault()).contains(str.uppercase(Locale.getDefault()))) {
                                db.collection(cmp)
                                    .document("masterdata")
                                    .collection("customer")
                                    .whereEqualTo("id", data["cusId"].toString().toInt())
                                    .whereEqualTo("sts", 1)
                                    .orderBy("name", com.google.firebase.firestore.Query.Direction.ASCENDING)
                                    .addSnapshotListener { value, error ->
                                        for (dc: DocumentChange in value?.documentChanges!!) {
                                            if (dc.type == DocumentChange.Type.ADDED) {
                                                cusRpt.add(dc.document.toObject(CustomerClass::class.java))
                                            }
                                        }
                                        cusAdp = CustomerAdaptor(cusRpt)
                                        rvewPage.rcyVew.adapter = cusAdp
                                        Log.d("error",error.toString())
                                    }
                            }
                        }
                        dialog.dismiss()
                    }
                }
        }
    }

    private fun loadUser() {
        rvewPage.txtHeading.setText(R.string.users)
        usrRpt.clear()
        db.collection(cmp)
            .document("masterdata")
            .collection("user")
            .whereEqualTo("sts", 1)
            .whereNotEqualTo("type", 1)
            .orderBy("branch", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .orderBy("name", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { value, error ->
                for (dc: DocumentChange in value?.documentChanges!!) {
                    if (dc.type == DocumentChange.Type.ADDED) {
                        //Log.d("from recycler loop", dc.document.data["name"].toString())
                        usrRpt.add(dc.document.toObject(UserClass::class.java))
                    }
                }
                usrAdp = UserAdaptor(usrRpt)
                rvewPage.rcyVew.adapter = usrAdp
                Log.d("error",error.toString())
            }
    }

    private fun deleteData(id : Int, col : String, msg : String) {
        db.collection(cmp)
            .document("masterdata")
            .collection(col)
            .document(id.toString())
            .update(mapOf(
                "sts" to 0
            )).addOnSuccessListener {
                if (typ == "customer") {
                    db.collection(cmp)
                        .document("masterdata")
                        .collection("vehicle")
                        .whereEqualTo("cusId", id)
                        .get().addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val documents = task.result?.documents ?: emptyList()
                                for (document in documents) {
                                    val data = document.data ?: continue
                                    db.collection(cmp)
                                        .document("masterdata")
                                        .collection("vehicle")
                                        .document(data["id"].toString())
                                        .update(mapOf(
                                            "sts" to 0
                                        )).addOnSuccessListener {

                                        }
                                }
                            } else {
                                Log.d("error", "Error getting documents: ", task.exception)
                            }
                        }
                }
                Toast.makeText(applicationContext,"$msg deleted", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendAddScreen(entNo : Int) {
        var mainIntt = Intent(this@RecyclerViewActivity, CustomerActivity::class.java)
        if (typ == "customer"){
            mainIntt = Intent(this@RecyclerViewActivity, CustomerActivity::class.java)
        } else if (typ == "user") {
            mainIntt = Intent(this@RecyclerViewActivity, UserActivity::class.java)
        }
        mainIntt.putExtra("type", typ)
        mainIntt.putExtra("entNo", entNo.toString())
        startActivity(mainIntt)
        finish()
    }

}