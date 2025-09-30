package com.ats.servicesight

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ats.servicesight.adaptor.VehSearchAdaptor
import com.ats.servicesight.classes.VehicleClass
import com.ats.servicesight.databinding.ActivityInwardBinding
import com.ats.servicesight.interfaces.OnSelectClickListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InwardActivity : AppCompatActivity() {
    private var db : FirebaseFirestore = FirebaseFirestore.getInstance()
    private val fbAuth : FirebaseAuth = FirebaseAuth.getInstance()
    private var currentUser = fbAuth.currentUser
    private var usr : String = ""
    private var usrType : String = ""
    private var cmp : String = ""
    private var entId : String = "0"
    private val vehRpt = ArrayList<VehicleClass>()
    private lateinit var vehAdp : VehSearchAdaptor
    private lateinit var inwPage : ActivityInwardBinding
    private var serNList = ArrayList<String>()
    private var serIList = ArrayList<String>()
    private var imgList = ArrayList<String>()
    private var imgUri = ArrayList<String>()
    private var imgName = ArrayList<String>()
    private var serId : String = "0"
    private var vehId : String = "0"
    private var cusId : String = "0"
    private var brnId : String = "0"
    @SuppressLint("SimpleDateFormat")
    private var sdf = SimpleDateFormat("dd-MM-yyyy hh:mm:ss aaa")
    override fun onCreate(savedInstanceState: Bundle?) {
        inwPage = ActivityInwardBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(inwPage.root)

        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = this.resources.getColor(R.color.heading)
        val hmeIntt = Intent(this@InwardActivity, RecyclerActivity::class.java)
        entId = intent.getStringExtra("entNo").toString()
        brnId = intent.getStringExtra("brnId").toString()
        usrType = intent.getStringExtra("usrtype").toString()

        currentUser = fbAuth.currentUser
        usr = fbAuth.currentUser?.email.toString()
        //Log.d("entNo", entId)

        cmp = if (usr != "" && usr != "null") {
            usr.substring(usr.indexOf(".") + 1, usr.length)
        } else {
            ""
        }

        inwPage.rcyVehicle.layoutManager = LinearLayoutManager(
            this@InwardActivity,
            LinearLayoutManager.VERTICAL,
            false
        )

        inwPage.rcyVehicle.setHasFixedSize(true)

        inwPage.btnMobileSrc.isEnabled = true
        inwPage.btnVehicleSrc.isEnabled = true
        loadService(cmp)

        if (entId != "0") {
            inwPage.btnMobileSrc.isEnabled = false
            inwPage.btnVehicleSrc.isEnabled = false
            db.collection(cmp)
                .document("entry")
                .collection("inward")
                .whereEqualTo("id", entId.toInt()).get()
                .addOnSuccessListener { inwqrySnapshot ->
                    if (!inwqrySnapshot.isEmpty) {
                        val document = inwqrySnapshot.documents[0]
                        getVehicledata((document.get("vehId") as Number).toInt())
                        setSpinnerData(inwPage.spnrService, serIList, (document.get("serId") as Number).toString())
                    }
                }
            /*loadData(entId.toInt())
            loadVehicle()*/
        }

        inwPage.btnMobileSrc.setOnClickListener {
            vehId = "0"
            cusId = "0"
            srcbyCustomer()
        }

        inwPage.btnVehicleSrc.setOnClickListener {
            vehId = "0"
            cusId = "0"
            srcbyVehicle()
        }

        val pulltoRefresh = findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        pulltoRefresh.setOnRefreshListener {
            pulltoRefresh.isRefreshing = false
        }

        inwPage.spnrService.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>,
                                        view: View, position: Int, id: Long) {
                serId = serIList[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                serId = "0"
            }
        }

        inwPage.btnNxt.setOnClickListener {
            if (vehId == "0" || serId == "0") {
                Toast.makeText(this, "Please select a Vehicle to continue", Toast.LENGTH_SHORT).show()
            } else {
                inwPage.btnNxt.isEnabled = false
                if (entId == "0") {
                    loadInwardId()
                } else {
                    updateInward(entId.toInt())
                }
            }
        }

        inwPage.btnBack.setOnClickListener {
            hmeIntt.putExtra("usrtype", usrType)
            hmeIntt.putExtra("type", "inward")
            startActivity(hmeIntt)
            finish()
        }

        val onBackPressedCallback = object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                hmeIntt.putExtra("usrtype", usrType)
                hmeIntt.putExtra("type", "inward")
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
                val adapter = ArrayAdapter(this@InwardActivity, R.layout.spinner_style, serNList)
                adapter.setDropDownViewResource(R.layout.spinner_style)
                inwPage.spnrService.adapter = adapter
            }
    }

    private fun srcbyCustomer () {
        val listener: OnSelectClickListener = object : OnSelectClickListener {
            override fun OnSelectClicked(id: Int) {
                vehId = "0"
                cusId = "0"
                getVehicledata(id)
            }
        }

        vehRpt.clear()
        vehAdp = VehSearchAdaptor(vehRpt, listener)
        inwPage.rcyVehicle.adapter = vehAdp
        val str = inwPage.txtMobile.text.toString().replace("\\s".toRegex(), "")
        if (str != "") {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder
                .setMessage("Loading Vehicle data.")
                .setTitle("Please wait.......")
            val dialog: AlertDialog = builder.create()
            dialog.show()
            db.collection(cmp)
                .document("masterdata")
                .collection("customer")
                .whereEqualTo("sts", 1)
                .get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val documents = task.result?.documents ?: emptyList()
                        for (document in documents) {
                            val data = document.data ?: continue
                            //data["name"].toString()
                            //Log.d("mobile", data["mob"].toString())
                            if (data["mob"].toString().uppercase(Locale.getDefault()).contains(
                                    str.uppercase(
                                        Locale.getDefault()
                                    )
                                )
                            ) {
                                //Log.d("mobile contain", data["id"].toString())
                                db.collection(cmp)
                                    .document("masterdata")
                                    .collection("vehicle")
                                    .whereEqualTo("cusId", data["id"].toString().toInt())
                                    .whereEqualTo("sts", 1)
                                    .orderBy("vehNo", com.google.firebase.firestore.Query.Direction.ASCENDING)
                                    .addSnapshotListener { value, error ->
                                        for (dc: DocumentChange in value?.documentChanges!!) {
                                            if (dc.type == DocumentChange.Type.ADDED) {
                                                vehRpt.add(dc.document.toObject(VehicleClass::class.java))
                                            }
                                        }
                                        vehAdp = VehSearchAdaptor(vehRpt, listener)
                                        inwPage.rcyVehicle.adapter = vehAdp
                                        Log.d("error",error.toString())
                                    }
                            }
                        }
                        dialog.dismiss()
                    }
                }
        }
    }

    private fun srcbyVehicle() {
        val listener: OnSelectClickListener = object : OnSelectClickListener {
            override fun OnSelectClicked(id: Int) {
                getVehicledata(id)
            }
        }

        vehRpt.clear()
        vehAdp = VehSearchAdaptor(vehRpt, listener)
        inwPage.rcyVehicle.adapter = vehAdp
        val str = inwPage.txtVno.text.toString().replace("\\s".toRegex(), "")
        if (str != "") {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder
                .setMessage("Loading Vehicle data.")
                .setTitle("Please wait.......")
            val dialog: AlertDialog = builder.create()
            dialog.show()
            db.collection(cmp)
                .document("masterdata")
                .collection("vehicle")
                .whereEqualTo("sts", 1)
                .orderBy("vehNo", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .addSnapshotListener { value, error ->
                    for (dc: DocumentChange in value?.documentChanges!!) {
                        if (dc.type == DocumentChange.Type.ADDED) {
                            if (dc.document.data["vehNo"].toString().uppercase(Locale.getDefault()).contains(
                                    str.uppercase(
                                        Locale.getDefault()
                                    )
                                )) {
                                vehRpt.add(dc.document.toObject(VehicleClass::class.java))
                            }
                        }
                    }
                    vehAdp = VehSearchAdaptor(vehRpt, listener)
                    inwPage.rcyVehicle.adapter = vehAdp
                    Log.d("error",error.toString())
                    dialog.dismiss()
                }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getVehicledata(id : Int) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
            .setMessage("Loading data.")
            .setTitle("Please wait.......")
        val dialog: AlertDialog = builder.create()
        dialog.show()
        if (entId == "0") {
            db.collection(cmp)
                .document("entry")
                .collection("inward")
                .whereEqualTo("vehId", id)
                .whereEqualTo("delId", 0).get()
                .addOnSuccessListener { inwqrySnapshot ->
                    if (inwqrySnapshot.isEmpty) {
                        getVehicleData(id, dialog)
                    } else {
                        dialog.dismiss()
                        val inwdoc = inwqrySnapshot.documents[0]
                        Toast.makeText(this, "Vehicle already Inward on " + (inwdoc.get("dte") as String).toString(), Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            getVehicleData(id, dialog)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getVehicleData(id : Int, dialog : AlertDialog) {
        db.collection(cmp)
            .document("masterdata")
            .collection("vehicle")
            .whereEqualTo("id", id)
            .whereEqualTo("sts", 1).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    vehId = id.toString()
                    val document = querySnapshot.documents[0]
                    cusId = (document.get("cusId") as Number).toString()
                    inwPage.txtVno.setText((document.get("vehNo") as String).toString())
                    inwPage.txtVehicle.text = (document.get("brand") as String).toString() + " - " + (document.get("model") as String).toString()
                    db.collection(cmp)
                        .document("masterdata")
                        .collection("customer")
                        .whereEqualTo("id", (document.get("cusId") as Number).toInt())
                        .whereEqualTo("sts", 1).get()
                        .addOnSuccessListener { cusqrySnapshot ->
                            if (!cusqrySnapshot.isEmpty) {
                                val cusdoc = cusqrySnapshot.documents[0]
                                inwPage.txtName.text = (cusdoc.get("name") as String).toString()
                                inwPage.txtMobile.setText((cusdoc.get("mob") as String).toString())
                            }

                            db.collection(cmp)
                                .document("entry")
                                .collection("inward")
                                .whereEqualTo("vehNo", id)
                                .orderBy("id", com.google.firebase.firestore.Query.Direction.ASCENDING)
                                .limitToLast(1).get()
                                .addOnSuccessListener { qrySnapshot ->
                                    if (!qrySnapshot.isEmpty) {
                                        val inwdocument = qrySnapshot.documents[0]
                                        inwPage.txtLastIInw.text = (inwdocument.get("date") as String).toString()
                                        db.collection(cmp)
                                            .document("masterdata")
                                            .collection("service")
                                            .whereEqualTo("id", (inwdocument.get("serId") as Number).toInt())
                                            .whereEqualTo("sts", 1).get()
                                            .addOnSuccessListener { serqrySnapshot ->
                                                if (!serqrySnapshot.isEmpty) {
                                                    val serdoc =
                                                        serqrySnapshot.documents[0]
                                                    inwPage.txtLastSer.text =
                                                        (serdoc.get("name") as String).toString()
                                                }
                                            }
                                        db.collection(cmp)
                                            .document("entry")
                                            .collection("delivery")
                                            .whereEqualTo("id", (inwdocument.get("delId") as Number).toInt()).get()
                                            .addOnSuccessListener { delqrySnapshot ->
                                                if (!delqrySnapshot.isEmpty) {
                                                    val deldoc =
                                                        delqrySnapshot.documents[0]
                                                    inwPage.txtLastIDel.text =
                                                        (deldoc.get("date") as String).toString()
                                                }
                                            }
                                    }
                                }


                            val listener: OnSelectClickListener = object : OnSelectClickListener {
                                override fun OnSelectClicked(id: Int) {
                                    getVehicledata(id)
                                }
                            }

                            vehRpt.clear()
                            vehAdp = VehSearchAdaptor(vehRpt, listener)
                            inwPage.rcyVehicle.adapter = vehAdp
                            dialog.dismiss()
                            //Log.d("VehNo", vehId)
                        }
                }
            }
    }

    private fun loadInwardId() {
        db.collection(cmp)
            .document("entry")
            .collection("inward")
            .orderBy("id", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .limitToLast(1).get()
            .addOnSuccessListener { qrySnapshot ->
                if (qrySnapshot.isEmpty) {
                    addInward(1)
                } else {
                    val document = qrySnapshot.documents[0]
                    //cmnCls.tmp = document.get("id") as Number
                    val aId : Number = document.get("id") as Number
                    val aid = aId.toInt() + 1
                    Log.d("name", aid.toString())
                    addInward(aid)
                }
            }
    }

    @SuppressLint("SimpleDateFormat")
    private fun addInward(id : Int) {
        imgList.clear()
        imgUri.clear()
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
            .setMessage("Adding your new Inward data for ${inwPage.txtVno.text}")
            .setTitle("Please wait.......")
        val dialog: AlertDialog = builder.create()
        dialog.show()
        sdf = SimpleDateFormat("dd-MM-yyyy hh:mm:ss aaa")
        val cdate = sdf.format(Date()).toString()
        sdf = SimpleDateFormat("dd-MM-yyyy")
        val dte = sdf.format(Date()).toString()
        sdf = SimpleDateFormat("hh:mm:ss aaa")
        val tme = sdf.format(Date()).toString()
        val data = hashMapOf(
            "cdate"   to cdate,
            "dte"     to dte,
            "id"      to id,
            "vehId"   to vehId.toInt(),
            "serId"   to serId.toInt(),
            "usrId"   to usr,
            "brnId"   to brnId.toInt(),
            "delId"   to 0,
            "cusId"   to cusId.toInt(),
            "tme"     to tme
        )
        db.collection(cmp)
            .document("entry")
            .collection("inward")
            .document(id.toString())
            .set(data)
            .addOnSuccessListener {
                db.collection(cmp)
                    .document("masterdata")
                    .collection("imgname")
                    .whereEqualTo("sts", 1)
                    .orderBy("id", com.google.firebase.firestore.Query.Direction.ASCENDING)
                    .get().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val documents = task.result?.documents ?: emptyList()
                            for (document in documents) {
                                val imgdata = document.data ?: continue
                                val nme = "img-" + imgdata["id"].toString()
                                imgUri.add("")
                                imgList.add(imgdata["id"].toString())
                                imgName.add(imgdata["name"].toString())
                                db.collection(cmp)
                                    .document("entry")
                                    .collection("inward")
                                    .document(id.toString())
                                    .update(mapOf(
                                        nme to ""
                                    )).addOnSuccessListener {

                                    }
                            }
                            //Toast.makeText(applicationContext, "Inward added for ${inwPage.txtVno.text} is Successfully", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            val mainIntt = Intent(this@InwardActivity, InwardUploadActivity::class.java)
                            mainIntt.putExtra("entNo", id.toString())
                            mainIntt.putExtra("vehNo", vehId)
                            mainIntt.putExtra("vehDet", inwPage.txtVno.text.toString())
                            mainIntt.putExtra("usrtype", usrType)
                            mainIntt.putExtra("inwtype", "add")
                            mainIntt.putStringArrayListExtra("imgList", imgList)
                            mainIntt.putStringArrayListExtra("imgName", imgName)
                            startActivity(mainIntt)
                            finish()
                        }
                    }
            }
            .addOnFailureListener { e ->
                Log.w("TAG", "Error deleting document", e)
                Toast.makeText(applicationContext, "Please check the details", Toast.LENGTH_SHORT).show()
                inwPage.btnNxt.isEnabled = true
            }
    }

    private fun updateInward(id : Int) {
        imgList.clear()
        imgUri.clear()
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
            .setMessage("Updating your Inward data for ${inwPage.txtVno.text}")
            .setTitle("Please wait.......")
        val dialog: AlertDialog = builder.create()
        dialog.show()
        db.collection(cmp)
            .document("entry")
            .collection("inward")
            .document(id.toString())
            .update(mapOf(
                "vehId"   to vehId.toInt(),
                "serId"   to serId.toInt(),
                "usrId"   to usr,
                "brnId"   to brnId.toInt(),
                "delId"   to 0,
                "cusId"   to cusId.toInt(),
            )).addOnSuccessListener {
                db.collection(cmp)
                    .document("masterdata")
                    .collection("imgname")
                    .whereEqualTo("sts", 1)
                    .orderBy("id", com.google.firebase.firestore.Query.Direction.ASCENDING)
                    .get().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val documents = task.result?.documents ?: emptyList()
                            for (document in documents) {
                                val imgdata = document.data ?: continue
                                imgList.add(imgdata["id"].toString())
                                imgName.add(imgdata["name"].toString())
                            }
                            Log.d("imgpath inward count", imgUri.count().toString())

                            //Toast.makeText(applicationContext, "Inward added for ${inwPage.txtVno.text} is Successfully", Toast.LENGTH_SHORT).show()
                            val mainIntt = Intent(this@InwardActivity, InwardUploadActivity::class.java)
                            mainIntt.putExtra("entNo", id.toString())
                            mainIntt.putExtra("vehNo", vehId)
                            mainIntt.putExtra("vehDet", inwPage.txtVno.text.toString())
                            mainIntt.putExtra("usrtype", usrType)
                            mainIntt.putExtra("inwtype", "update")
                            mainIntt.putStringArrayListExtra("imgList", imgList)
                            mainIntt.putStringArrayListExtra("imgName", imgName)
                            //mainIntt.putStringArrayListExtra("imgPath", imgUri)
                            dialog.dismiss()
                            startActivity(mainIntt)
                            finish()
                        }
                    }
            }
            .addOnFailureListener { e ->
                Log.w("TAG", "Error deleting document", e)
                Toast.makeText(applicationContext, "Please check the details", Toast.LENGTH_SHORT).show()
                inwPage.btnNxt.isEnabled = true
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
}