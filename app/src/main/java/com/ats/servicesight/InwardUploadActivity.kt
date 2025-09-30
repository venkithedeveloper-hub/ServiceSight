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
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.ats.servicesight.classes.CommonClass
import com.ats.servicesight.databinding.ActivityInwardUploadBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.io.FileDescriptor
import java.io.IOException
import androidx.core.graphics.scale
import java.io.File
import kotlin.collections.count

class InwardUploadActivity : AppCompatActivity() {
    private var db : FirebaseFirestore = FirebaseFirestore.getInstance()
    private val fbAuth : FirebaseAuth = FirebaseAuth.getInstance()
    private val fbStorage : FirebaseStorage = FirebaseStorage.getInstance()
    private val storageRef : StorageReference = fbStorage.reference
    private var currentUser = fbAuth.currentUser
    private var usr : String = ""
    private var cmp : String = ""
    private var entId : String = "0"
    private var vehId : String = "0"
    private var vehNo : String = "0"
    private var imgId : Int = 1
    private var imgUri: Uri? = null
    private var imgUriList = ArrayList<Uri>().toMutableList()
    private var imgUriId = ArrayList<String>().toMutableList()
    private var imgIdList = ArrayList<String>().toMutableList()
    private var imgName = ArrayList<String>().toMutableList()
    private var imgNme = ArrayList<String>()
    private var usrType : String = ""
    private var inwType : String = ""
    private lateinit var inwPage : ActivityInwardUploadBinding
    private var cmnCls = CommonClass()
    private var imgCount = ArrayList<String>()
    private var pageOpen : Int = 1
    private var imageuri: Uri? = null
    private var inputImage : Bitmap? = null
    private var rotated : Bitmap? = null
    private var galleryActivityResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            imageuri = it.data?.data
            inputImage = uriToBitmap(imageuri!!)
            rotated = rotateBitmap(inputImage!!)
            imgUri = imageuri
            inwPage.imgVehicle.setImageBitmap(rotated)
        } else {
            imgUri = null
            inwPage.imgVehicle.setImageResource(R.drawable.icon_dly)
        }
    }

    
    private var cameraActivityResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            inputImage = uriToBitmap(imageuri!!)
            rotated = rotateBitmap(inputImage!!)
            imgUri = imageuri
            inwPage.imgVehicle.setImageBitmap(rotated)
        } else {
            imgUri = null
            inwPage.imgVehicle.setImageResource(R.drawable.icon_dly)
        }
    }

    
    private fun uriToBitmap(selectedFileUri: Uri): Bitmap? {
        try {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(selectedFileUri, "r")
            val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
            //image.compress(Bitmap.CompressFormat.PNG,10, ByteArrayOutputStream())
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
        //Log.d("tryOrientation", orientation.toString() + "")
        val rotationMatrix = Matrix()
        rotationMatrix.setRotate(orientation.toFloat())
        return Bitmap.createBitmap(input, 0, 0, input.width, input.height, rotationMatrix, true)
    }

    private fun convertImageToWebP(imageUri: Uri): ByteArray {
        val drawable = inwPage.imgVehicle.drawable
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
        //Log.d("width", oriWdt.toString())
        val inputStream = contentResolver.openInputStream(imageUri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val resizedBitmap = bitmap.scale(oriWdt, oriHgt)
        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.WEBP, 100, outputStream)
        return outputStream.toByteArray()
    }

    override fun onStart(){
        super.onStart()
        val hmeIntt = Intent(this@InwardUploadActivity, RecyclerActivity::class.java)
        if (pageOpen == 0) {
            hmeIntt.putExtra("usrtype", usrType)
            hmeIntt.putExtra("type", "inward")
            startActivity(hmeIntt)
            finish()
        }
    }
    
    @SuppressLint("SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        inwPage = ActivityInwardUploadBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(inwPage.root)

        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = this.resources.getColor(R.color.heading)

        currentUser = fbAuth.currentUser
        usr = fbAuth.currentUser?.email.toString()
        //Log.d("entNo", "$entId - $vehId")

        cmp = if (usr != "" && usr != "null") {
            usr.substring(usr.indexOf(".") + 1, usr.length)
        } else {
            ""
        }
        /*val directory = this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS + "/fbtempimg")
        directory?.exists()?.let {
            if (!it) {
                directory.mkdirs()
                Log.d("directory", "create")
            } else {
                directory.delete()
                directory.mkdirs()
                Log.d("directory", "delete & create")
            }
        }*/
        val directory = this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val hmeIntt = Intent(this@InwardUploadActivity, RecyclerActivity::class.java)

        entId = intent.getStringExtra("entNo").toString()
        vehId = intent.getStringExtra("vehNo").toString()
        vehNo = intent.getStringExtra("vehDet").toString()
        usrType = intent.getStringExtra("usrtype").toString()
        inwType = intent.getStringExtra("inwtype").toString()
        imgCount.clear()
        imgUriList.clear()
        imgUriId.clear()
        imgIdList.clear()
        imgName.clear()
        imgNme.clear()
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
            .setMessage("Image Loading")
            .setTitle("Please wait.......")
        val dialog: AlertDialog = builder.create()
        dialog.show()
        var listId = intent?.getStringArrayListExtra("imgList")
        if (listId != null) {
            imgIdList.addAll(listId)
        }
        listId?.clear()
        listId = intent?.getStringArrayListExtra("imgName")
        if (listId != null) {
            imgNme.addAll(listId)
        }
        listId?.clear()
        val uri : Uri = ("android.resource://" + packageName + "/" + R.drawable.icon_dly).toUri()
        //Log.d("Img count", imgIdList.count().toString())
        for (i in 0 until imgIdList.last().toInt() + 1) {
            imgUriList.add(i, uri)
            //imgUriList.add(i, uri)
            imgName.add(i, "")
        }

        /*db.collection(cmp)
            .document("masterdata")
            .collection("imgname")
            .whereEqualTo("sts", 1)
            .orderBy("id", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val documents = task.result?.documents ?: emptyList()
                    for (document in documents) {
                        val imgdata = document.data ?: continue
                        imgName.add(imgdata["name"].toString())
                        db.collection(cmp)
                            .document("entry")
                            .collection("inward")
                            .whereEqualTo("id", entId.toInt()).get()
                            .addOnSuccessListener { imgqrySnapshot ->
                                if (!imgqrySnapshot.isEmpty) {
                                    val imgdocu = imgqrySnapshot.documents[0]
                                    if ((imgdocu.get("img-" + imgdata["id"].toString()) as String) != "") {
                                        Log.d("Image Path", imgdocu.get("img-" + imgdata["id"].toString()) as String)
                                        imgUriList[imgdata["id"].toString().toInt()] = (imgdocu.get("img-" + imgdata["id"].toString()) as String).toUri()
                                    }
                                }
                            }
                    }
                }
            }*/


        val imgList = intent?.getStringArrayListExtra("imgList")
        if (imgList != null) {
            imgCount.addAll(imgList)
        }

        //getImageCount()
        //inwPage.btnUpdate.isVisible = false
        inwPage.prgRefresh.isVisible = false

        inwPage.txtVno.text = vehNo

        //loadImg(1, "fwd")

        var x = 0
        for (i in 0 until imgNme.count()) {
            db.collection(cmp)
                .document("entry")
                .collection("inward")
                .whereEqualTo("id", entId.toInt()).get()
                .addOnSuccessListener { imgqrySnapshot ->
                    if (!imgqrySnapshot.isEmpty) {
                        val imgdocu = imgqrySnapshot.documents[0]
                        if ((imgdocu.get("img-" + imgIdList[i]) as String) != "") {
                            downloadToLocal(imgIdList[i].toInt(),"img-" + imgIdList[i] + ".webp",directory, x, dialog)
                            x =+ 1
                        }
                    }
                }


            imgName[imgIdList[i].toInt()] = imgNme[i]
            Log.d("Img Name & ID", imgIdList[i] + "-" + imgNme[i])
        }

        if (x == 0) {
            /*inwPage.txtLocation.text = imgName[imgId]
            inwPage.imgVehicle.setImageURI(imgUriList[imgId])*/
            imgId = 1
            loadLocalImg("fwd")
            dialog.dismiss()
        }

        inwPage.btnPhoto.setOnClickListener {
            val galleryIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryActivityResultLauncher.launch(galleryIntent)
        }

        inwPage.btnCamera.setOnClickListener {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_DENIED
            ) {
                val permission = arrayOf(
                    android.Manifest.permission.CAMERA
                )
                requestPermissions(permission, 112)
                //Log.d("error", "not open")
            } else {
                //Log.d("error", "open")
                openCamera()
            }
        }

        inwPage.btnBefore.setOnClickListener {
            if (imgId > 1) {
                imgId -= 1
            }
            //loadImg(imgId, "bwd")
            //loadLocalImg(imgUriList[imgId])
            /*inwPage.imgVehicle.setImageURI(imgUriList[imgId])
            inwPage.txtLocation.text = imgName[imgId]*/
            loadLocalImg( "bwd")
        }

        inwPage.btnNext.setOnClickListener {
            imgId += 1
            //loadImg(imgId, "fwd")
            /*inwPage.imgVehicle.setImageURI(imgUriList[imgId])
            inwPage.txtLocation.text = imgName[imgId]*/
            loadLocalImg("fwd")
        }

        inwPage.btnUpdate.setOnClickListener {
            inwPage.btnUpdate.isEnabled = false
            if (imgUri == null) {
                Toast.makeText(this, "Please check the Image before update.", Toast.LENGTH_SHORT).show()
                inwPage.btnUpdate.isEnabled = true
            } else {
                //uploadStorage(entId, imgId.toString(), imgUri!!)
                addToList(imgId.toString(),imgUri!!)
            }
        }

        inwPage.btnFinish.setOnClickListener {
            uploadOnline()
            /*db.collection(cmp)
                .document("entry")
                .collection("inward")
                .whereEqualTo("id", entId.toInt()).get()
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
                                    val url = "http://27.100.26.35"
                                    val mob = "+91" + cusdoc.get("mob") as String
                                    Log.d("Customer Mobile", "+91" + cusdoc.get("mob") as String)
                                    //sendSMS(mob, "Your Vehicle No. $vehNo is Inward on ${document.get("cdate") as String}. Click the link to view your Vehicle Inward Spot Pics $url. Thank you")
                                    sendWhatsapp(mob, "Your Vehicle No. $vehNo is Inward on ${document.get("cdate") as String}. Click the link to view your Vehicle Inward Spot Pics $url. Thank you")
                                    /*val inwIntt = Intent(this@InwardUploadActivity, RecyclerActivity::class.java)
                                    inwIntt.putExtra("usrtype", usrType)
                                    inwIntt.putExtra("type", "inward")
                                    startActivity(inwIntt)
                                    finish()*/
                                }
                            }
                    }
                }*/
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

    
    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        imageuri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageuri)
        cameraActivityResultLauncher.launch(cameraIntent)
    }

    /*private fun loadLocalImg(id : Int, typ : String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
            .setMessage("Image Loading")
            .setTitle("Please wait.......")
        val dialog: AlertDialog = builder.create()
        dialog.show()
        var per = 0.0
        val img = imgCount.count()
        if (id > imgCount.last().toInt()) {
            imgId = 1
            dialog.dismiss()
            loadLocalImg(imgId, typ)
        } else {
            var x = -1
            for (position in 0 until imgCount.count()) {
                if (imgCount[position] == imgId.toString()) {
                    x = position
                    break
                }
            }
            Log.d("img uri", imgUriList[x].toString())
            if (x > -1 && imgUriList[x].toString() != "") {
                imgUri = imgUriList[x]
                inputImage = uriToBitmap(imageuri!!)
                rotated = rotateBitmap(inputImage!!)
                imgUri = imageuri
                inwPage.imgVehicle.setImageBitmap(rotated)
            } else {
                inwPage.imgVehicle.setImageResource(R.drawable.icon_dly)
            }

            for (i in 0 until imgUriList.count()) {
                if (imgCount[i] != "") {
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
    }*/

    private fun loadImg(id : Int, typ : String) {
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
        //Log.d("img Last", imgCount.last())
        if (id > imgCount.last().toInt()) {
            imgId = 1
            dialog.dismiss()
            loadImg(imgId, typ)
        } else {
            db.collection(cmp)
                .document("masterdata")
                .collection("imgname")
                .whereEqualTo("id", id)
                .whereEqualTo("sts", 1).get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val imgdoc = querySnapshot.documents[0]
                        inwPage.txtLocation.text = imgdoc.get("name") as String
                        db.collection(cmp)
                            .document("entry")
                            .collection("inward")
                            .whereEqualTo("id", entId.toInt()).get()
                            .addOnSuccessListener { imgqrySnapshot ->
                                if (!imgqrySnapshot.isEmpty) {
                                    val imgdocu = imgqrySnapshot.documents[0]
                                    //inwPage.txtLocation.text = imgdocu.get("name") as String
                                    if ((imgdocu.get("img-$id") as String) != "") {
                                        cmnCls.loadImg(imgdocu.get("img-$id") as String, inwPage.imgVehicle)
                                    } else {
                                        inwPage.imgVehicle.setImageResource(R.drawable.icon_dly)
                                    }

                                    for (i in 0 until imgCount.count()) {
                                        if ((imgdocu.get("img-" + imgCount[i]) as String) != "") {
                                            per += 1
                                        }
                                    }
                                    //Log.d("img value", img.toString())
                                    //Log.d("per value", per.toString())
                                    per = (per / img) * 100
                                    //Log.d("per percentage", per.toString())
                                    progressValue(per.toInt())
                                    dialog.dismiss()
                                    //inwPage.txtPrg.isVisible = true
                                    //inwPage.prgRefresh.isVisible = false
                                }
                            }
                    } else {
                        if (typ == "fwd") {
                            imgId += 1
                        } else {
                            imgId -= 1
                        }
                        //inwPage.txtPrg.isVisible = true
                        //inwPage.prgRefresh.isVisible = false
                        progressValue(per.toInt())
                        dialog.dismiss()
                        loadImg(imgId, typ)
                    }
                }


            /*db.collection(cmp)
            .document("masterdata")
            .collection("imgname")
            .orderBy("id", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .limitToLast(1).get()
            .addOnSuccessListener { qrySnapshot ->
                if (!qrySnapshot.isEmpty) {
                    val document = qrySnapshot.documents[0]
                    //cmnCls.tmp = document.get("id") as Number
                    if (id > (document.get("id") as Number).toInt()) {
                        imgId = 1
                        loadImg(imgId, typ)
                    } else {
                        db.collection(cmp)
                            .document("masterdata")
                            .collection("imgname")
                            .whereEqualTo("id", id)
                            .whereEqualTo("sts", 1).get()
                            .addOnSuccessListener { querySnapshot ->
                                if (!querySnapshot.isEmpty) {
                                    val imgdoc = querySnapshot.documents[0]
                                    inwPage.txtLocation.text = imgdoc.get("name") as String
                                    db.collection(cmp)
                                        .document("entry")
                                        .collection("inward")
                                        .whereEqualTo("id", entId.toInt()).get()
                                        .addOnSuccessListener { imgqrySnapshot ->
                                            if (!imgqrySnapshot.isEmpty) {
                                                var per = 0.0
                                                var img = 0
                                                val imgdocu = imgqrySnapshot.documents[0]
                                                //inwPage.txtLocation.text = imgdocu.get("name") as String
                                                if ((imgdocu.get("img-$id") as String) != "") {
                                                    cmnCls.loadImg(imgdocu.get("img-$id") as String, inwPage.imgVehicle)
                                                } else {
                                                    inwPage.imgVehicle.setImageResource(R.drawable.icon_dly)
                                                }
                                                Log.d("img Count", imgCount.count().toString())
                                                for (i in 0 until imgCount.count()) {
                                                    if ((imgdocu.get("img-" + imgCount[i]) as String) != "") {
                                                        img += 1
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

                                    loadImg(imgId, typ)
                                }
                            }
                    }
                }
            }*/


            //Log.d("imgID", id.toString())

        }
    }

    private fun uploadStorage(id : String, imgid : String, fUri : Uri, str : String, dialog: AlertDialog) {

        val imageData: ByteArray = convertImageToWebP(fUri)
        inwPage.txtPrg.isVisible = false
        inwPage.prgRefresh.isVisible = true
        /*fbStorage.getReference().child(cmp).child("inward").child(id)
            .child("img-$imgid").putFile(fUri).addOnSuccessListener {
                storageRef.child(cmp).child("inward").child(id)
                    .child("img-$imgid").downloadUrl.addOnSuccessListener { uri: Uri ->
                        val url = uri.toString()
                        updateImage("img-$imgid", url, dialog)
                    }
            }.addOnFailureListener {
                Toast.makeText(applicationContext, "Fail to Upload Image..", Toast.LENGTH_SHORT).show()
            }*/
        fbStorage.getReference().child(cmp).child("inward").child(id)
            .child("img-$imgid").putBytes(imageData).addOnSuccessListener {
                storageRef.child(cmp).child("inward").child(id)
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
        storageRef.child(cmp).child("inward").child(id)
    }

    private fun updateImage(fld : String, pth : String, dialog: AlertDialog, str : String) {

        db.collection(cmp)
            .document("entry")
            .collection("inward")
            .document(entId)
            .update(mapOf(
                fld to pth
            )).addOnSuccessListener {
                //Toast.makeText(applicationContext, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                //Log.d("Image path : $fld", pth)
                endPage(str, dialog)
                /*imgUri = null
                inwPage.btnUpdate.isEnabled = true
                inwPage.txtPrg.isVisible = true
                inwPage.prgRefresh.isVisible = false
                imgId += 1
                loadImg(imgId, "fwd")*/
                //dialog.dismiss()
            }
    }

    @SuppressLint("SetTextI18n")
    private fun progressValue(txt : Int) {
        inwPage.prgBar.progress = txt
        inwPage.txtPrg.text = "$txt %"
        inwPage.btnFinish.isVisible = inwPage.txtPrg.text.toString() == "100 %"
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
                    //Log.d ("img cnt", imgCount.count().toString())
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
            val smsManager:SmsManager = this.getSystemService(SmsManager::class.java)

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

    private fun addToList(imgid : String, fUri : Uri) {
        imgUriList[imgid.toInt()] = fUri
        //imgUriId.add(imgid.toInt(),imgid)
        //imgUriList[imgid.toInt()] = fUri
        //imgUriId[imgid.toInt()] = imgid
        imgId += 1
        //inwPage.imgVehicle.setImageResource(R.drawable.icon_dly)
        loadLocalImg("fwd")
        //Log.d("List", imgUriList.count().toString())
        inwPage.btnUpdate.isEnabled = true
    }

    private fun loadLocalImg(typ : String) {
        //inwPage.imgVehicle.setImageURI(fUri)
        //Log.d("img id", imgId.toString())
        //Log.d("img Last", imgIdList.last())
        if (imgId > imgIdList.last().toInt()) {
            imgId = 1
            loadLocalImg(typ)
        } else {
            if (imgName[imgId] == "") {
                if (typ == "fwd") {
                    imgId += 1
                } else if (typ == "bwd") {
                    imgId -= 1
                }
                if (imgId <= imgIdList.last().toInt()) {
                    loadLocalImg(typ)
                }
            } else {
                loadPercentage()
                inwPage.txtLocation.text = imgName[imgId]
                inwPage.imgVehicle.setImageURI(imgUriList[imgId])
            }
        }
    }

    private fun downloadToLocal(
        i: Int,
        fbImagePath: String,
        directory: File?,
        x : Int,
        dialog: AlertDialog
    ) {


        // Prepare the file destination inside this folder
        val localFile = File(directory, fbImagePath)
        //Log.d("Check Localdata", "$directory-$localFile")
        //Log.d("Check Img URI & ID", i.toString() + "-" + imgUriList[i].toString())
        storageRef.child(cmp).child("inward")
            .child(entId).child("img-$i").getFile(localFile)
            .addOnSuccessListener {
                // Convert downloaded file to URI and display
                val imageUri: Uri = localFile.toUri()
                //imageView.setImageURI(imageUri)
                imgUriList[i] = imageUri
                //Log.d("Check Localfile", "$localFile")
                //Log.d("Img URI & ID", i.toString() + "-" + imgUriList[i].toString())
                if (x == 0) {
                    imgId = 1
                    loadLocalImg("fwd")
                }
                loadPercentage()
                dialog.dismiss()
                //inwPage.imgVehicle.setImageURI(imageUri)
            }
            .addOnFailureListener { exception ->
                // Handle the error appropriately
                //Log.d("download error", exception.message.toString())
                dialog.dismiss()
            }
    }

    private fun loadPercentage(){
        //val img = imgIdList.count()
        var ig = 0
        var per = 0.0
        val uri : Uri = ("android.resource://" + packageName + "/" + R.drawable.icon_dly).toUri()
        for (j in 0 until imgIdList.last().toInt() + 1) {
            //.d("imguri", imgUriList[j].toString())
            //Log.d("uri", uri.toString())
            if (imgUriList[j] != uri && imgName[j] != "") {
                per += 1
            }
            if (imgName[j] != "") {
                ig += 1
            }
        }
        //Log.d("per value", per.toString())
        //Log.d("img value", ig.toString())
        per = (per / ig) * 100
        //Log.d("per percentage", per.toString())
        progressValue(per.toInt())
    }

    private fun uploadOnline() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
            .setMessage("Image is uploading")
            .setTitle("Please wait.......")
        val dialog: AlertDialog = builder.create()
        dialog.show()
        //val img = imgIdList.count()
        val uri : Uri = ("android.resource://" + packageName + "/" + R.drawable.icon_dly).toUri()
        for (j in 0 until imgIdList.last().toInt() + 1) {
            //.d("imguri", imgUriList[j].toString())
            //Log.d("uri", uri.toString())
            var str = "start"
            if (j == imgIdList.last().toInt()) {
                str = "end"
            }
            if (imgUriList[j] != uri && imgName[j] != "") {
                uploadStorage(entId, j.toString(), imgUriList[j], str, dialog)
            }
            //endPage(str, dialog)
        }
    }

    private fun endPage(str : String, dialog: AlertDialog){
        if (str == "end") {
            dialog.dismiss()
            db.collection(cmp)
                .document("entry")
                .collection("inward")
                .whereEqualTo("id", entId.toInt()).get()
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
                                    val url = "http://27.100.26.35"
                                    val mob = "+91" + cusdoc.get("mob") as String
                                    //Log.d("Customer Mobile", "+91" + cusdoc.get("mob") as String)
                                    sendSMS(mob, "Your Vehicle No. $vehNo is Inward on ${document.get("cdate") as String}. Click the link to view your Vehicle Inward Spot Pics $url. Thank you")
                                    sendWhatsapp(mob, "Your Vehicle No. $vehNo is Inward on ${document.get("cdate") as String}. Click the link to view your Vehicle Inward Spot Pics $url. Thank you")
                                    pageOpen = 0
                                    /*val hmeIntt = Intent(this@InwardUploadActivity, RecyclerActivity::class.java)
                                    hmeIntt.putExtra("usrtype", usrType)
                                    hmeIntt.putExtra("type", "inward")
                                    startActivity(hmeIntt)
                                    finish()*/
                                }
                            }
                    }
                }
        }
    }
}