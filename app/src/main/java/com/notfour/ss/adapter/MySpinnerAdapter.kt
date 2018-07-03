package com.notfour.ss.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.github.shadowsocks.database.Profile
import com.notfour.ss.R

/**
 * Created with author.
 * Description:
 * Date: 2018-07-03
 * Time: 上午11:09
 */
class MySpinnerAdapter : ArrayAdapter<Profile> {
    val list = mutableListOf<Profile>()

    constructor(context: Context?, resource: Int) : super(context, resource)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var convertView = convertView
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.activity_main_spinner_item, null)
        }
        val textView = convertView!!.findViewById<TextView>(R.id.main_spinner_item_text)
        textView.text = list[position].name
        return convertView!!
    }

    override fun getItem(position: Int): Profile {
        return list[position]
    }

    override fun getCount(): Int {
        return list.size
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var convertView = convertView
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.activity_main_spinner_item, null)
        }
        val textView = convertView!!.findViewById<TextView>(R.id.main_spinner_item_text)
        textView.text = list[position].name
        return convertView!!
    }

    fun refreshItems(list: List<Profile>) {
        this.list.clear()
        this.list.addAll(list)
        notifyDataSetChanged()
    }
}