package com.ats.servicesight

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.telephony.SmsManager
import android.util.Log
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.ats.servicesight.classes.CommonClass
import com.ats.servicesight.databinding.ActivityDeliveryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileDescriptor
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class DeliveryActivity : AppCompatActivity() {
    private var db : FirebaseFirestore = FirebaseFirestore.getInstance()
    private val fbAuth : FirebaseAuth = FirebaseAuth.getInstance()
    private val fbStorage : FirebaseStorage = FirebaseStorage.getInstance()
    private val storageRef : StorageReference = fbStorage.reference
    private var currentUser = fbAuth.currentUser
    private var usr : String = ""
    private var cmp : String = ""
    private var inwId : String = "0"
    private var delId : String = "0"
    private var vehId : String = "0"
    private var inwType : String = "delivery"
    private var imgUri: Uri? = null
    private var imgInwUri = ArrayList<Uri>().toMutableList()
    private var imgDelUri = ArrayList<Uri>().toMutableList()
    private var imgName = ArrayList<String>().toMutableList()
    private var imgIdList = ArrayList<String>().toMutableList()
    private var vehNo : String = "0"
    private var imgId : Int = 1
    private var usrType : String = ""
    private var imgList = ArrayList<String>()
    @SuppressLint("SimpleDateFormat")
    private var sdf = SimpleDateFormat("dd-MM-yyyy hh:mm:ss aaa")
    private lateinit var delPage : ActivityDeliveryBinding
    private var cmnCls = CommonClass()
    private var imageuri: Uri? = null
    private var inputImage : Bitmap? = null
    private var rotated : Bitmap? = null
    private var imgCount = ArrayList<String>()
    private var pageOpen : Int = 1

    private var galleryActivityResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            imageuri = it.data?.data
            inputImage = uriToBitmap(imageuri!!)
            rotated = rotateBitmap(inputImage!!)
            imgUri = imageuri
            delPage.imgVehicle.setImageBitmap(rotated)
        } else {
            imgUri = null
            delPage.imgVehicle.setImageResource(R.drawable.icon_dly)
        }
    }

    private var cameraActivityResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            inputImage = uriToBitmap(imageuri!!)
            rotated = rotateBitmap(inputImage!!)
            imgUri = imageuri
            delPage.imgVehicle.setImageBitmap(rotated)
        } else {
            imgUri = null
            delPage.imgVehicle.setImageResource(R.drawable.icon_dly)
        }
    }

    private fun uriToBitmap(selectedFileUri: Uri): Bitmap? {
        try {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(selectedFileUri, "r")
            val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
            return image
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    @SuppressLint("Range", "Recycle")
    fun rotateBitmap(input: Bitmap): Bitmap {
        val orientationColumn =
            arrayOf(MediaStore.Images.Media.ORIENTATION)
        val cur: Cursor? = contentResolver.query(imageuri!!, orientationColumn, null, null, null)
        var orientation = -1
        if (cur != null && cur.moveToFirst()) {
            orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]))
        }
        Log.d("tryOrientation", orientation.toString() + "")
        val rotationMatrix = Matrix()
        rotationMatrix.setRotate(orientation.toFloat())
        return Bitmap.createBitmap(input, 0, 0, input.width, input.height, rotationMatrix, true)
    }

    private fun convertImageToWebP(imageUri: Uri): ByteArray {
        val drawable = delPage.imgVehicle.drawable
        var oriWdt = 412
        var oriHgt = 915
        if (drawable is BitmapDrawable) {
            //oriWdt = drawable.bitmap.width / 3
            //oriHgt = drawable.bitmap.height / 3
            oriWdt = drawable.bitmap.width
            oriHgt = drawable.bitmap.height
            // Now you have the original width and height
            //println("Original Image Width: $originalWidth, Original Image Height: $originalHeight")
        }
        val inputStream = contentResolver.openInputStream(imageUri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val resizedBitmap = bitmap.scale(oriWdt, oriHgt)
        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.WEBP, 100, outputStream)
        return outputStream.toByteArray()
    }

    override fun onStart(){
        super.onStart()
        val hmeIntt = Intent(this@DeliveryActivity, RecyclerActivity::class.java)
        if (pageOpen == 0) {
            hmeIntt.putExtra("usrtype", usrType)
            hmeIntt.putExtra("type", "delivery")
            startActivity(hmeIntt)
            finish()
        }
    }

    @SuppressLint("SetTextI18n", "SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        delPage = ActivityDeliveryBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(delPage.root)

        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = this.resources.getColor(R.color.heading)
        val uri : Uri = ("android.resource://" + packageName + "/" + R.drawable.icon_dly).toUri()
        val directory = this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val hmeIntt = Intent(this@DeliveryActivity, RecyclerActivity::class.java)
        inwId = intent.getStringExtra("inwId").toString()
        delId = intent.getStringExtra("delId").toString()
        usrType = intent.getStringExtra("usrtype").toString()
        imgCount.clear()
        /*val imgList = intent?.getStringArrayListExtra("imgList")
        if (imgList != null) {
            imgCount.addAll(imgList)
        }*/
        currentUser = fbAuth.currentUser
        usr = fbAuth.currentUser?.email.toString()
        //Log.d("entNo", entId)

        cmp = if (usr != "" && usr != "null") {
            usr.substring(usr.indexOf(".") + 1, usr.length)
        } else {
            ""
        }

        delPage.prgRefresh.isVisible = false
        delPage.txtImageHead.text = "Preview of Delivery Image"
        loadData(inwId.toInt())
        //generateDeliveryData(inwId.toInt())
        loadImageId(uri, directory)

        delPage.btnDelivery.isEnabled = false
        delPage.btnInward.isEnabled = true

        delPage.btnInward.setOnClickListener {
            delPage.txtImageHead.text = "Preview of Inward Image"
            delPage.btnDelivery.isEnabled = true
            delPage.btnInward.isEnabled = false
            delPage.btnUpdate.isEnabled = false
            delPage.btnPhoto.isEnabled = false
            delPage.btnCamera.isEnabled = false
            inwType = "inward"
            //loadImg(imgId, "fwd", "inward", inwId.toInt())
            loadLocalImg("fwd", "inward", uri)
        }

        delPage.btnDelivery.setOnClickListener {
            delPage.txtImageHead.text = "Preview of Delivery Image"
            delPage.btnDelivery.isEnabled = false
            delPage.btnInward.isEnabled = true
            delPage.btnUpdate.isEnabled = true
            delPage.btnPhoto.isEnabled = true
            delPage.btnCamera.isEnabled = true
            inwType = "delivery"
            //loadImg(imgId, "fwd", "delivery", delId.toInt())
            loadLocalImg("fwd", "delivery", uri)
        }

        delPage.btnPhoto.setOnClickListener {
            val galleryIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryActivityResultLauncher.launch(galleryIntent)
        }

        delPage.btnCamera.setOnClickListener {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_DENIED
            ) {
                val permission = arrayOf(
                    android.Manifest.permission.CAMERA
                )
                requestPermissions(permission, 112)
                Log.d("error", "not open")
            } else {
                Log.d("error", "open")
                openCamera()
            }
        }

        delPage.btnBefore.setOnClickListener {
            if (imgId > 1) {
                imgId -= 1
            }
            /*val id = if (inwType == "inward") {
                inwId.toInt()
            } else {
                delId.toInt()
            }
            loadImg(imgId, "bwd", inwType, id)*/
            loadLocalImg("bwd", inwType, uri)
        }

        delPage.btnNext.setOnClickListener {
            imgId += 1
            /*val id = if (inwType == "inward") {
                inwId.toInt()
            } else {
                delId.toInt()
            }
            loadImg(imgId, "fwd", inwType, id)*/
            loadLocalImg("fwd", inwType, uri)
        }

        delPage.btnUpdate.setOnClickListener {
            delPage.btnUpdate.isEnabled = false
            if (imgUri == null) {
                Toast.makeText(this, "Please check the Image before update.", Toast.LENGTH_SHORT).show()
                delPage.btnUpdate.isEnabled = true
            } else {
                //uploadStorage(delId, imgId.toString(), imgUri!!)
                addToList(imgId.toString(),imgUri!!, uri)
            }
        }

        delPage.btnFinish.setOnClickListener {
            generateDeliveryData(inwId.toInt())
            /*db.collection(cmp)
                .document("entry")
                .collection("inward")
                .whereEqualTo("id", inwId.toInt()).get()
                .addOnSuccessListener { inwqrySnapshot ->
                    if (!inwqrySnapshot.isEmpty) {
                        val document = inwqrySnapshot.documents[0]
                        db.collection(cmp)
                            .document("masterdata")
                            .collection("customer")
                            .whereEqualTo("id", (document.get("cusId") as Number).toInt())
                            .whereEqualTo("sts", 1).get()
                            .addOnSuccessListener { cusqrySnapshot ->
                                if (!cusqrySnapshot.isEmpty) {
                                    val cusdoc = cusqrySnapshot.documents[0]
                                    //inwPage.txtName.text = (cusdoc.get("name") as String).toString()
                                    //inwPage.txtMobile.setText((cusdoc.get("mob") as String).toString())
                                    val url = ""
                                    //sendSMS((cusdoc.get("mob") as String).toString(), "Your Vehicle No. $vehNo is Delivered on ${document.get("cdte") as String}. Click the link to view your Vehicle Delivered Spot Pics $url. Thank you")
                                    sendWhatsapp((cusdoc.get("mob") as String).toString(), "Your Vehicle No. $vehNo is Delivered on ${document.get("cdte") as String}. Click the link to view your Vehicle Delivered Spot Pics $url. Thank you")
                                    /*val inwIntt = Intent(this@DeliveryActivity, RecyclerActivity::class.java)
                                    inwIntt.putExtra("usrtype", usrType)
                                    inwIntt.putExtra("type", "delivery")
                                    startActivity(inwIntt)
                                    finish()*/
                                }
                            }
                    }
                }*/
        }

        delPage.btnBack.setOnClickListener {
            hmeIntt.putExtra("usrtype", usrType)
            hmeIntt.putExtra("type", "delivery")
            startActivity(hmeIntt)
            finish()
        }

        val onBackPressedCallback = object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                hmeIntt.putExtra("usrtype", usrType)
                hmeIntt.putExtra("type", "delivery")
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

    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        imageuri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageuri)
        cameraActivityResultLauncher.launch(cameraIntent)
    }

    private fun loadData(id : Int) {
        db.collection(cmp)
            .document("entry")
            .collection("inward")
            .whereEqualTo("id", id).get()
            .addOnSuccessListener { inwqrySnapshot ->
                if (!inwqrySnapshot.isEmpty) {
                    val document = inwqrySnapshot.documents[0]
                    db.collection(cmp)
                        .document("masterdata")
                        .collection("vehicle")
                        .whereEqualTo("id", (document.get("vehId") as Number).toInt())
                        .whereEqualTo("sts", 1).get()
                        .addOnSuccessListener { querySnapshot ->
                            if (!querySnapshot.isEmpty) {
                                vehId = (document.get("vehId") as Number).toString()
                                val vehdoc = querySnapshot.documents[0]
                                delPage.txtVno.text = vehdoc.get("vehNo") as String
                            }
                        }
                }
            }
    }

    private fun generateDeliveryData(inwId : Int) {
        db.collection(cmp)
            .document("entry")
            .collection("delivery")
            .orderBy("id", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .limitToLast(1).get()
            .addOnSuccessListener { qrySnapshot ->
                if (qrySnapshot.isEmpty) {
                    delId = "1"
                    addDelivery(1, inwId)
                } else {
                    val document = qrySnapshot.documents[0]
                    //cmnCls.tmp = document.get("id") as Number
                    val aId : Number = document.get("id") as Number
                    val aid = aId.toInt() + 1
                    Log.d("name", aid.toString())
                    delId = aid.toString()
                    addDelivery(aid, inwId)
                }
            }
    }

    @SuppressLint("SimpleDateFormat")
    private fun addDelivery(delId : Int, inwId : Int) {
        imgCount.clear()
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
            .setMessage("Delivery Image is uploading")
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
            "id"      to delId,
            "inwId"   to inwId,
            "usrId"   to usr,
            "tme"     to tme
        )
        db.collection(cmp)
            .document("entry")
            .collection("delivery")
            .document(delId.toString())
            .set(data)
            .addOnSuccessListener {
                db.collection(cmp)
                    .document("entry")
                    .collection("inward")
                    .document(inwId.toString())
                    .update(mapOf(
                        "delId" to delId
                    )).addOnSuccessListener {

                    }
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
                                imgCount.add(imgdata["id"].toString())
                                db.collection(cmp)
                                    .document("entry")
                                    .collection("delivery")
                                    .document(delId.toString())
                                    .update(mapOf(
                                        nme to ""
                                    )).addOnSuccessListener {

                                    }
                            }
                            //Toast.makeText(applicationContext, "Inward added for ${inwPage.txtVno.text} is Successfully", Toast.LENGTH_SHORT).show()
                            uploadOnline(dialog)
                            //dialog.dismiss()
                            //loadImg(1, "fwd", "delivery", delId)
                        }
                    }
            }
            .addOnFailureListener { e ->
                //Log.w("TAG", "Error deleting document", e)
                //Toast.makeText(applicationContext, "Please check the details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadImg(id : Int, typ : String, ent : String, eid : Int) {
        //inwPage.txtPrg.isVisible = false
        //inwPage.prgRefresh.isVisible = true
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
            .setMessage("Image Loading")
            .setTitle("Please wait.......")
        val dialog: AlertDialog = builder.create()
        dialog.show()
        var per = 0.0
        val img = imgCount.count()
        //Log.d("img Count", imgCount.count().toString())
        //Log.d("img Last", imgCount.last().toString())
        if (id > imgCount.last().toInt()) {
            //Log.d("img greater", imgId.toString())
            imgId = 1
            dialog.dismiss()
            loadImg(imgId, typ, ent, eid)
        } else {
            db.collection(cmp)
                .document("masterdata")
                .collection("imgname")
                .whereEqualTo("id", id)
                .whereEqualTo("sts", 1).get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val imgdoc = querySnapshot.documents[0]
                        delPage.txtLocation.text = imgdoc.get("name") as String
                        db.collection(cmp)
                            .document("entry")
                            .collection(ent)
                            .whereEqualTo("id", eid).get()
                            .addOnSuccessListener { imgqrySnapshot ->
                                if (!imgqrySnapshot.isEmpty) {
                                    val imgdocu = imgqrySnapshot.documents[0]
                                    //inwPage.txtLocation.text = imgdocu.get("name") as String
                                    if ((imgdocu.get("img-$id") as String) != "") {
                                        cmnCls.loadImg(imgdocu.get("img-$id") as String, delPage.imgVehicle)
                                    } else {
                                        delPage.imgVehicle.setImageResource(R.drawable.icon_dly)
                                    }

                                    for (i in 0 until imgCount.count()) {
                                        if ((imgdocu.get("img-" + imgCount[i]) as String) != "") {
                                            per += 1
                                        }
                                    }
                                    Log.d("img value", img.toString())
                                    Log.d("per value", per.toString())
                                    per = (per / img) * 100
                                    Log.d("per percentage", per.toString())
                                    progressValue(per.toInt())
                                    dialog.dismiss()
                                }
                            }
                    } else {
                        if (typ == "fwd") {
                            imgId += 1
                        } else {
                            imgId -= 1
                        }
                        progressValue(per.toInt())
                        dialog.dismiss()
                        loadImg(imgId, typ, ent, eid)
                    }
                }
        }
    }

    /*private fun uploadStorage(id : String, imgid : String, fUri : Uri) {
        val imageData: ByteArray = convertImageToWebP(fUri)
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
            .setMessage("Image is uploading")
            .setTitle("Please wait.......")
        val dialog: AlertDialog = builder.create()
        dialog.show()
        delPage.txtPrg.isVisible = false
        delPage.prgRefresh.isVisible = true
        fbStorage.getReference().child(cmp).child("delivery").child(id)
            .child("img-$imgid").putBytes(imageData).addOnSuccessListener {
                storageRef.child(cmp).child("delivery").child(id)
                    .child("img-$imgid").downloadUrl.addOnSuccessListener { uri: Uri ->
                        val url = uri.toString()
                        updateImage("img-$imgid", url, dialog)
                    }.addOnFailureListener { exception ->
                        Log.d("Error getting download URI:",exception.message.toString())
                    }
            }.addOnFailureListener {
                Toast.makeText(applicationContext, "Fail to Upload Image..", Toast.LENGTH_SHORT).show()
            }
        storageRef.child(cmp).child("delivery").child(id)

    }

    private fun updateImage(fld : String, pth : String, dialog: AlertDialog) {

        db.collection(cmp)
            .document("entry")
            .collection("delivery")
            .document(delId)
            .update(mapOf(
                fld to pth
            )).addOnSuccessListener {
                //Toast.makeText(applicationContext, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                Log.d("Image path : $fld", pth)
                imgUri = null
                delPage.btnUpdate.isEnabled = true
                delPage.txtPrg.isVisible = true
                delPage.prgRefresh.isVisible = false
                imgId += 1
                loadImg(imgId, "fwd", "delivery", delId.toInt())
                dialog.dismiss()
            }
    }*/


    private fun uploadStorage(id : String, imgid : String, fUri : Uri, str : String, dialog: AlertDialog) {

        val imageData: ByteArray = convertImageToWebP(fUri)
        delPage.txtPrg.isVisible = false
        delPage.prgRefresh.isVisible = true
        fbStorage.getReference().child(cmp).child("delivery").child(id)
            .child("img-$imgid").putBytes(imageData).addOnSuccessListener {
                storageRef.child(cmp).child("delivery").child(id)
                    .child("img-$imgid").downloadUrl.addOnSuccessListener { uri: Uri ->
                        val url = uri.toString()
                        updateImage("img-$imgid", url, dialog, str)
                    }.addOnFailureListener { exception ->
                        //Log.d("Error getting download URI:",exception.message.toString())
                        endPage(str, dialog)
                    }
            }.addOnFailureListener {
                Toast.makeText(applicationContext, "Fail to Upload Image..", Toast.LENGTH_SHORT).show()
                endPage(str, dialog)
            }
        storageRef.child(cmp).child("delivery").child(id)
    }

    private fun updateImage(fld : String, pth : String, dialog: AlertDialog, str : String) {
        db.collection(cmp)
            .document("entry")
            .collection("delivery")
            .document(delId)
            .update(mapOf(
                fld to pth
            )).addOnSuccessListener {
                endPage(str, dialog)
            }
    }

    @SuppressLint("SetTextI18n")
    private fun progressValue(txt : Int) {
        delPage.prgBar.progress = txt
        delPage.txtPrg.text = "$txt %"
        delPage.btnFinish.isVisible = delPage.txtPrg.text.toString() == "100 %"
    }

    private fun getImageCount() {
        imgCount.clear()
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
                        imgCount.add(imgdata["id"].toString())
                    }
                    Log.d ("img cnt", imgCount.count().toString())
                }
            }
    }

    private fun sendWhatsapp(mob : String, msg : String) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                String.format(
                    "https://api.whatsapp.com/send?phone=%s&text=%s",
                    mob,
                    msg
                ).toUri()
            )
        )
        val sentPI: PendingIntent = PendingIntent.getBroadcast(this, 0, Intent("SMS_SENT"),
            PendingIntent.FLAG_IMMUTABLE)
        SmsManager.getDefault().sendTextMessage(mob, null, msg, sentPI, null)
    }

    private fun sendSMS(mob : String, msg : String) {
        /*try {
            val smsManager: SmsManager = this.getSystemService(SmsManager::class.java)

            smsManager.sendTextMessage(mob, null, msg, null, null)

            Toast.makeText(applicationContext, "Message Sent", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(applicationContext, "Please enter all the data.."+e.message.toString(), Toast.LENGTH_LONG)
                .show()
        }*/
        val sentPI: PendingIntent = PendingIntent.getBroadcast(this, 0, Intent("SMS_SENT"),
            PendingIntent.FLAG_IMMUTABLE)
        SmsManager.getDefault().sendTextMessage(mob, null, msg, sentPI, null)
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                String.format(
                    "https://api.whatsapp.com/send?phone=%s&text=%s",
                    mob,
                    msg
                ).toUri()
            )
        )
    }

    private fun loadLocalImg(typ : String, imgTyp : String, uri : Uri) {
        if (imgId > imgIdList.last().toInt()) {
            imgId = 1
            loadLocalImg(typ,imgTyp, uri)
        } else {
            if (imgName[imgId] == "") {
                if (typ == "fwd") {
                    imgId += 1
                } else if (typ == "bwd") {
                    imgId -= 1
                }
                loadLocalImg(typ, imgTyp, uri)
            } else {
                delPage.txtLocation.text = imgName[imgId]
                if (imgTyp == "inward") {
                    delPage.imgVehicle.setImageURI(imgInwUri[imgId])
                } else {
                    delPage.imgVehicle.setImageURI(imgDelUri[imgId])
                    loadPercentage(uri)
                }
            }
        }
    }

    private fun loadImageId(uri : Uri, directory: File?) {
        imgIdList.clear()
        imgInwUri.clear()
        imgDelUri.clear()
        imgName.clear()
        var x = 0
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
            .setMessage("Delivery detail is loading")
            .setTitle("Please wait.......")
        val dialog: AlertDialog = builder.create()
        dialog.show()
        db.collection(cmp)
            .document("masterdata")
            .collection("imgname")
            .whereEqualTo("sts", 1)
            .orderBy("id", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .limitToLast(1).get()
            .addOnSuccessListener { qrySnapshot ->
                val document = qrySnapshot.documents[0]
                val aId : Number = document.get("id") as Number
                for (i in 0 until aId.toInt() + 1) {
                    imgIdList.add(i, i.toString())
                    imgInwUri.add(i, uri)
                    imgDelUri.add(i, uri)
                    imgName.add(i, "")
                    db.collection(cmp)
                        .document("masterdata")
                        .collection("imgname")
                        .whereEqualTo("id", i)
                        .whereEqualTo("sts", 1)
                        .get().addOnSuccessListener { imgQry ->
                            if (!imgQry.isEmpty) {
                                val imgdoc = imgQry.documents[0]
                                imgName[i] = imgdoc.get("name") as String
                                db.collection(cmp)
                                    .document("entry")
                                    .collection("inward")
                                    .whereEqualTo("id", inwId.toInt()).get()
                                    .addOnSuccessListener { imgqrySnapshot ->
                                        if (!imgqrySnapshot.isEmpty) {
                                            val imgdocu = imgqrySnapshot.documents[0]
                                            if ((imgdocu.get("img-$i") as String) != "") {
                                                x += 1
                                                downloadToLocal(i, "img-$i.webp",directory, dialog)
                                            }
                                        }
                                    }
                            }

                        }
                }
            }
        db.collection(cmp)
            .document("masterdata")
            .collection("imgname")
            .whereEqualTo("sts", 1)
            .orderBy("id", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limitToLast(1).get()
            .addOnSuccessListener { qrySnapshot ->
                val document = qrySnapshot.documents[0]
                delPage.txtLocation.text = document.get("name") as String
                delPage.imgVehicle.setImageURI(uri)
            }

    }

    private fun downloadToLocal(
        i: Int,
        fbImagePath: String,
        directory: File?,
        dialog: AlertDialog
    ) {
        val localFile = File(directory, fbImagePath)
        storageRef.child(cmp).child("inward")
            .child(inwId).child("img-$i").getFile(localFile)
            .addOnSuccessListener {
                val imageUri: Uri = localFile.toUri()
                imgInwUri[i] = imageUri
                //Log.d("Downloaded ids", "$i - $imageUri")
                if (i == 1) {
                    dialog.dismiss()
                }
            }
            .addOnFailureListener { exception ->
                Log.d("download error", exception.message.toString())
            }
    }

    private fun loadPercentage(uri : Uri){
        var ig = 0
        var per = 0.0
        for (j in 0 until imgIdList.last().toInt() + 1) {
            if (imgDelUri[j] != uri && imgName[j] != "") {
                per += 1
            }
            if (imgName[j] != "") {
                ig += 1
            }
        }
        per = (per / ig) * 100
        progressValue(per.toInt())
    }

    private fun addToList(imgid : String, fUri : Uri, uri : Uri) {
        imgDelUri[imgid.toInt()] = fUri
        imgId += 1
        loadLocalImg("fwd","delivery", uri)
        delPage.btnUpdate.isEnabled = true
    }

    private fun uploadOnline(dialog: AlertDialog) {
        val uri : Uri = ("android.resource://" + packageName + "/" + R.drawable.icon_dly).toUri()
        for (j in 0 until imgIdList.last().toInt() + 1) {
            var str = "start"
            if (j == imgIdList.last().toInt()) {
                str = "end"
            }
            if (imgDelUri[j] != uri && imgName[j] != "") {
                uploadStorage(delId, j.toString(), imgDelUri[j], str, dialog)
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun endPage(str : String, dialog: AlertDialog){
        if (str == "end") {
            sdf = SimpleDateFormat("dd-MM-yyyy hh:mm:ss aaa")
            val cdate = sdf.format(Date()).toString()
            dialog.dismiss()
            db.collection(cmp)
                .document("entry")
                .collection("inward")
                .whereEqualTo("id", inwId.toInt()).get()
                .addOnSuccessListener { inwqrySnapshot ->
                    if (!inwqrySnapshot.isEmpty) {
                        val document = inwqrySnapshot.documents[0]
                        db.collection(cmp)
                            .document("masterdata")
                            .collection("customer")
                            .whereEqualTo("id", (document.get("cusId") as Number).toInt())
                            .whereEqualTo("sts", 1).get()
                            .addOnSuccessListener { cusqrySnapshot ->
                                if (!cusqrySnapshot.isEmpty) {
                                    val cusdoc = cusqrySnapshot.documents[0]
                                    val url = "http://27.100.26.35"
                                    val mob = "+91" + cusdoc.get("mob") as String
                                    sendSMS(mob, "Your Vehicle No. $vehNo is Delivered on $cdate. Click the link to view your Vehicle Delivered Spot Pics $url. Thank you")
                                    sendWhatsapp(mob, "Your Vehicle No. $vehNo is Delivered on $cdate. Click the link to view your Vehicle Delivered Spot Pics $url. Thank you")
                                    pageOpen = 0
                                }
                            }
                    }
                }
        }
    }

}