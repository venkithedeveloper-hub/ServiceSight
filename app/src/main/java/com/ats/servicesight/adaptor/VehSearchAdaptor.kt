package com.ats.servicesight.adaptor

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ats.servicesight.classes.VehicleClass
import com.ats.servicesight.databinding.ViewVehsearchBinding
import com.ats.servicesight.interfaces.OnSelectClickListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VehSearchAdaptor(private var vehRpt: ArrayList<VehicleClass>, private var clickListener: OnSelectClickListener) : RecyclerView.Adapter<VehSearchAdaptor.UsrViewHldr>()  {
    private var db : FirebaseFirestore = FirebaseFirestore.getInstance()
    private val fbAuth : FirebaseAuth = FirebaseAuth.getInstance()
    private var usr : String = ""
    private var cmp : String = ""
    inner class UsrViewHldr(val vehBnd: ViewVehsearchBinding) :
        RecyclerView.ViewHolder(vehBnd.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsrViewHldr {
        val bnd = ViewVehsearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return UsrViewHldr(bnd)
    }

    override fun getItemCount(): Int {
        return vehRpt.size
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
        holder.vehBnd.txtNo.text = vehRpt[position].vehNo
        holder.vehBnd.txtBrand.text = vehRpt[position].brand + "-" + vehRpt[position].model
        db.collection(cmp)
            .document("masterdata")
            .collection("customer")
            .whereEqualTo("id", vehRpt[position].cusId)
            .whereEqualTo("sts", 1).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    holder.vehBnd.txtModel.text = document.get("name") as String
                    holder.vehBnd.txtYear.text = document.get("mob") as String
                }
            }

        holder.vehBnd.btnSelect.setOnClickListener {
            clickListener.OnSelectClicked(vehRpt[position].id)
        }
    }

    fun getUsrId(position: Int) : Int{
        return vehRpt[position].id
    }


}