package com.ats.servicesight.adaptor

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ats.servicesight.classes.UserClass
import com.ats.servicesight.databinding.ViewUserBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserAdaptor(private var usrRpt: ArrayList<UserClass>) : RecyclerView.Adapter<UserAdaptor.UsrViewHldr>() {
    private var db : FirebaseFirestore = FirebaseFirestore.getInstance()
    private val fbAuth : FirebaseAuth = FirebaseAuth.getInstance()
    private var usr : String = ""
    private var cmp : String = ""
    private var brn : String = ""
    inner class UsrViewHldr(val usrBnd: ViewUserBinding) :
        RecyclerView.ViewHolder(usrBnd.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserAdaptor.UsrViewHldr {
        val bnd = ViewUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return UsrViewHldr(bnd)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: UserAdaptor.UsrViewHldr, position: Int) {
        holder.usrBnd.txtName.text = usrRpt[position].name + " (" + usrRpt[position].username + ")"
        if (usrRpt[position].type == 2) {
            holder.usrBnd.txtType.text = "Team Lead"
        } else if (usrRpt[position].type == 3) {
            holder.usrBnd.txtType.text = "Service Advisor"
        }
        usr = fbAuth.currentUser?.email.toString()
        //Log.d("user", usr)
        if (usr != "" && usr != "null") {
            cmp = usr.substring(usr.indexOf(".") + 1, usr.length)
            db.collection(cmp)
                .document("masterdata")
                .collection("branch")
                .whereEqualTo("id", usrRpt[position].branch)
                .whereEqualTo("sts", 1).get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        brn = "Branch : ?"
                    } else {
                        val document = querySnapshot.documents[0]
                        brn = "Branch : " + (document.get("name") as String).toString()

                    }
                    holder.usrBnd.txtBranch.text = brn
                }
        } else {
            holder.usrBnd.txtBranch.text = "Branch : ?"
        }
    }

    override fun getItemCount(): Int {
        return usrRpt.size
    }

    fun getUsrId(position: Int) : Int{
        return usrRpt[position].id
    }
}