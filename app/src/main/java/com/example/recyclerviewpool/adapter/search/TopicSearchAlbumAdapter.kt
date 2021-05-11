package com.example.recyclerviewpool.adapter.search

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recyclerviewpool.databinding.ItemTopicAlbumSongBinding
import com.example.recyclerviewpool.model.itemdata.ItemSong
import com.example.recyclerviewpool.model.itemdata.ItemMusicList
import com.example.recyclerviewpool.view.MainActivity
import com.example.recyclerviewpool.view.fragment.search.ManagerFragmentSearch
import com.example.recyclerviewpool.viewmodel.LoadDataUtils
import com.example.recyclerviewpool.viewmodel.SearchModel
import com.example.recyclerviewpool.viewmodel.SetDataSlidingPanel

class TopicSearchAlbumAdapter : RecyclerView.Adapter<TopicSearchAlbumAdapter.ItemCategoriesHolder> {
    private var iCategories: ICategories
    private var fragmentSearchManager: ManagerFragmentSearch
    private var model : MainActivity
    var lifecycleOwner: LifecycleOwner
    private var sharedViewModel : SearchModel

    constructor(
        sharedViewModel: SearchModel,
        model: MainActivity,
        iCategories: ICategories,
        fragmentSearchManager: ManagerFragmentSearch,
        lifecycleOwner: LifecycleOwner
    ) {
        this.sharedViewModel = sharedViewModel
        this.model = model
        this.iCategories = iCategories
        this.fragmentSearchManager = fragmentSearchManager
        this.lifecycleOwner = lifecycleOwner


    }

    interface ICategories {
        fun getSearchAlbumCount(): Int
        fun getSearchAlbumData(position: Int): ItemMusicList<ItemSong>?

    }

    class ItemCategoriesHolder(val binding: ItemTopicAlbumSongBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemCategoriesHolder {

        val holder = ItemCategoriesHolder(
            ItemTopicAlbumSongBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

        holder.binding.rcCategories.layoutManager =
           LinearLayoutManager(parent.context)
        return holder
    }

    override fun onBindViewHolder(holder: ItemCategoriesHolder, parenPosition: Int) {
        val data = iCategories.getSearchAlbumData(parenPosition)


        val iCategories = object : SearchAlbumAdapter.ISearchAlbum {
            override fun getSearchAlbumCount() :Int{
                return 3
            }

            override fun getSearchAlbumData(chilPosition: Int): ItemSong {
                if (data?.values.isNullOrEmpty()){
                    return ItemSong("1", "", "", "", "", "", "", "", "", "" , "")
                }
                return  data!!.values[chilPosition]
            }

            override fun getSearchAlbumOnClickItem(position: Int) {
                LoadDataUtils.hideSoftKeyboard(model)
                model.getPlaySevice()!!.currentPositionSong = position
                sharedViewModel.setData(
                    data!!.values[position].imgSong,
                    data.values[position].nameSong,
                    data.values[position].singerSong
                )

                SetDataSlidingPanel.setDataSlidingPanel(model,data.values!!,  position)
                model.getDiscoverModel().infoAlbum.observe(lifecycleOwner, Observer
                {
                    SetDataSlidingPanel.setDataMusic(model, data.values!!, it, position)
                })

            }


        }

        if (holder.binding.rcCategories.adapter == null) {
            holder.binding.rcCategories.adapter = SearchAlbumAdapter(iCategories)
            holder.binding.titleCategories.text = data!!.nameCategories
            holder.binding.titleCategories.setTextColor(Color.WHITE)

        } else {

            (holder.binding.rcCategories.adapter as SearchAlbumAdapter).iSearchAlbum = iCategories

            holder.binding.rcCategories.adapter!!.notifyDataSetChanged()


        }

    }

    override fun getItemCount() = iCategories.getSearchAlbumCount()
    override fun getItemId(position: Int): Long {
        return if (position == 0) {
            0
        } else {
            10
        }

    }
}