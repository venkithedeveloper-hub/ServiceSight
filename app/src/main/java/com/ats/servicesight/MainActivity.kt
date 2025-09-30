package com.ats.servicesight

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.StrictMode
import android.provider.Settings
import android.provider.Settings.Secure.ANDROID_ID
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.ats.servicesight.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale


private lateinit var drawerLayout: DrawerLayout
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener  {
    private var db : FirebaseFirestore = FirebaseFirestore.getInstance()
    private val fbAuth : FirebaseAuth = FirebaseAuth.getInstance()
    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    private var currentUser = fbAuth.currentUser
    private var usr : String = ""
    private var cmp : String = ""
    private var usrType : String = ""
    private lateinit var mainPage : ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        mainPage = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(mainPage.root)
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        drawerLayout = mainPage.main
        val tbar = mainPage.tlbar
        setSupportActionBar(tbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        val navigationview  = mainPage.navView1
        navigationview.setNavigationItemSelectedListener(this)
        val toggle = ActionBarDrawerToggle(this, drawerLayout, tbar, R.string.open_nav, R.string.close_nav)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = this.resources.getColor(R.color.heading)

        navigationview.menu.findItem(R.id.menuUser).isVisible = false

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        currentUser = fbAuth.currentUser
        usr = fbAuth.currentUser?.email.toString()
        //Log.d("user", usr)
        if (usr != "" && usr != "null") {
            cmp = usr.substring(usr.indexOf(".") + 1, usr.length)
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)

            checkNetworkConnection(this, mainPage.navView1, usr, cmp)
        } else {
            val logIntt = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(logIntt)
            finish()
        }

        val onBackPressedCallback = object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        var viewIntt = Intent(this@MainActivity, RecyclerViewActivity::class.java)
        val inwIntt = Intent(this@MainActivity, RecyclerActivity::class.java)
        when (item.itemId) {
            R.id.menuCustomer -> {
                viewIntt.putExtra("type", "customer")
                startActivity(viewIntt)
            }
            R.id.menuInward -> {
                inwIntt.putExtra("usrtype", usrType)
                inwIntt.putExtra("type", "inward")
                startActivity(inwIntt)
            }
            R.id.menuDelivery -> {
                inwIntt.putExtra("usrtype", usrType)
                inwIntt.putExtra("type", "delivery")
                startActivity(inwIntt)
            }
            R.id.menuUser -> {
                viewIntt.putExtra("type", "user")
                startActivity(viewIntt)
            }
            R.id.menuLogout -> {
                viewIntt = Intent(this@MainActivity, LoginActivity::class.java)
                startActivity(viewIntt)
            }
        }
        finish()
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    @SuppressLint("HardwareIds")
    private fun checkNetworkConnection(context: Context, nav : NavigationView, usr : String, cmp : String){
        val currentNetwork = context.getSystemService(ConnectivityManager::class.java).activeNetwork

        if (currentNetwork == null) {
            Log.d("internet", "false")
            val errIntt = Intent(this@MainActivity, ErrorActivity::class.java)
            startActivity(errIntt)
            finish()
        } else {
            if (currentUser == null){
                //fbAuth.signOut()
                val logIntt = Intent(this@MainActivity, LoginActivity::class.java)
                startActivity(logIntt)
                finish()
            }else{
                //Log.d("user", usr + " - " + cmp)
                val logIntt = Intent(this@MainActivity, LoginActivity::class.java)
                currentUser!!.reload().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        db.collection(cmp)
                            .document("masterdata")
                            .collection("user")
                            .whereEqualTo("user", usr)
                            .whereEqualTo("sts", 1).get()
                            .addOnSuccessListener { querySnapshot ->
                                if (querySnapshot.isEmpty) {
                                    Toast.makeText(
                                        this,
                                        "User not exist or deactivated. Please contact Administrator",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    startActivity(logIntt)
                                    finish()
                                } else {
                                    val document = querySnapshot.documents[0]
                                    usrType = (document.get("type") as Number).toString()
                                    if ((document.get("uid") as String).toString() != Settings.Secure.getString(contentResolver,ANDROID_ID)) {
                                        Toast.makeText(this, "User already connected with another mobile.",Toast.LENGTH_SHORT).show()
                                        startActivity(logIntt)
                                        finish()
                                    } else {
                                        if (checkSelfPermission(android.Manifest.permission.CAMERA) ==
                                            PackageManager.PERMISSION_DENIED) {
                                            val permission = arrayOf(
                                                android.Manifest.permission.CAMERA
                                            )
                                            requestPermissions(permission, 112)
                                        }
                                        requestPermissions(
                                            arrayOf(android.Manifest.permission.RECEIVE_SMS,
                                                android.Manifest.permission.SEND_SMS,
                                                android.Manifest.permission.READ_SMS), PackageManager.PERMISSION_GRANTED
                                        )
                                        loadRights(nav, (document.get("type") as Number).toInt(), (document.get("name") as String).toString().uppercase(Locale.getDefault()))
                                        loadDashboard()
                                    }
                                }
                            }
                    } else {
                        startActivity(logIntt)
                        finish()
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadRights(navigationview : NavigationView, typ : Int, nme : String) {
        navigationview.menu.findItem(R.id.menuUser).isVisible = typ == 1
        val navigationView : NavigationView  = findViewById(R.id.navView1)

        val headerView : View = navigationView.getHeaderView(0)
        val navUsername : TextView = headerView.findViewById(R.id.txtHeadUser)
        navUsername.text = "Hello, $nme"
    }

    private fun loadDashboard() {

    }
}