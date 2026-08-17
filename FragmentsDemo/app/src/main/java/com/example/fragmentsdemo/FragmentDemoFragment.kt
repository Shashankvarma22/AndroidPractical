package com.example.fragmentsdemo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fragmentsdemo.databinding.FragmentFragmentDemoBinding

class FragmentDemoFragment : Fragment() {

    private var _binding: FragmentFragmentDemoBinding? = null
    private val binding get() = _binding!!
    private var fragmentCount = 0
    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private val fragmentTags = mutableListOf<String>()
    private var currentTag: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (savedInstanceState != null) {
            fragmentCount = savedInstanceState.getInt("fragmentCount", 0)
            savedInstanceState.getStringArrayList("fragmentTags")?.let {
                fragmentTags.addAll(it)
            }
            currentTag = savedInstanceState.getString("currentTag")
        }

        // Handle back press for child fragments backstack
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (childFragmentManager.backStackEntryCount > 0) {
                    childFragmentManager.popBackStack()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFragmentDemoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinner()

        binding.btnAddFragment.setOnClickListener {
            addFragment()
        }

        binding.btnBackToDashboard.setOnClickListener {
            // Navigate back to Dashboard using existing NavController
            findNavController().popBackStack()
        }
        
        childFragmentManager.addOnBackStackChangedListener {
            updateBackstackCount()
            // When popping backstack, sync currentTag and spinner selection
            val count = childFragmentManager.backStackEntryCount
            if (count > 0) {
                val entry = childFragmentManager.getBackStackEntryAt(count - 1)
                val tag = entry.name
                if (tag != null && tag != currentTag) {
                    currentTag = tag
                    val pos = fragmentTags.indexOf(tag)
                    if (pos >= 0) {
                        binding.spinnerFragments.setSelection(pos)
                    }
                }
            } else {
                currentTag = null
            }
        }
        
        updateBackstackCount()
    }

    private fun setupSpinner() {
        spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, fragmentTags)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFragments.adapter = spinnerAdapter

        binding.spinnerFragments.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val tag = fragmentTags[position]
                if (tag != currentTag) {
                    changeView(tag)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Sync selection if currentTag is set (e.g. after rotation)
        currentTag?.let { tag ->
            val pos = fragmentTags.indexOf(tag)
            if (pos >= 0) {
                binding.spinnerFragments.setSelection(pos)
            }
        }
    }

    private fun addFragment() {
        fragmentCount++
        val tag = fragmentCount.toString()
        fragmentTags.add(tag)
        spinnerAdapter.notifyDataSetChanged()
        
        currentTag = tag
        binding.spinnerFragments.setSelection(fragmentTags.size - 1)

        // Demonstrate dynamic creation and Bundle arguments
        val fragment = DemoChildFragment.newInstance(fragmentCount)
        
        // Demonstrate beginTransaction, replace, tag, addToBackStack, and commit
        // Added a standard built-in transition effect as requested
        childFragmentManager.beginTransaction()
            .setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .replace(R.id.demo_child_container, fragment, tag)
            .addToBackStack(tag)
            .commit()
            
        binding.tvStatus.text = "Status: Created New Fragment $tag"
    }

    private fun changeView(tag: String) {
        // Demonstrate findFragmentByTag to retrieve an already-created fragment
        val fragment = childFragmentManager.findFragmentByTag(tag)
        
        if (fragment != null) {
            currentTag = tag
            // Demonstrate re-displaying an existing fragment instance
            childFragmentManager.beginTransaction()
                .setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.demo_child_container, fragment, tag)
                .addToBackStack(tag)
                .commit()
            binding.tvStatus.text = "Status: Retrieved Existing Fragment $tag"
        } else {
            binding.tvStatus.text = "Status: Fragment $tag not found"
        }
    }

    private fun updateBackstackCount() {
        val count = childFragmentManager.backStackEntryCount
        binding.tvBackstackCount.text = "Backstack Count: $count"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("fragmentCount", fragmentCount)
        outState.putStringArrayList("fragmentTags", ArrayList(fragmentTags))
        outState.putString("currentTag", currentTag)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
