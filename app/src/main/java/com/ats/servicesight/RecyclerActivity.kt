package com.ats.servicesight

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
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
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ats.servicesight.adaptor.InwardAdaptor
import com.ats.servicesight.classes.InwardClass
import com.ats.servicesight.databinding.ActivityRecyclerBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class RecyclerActivity : AppCompatActivity() {
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val fbAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val fbStorage : FirebaseStorage = FirebaseStorage.getInstance()
    private val storageRef : StorageReference = fbStorage.reference
    private lateinit var rvewPage: ActivityRecyclerBinding
    private var typ: String = ""
    private var dte: String = ""
    private var currentUser = fbAuth.currentUser
    private var usr : String = ""
    private var usrType : String = ""
    private var cmp : String = ""
    private var fDate = LocalDate.of(2025, 1, 1)
    private var tDate = LocalDate.of(2025, 1, 1)
    private var brnNList = ArrayList<String>()
    private var brnIList = ArrayList<String>()
    private var brnId : String = "0"
    private var usrNList = ArrayList<String>()
    private var usrIList = ArrayList<String>()
    private var usrId : String = "0"
    private var serNList = ArrayList<String>()
    private var serIList = ArrayList<String>()
    private var serId : String = "0"
    private val inwRpt = ArrayList<InwardClass>()
    private lateinit var inwAdp : InwardAdaptor
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        rvewPage = ActivityRecyclerBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(rvewPage.root)

        val cal: Calendar = Calendar.getInstance(TimeZone.getDefault())
        val datePickerListener =
            DatePickerDialog.OnDateSetListener { view, selectedYear, selectedMonth, selectedDay ->
                Log.d("view", view.toString())
                val year1 = selectedYear.toString()
                var month1 = (selectedMonth + 1).toString()
                var day1 = selectedDay.toString()
                if (selectedDay < 10) {
                    day1 = "0$day1"
                }
                if ((selectedMonth + 1) < 10) {
                    month1 = "0$month1"
                }
                if (dte == "from") {
                    rvewPage.txtFDate.text = "$day1-$month1-$year1"
                    fDate = LocalDate.of(
                        rvewPage.txtFDate.text.toString().substring(6, 10).toInt(),
                        rvewPage.txtFDate.text.toString().substring(3, 5).toInt(),
                        rvewPage.txtFDate.text.toString().substring(0, 2).toInt()
                    )
                    if ((fDate.plusDays(9) < tDate) || tDate < fDate) {
                        tDate = fDate.plusDays(9)
                        rvewPage.txtTDate.text = tDate.toString().substring(8, 10) + "-" + tDate.toString().substring(5, 7) + "-" + tDate.toString().substring(0, 4)
                    }
                } else if (dte == "to") {
                    rvewPage.txtTDate.text = "$day1-$month1-$year1"
                    tDate = LocalDate.of(
                        rvewPage.txtTDate.text.toString().substring(6, 10).toInt(),
                        rvewPage.txtTDate.text.toString().substring(3, 5).toInt(),
                        rvewPage.txtTDate.text.toString().substring(0, 2).toInt()
                    )
                    if ((tDate.plusDays(-9) > fDate) || tDate < fDate) {
                        fDate = tDate.plusDays(-9)
                        rvewPage.txtFDate.text = fDate.toString().substring(8, 10) + "-" + fDate.toString().substring(5, 7) + "-" + fDate.toString().substring(0, 4)
                    }
                }
            }

        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = this.resources.getColor(R.color.heading)
        val hmeIntt = Intent(this@RecyclerActivity, MainActivity::class.java)

        rvewPage.rcyVew.layoutManager = LinearLayoutManager(
            this@RecyclerActivity,
            LinearLayoutManager.VERTICAL,
            false
        )

        rvewPage.rcyVew.setHasFixedSize(true)

        rvewPage.txtFDate.text = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        rvewPage.txtTDate.text = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

        fDate = LocalDate.of(
            rvewPage.txtFDate.text.toString().substring(6, 10).toInt(),
            rvewPage.txtFDate.text.toString().substring(3, 5).toInt(),
            rvewPage.txtFDate.text.toString().substring(0, 2).toInt()
        )
        tDate = LocalDate.of(
            rvewPage.txtTDate.text.toString().substring(6, 10).toInt(),
            rvewPage.txtTDate.text.toString().substring(3, 5).toInt(),
            rvewPage.txtTDate.text.toString().substring(0, 2).toInt()
        )

        rvewPage.txtFDate.setOnClickListener {
            dte = "from"
            val datePicker = DatePickerDialog(
                this,
                R.style.DialogDateTheme, datePickerListener,
                cal[Calendar.YEAR],
                cal[Calendar.MONTH],
                cal[Calendar.DAY_OF_MONTH]
            )
            datePicker.setCancelable(false)
            datePicker.setTitle("Select the date")
            datePicker.show()
        }

        rvewPage.txtTDate.setOnClickListener {
            dte = "to"
            val datePicker = DatePickerDialog(
                this,
                R.style.DialogDateTheme, datePickerListener,
                cal[Calendar.YEAR],
                cal[Calendar.MONTH],
                cal[Calendar.DAY_OF_MONTH]
            )
            datePicker.setCancelable(false)
            datePicker.setTitle("Select the date")
            datePicker.show()
        }

        typ = intent.getStringExtra("type").toString()
        usrType = intent.getStringExtra("usrtype").toString()

        currentUser = fbAuth.currentUser
        usr = fbAuth.currentUser?.email.toString()
        //Log.d("usertype", usrtype)
        cmp = if (usr != "" && usr != "null") {
            usr.substring(usr.indexOf(".") + 1, usr.length)
        } else {
            ""
        }

        if (typ == "delivery") {
            rvewPage.txtFDate.text = ""
            rvewPage.txtTDate.text = ""
        }

        loadUser(cmp, usr)
        loadService(cmp)

        if (usrType == "1") {
            rvewPage.fltAdd.visibility = View.INVISIBLE
        } else {
            rvewPage.fltAdd.visibility = View.VISIBLE
        }

        rvewPage.rcyVew.layoutManager = LinearLayoutManager(
            this@RecyclerActivity,
            LinearLayoutManager.VERTICAL,
            false
        )

        rvewPage.rcyVew.setHasFixedSize(true)

        rvewPage.spnrBranch.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>,
                                        view: View, position: Int, id: Long) {
                brnId = brnIList[position]
                if (brnId == "0") {
                    usrNList.clear()
                    usrNList.add("All SA")
                    usrIList.clear()
                    usrIList.add("0")
                    val adapter = ArrayAdapter(this@RecyclerActivity, R.layout.spinner_style, usrNList)
                    adapter.setDropDownViewResource(R.layout.spinner_style)
                    rvewPage.spnrUser.adapter = adapter
                    if (typ == "inward") {
                        loadInward()
                    } else if (typ == "delivery") {
                        loadDelivery()
                    }
                } else {
                    db.collection(cmp)
                        .document("masterdata")
                        .collection("user")
                        .whereEqualTo("user", usr).get()
                        .addOnSuccessListener { querySnapshot ->
                            if (!querySnapshot.isEmpty) {
                                val document = querySnapshot.documents[0]
                                val br = (document.get("type") as Number).toInt()
                                val uid = (document.get("id") as Number).toInt()
                                loadSA(cmp, br, brnId, uid, typ)
                            }
                        }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                brnId = "0"
            }
        }

        rvewPage.spnrUser.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>,
                                        view: View, position: Int, id: Long) {
                usrId = usrIList[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                usrId = "0"
            }
        }

        rvewPage.spnrService.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>,
                                        view: View, position: Int, id: Long) {
                serId = serIList[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                serId = "0"
            }
        }

        rvewPage.btnBack.setOnClickListener {
            startActivity(hmeIntt)
            finish()
        }

        rvewPage.fltAdd.setOnClickListener {
            sendToInward(0)
        }

        rvewPage.btnSearch.setOnClickListener {
            if (typ == "inward") {
                loadInward()
            } else if (typ == "delivery") {
                loadDelivery()
            }
        }

        val pulltoRefresh = findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        pulltoRefresh.setOnRefreshListener {
            if (typ == "inward") {
                loadInward()
            } else if (typ == "delivery") {
                loadDelivery()
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
                        "inward" -> {
                            //deleteInwardData(inwAdp.getUsrId(viewHolder.adapterPosition))
                            deleteInward(inwAdp.getUsrId(viewHolder.adapterPosition))
                        }
                        "delivery" -> {
                            Toast.makeText(applicationContext, "Inward cannot be delete in Pending View", Toast.LENGTH_SHORT).show()
                            loadDelivery()
                        }
                    }
                } else if (direction == ItemTouchHelper.RIGHT) {
                    when (typ) {
                        "inward" -> {
                            sendToInward(inwAdp.getUsrId(viewHolder.adapterPosition))
                        }
                        "delivery" -> {
                            sendToDelivery(inwAdp.getUsrId(viewHolder.adapterPosition))
                        }
                    }
                }
                /*if (typ == "customer") {
                    loadCustomer()
                } else if (typ == "user") {
                    loadUser()
                }*/
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

    @SuppressLint("SetTextI18n")
    private fun loadInward() {
        val dteList = mutableListOf("")
        dteList.clear()
        rvewPage.txtHeading.text = "View Inward"
        var fdte = LocalDate.of(rvewPage.txtFDate.text.toString().substring(6, 10).toInt(), rvewPage.txtFDate.text.toString().substring(3, 5).toInt(), rvewPage.txtFDate.text.toString().substring(0, 2).toInt())
        val tdte = LocalDate.of(rvewPage.txtTDate.text.toString().substring(6, 10).toInt(), rvewPage.txtTDate.text.toString().substring(3, 5).toInt(), rvewPage.txtTDate.text.toString().substring(0, 2).toInt())
        if (fdte >= tdte) {
            dteList += rvewPage.txtFDate.text.toString()
            getInward(dteList, rvewPage.txtSearch.text.toString())
        } else {
            while (!fdte.isAfter(tdte)) {
                val fd = fdte.toString().substring(8, 10) + "-" + fdte.toString().substring(5, 7) + "-"  + fdte.toString().substring(0, 4)
                dteList += fd
                //Log.d("Fdte", fd)
                fdte = fdte.plusDays(1)
            }
            getInward(dteList, rvewPage.txtSearch.text.toString())
        }
    }

    private fun getInward(dte : MutableList<String>, txt: String) {
        inwRpt.clear()
        //Log.d("IDs", "$brnId-$usrId-$serId-$txt-$cmp-$dte")
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
            .setMessage("Getting your Inward data.")
            .setTitle("Please wait.......")
        val dialog: AlertDialog = builder.create()
        dialog.show()

        var qry = db.collection(cmp)
            .document("entry")
            .collection("inward")
            .whereEqualTo("delId", 0)

        if (typ == "inward") {
            qry = db.collection(cmp)
                .document("entry")
                .collection("inward")
                .whereEqualTo("delId", 0)
                .whereIn("dte", dte)
        } else if (typ == "delivery") {
            qry = db.collection(cmp)
                .document("entry")
                .collection("inward")
                .whereEqualTo("delId", 0)
        }

        qry.orderBy("dte", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { value, error ->
                for (dc: DocumentChange in value?.documentChanges!!) {
                    if (dc.type == DocumentChange.Type.ADDED) {
                        //Log.d("Branch IDs", dc.document.data["brnId"].toString())
                        if (brnId == "0" && usrId == "0" && serId == "0" && txt == "") {
                            inwRpt.add(dc.document.toObject(InwardClass::class.java))
                        } else {
                            if (serId != "0") {
                                if (brnId != "0" && usrId != "0") {
                                    if (dc.document.data["brnId"].toString() == brnId && dc.document.data["usrId"].toString() == usrId && dc.document.data["serId"].toString() == serId) {
                                        inwRpt.add(dc.document.toObject(InwardClass::class.java))
                                    }
                                } else if (brnId != "0" && usrId == "0") {
                                    if (dc.document.data["brnId"].toString() == brnId && dc.document.data["serId"].toString() == serId) {
                                        inwRpt.add(dc.document.toObject(InwardClass::class.java))
                                    }
                                } else if (brnId == "0" && usrId != "0") {
                                    if (dc.document.data["usrId"].toString() == usrId && dc.document.data["serId"].toString() == serId) {
                                        inwRpt.add(dc.document.toObject(InwardClass::class.java))
                                    }
                                } else if (brnId == "0" && usrId == "0") {
                                    if (dc.document.data["serId"].toString() == serId) {
                                        inwRpt.add(dc.document.toObject(InwardClass::class.java))
                                    }
                                }
                            } else {
                                if (brnId != "0" && usrId != "0") {
                                    if (dc.document.data["brnId"].toString() == brnId && dc.document.data["usrId"].toString() == usrId) {
                                        inwRpt.add(dc.document.toObject(InwardClass::class.java))
                                    }
                                } else if (brnId != "0" && usrId == "0") {
                                    if (dc.document.data["brnId"].toString() == brnId) {
                                        inwRpt.add(dc.document.toObject(InwardClass::class.java))
                                    }
                                } else if (brnId == "0" && usrId != "0") {
                                    if (dc.document.data["usrId"].toString() == usrId) {
                                        inwRpt.add(dc.document.toObject(InwardClass::class.java))
                                    }
                                }
                            }
                            //Log.d("Branch IDs", dc.document.data["brnId"].toString())
                            /*if (txt == "") {
                                if (serId != "0") {
                                    if (brnId != "0" && usrId != "0") {
                                        if (dc.document.data["brnId"].toString() == brnId && dc.document.data["usrId"].toString() == usrId && dc.document.data["serId"].toString() == serId) {
                                            inwRpt.add(dc.document.toObject(InwardClass::class.java))
                                        }
                                    } else if (brnId != "0" && usrId == "0") {
                                        if (dc.document.data["brnId"].toString() == brnId && dc.document.data["serId"].toString() == serId) {
                                            inwRpt.add(dc.document.toObject(InwardClass::class.java))
                                        }
                                    } else if (brnId == "0" && usrId != "0") {
                                        if (dc.document.data["usrId"].toString() == usrId && dc.document.data["serId"].toString() == serId) {
                                            inwRpt.add(dc.document.toObject(InwardClass::class.java))
                                        }
                                    } else if (brnId == "0" && usrId == "0") {
                                        if (dc.document.data["serId"].toString() == serId) {
                                            inwRpt.add(dc.document.toObject(InwardClass::class.java))
                                        }
                                    }
                                } else {
                                    if (brnId != "0" && usrId != "0") {
                                        if (dc.document.data["brnId"].toString() == brnId && dc.document.data["usrId"].toString() == usrId) {
                                            inwRpt.add(dc.document.toObject(InwardClass::class.java))
                                        }
                                    } else if (brnId != "0" && usrId == "0") {
                                        if (dc.document.data["brnId"].toString() == brnId) {
                                            inwRpt.add(dc.document.toObject(InwardClass::class.java))
                                        }
                                    } else if (brnId == "0" && usrId != "0") {
                                        if (dc.document.data["usrId"].toString() == usrId) {
                                            inwRpt.add(dc.document.toObject(InwardClass::class.java))
                                        }
                                    }
                                }
                            } else {
                                db.collection(cmp)
                                    .document("masterdata")
                                    .collection("vehicle")
                                    .whereEqualTo("sts", 1).get()
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val documents = task.result?.documents ?: emptyList()
                                            for (document in documents) {
                                                val data = document.data ?: continue
                                                Log.d("vehNo", data["vehNo"].toString().uppercase(Locale.getDefault()) + "-" + txt.uppercase(Locale.getDefault()))
                                                if (data["vehNo"].toString()
                                                        .uppercase(Locale.getDefault())
                                                        .contains(txt.uppercase(Locale.getDefault()))
                                                ) {
                                                    if (serId != "0") {
                                                        if (brnId != "0" && usrId != "0") {
                                                            if (dc.document.data["brnId"].toString() == brnId && dc.document.data["usrId"].toString() == usrId && dc.document.data["serId"].toString() == serId) {
                                                                inwRpt.add(dc.document.toObject(InwardClass::class.java))
                                                            }
                                                        } else if (brnId != "0" && usrId == "0") {
                                                            if (dc.document.data["brnId"].toString() == brnId && dc.document.data["serId"].toString() == serId) {
                                                                inwRpt.add(dc.document.toObject(InwardClass::class.java))
                                                            }
                                                        } else if (brnId == "0" && usrId != "0") {
                                                            if (dc.document.data["usrId"].toString() == usrId && dc.document.data["serId"].toString() == serId) {
                                                                inwRpt.add(dc.document.toObject(InwardClass::class.java))
                                                            }
                                                        } else if (brnId == "0" && usrId == "0") {
                                                            if (dc.document.data["serId"].toString() == serId) {
                                                                inwRpt.add(dc.document.toObject(InwardClass::class.java))
                                                            }
                                                        }
                                                    } else {
                                                        if (brnId != "0" && usrId != "0") {
                                                            if (dc.document.data["brnId"].toString() == brnId && dc.document.data["usrId"].toString() == usrId) {
                                                                inwRpt.add(dc.document.toObject(InwardClass::class.java))
                                                            }
                                                        } else if (brnId != "0" && usrId == "0") {
                                                            if (dc.document.data["brnId"].toString() == brnId) {
                                                                inwRpt.add(dc.document.toObject(InwardClass::class.java))
                                                            }
                                                        } else if (brnId == "0" && usrId != "0") {
                                                            if (dc.document.data["usrId"].toString() == usrId) {
                                                                inwRpt.add(dc.document.toObject(InwardClass::class.java))
                                                            }
                                                        }
                                                    }
                                                    vn = dc.document.data["id"].toString().toInt()
                                                } else {
                                                    vn = 0
                                                }
                                            }
                                            if (vn == 0) {
                                                db.collection(cmp)
                                                    .document("masterdata")
                                                    .collection("customer")
                                                    .whereEqualTo("sts", 1).get()
                                                    .addOnCompleteListener { custask ->
                                                        if (task.isSuccessful) {
                                                            val cusdocuments = custask.result?.documents
                                                                ?: emptyList()
                                                            for (document in cusdocuments) {
                                                                val data = document.data ?: continue
                                                                if (data["mob"].toString()
                                                                        .uppercase(Locale.getDefault())
                                                                        .contains(txt.uppercase(Locale.getDefault()))
                                                                ) {
                                                                    if (serId != "0") {
                                                                        if (brnId != "0" && usrId != "0") {
                                                                            if (dc.document.data["brnId"].toString() == brnId && dc.document.data["usrId"].toString() == usrId && dc.document.data["serId"].toString() == serId) {
                                                                                inwRpt.add(
                                                                                    dc.document.toObject(
                                                                                        InwardClass::class.java
                                                                                    )
                                                                                )
                                                                            }
                                                                        } else if (brnId != "0" && usrId == "0") {
                                                                            if (dc.document.data["brnId"].toString() == brnId && dc.document.data["serId"].toString() == serId) {
                                                                                inwRpt.add(
                                                                                    dc.document.toObject(
                                                                                        InwardClass::class.java
                                                                                    )
                                                                                )
                                                                            }
                                                                        } else if (brnId == "0" && usrId != "0") {
                                                                            if (dc.document.data["usrId"].toString() == usrId && dc.document.data["serId"].toString() == serId) {
                                                                                inwRpt.add(
                                                                                    dc.document.toObject(
                                                                                        InwardClass::class.java
                                                                                    )
                                                                                )
                                                                            }
                                                                        } else if (brnId == "0" && usrId == "0") {
                                                                            if (dc.document.data["serId"].toString() == serId) {
                                                                                inwRpt.add(
                                                                                    dc.document.toObject(
                                                                                        InwardClass::class.java
                                                                                    )
                                                                                )
                                                                            }
                                                                        }
                                                                    } else {
                                                                        if (brnId != "0" && usrId != "0") {
                                                                            if (dc.document.data["brnId"].toString() == brnId && dc.document.data["usrId"].toString() == usrId) {
                                                                                inwRpt.add(
                                                                                    dc.document.toObject(
                                                                                        InwardClass::class.java
                                                                                    )
                                                                                )
                                                                            }
                                                                        } else if (brnId != "0" && usrId == "0") {
                                                                            if (dc.document.data["brnId"].toString() == brnId) {
                                                                                inwRpt.add(
                                                                                    dc.document.toObject(
                                                                                        InwardClass::class.java
                                                                                    )
                                                                                )
                                                                            }
                                                                        } else if (brnId == "0" && usrId != "0") {
                                                                            if (dc.document.data["usrId"].toString() == usrId) {
                                                                                inwRpt.add(
                                                                                    dc.document.toObject(
                                                                                        InwardClass::class.java
                                                                                    )
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                            }
                                        }
                                    }
                            }*/

                        }
                    }
                }
                inwAdp = InwardAdaptor(inwRpt)
                rvewPage.rcyVew.adapter = inwAdp
                Log.d("error",error.toString())
                dialog.dismiss()
            }
    }

    @SuppressLint("SetTextI18n")
    private fun loadDelivery() {
        val dteList = mutableListOf("")
        dteList.clear()
        getInward(dteList, "")
        rvewPage.txtHeading.text = "Pending Inwards"
        rvewPage.fltAdd.isVisible = false
        rvewPage.txtFDate.isClickable = false
        rvewPage.txtTDate.isClickable = false
    }

    private fun loadUser(cmp : String, usr : String) {
        val logIntt = Intent(this@RecyclerActivity, LoginActivity::class.java)
        currentUser!!.reload().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                db.collection(cmp)
                    .document("masterdata")
                    .collection("user")
                    .whereEqualTo("user", usr)
                    .whereEqualTo("sts", 1).get()
                    .addOnSuccessListener { querySnapshot ->
                        if (querySnapshot.isEmpty) {
                            startActivity(logIntt)
                            finish()
                        } else {
                            val document = querySnapshot.documents[0]
                            loadBranch(cmp , (document.get("type") as Number).toInt())
                        }
                    }
            } else {
                startActivity(logIntt)
                finish()
            }
        }
    }

    private fun loadBranch(cmp : String, typ : Int) {
        brnNList.clear()
        brnNList.add("All Branch")
        brnIList.clear()
        brnIList.add("0")
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
                        brnNList.add(data["name"].toString().uppercase(Locale.getDefault()))
                        brnIList.add(data["id"].toString())
                    }
                } else {
                    Log.d("error", "Error getting documents: ", task.exception)
                }
                val adapter = ArrayAdapter(this@RecyclerActivity, R.layout.spinner_style, brnNList)
                adapter.setDropDownViewResource(R.layout.spinner_style)
                rvewPage.spnrBranch.adapter = adapter
                if (typ > 1) {
                    db.collection(cmp)
                        .document("masterdata")
                        .collection("user")
                        .whereEqualTo("user", usr).get()
                        .addOnSuccessListener { querySnapshot ->
                            if (!querySnapshot.isEmpty) {
                                val document = querySnapshot.documents[0]
                                val b = (document.get("branch") as Number).toInt()
                                setSpinnerData(rvewPage.spnrBranch, brnIList, b.toString(), "B")
                                //Log.d("Branch ID", brnId)
                            }
                        }
                    rvewPage.spnrBranch.isEnabled = false
                    rvewPage.spnrUser.isEnabled = typ == 2
                } else {
                    rvewPage.spnrBranch.isEnabled = true
                    rvewPage.spnrUser.isEnabled = true
                }

            }
    }

    private fun loadSA(cmp : String, typ : Int, brn : String, uid : Int, tp : String) {
        if (brn != "0") {
            usrNList.clear()
            usrNList.add("All SA")
            usrIList.clear()
            usrIList.add("0")
            db.collection(cmp)
                .document("masterdata")
                .collection("user")
                .whereEqualTo("sts", 1)
                .whereEqualTo("branch",brn.toInt())
                .whereNotEqualTo("type", 1)
                .orderBy("name", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val documents = task.result?.documents ?: emptyList()
                        for (document in documents) {
                            val data = document.data ?: continue
                            usrNList.add(data["name"].toString().uppercase(Locale.getDefault()))
                            usrIList.add(data["user"].toString())
                        }
                    } else {
                        Log.d("error", "Error getting documents: ", task.exception)
                    }
                    val adapter = ArrayAdapter(this@RecyclerActivity, R.layout.spinner_style, usrNList)
                    adapter.setDropDownViewResource(R.layout.spinner_style)
                    rvewPage.spnrUser.adapter = adapter
                    if (typ == 3) {
                        setSpinnerData(rvewPage.spnrUser, usrIList, uid.toString(), "U")
                        //Log.d("User ID", usrId)
                        rvewPage.spnrUser.isEnabled = false
                    } else {
                        rvewPage.spnrUser.isEnabled = true
                    }
                    if (tp == "inward") {
                        loadInward()
                    } else if (tp == "delivery") {
                        loadDelivery()
                    }
                }
        }
    }

    private fun loadService(cmp : String) {
        serNList.clear()
        serNList.add("All Service Type")
        serIList.clear()
        serIList.add("0")
        db.collection(cmp)
            .document("masterdata")
            .collection("service")
            .whereEqualTo("sts", 1)
            .orderBy("name", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val documents = task.result?.documents ?: emptyList()
                    for (document in documents) {
                        val data = document.data ?: continue
                        serNList.add(data["name"].toString())
                        serIList.add(data["id"].toString())
                    }
                } else {
                    Log.d("error", "Error getting documents: ", task.exception)
                }
                val adapter = ArrayAdapter(this@RecyclerActivity, R.layout.spinner_style, serNList)
                adapter.setDropDownViewResource(R.layout.spinner_style)
                rvewPage.spnrService.adapter = adapter
            }
    }

    private fun sendToInward(id : Int) {
        val mainIntt = Intent(this@RecyclerActivity, InwardActivity::class.java)
        mainIntt.putExtra("usrtype", usrType)
        mainIntt.putExtra("brnId", brnId)
        mainIntt.putExtra("type", typ)
        mainIntt.putExtra("entNo", id.toString())
        startActivity(mainIntt)
        finish()
    }

    private fun setSpinnerData(spinner : Spinner, idAry : ArrayList<String>, value : String, valId : String) {
        for (position in 0 until idAry.count()) {
            if (idAry[position] == value) {
                spinner.setSelection(position)
                if (valId == "B") {
                    brnId = idAry[position]
                } else if (valId == "U") {
                    usrId = idAry[position]
                }
                return
            }
        }
    }

    private fun deleteInwardData(id : Int) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Deletion")
        builder.setMessage("Are you want to delete this Inward?")

        builder.setPositiveButton(
            "YES",
            DialogInterface.OnClickListener { dialog, which -> // Do nothing but close the dialog
                //Log.d("Command", "yes")
                deleteInward(id)
                dialog.dismiss()
            })

        builder.setNegativeButton(
            "NO",
            DialogInterface.OnClickListener { dialog, which -> // Do nothing
                //Log.d("Command", "no")
                loadInward()
                dialog.dismiss()
            })

        builder.create()?.show()
    }

    private fun deleteInward(id : Int) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
            .setMessage("Inward is deleting")
            .setTitle("Please wait.......")
        val dialog: AlertDialog = builder.create()
        dialog.show()
        db.collection(cmp)
            .document("masterdata")
            .collection("imgname")
            .whereEqualTo("sts", 1)
            .orderBy("id", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val imgdocs = task.result?.documents ?: emptyList()
                    for (imgdoct in imgdocs) {
                        val imgdata = imgdoct.data ?: continue
                        storageRef.child(cmp).child("inward").child(id.toString()).child("img-" + imgdata["id"].toString())
                            .delete().addOnSuccessListener {
                                Log.d ("Image Delete", "yes")
                            }.addOnFailureListener { exception ->
                                // Handle any errors
                                Log.d ("Image Delete", "Error deleting file: ${exception.message}")
                                //loadInward()
                                //dialog.dismiss()
                                //Toast.makeText(this, "Inward is not deleted successfully.", Toast.LENGTH_SHORT).show()
                            }
                    }
                    db.collection(cmp)
                        .document("entry")
                        .collection("inward")
                        .document(id.toString())
                        .delete().addOnSuccessListener {
                            //Toast.makeText(this, "Inward is deleted successfully.", Toast.LENGTH_SHORT).show()
                        }
                }
                dialog.dismiss()
                loadInward()
            }
    }

    private fun sendToDelivery(id : Int) {
        val mainIntt = Intent(this@RecyclerActivity, DeliveryActivity::class.java)
        mainIntt.putExtra("usrtype", usrType)
        mainIntt.putExtra("inwId", id.toString())
        mainIntt.putExtra("delId", "0")
        startActivity(mainIntt)
        finish()
    }

}