package com.ats.servicesight.adaptor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ats.servicesight.classes.VehicleClass
import com.ats.servicesight.databinding.ViewVehicleBinding

class VehicleAdaptor(private var vehRpt: ArrayList<VehicleClass>) : RecyclerView.Adapter<VehicleAdaptor.UsrViewHldr>()  {
    inner class UsrViewHldr(val vehBnd: ViewVehicleBinding) :
        RecyclerView.ViewHolder(vehBnd.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsrViewHldr {
        val bnd = ViewVehicleBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return UsrViewHldr(bnd)
    }

    override fun getItemCount(): Int {
        return vehRpt.size
    }

    override fun onBindViewHolder(holder: UsrViewHldr, position: Int) {
        holder.vehBnd.txtNo.text = vehRpt[position].vehNo
        holder.vehBnd.txtBrand.text = vehRpt[position].brand
        holder.vehBnd.txtModel.text = vehRpt[position].model
        holder.vehBnd.txtYear.text = vehRpt[position].year
    }

    fun getUsrId(position: Int) : Int{
        return vehRpt[position].id
    }
}