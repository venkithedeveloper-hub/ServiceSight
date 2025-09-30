package com.ats.servicesight

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.ats.servicesight.adaptor.VehicleAdaptor
import com.ats.servicesight.classes.VehicleClass
import com.ats.servicesight.databinding.ActivityCustomerBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore

class CustomerActivity : AppCompatActivity() {
    private var db : FirebaseFirestore = FirebaseFirestore.getInstance()
    private val fbAuth : FirebaseAuth = FirebaseAuth.getInstance()
    private var currentUser = fbAuth.currentUser
    private var usr : String = ""
    private var cmp : String = ""
    private var entId : String = "0"
    private val vehRpt = ArrayList<VehicleClass>()
    private lateinit var vehAdp : VehicleAdaptor
    private lateinit var cusPage : ActivityCustomerBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        cusPage = ActivityCustomerBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(cusPage.root)

        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = this.resources.getColor(R.color.heading)
        val hmeIntt = Intent(this@CustomerActivity, RecyclerViewActivity::class.java)
        entId = intent.getStringExtra("entNo").toString()

        currentUser = fbAuth.currentUser
        usr = fbAuth.currentUser?.email.toString()
        //Log.d("user", usr)

        cmp = if (usr != "" && usr != "null") {
            usr.substring(usr.indexOf(".") + 1, usr.length)
        } else {
            ""
        }

        cusPage.rcyVehicle.layoutManager = LinearLayoutManager(
            this@CustomerActivity,
            LinearLayoutManager.VERTICAL,
            false
        )

        cusPage.rcyVehicle.setHasFixedSize(true)
        clearRcy(usr, cmp)

        cusPage.btnCustomerSrc.isEnabled = true

        if (entId != "0") {
            cusPage.btnCustomerSrc.isEnabled = false
            loadData(entId.toInt())
            loadVehicle()
        }

        cusPage.btnBack.setOnClickListener {
            hmeIntt.putExtra("type", "customer")
            startActivity(hmeIntt)
            finish()
        }

        cusPage.btnCustomerSrc.setOnClickListener {
            if (cusPage.txtMobile.text.toString() == "" || cusPage.txtMobile.text.toString().length < 10 ) {
                Toast.makeText(this, "Please check the mobile number", Toast.LENGTH_SHORT).show()
            } else {
                cusPage.btnCustomerSrc.isEnabled = false
                searchCustomer(cusPage.txtMobile.text.toString())
            }
        }

        cusPage.btnVehicleAdd.setOnClickListener {
            if (cusPage.txtVno.text.toString() == "" || cusPage.txtVbrd.text.toString() == "" ) {
                Toast.makeText(this, "Please check the Vehicle Details", Toast.LENGTH_SHORT).show()
            } else {
                cusPage.btnVehicleAdd.isEnabled = false
                checkRcy(usr, cmp)
            }
        }

        cusPage.btnUpd.setOnClickListener {
            if (cusPage.txtName.text.toString() == "" ||
                cusPage.txtMobile.text.toString() == "" || cusPage.txtMobile.text.toString().length < 10 ) {
                Toast.makeText(this, "Please check the details", Toast.LENGTH_SHORT).show()
            } else {
                cusPage.btnUpd.isEnabled = true
                addCustomer()
            }
        }

        val pulltoRefresh = findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        pulltoRefresh.setOnRefreshListener {
            loadVehicle()
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
                    deleteRcyData(vehAdp.getUsrId(viewHolder.adapterPosition))
                } else if (direction == ItemTouchHelper.RIGHT) {
                    deleteRcyData(vehAdp.getUsrId(viewHolder.adapterPosition))
                }
                loadVehicle()
            }
            @SuppressLint("UseCompatLoadingForDrawables")
            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val p = Paint()
                    val itemView = viewHolder.itemView
                    if (dX > 0) {
                        p.color = "#FF6868".toColorInt()
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
        }).attachToRecyclerView(cusPage.rcyVehicle)

        val onBackPressedCallback = object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                hmeIntt.putExtra("type", "customer")
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

    private fun clearRcy(usr : String, cmp : String) {
        db.collection(cmp)
            .document(usr)
            .collection("vehicle")
            .get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val documents = task.result?.documents ?: emptyList()
                    for (document in documents) {
                        val data = document.data ?: continue
                        db.collection(cmp)
                            .document(usr)
                            .collection("vehicle")
                            .document(data["id"].toString())
                            .delete().addOnSuccessListener {
                                Log.d("delete user vehicle", fbAuth.currentUser?.email.toString())
                            }
                    }
                } else {
                    Log.d("error", "Error getting documents: ", task.exception)
                }
                loadVehicle()
            }
    }

    private fun checkRcy(usr : String, cmp : String) {
        val vno = cusPage.txtVno.text.toString().replace(" ","")
        db.collection(cmp)
            .document(usr)
            .collection("vehicle")
            .whereEqualTo("vehNo", vno).get()
            .addOnSuccessListener { qrySnapshot ->
                if (qrySnapshot.isEmpty) {
                    addRcy(usr, cmp)
                } else {
                    Toast.makeText(this, "Vehicle No. Already exists", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun addRcy(usr : String, cmp : String) {
        var pId : Int
        db.collection(cmp)
            .document(usr)
            .collection("vehicle")
            .orderBy("id", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .limitToLast(1).get()
            .addOnSuccessListener { qrySnapshot ->
                if (qrySnapshot.isEmpty) {
                    pId = 1
                } else {
                    val document = qrySnapshot.documents[0]
                    val pid = document.get("id") as Number
                    pId = pid.toInt() + 1
                }
                addRcyData(pId)
            }
            .addOnFailureListener { e ->
                Log.d("nme failure", e.message.toString())
                pId = 1
                addRcyData(pId)
            }
    }

    private fun addRcyData(id : Int) {
        val vno = cusPage.txtVno.text.toString().replace(" ","")
        val data = hashMapOf(
            "brand"  to cusPage.txtVbrd.text.toString(),
            "id"     to id,
            "model"  to cusPage.txtVmdl.text.toString(),
            "vehNo"  to vno,
            "year"   to cusPage.txtVyr.text.toString()
        )
        db.collection(cmp)
            .document(usr)
            .collection("vehicle")
            .document(id.toString())
            .set(data)
            .addOnSuccessListener {
                cusPage.txtVno.setText("")
                cusPage.txtVbrd.setText("")
                cusPage.txtVmdl.setText("")
                cusPage.txtVyr.setText("")
                cusPage.btnVehicleAdd.isEnabled = true
                loadVehicle()
            }
    }

    private fun deleteRcyData(id : Int) {
        db.collection(cmp)
            .document(usr)
            .collection("vehicle")
            .document(id.toString())
            .delete().addOnSuccessListener {
                loadVehicle()
            }
    }

    private fun searchCustomer(mob : String) {
        db.collection(cmp)
            .document("masterdata")
            .collection("customer")
            .whereEqualTo("mob", mob)
            .whereEqualTo("sts", 1).get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(
                        this,
                        "Customer profile not exist",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val document = querySnapshot.documents[0]
                    loadData((document.get("id") as Number).toInt())
                    cusPage.btnCustomerSrc.isEnabled = true
                }
            }
    }

    private fun loadVehicle() {
        /*vehRpt.clear()
        if (cusId == 0) {
            db.collection(cmp)
                .document(usr)
                .collection("vehicle")
                .orderBy("vehNo", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .addSnapshotListener { value, error ->
                    for (dc: DocumentChange in value?.documentChanges!!) {
                        if (dc.type == DocumentChange.Type.ADDED) {
                            vehRpt.add(dc.document.toObject(VehicleClass::class.java))
                        }
                    }
                    vehAdp = VehicleAdaptor(vehRpt)
                    cusPage.rcyVehicle.adapter = vehAdp
                    Log.d("error",error.toString())
                }
        } else {
            db.collection(cmp)
                .document("masterdata")
                .collection("vehicle")
                .whereEqualTo("cusId", cusId)
                .whereEqualTo("sts", 1)
                .orderBy("vehNo", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .addSnapshotListener { value, error ->
                    for (dc: DocumentChange in value?.documentChanges!!) {
                        if (dc.type == DocumentChange.Type.ADDED) {
                            vehRpt.add(dc.document.toObject(VehicleClass::class.java))
                        }
                    }
                    vehAdp = VehicleAdaptor(vehRpt)
                    cusPage.rcyVehicle.adapter = vehAdp
                    Log.d("error",error.toString())
                }
        }*/
        vehRpt.clear()
        db.collection(cmp)
            .document(usr)
            .collection("vehicle")
            .orderBy("vehNo", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { value, error ->
                for (dc: DocumentChange in value?.documentChanges!!) {
                    if (dc.type == DocumentChange.Type.ADDED) {
                        vehRpt.add(dc.document.toObject(VehicleClass::class.java))
                    }
                }
                vehAdp = VehicleAdaptor(vehRpt)
                cusPage.rcyVehicle.adapter = vehAdp
                Log.d("error",error.toString())
            }

    }

    private fun addCustomer() {
        cusPage.txtMobile.setText(cusPage.txtMobile.text.toString().replace("\\s".toRegex(), ""))
        db.collection(cmp)
            .document("masterdata")
            .collection("customer")
            .whereEqualTo("mob", cusPage.txtMobile.text.toString())
            .whereEqualTo("sts", 1)
            .get().addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    if (entId == "0") {
                        db.collection(cmp)
                            .document("masterdata")
                            .collection("customer")
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
        addVehicles(tid.toInt())
        val data = hashMapOf(
            "eml"   to cusPage.txtEmail.text.toString(),
            "id"    to tid.toInt(),
            "mob"   to cusPage.txtMobile.text.toString(),
            "name"  to cusPage.txtName.text.toString(),
            "sts"   to 1
        )
        db.collection(cmp)
            .document("masterdata")
            .collection("customer")
            .document(tid.toString())
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(applicationContext, "Details successfully added", Toast.LENGTH_SHORT).show()
                val mainIntt = Intent(this@CustomerActivity, RecyclerViewActivity::class.java)
                mainIntt.putExtra("type", "customer")
                startActivity(mainIntt)
                finish()
            }
            .addOnFailureListener { e ->
                Log.w("TAG", "Error deleting document", e)
                Toast.makeText(applicationContext, "Please check the details", Toast.LENGTH_SHORT).show()
                cusPage.btnUpd.isEnabled = true
            }
    }

    private fun updateData(tid : Number){
        addVehicles(tid.toInt())
        db.collection(cmp)
            .document("masterdata")
            .collection("customer")
            .document(tid.toString())
            .update(mapOf(
                "eml"   to cusPage.txtEmail.text.toString(),
                "id"    to tid.toInt(),
                "mob"   to cusPage.txtMobile.text.toString(),
                "name"  to cusPage.txtName.text.toString(),
                "sts"   to 1
            )).addOnSuccessListener {
                Toast.makeText(applicationContext, "Details successfully updated", Toast.LENGTH_SHORT).show()
                val mainIntt = Intent(this@CustomerActivity, RecyclerViewActivity::class.java)
                mainIntt.putExtra("type", "customer")
                startActivity(mainIntt)
                finish()
            }
            .addOnFailureListener { e ->
                Log.w("TAG", "Error deleting document", e)
                Toast.makeText(applicationContext, "Please check the details", Toast.LENGTH_SHORT).show()
                cusPage.btnUpd.isEnabled = true
            }
    }

    private fun loadData(entId : Int) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
            .setMessage("Your data is fetching")
            .setTitle("Please wait.......")

        val dialog: AlertDialog = builder.create()
        dialog.show()
        clearRcy(usr, cmp)
        runAfterDelay(2000) {
            db.collection(cmp)
                .document("masterdata")
                .collection("customer")
                .whereEqualTo("id", entId).get()
                .addOnSuccessListener { qrySnapshot ->
                    val document = qrySnapshot.documents[0]
                    getVehicles((document.get("id") as Number).toInt())
                    cusPage.txtName.setText((document.get("name") as String).toString())
                    cusPage.txtMobile.setText((document.get("mob") as String).toString())
                    cusPage.txtEmail.setText((document.get("eml") as String).toString())
                    dialog.dismiss()
                }
        }
    }

    private fun getVehicles(cusId : Int) {

        var x = 1
        db.collection(cmp)
            .document("masterdata")
            .collection("vehicle")
            .whereEqualTo("cusId", cusId)
            .whereEqualTo("sts", 1)
            .orderBy("vehNo", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val documents = task.result?.documents ?: emptyList()
                    for (document in documents) {
                        val data = document.data ?: continue
                        val vehdata = hashMapOf(
                            "id"     to  x,
                            "brand"  to  data["brand"].toString(),
                            "model"  to  data["model"].toString(),
                            "vehNo"  to  data["vehNo"].toString(),
                            "year"   to  data["year"].toString()
                        )
                        db.collection(cmp)
                            .document(usr)
                            .collection("vehicle")
                            .document(x.toString())
                            .set(vehdata)
                            .addOnSuccessListener {

                            }
                        x += 1
                    }
                    loadVehicle()
                } else {
                    Log.d("error", "Error getting documents: ", task.exception)
                }
            }
    }

    private fun addVehicles(cusId : Int) {
        var x = 1
        db.collection(cmp)
            .document(usr)
            .collection("vehicle")
            .get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val documents = task.result?.documents ?: emptyList()
                    for (document in documents) {
                        val data = document.data ?: continue
                        db.collection(cmp)
                            .document("masterdata")
                            .collection("vehicle")
                            .orderBy("id", com.google.firebase.firestore.Query.Direction.ASCENDING)
                            .limitToLast(1).get()
                            .addOnSuccessListener { qrySnapshot ->
                                if (qrySnapshot.isEmpty) {
                                    addVehicle(1, data["vehNo"].toString(), data["brand"].toString(), data["model"].toString(), data["year"].toString(), cusId)
                                } else {
                                    val vehdoc = qrySnapshot.documents[0]
                                    //cmnCls.tmp = document.get("id") as Number
                                    val aId: Number = vehdoc.get("id") as Number
                                    val aid = aId.toInt() + 1
                                    Log.d("name", aid.toString())
                                    addVehicle(aid, data["vehNo"].toString(), data["brand"].toString(), data["model"].toString(), data["year"].toString(), cusId)
                                }
                            }
                    }
                } else {
                    Log.d("error", "Error getting documents: ", task.exception)
                }
            }
    }

    private fun addVehicle(id : Int, vno : String, brd : String, mdl : String, yr : String, cusId : Int) {
        val vehdata = hashMapOf(
            "brand"  to  brd,
            "cusId"  to  cusId,
            "id"     to  id,
            "model"  to  mdl,
            "sts"    to  1,
            "vehNo"  to  vno,
            "year"   to  yr
        )
        db.collection(cmp)
            .document("masterdata")
            .collection("vehicle")
            .document(id.toString())
            .set(vehdata)
            .addOnSuccessListener {

            }
    }

    private fun runAfterDelay(delayMillis: Long, action: () -> Unit) {
        Handler(Looper.getMainLooper()).postDelayed(action, delayMillis)
    }
}