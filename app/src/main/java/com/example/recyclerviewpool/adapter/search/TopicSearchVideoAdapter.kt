package com.example.recyclerviewpool.adapter.search

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recyclerviewpool.databinding.ItemTopicAlbumSongBinding
import com.example.recyclerviewpool.model.itemdata.ItemSong
import com.example.recyclerviewpool.model.itemdata.ItemMusicList
import com.example.recyclerviewpool.view.MainActivity
import com.example.recyclerviewpool.view.fragment.search.ManagerFragmentSearch
import com.example.recyclerviewpool.viewmodel.LoadDataUtils
import com.example.recyclerviewpool.viewmodel.SearchModel

class TopicSearchVideoAdapter : RecyclerView.Adapter<TopicSearchVideoAdapter.ItemCategoriesHolder> {
    private var iCategories: ICategories
    private var fragmentSearchManager: ManagerFragmentSearch
    private var model: MainActivity

    constructor(
        iCategories: ICategories,
        fragmentSearchManager: ManagerFragmentSearch,
        model: MainActivity
    ) {

        this.iCategories = iCategories
        this.fragmentSearchManager = fragmentSearchManager
        this.model=  model

    }

    interface ICategories {
        fun getSearchVideoCount(): Int
        fun getSearchVideoData(position: Int): ItemMusicList<ItemSong>?

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
        val data = iCategories.getSearchVideoData(parenPosition)


        val iCategories = object : SearchVideoAdapter.ISearchVideo {
            override fun getSearchVideoCount() :Int{
                if (iCategories.getSearchVideoCount() ==0){
                    return 0
                }else{
                    return 3
                }
            }

            override fun getSearchVideoData(chilPosition: Int): ItemSong? {
                if (iCategories.getSearchVideoCount() == 0 || data == null) {
                    return null
                } else {
                    return data.values[chilPosition]
                }
            }

            override fun getSearchVideoOnClickItem(position: Int) {
                LoadDataUtils.hideSoftKeyboard(model)
                model.getDiscoverModel().getRelateVideo(data!!.values[position].linkSong)
                model.getDiscoverModel().getInfo(data!!.values[position].linkSong)
                fragmentSearchManager.openVideo()
            }


        }

        if (holder.binding.rcCategories.adapter == null) {
            holder.binding.rcCategories.adapter = SearchVideoAdapter(iCategories)
            holder.binding.titleCategories.text = data!!.nameCategories
            holder.binding.titleCategories.setTextColor(Color.WHITE)

        } else {

            (holder.binding.rcCategories.adapter as SearchVideoAdapter).iSearchVideo = iCategories

            holder.binding.rcCategories.adapter!!.notifyDataSetChanged()


        }

    }

    override fun getItemCount() = iCategories.getSearchVideoCount()
    override fun getItemId(position: Int): Long {
        return if (position == 0) {
            0
        } else {
            10
        }

    }
}