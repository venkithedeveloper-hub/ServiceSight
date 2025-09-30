package com.ats.servicesight.adaptor

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ats.servicesight.R
import com.ats.servicesight.classes.InwardClass
import com.ats.servicesight.databinding.ViewInwardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class InwardAdaptor(private var inwRpt: ArrayList<InwardClass>) : RecyclerView.Adapter<InwardAdaptor.UsrViewHldr>()  {
    private var db : FirebaseFirestore = FirebaseFirestore.getInstance()
    private val fbAuth : FirebaseAuth = FirebaseAuth.getInstance()
    private var usr : String = ""
    private var cmp : String = ""
    private var per : Double = 0.0
    private var img : Int = 0
    inner class UsrViewHldr(val inwBnd: ViewInwardBinding) :
        RecyclerView.ViewHolder(inwBnd.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsrViewHldr {
        val bnd = ViewInwardBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return UsrViewHldr(bnd)
    }

    override fun getItemCount(): Int {
        return inwRpt.size
    }

    fun getUsrId(position: Int) : Int{
        return inwRpt[position].id
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: UsrViewHldr, position: Int) {
        usr = fbAuth.currentUser?.email.toString()
        //Log.d("user", usr)
        cmp = if (usr != "" && usr != "null") {
            usr.substring(usr.indexOf(".") + 1, usr.length)
        } else {
            ""
        }
        /*holder.inwBnd.txtDate.text = inwRpt[position].date.substring(0, 10).uppercase(Locale.getDefault())
        holder.inwBnd.txtTime.text = inwRpt[position].date.substring(11, 22).uppercase(Locale.getDefault())*/
        holder.inwBnd.txtDate.text = inwRpt[position].dte
        holder.inwBnd.txtTime.text = inwRpt[position].tme.uppercase(Locale.getDefault())

        db.collection(cmp)
            .document("masterdata")
            .collection("vehicle")
            .whereEqualTo("id", inwRpt[position].vehId)
            .whereEqualTo("sts", 1).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    holder.inwBnd.txtVno.text = document.get("vehNo") as String
                    holder.inwBnd.txtMdl.text = document.get("brand") as String + " " + document.get("model") as String
                }
            }
        db.collection(cmp)
            .document("masterdata")
            .collection("service")
            .whereEqualTo("id", inwRpt[position].serId)
            .whereEqualTo("sts", 1).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    holder.inwBnd.txtService.text = document.get("name") as String
                }
            }
        db.collection(cmp)
            .document("masterdata")
            .collection("branch")
            .whereEqualTo("id", inwRpt[position].brnId)
            .whereEqualTo("sts", 1).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    holder.inwBnd.txtBranch.text = "Branch : " + (document.get("name") as String).toString()
                }
            }
        db.collection(cmp)
            .document("masterdata")
            .collection("user")
            .whereEqualTo("user", inwRpt[position].usrId)
            .whereEqualTo("sts", 1).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    holder.inwBnd.txtUser.text = "SA : " + (document.get("name") as String).toString()
                    holder.inwBnd.txtMobile.text = document.get("mobile") as String
                }
            }
    }
}