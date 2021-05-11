package com.example.recyclerviewpool.view

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.media.MediaPlayer
import android.os.*
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager
import com.aurelhubert.ahbottomnavigation.AHBottomNavigation
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem
import com.example.recyclerviewpool.R
import com.example.recyclerviewpool.checkconnect.NetworkChangeReceiver
import com.example.recyclerviewpool.checkconnect.NetworkUtil
import com.example.recyclerviewpool.databinding.ActivityMainBinding
import com.example.recyclerviewpool.databinding.BottomSheetBinding
import com.example.recyclerviewpool.databinding.SlidingUpPanelBinding
import com.example.recyclerviewpool.lyricsong.LyricManager
import com.example.recyclerviewpool.model.itemdata.ItemSong
import com.example.recyclerviewpool.model.itemlyric.LineInfo
import com.example.recyclerviewpool.playmusic.PlayMusic
import com.example.recyclerviewpool.playmusic.service.CreateNotification
import com.example.recyclerviewpool.playmusic.service.PlayService
import com.example.recyclerviewpool.playmusic.service.Playable
import com.example.recyclerviewpool.view.fragment.discover.ManagerFragmentDiscover
import com.example.recyclerviewpool.view.fragment.discover.song.ViewPagerInfoSong
import com.example.recyclerviewpool.view.fragment.discover.song.ViewPagerLyricSong
import com.example.recyclerviewpool.view.fragment.discover.song.ViewPagerPlaySong
import com.example.recyclerviewpool.view.fragment.personal.ManagerPersonal
import com.example.recyclerviewpool.view.fragment.personal.PersonalFragment
import com.example.recyclerviewpool.view.fragment.ranking.ManagerRanking
import com.example.recyclerviewpool.view.fragment.search.ManagerFragmentSearch
import com.example.recyclerviewpool.viewmodel.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.makeramen.roundedimageview.RoundedImageView
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import kotlinx.android.synthetic.main.sliding_up_panel.view.*
import java.text.SimpleDateFormat


class MainActivity : AppCompatActivity(), PlayMusic.IPlayMusic, Playable, View.OnClickListener {
    var networkChangeReceiver: NetworkChangeReceiver? = null
    private var asyPlay: MyAsyn? = null
    private lateinit var lyricManager: LyricManager
    private var playService: PlayService? = null
    private lateinit var personalFragment: PersonalFragment
    lateinit var managerDiscover: ManagerFragmentDiscover
    lateinit var songAlbums: MutableList<ItemSong>
    private lateinit var conn: ServiceConnection
    private lateinit var broadcastReceiver: BroadcastReceiver
    private lateinit var navigationBar: AHBottomNavigation
    lateinit var slidingUpPanelLayout: SlidingUpPanelLayout
    private lateinit var discoverModel: DiscoverModel
    private lateinit var rankingModel: RankingModel
    private lateinit var modelPersonal: PersonalModel
    private lateinit var searchModel: SearchModel
    lateinit var bottomShetView: BottomSheetBinding
    var mFragmentList: MutableList<Fragment> = ArrayList()
    private val mFragmentListSlideSong: MutableList<Fragment> = ArrayList()
    lateinit var slidingUpPanelLayoutView: SlidingUpPanelBinding
    lateinit var viewPagerSliding: ViewPager
    private lateinit var binding: ActivityMainBinding
    lateinit var imgPlaySong: RoundedImageView
    private var notificationManager: NotificationManager? = null
    private var isPlaying = false
    lateinit var a: MutableList<ItemSong>
    var onRamdom = 0
    lateinit var lineInfo: LineInfo


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        discoverModel = DiscoverModel()
        rankingModel = RankingModel()
        modelPersonal = PersonalModel()
        personalFragment = PersonalFragment()
        searchModel = SearchModel()
        navigationBar = binding.bottomNavigation
        slidingUpPanelLayoutView = binding.slide
        bottomShetView = binding.bottomSheet
        slidingUpPanelLayout = binding.slideMain
        viewPagerSliding = binding.slide.viewPagerSong
        binding.slideMain.setDragView(slidingUpPanelLayout)
        lyricManager = LyricManager(this)
        lineInfo = LineInfo()

        createConnectionsToService()
        slidingPanelUp()
        addNavigation()
        setOnClickListener()
        addFragmentSlide()
        initBroadcastReceiver()
        checkConnectInternet()
        setSeekBar()

        val channel = NotificationChannel(
            CreateNotification!!.CHANNEL_ID,
            "Soom Dev", NotificationManager.IMPORTANCE_LOW
        )
        notificationManager = getSystemService(NotificationManager::class.java)
        if (notificationManager != null) {
            notificationManager!!.createNotificationChannel(channel)
        }
        registerReceiver(broadcastReceiver, IntentFilter("TRACKS_TRACKS"))
        startService(Intent(this, PlayService::class.java))


    }

    private fun addBottomSheet() {
        val llBottomSheet = findViewById<View>(R.id.bottom_sheet) as LinearLayout
        llBottomSheet.visibility = View.VISIBLE
        val bottomSheetBehavior = BottomSheetBehavior.from(llBottomSheet)
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED)
    }

    fun getPlaySevice() = playService
    fun getSlidingPanelUp() = slidingUpPanelLayout
    fun getNavigation() = navigationBar
    fun getDiscoverModel() = discoverModel
    fun getRankingModel() = rankingModel
    fun getBottomSheet() = bottomShetView
    fun getViewPager() = viewPagerSliding


    @RequiresApi(Build.VERSION_CODES.O)
    private fun setSeekBar() {

        binding.slide.seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    playService!!.setSeekTo(
                        progress * playService!!.getTotalTime() / 100,
                        MediaPlayer.SEEK_NEXT_SYNC
                    )

                    asyPlay!!.isRunning = false

                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                asyPlay!!.isRunning = false
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                panelAsynTask()

            }

        })
    }


    fun panelAsynTask() {
        if (asyPlay != null) {
            asyPlay!!.isRunning = false
        }
        asyPlay = MyAsyn()
        asyPlay!!.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun addFragmentSlide() {
        mFragmentListSlideSong.add(ViewPagerInfoSong(ManagerFragmentDiscover()))
        mFragmentListSlideSong.add(ViewPagerPlaySong())
        mFragmentListSlideSong.add(ViewPagerLyricSong())
        val viewPagerSlideSong = binding.slide.viewPagerSong
        val adapterSlideSong: FragmentStatePagerAdapter =
            object : FragmentStatePagerAdapter(supportFragmentManager!!) {
                override fun getItem(position: Int): Fragment {
                    return mFragmentListSlideSong[position]

                }

                override fun getCount(): Int {
                    return mFragmentListSlideSong.size
                }

            }
        viewPagerSlideSong.adapter = adapterSlideSong
        viewPagerSlideSong.currentItem = 1
        viewPagerSlideSong.offscreenPageLimit = mFragmentListSlideSong.size + 1
        viewPagerSlideSong.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                when (position) {
                    0 -> {
                        binding.slide.playerLayout.visibility = View.GONE
                        binding.slide.playerLayout.animate()
                            .translationY(binding.slide.playerLayout.height.toFloat()).duration =
                            1500

                    }
                    1 -> {
                        binding.slide.playerLayout.visibility = View.VISIBLE
                        binding.slide.playerLayout.animate()
                            .translationY(0f).duration = 200
                    }
                    2 -> {
                        binding.slide.playerLayout.animate()
                            .translationY(binding.slide.playerLayout.height.toFloat()).duration =
                            1500
                        if (binding.slide.playerLayout.translationY == binding.slide.playerLayout.height.toFloat())

                            binding.slide.playerLayout.visibility = View.GONE


                    }
                }
            }

            override fun onPageSelected(position: Int) {
            }

            override fun onPageScrollStateChanged(state: Int) {
            }

        })
    }

    private fun addNavigation() {
        val titles = arrayOf("Khám phá", "Xếp hạng", "Tìm kiếm", "Cá nhân")
        val naviMusic =
            AHBottomNavigationItem(titles[0], R.drawable.music)
        val naviRank = AHBottomNavigationItem(
            titles[1], R.drawable.sound_waves
        )
        val naviSearch = AHBottomNavigationItem(
            titles[2], R.drawable.top
        )
        val naviPersonal = AHBottomNavigationItem(
            titles[3], R.drawable.clapperboard
        )

        navigationBar.setTitleState(AHBottomNavigation.TitleState.ALWAYS_SHOW);
        navigationBar.addItem(naviMusic)
        navigationBar.addItem(naviRank)
        navigationBar.addItem(naviSearch)
        navigationBar.addItem(naviPersonal)



        navigationBar.setOnTabSelectedListener { position, wasSelected ->
            binding.viewPager.currentItem = position
            true
        }
        mFragmentList.add(ManagerFragmentDiscover())
        mFragmentList.add(ManagerRanking())
        mFragmentList.add(ManagerFragmentSearch())
        mFragmentList.add(ManagerPersonal(personalFragment))
        val adapter: FragmentPagerAdapter =
            object : FragmentPagerAdapter(supportFragmentManager!!) {
                override fun getItem(position: Int): Fragment {
                    return mFragmentList[position]
                }

                override fun getCount(): Int {
                    return mFragmentList.size
                }
            }
        binding.viewPager.adapter = adapter


    }

    private fun setOnClickListener() {
        binding.slide.playSong.setOnClickListener(this)
        binding.slide.btnPrevious.setOnClickListener(this)
        binding.slide.btnNext.setOnClickListener(this)
        binding.slide.shuffleOn.setOnClickListener(this)
        binding.slide.shuffleOff.setOnClickListener(this)
        binding.slide.repeatAll.setOnClickListener(this)
        binding.slide.repeatOne.setOnClickListener(this)
        binding.slide.repeatOff.setOnClickListener(this)
        binding.slide.keyboardDown.setOnClickListener(this)
        binding.slide.play.setOnClickListener(this)
        binding.slide.next.setOnClickListener(this)
        binding.slide.previous.setOnClickListener(this)
        binding.slide.moreVert.setOnClickListener(this)
    }

    override fun onClick(v: View?) {

        when (v) {
            v!!.play_song -> {
                if (isPlaying) {
                    onMusicPause()
                } else {
                    onMusicPlay()

                }
            }
            v!!.btn_next -> {
                onMusicNext()
            }
            v!!.btn_previous -> {
                onMusicPrevious()

            }
            v!!.shuffle_on -> {
                binding.slide.shuffleOn.visibility = View.INVISIBLE
                binding.slide.shuffleOff.visibility = View.VISIBLE
                onRamdom++
            }
            v!!.shuffle_off -> {
                binding.slide.shuffleOff.visibility = View.INVISIBLE
                binding.slide.shuffleOn.visibility = View.VISIBLE
                onRamdom--

            }
            v!!.play -> {
                if (isPlaying) {
                    onMusicPause()
                } else {
                    onMusicPlay()
                }
            }
            v!!.previous -> {
                onMusicPrevious()
            }
            v!!.next -> {
                onMusicNext()
            }
            v!!.repeat_all -> {
                playService!!.setLooping(true)
                playService!!.playMusic()
                binding.slide.repeatOff.visibility = View.INVISIBLE
                binding.slide.repeatOne.visibility = View.VISIBLE
                binding.slide.repeatAll.visibility = View.INVISIBLE
            }
            v!!.repeat_off -> {
                playService!!.setLooping(false)
                if (playService!!.currentPositionSong == songAlbums!!.size - 1) {
                    playService!!.currentPositionSong = 0
                    discoverModel.getInfo(songAlbums!![playService!!.currentPositionSong].linkSong)
                    discoverModel.infoAlbum.observe(this, Observer {
                        playService!!.createNotification(
                            this,
                            songAlbums!![playService!!.currentPositionSong],
                            R.drawable.ic_pause_black_24dp,
                            playService!!.currentPositionSong,
                            songAlbums!!.size - 1
                        )
                        binding.slide.nameSong.text = it.nameSong
                        binding.slide.singerSong.text =

                            it.singerSong.substring(7 until it.singerSong.length)
                        binding.slide.playNameSong.text = it.nameSong
                        binding.slide.playSingerSong.text =
                            it.singerSong.substring(7 until it.singerSong.length)
                    })
                }
                binding.slide.repeatOff.visibility = View.INVISIBLE
                binding.slide.repeatOne.visibility = View.INVISIBLE
                binding.slide.repeatAll.visibility = View.VISIBLE
            }
            v!!.repeat_one -> {
                playService!!.setLooping(false)
                binding.slide.repeatOff.visibility = View.VISIBLE
                binding.slide.repeatOne.visibility = View.INVISIBLE
                binding.slide.repeatAll.visibility = View.INVISIBLE
            }
            v!!.keyboard_down -> {
                navigationBar.visibility = View.VISIBLE
                slidingUpPanelLayout.panelState = PanelState.COLLAPSED

            }
            v!!.more_vert -> {
                addBottomSheet()
            }
        }

    }

    private fun slidingPanelUp() {
        discoverModel.infoAlbum.observe(this, Observer {
            binding.slide.nameSong.text = it.nameSong
            binding.slide.singerSong.text = it.singerSong.substring(7, it.singerSong.length)
            binding.slide.playNameSong.text = it.nameSong
            binding.slide.playSingerSong.text = it.singerSong.substring(7, it.singerSong.length)
            LoadDataUtils.loadImg(binding.slide.cirImagesSong, it.imgSong)
        })

        binding.slide.slidePlaySongMini.setOnClickListener {
            if (binding.slideMain.panelState == PanelState.EXPANDED) {
                binding.slide.keyboardDown.setOnClickListener {
                    binding.slideMain.panelState = PanelState.COLLAPSED
                }
            } else {
                binding.slideMain.panelState = PanelState.EXPANDED
            }
        }
        binding.slide.listPlay.setOnClickListener {

        }

        slidingUpPanelLayout.addPanelSlideListener(
            object :
                SlidingUpPanelLayout.PanelSlideListener {
                override fun onPanelSlide(panel: View?, slideOffset: Float) {
                    panel!!.slide_play_song_mini.alpha = 1 - slideOffset
                }

                override fun onPanelStateChanged(
                    panel: View?,
                    previousState: PanelState?,
                    newState: PanelState
                ) {
                    if (newState == PanelState.EXPANDED) {
                        binding.slide.slidePlaySongBig.visibility = View.VISIBLE
                    } else {
                        binding.slide.slidePlaySongBig.visibility = View.INVISIBLE
                    }
                }
            })
    }

    private fun createConnectionsToService() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.extras!!.getString("actionname")) {
                    CreateNotification!!.ACTION_PREVIUOS -> onMusicPrevious()
                    CreateNotification!!.ACTION_PLAY -> if (isPlaying) {
                        onMusicPause()
                    } else {
                        onMusicPlay()
                    }
                    CreateNotification!!.ACTION_NEXT -> onMusicNext()
                    CreateNotification!!.ACTION_CLOSE -> onMusicClose()
                }
            }
        }
        conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                playService = (service as PlayService.MyBinder).service
                playService!!.inter = this@MainActivity

            }

            override fun onServiceDisconnected(name: ComponentName?) {
                playService = null
            }
        }
        val intent = Intent(this, PlayService::class.java)
        bindService(intent, conn, Context.BIND_AUTO_CREATE)

    }

    private fun initBroadcastReceiver() {
        networkChangeReceiver = NetworkChangeReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction(getString(R.string.uses_permission))
        registerReceiver(networkChangeReceiver, intentFilter)
    }

    private fun checkConnectInternet() {
        NetworkUtil.getConnectivityStatus(this)
        NetworkUtil.getConnectivityStatusString(this)
    }


    override fun onPrepared() {
        val total = playService!!.getTotalTime()
        val format = SimpleDateFormat("mm:ss")
        binding.slide.tvTimeEnd.text = (format.format(total))
        asyPlay?.isRunning = false
        asyPlay = MyAsyn()
        asyPlay!!.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }


    override fun setOnCompletionListener() {
        asyPlay!!.cancel(true)
        if (onRamdom == 1) {
            var rd = (0 until songAlbums!!.size).random()
            while (rd == playService!!.currentPositionSong) {
                rd = (0 until songAlbums!!.size).random()
            }
            discoverModel.getInfo(songAlbums!![rd].linkSong)
            discoverModel.infoAlbum.observe(this, Observer {
                playService!!.createNotification(
                    this,
                    songAlbums!![rd],
                    R.drawable.ic_pause_black_24dp, rd,
                    songAlbums!!.size - 1
                )
                binding.slide.nameSong.text = it.nameSong
                binding.slide.singerSong.text =
                    it.singerSong.substring(7 until it.singerSong.length)
                binding.slide.playNameSong.text = it.nameSong
                binding.slide.playSingerSong.text =
                    it.singerSong.substring(7 until it.singerSong.length)
            })
        } else {
            if (discoverModel.newSong.value!!.size == 0) {
                return
            } else {
                playService!!.currentPositionSong++
                discoverModel!!.getInfo(songAlbums!![playService!!.currentPositionSong].linkSong)
                discoverModel.infoAlbum.observe(this, Observer {
                    playService!!.createNotification(
                        this,
                        songAlbums!![playService!!.currentPositionSong],
                        R.drawable.ic_pause_black_24dp, playService!!.currentPositionSong,
                        songAlbums!!.size - 1
                    )
                    binding.slide.nameSong.text = it.nameSong
                    binding.slide.singerSong.text =
                        it.singerSong.substring(7 until it.singerSong.length)
                    binding.slide.playNameSong.text = it.nameSong
                    binding.slide.playSingerSong.text =
                        it.singerSong.substring(7 until it.singerSong.length)
                })
            }

        }

    }


    @SuppressLint("StaticFieldLeak")
    inner class MyAsyn : AsyncTask<Void, Int?, Void?>() {
        var isRunning = true

        @SuppressLint("SimpleDateFormat")
        val format = SimpleDateFormat("mm:ss")

        @SuppressLint("WrongThread")
        override fun doInBackground(vararg params: Void?): Void? {
            while (isRunning) {
//                publishProgress(playService!!.getCurrentPosition())
                SystemClock.sleep(1000)
            }


            return null
        }

        override fun onProgressUpdate(vararg values: Int?) {
            binding.slide.tvTimeBegin.text = (format.format(values[0]))
            binding.slide.seekbar.progress =
                (values[0]!! * 100 / playService!!.getTotalTime())

        }
    }

    fun getCurrentFragment(): Fragment? {
        val fragments = supportFragmentManager.fragments
        for (f in fragments) {
            if (f != null && f.isVisible) {
                return f
            }
        }
        return null
    }

    override fun onBackPressed() {
        if (slidingUpPanelLayout.panelState == PanelState.EXPANDED || slidingUpPanelLayout.panelState == PanelState.ANCHORED) {
            slidingUpPanelLayout.panelState = PanelState.COLLAPSED
        }
        val f = getCurrentFragment()
        if (f == null) {
            super.onBackPressed()
        } else {
            if (f is ManagerFragmentDiscover) {
                val countB = f.childFragmentManager.backStackEntryCount
                if (countB > 0) {
                    f.childFragmentManager.popBackStackImmediate()
                    return
                }
            }
        }
        super.onBackPressed()


    }

    override fun onStop() {
        super.onStop()
    }

    override fun onMusicPrevious() {

        asyPlay!!.cancel(true)
        playService!!.currentPositionSong--
        if (playService!!.currentPositionSong >= 0) {
            discoverModel.getInfo(songAlbums!![playService!!.currentPositionSong].linkSong)
            discoverModel.infoAlbum.observe(this, Observer {
                songAlbums!![playService!!.currentPositionSong].linkMusic =
                    it.linkMusic
                playService!!.createNotification(
                    this,
                    songAlbums!![playService!!.currentPositionSong],
                    R.drawable.ic_pause_black_24dp, playService!!.currentPositionSong,
                    songAlbums!!.size - 1
                )
                binding.slide.nameSong.text = it.nameSong
                binding.slide.singerSong.text =
                    it.singerSong.substring(7 until it.singerSong.length)
                binding.slide.playNameSong.text = it.nameSong
                binding.slide.playSingerSong.text =
                    it.singerSong.substring(7 until it.singerSong.length)
            })

        } else {
            playService!!.currentPositionSong = songAlbums!!.size - 1
            discoverModel.getInfo(songAlbums!![playService!!.currentPositionSong].linkSong)
            discoverModel.infoAlbum.observe(this, Observer {
                songAlbums!![playService!!.currentPositionSong].linkMusic =
                    it.linkMusic
                playService!!.createNotification(
                    this,
                    songAlbums!![playService!!.currentPositionSong],
                    R.drawable.ic_pause_black_24dp, playService!!.currentPositionSong,
                    songAlbums!!.size - 1
                )
                binding.slide.nameSong.text = it.nameSong
                binding.slide.singerSong.text =
                    it.singerSong.substring(7 until it.singerSong.length)
                binding.slide.playNameSong.text = it.nameSong
                binding.slide.playSingerSong.text =
                    it.singerSong.substring(7 until it.singerSong.length)

            })
        }
    }


    override fun onMusicPlay() {
        playService!!.pauseMusic()
        playService!!.createNotification(
            this,
            songAlbums!![playService!!.currentPositionSong],
            R.drawable.ic_play_arrow_black_24dp,
            playService!!.currentPositionSong,
            songAlbums!!.size - 1
        )
        binding.slide.playSong.setImageResource(R.drawable.ic_play_arrow_black_48dp)
        binding.slide.play.setImageResource(R.drawable.ic_play_arrow_black_48dp)
        isPlaying = true

    }

    override fun onMusicPause() {
        playService!!.playMusic()
        playService!!.createNotification(
            this,
            songAlbums!![playService!!.currentPositionSong],
            R.drawable.ic_pause_black_24dp,
            playService!!.currentPositionSong,
            songAlbums!!.size - 1
        )
        binding.slide.playSong.setImageResource(R.drawable.ic_pause_black_48dp)
        binding.slide.play.setImageResource(R.drawable.ic_pause_black_48dp)
        isPlaying = false

    }


    override fun onMusicNext() {

        asyPlay!!.cancel(true)
        playService!!.releaseMusic()
        playService!!.currentPositionSong++
        if (playService!!.currentPositionSong <= songAlbums!!.size - 1) {
            discoverModel.getInfo(songAlbums!![playService!!.currentPositionSong].linkSong)
            discoverModel.infoAlbum.observe(this, Observer {
                playService!!.createNotification(
                    this,
                    songAlbums!![playService!!.currentPositionSong],
                    R.drawable.ic_pause_black_24dp,
                    playService!!.currentPositionSong,
                    songAlbums!!.size - 1
                )
                binding.slide.nameSong.text = it.nameSong
                binding.slide.singerSong.text =
                    it.singerSong.substring(7 until it.singerSong.length)
                binding.slide.playNameSong.text = it.nameSong
                binding.slide.playSingerSong.text =
                    it.singerSong.substring(7 until it.singerSong.length)
            })

        } else {
            playService!!.currentPositionSong = 0
            discoverModel.getInfo(songAlbums!![playService!!.currentPositionSong].linkSong)
            discoverModel.infoAlbum.observe(this, Observer {
                playService!!.createNotification(
                    this,
                    songAlbums!![playService!!.currentPositionSong],
                    R.drawable.ic_pause_black_24dp,
                    playService!!.currentPositionSong,
                    songAlbums!!.size - 1
                )
                binding.slide.nameSong.text = it.nameSong
                binding.slide.singerSong.text =
                    it.singerSong.substring(7 until it.singerSong.length)
                binding.slide.playNameSong.text = it.nameSong
                binding.slide.playSingerSong.text =
                    it.singerSong.substring(7 until it.singerSong.length)
            })
        }
    }

    override fun onMusicClose() {
        NotificationManagerCompat.from(
            this
        ).cancel(1)
    }

    override fun onDestroy() {
        unbindService(conn)
        unregisterReceiver(broadcastReceiver)
        super.onDestroy()

        asyPlay?.isRunning = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager!!.cancelAll()
        }
    }


}







