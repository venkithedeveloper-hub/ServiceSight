package com.ats.servicesight.adaptor

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ats.servicesight.classes.CustomerClass
import com.ats.servicesight.databinding.ViewCustomerBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore

class CustomerAdaptor(private var cusRpt: ArrayList<CustomerClass>) : RecyclerView.Adapter<CustomerAdaptor.UsrViewHldr>()  {
    private var db : FirebaseFirestore = FirebaseFirestore.getInstance()
    private val fbAuth : FirebaseAuth = FirebaseAuth.getInstance()
    private var usr : String = ""
    private var cmp : String = ""
    private var cnt : String = ""
    inner class UsrViewHldr(val cusBnd: ViewCustomerBinding) :
        RecyclerView.ViewHolder(cusBnd.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerAdaptor.UsrViewHldr {
        val bnd = ViewCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return UsrViewHldr(bnd)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: CustomerAdaptor.UsrViewHldr, position: Int) {
        holder.cusBnd.txtName.text = cusRpt[position].name
        holder.cusBnd.txtMobile.text = "Mob : " + cusRpt[position].mob
        //holder.cusBnd.txtEmail.text = "Email : " + cusRpt[position].eml
        usr = fbAuth.currentUser?.email.toString()
        //Log.d("user", usr)
        if (usr != "" && usr != "null") {
            cmp = usr.substring(usr.indexOf(".") + 1, usr.length)
            db.collection(cmp)
                .document("masterdata")
                .collection("vehicle")
                .whereEqualTo("cusId", cusRpt[position].id)
                .whereEqualTo("sts", 1).count()
                .get(AggregateSource.SERVER).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val snapshot = task.result
                        Log.d("Count", "Count: ${snapshot.count}")
                        cnt = snapshot.count.toString()
                    } else {
                        Log.d("Count", "Count failed: ", task.exception)
                        cnt = "0"
                    }
                    holder.cusBnd.txtEmail.text = "No.Of Cars : $cnt"
                }
        } else {
            holder.cusBnd.txtEmail.text = "No.Of Cars : 0"
        }
    }

    override fun getItemCount(): Int {
        return cusRpt.size
    }

    fun getUsrId(position: Int) : Int{
        return cusRpt[position].id
    }
}