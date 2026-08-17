package com.example.fragmentsdemo

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class DemoChildFragment : Fragment() {

    companion object {
        private const val ARG_COUNT = "fragment_count"

        fun newInstance(count: Int): DemoChildFragment {
            val fragment = DemoChildFragment()
            val args = Bundle()
            args.putInt(ARG_COUNT, count)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val count = arguments?.getInt(ARG_COUNT) ?: 0
        
        return TextView(requireContext()).apply {
            text = "Fragment $count"
            textSize = 32f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }
}
