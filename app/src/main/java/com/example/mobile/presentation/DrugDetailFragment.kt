package com.example.mobile.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.domain.model.Drug
import com.example.mobile.R

class DrugDetailFragment : Fragment() {

    companion object {
        private const val ARG_DRUG = "arg_drug"

        fun newInstance(drug: Drug): DrugDetailFragment {
            return DrugDetailFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_DRUG, drug)
                }
            }
        }
    }

    private var drug: Drug? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        drug = arguments?.getSerializable(ARG_DRUG) as? Drug
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_drug_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val backButton = view.findViewById<ImageButton>(R.id.button_back)
        val imageView = view.findViewById<ImageView>(R.id.image_drug)
        val manufacturerText = view.findViewById<TextView>(R.id.text_manufacturer)
        val nameText = view.findViewById<TextView>(R.id.text_name)
        val priceText = view.findViewById<TextView>(R.id.text_price)
        val descriptionText = view.findViewById<TextView>(R.id.text_description)

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val item = drug ?: return

        imageView.setImageResource(R.drawable.search_result_placeholder)
        manufacturerText.text = item.manufacturer
        nameText.text = item.name
        priceText.text = item.price
        descriptionText.text = item.description
    }
}

