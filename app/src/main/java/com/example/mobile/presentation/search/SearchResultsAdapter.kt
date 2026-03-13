package com.example.mobile.presentation.search

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.domain.model.Drug
import com.example.mobile.R

class SearchResultsAdapter(
    context: Context,
    items: MutableList<Drug>
) : ArrayAdapter<Drug>(context, 0, items) {

    private val inflater = LayoutInflater.from(context)

    var onAddToBookmark: ((Drug) -> Unit)? = null
    var onItemClick: ((Drug) -> Unit)? = null

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_search_result, parent, false)

        val item = getItem(position) ?: return view

        val addToBookmarkButton = view.findViewById<android.widget.ImageButton>(R.id.button_add_to_bookmark)
        val manufacturerText = view.findViewById<TextView>(R.id.text_manufacturer)
        val nameText = view.findViewById<TextView>(R.id.text_name)
        val priceText = view.findViewById<TextView>(R.id.text_price)

        addToBookmarkButton.setOnClickListener { onAddToBookmark?.invoke(item) }
        view.setOnClickListener { onItemClick?.invoke(item) }
        manufacturerText.text = item.manufacturer.ifBlank { "" }
        nameText.text = item.name
        priceText.text = item.price

        return view
    }
}

