package com.example.recyclerviewpool.view.fragment.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recyclerviewpool.R
import com.example.recyclerviewpool.databinding.FragmentDetailSearchBinding
import com.example.recyclerviewpool.view.MainActivity
import com.example.recyclerviewpool.adapter.search.TopicDetailSearchAdapter
import com.example.recyclerviewpool.view.fragment.discover.ManagerFragmentDiscover
import com.example.recyclerviewpool.view.fragment.discover.video.FragmentVideo
import com.example.recyclerviewpool.viewmodel.DiscoverModel

class DetailSearchFragment : Fragment, TopicDetailSearchAdapter.ICategories, View.OnClickListener {
    private var managerSearch: ManagerFragmentSearch
    private lateinit var sharedViewModel: DiscoverModel
    private lateinit var model: MainActivity
    private lateinit var binding: FragmentDetailSearchBinding

    constructor(managerFragmentSearch: ManagerFragmentSearch) {
        this.managerSearch = managerFragmentSearch
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        model = (activity as MainActivity)
        binding = FragmentDetailSearchBinding.inflate(inflater, container, false)
        sharedViewModel = ViewModelProvider(requireActivity()).get(DiscoverModel::class.java)
        //setUpCategoriesCountry
        binding.rcSugCountry.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TopicDetailSearchAdapter(this@DetailSearchFragment,
                model,
                managerSearch,
                sharedViewModel)

        }

        binding.search.setOnClickListener(this)
        reg()
        model.getDiscoverModel().categoriesCountry()
        return binding.root
    }

    private fun reg() {
        model.getDiscoverModel().categoriesCountry.observe(viewLifecycleOwner, Observer {
            binding.rcSugCountry.adapter!!.notifyDataSetChanged()

        })
    }


    override fun onClick(v: View?) {
        managerSearch.openSearch()
    }

    override fun getCategoriesCountryCount(): Int {
        return if (model.getDiscoverModel().categoriesCountry.value == null) {
            0
        } else {
            model.getDiscoverModel().categoriesCountry.value!!.size
        }
    }

    override fun getCategoriesCountryData(position: Int) =
        model.getDiscoverModel().categoriesCountry.value!![position]


}