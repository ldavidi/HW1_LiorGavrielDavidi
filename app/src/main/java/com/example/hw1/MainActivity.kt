package com.example.hw1

import android.os.Bundle
import android.os.Handler
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var spaceship: ImageView
    private lateinit var leftButton: ImageButton
    private lateinit var rightButton: ImageButton
    private lateinit var gameContainer: FrameLayout
    private lateinit var lifeIcons: List<ImageView>

    private val laneCount = 3
    private var currentLane = 1
    private var lives = 3

    private val asteroidSpawnInterval = 1500L
    private val asteroidFallDuration = 4000L
    private val asteroidSize = 300
    private val asteroidList = mutableListOf<ImageView>()
    private val asteroidHandler = Handler()
    private var asteroidSpawnerRunnable: Runnable? = null
    private var isGamePaused = false
    private lateinit var hitSound: android.media.MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        hitSound = android.media.MediaPlayer.create(this, R.raw.hit)

        spaceship = findViewById(R.id.spaceship)
        leftButton = findViewById(R.id.left_button)
        rightButton = findViewById(R.id.right_button)
        gameContainer = findViewById(R.id.game_container)

        lifeIcons = listOf(
            findViewById(R.id.life1),
            findViewById(R.id.life2),
            findViewById(R.id.life3)
        )

        gameContainer.post {
            moveSpaceshipToLane(currentLane)
            startAsteroidSpawner()
        }

        leftButton.setOnClickListener {
            if (currentLane > 0) {
                currentLane--
                moveSpaceshipToLane(currentLane)
            }
        }

        rightButton.setOnClickListener {
            if (currentLane < laneCount - 1) {
                currentLane++
                moveSpaceshipToLane(currentLane)
            }
        }

        updateLivesUI()
    }

    private fun moveSpaceshipToLane(lane: Int) {
        val laneWidth = gameContainer.width / laneCount
        val targetLeftMargin = lane * laneWidth + laneWidth / 2 - spaceship.width / 2

        val params = spaceship.layoutParams as FrameLayout.LayoutParams
        params.leftMargin = targetLeftMargin
        spaceship.layoutParams = params
    }


    private fun startAsteroidSpawner() {
        asteroidSpawnerRunnable = object : Runnable {
            override fun run() {
                if (!isGamePaused) {
                    spawnAsteroid()
                    asteroidHandler.postDelayed(this, asteroidSpawnInterval)
                }
            }
        }
        asteroidHandler.post(asteroidSpawnerRunnable!!)
    }


    private fun spawnAsteroid() {
        val asteroid = ImageView(this)
        asteroid.setImageResource(R.drawable.asteroid)

        val lane = Random.nextInt(0, laneCount)
        val laneWidth = gameContainer.width / laneCount
        val x = lane * laneWidth + laneWidth / 2 - asteroidSize / 2

        val layoutParams = FrameLayout.LayoutParams(asteroidSize, asteroidSize)
        layoutParams.leftMargin = x
        layoutParams.topMargin = 0

        gameContainer.addView(asteroid, layoutParams)
        asteroidList.add(asteroid)

        // Animate falling and check collision in real-time
        val startTime = System.currentTimeMillis()
        val updateInterval = 30L
        val handler = Handler()

        val runnable = object : Runnable {
            override fun run() {
                if (isGamePaused) {
                    handler.postDelayed(this, updateInterval)
                    return
                }

                val elapsed = System.currentTimeMillis() - startTime
                val progress = elapsed.toFloat() / asteroidFallDuration

                val params = asteroid.layoutParams as FrameLayout.LayoutParams
                params.topMargin = (progress * gameContainer.height).toInt()
                asteroid.layoutParams = params

                if (checkCollisionRealtime(asteroid)) {
                    handleHit()
                    gameContainer.removeView(asteroid)
                    asteroidList.remove(asteroid)
                    return
                }

                if (progress < 1f) {
                    handler.postDelayed(this, updateInterval)
                } else {
                    gameContainer.removeView(asteroid)
                    asteroidList.remove(asteroid)
                }
            }
        }
        handler.post(runnable)
    }
    private fun checkCollisionRealtime(asteroid: ImageView): Boolean {
        val asteroidParams = asteroid.layoutParams as FrameLayout.LayoutParams
        val spaceshipParams = spaceship.layoutParams as FrameLayout.LayoutParams

        val asteroidCenterX = asteroidParams.leftMargin + asteroid.width / 2
        val asteroidCenterY = asteroidParams.topMargin + asteroid.height / 2

        val spaceshipCenterX = spaceshipParams.leftMargin + spaceship.width / 2
        val spaceshipCenterY = gameContainer.height - spaceship.height / 2 - 100 // 100 is bottom margin

        val collisionX = abs(asteroidCenterX - spaceshipCenterX) < spaceship.width * 0.6
        val collisionY = abs(asteroidCenterY - spaceshipCenterY) < spaceship.height * 0.6

        return collisionX && collisionY
    }



    private fun handleHit() {
        if (isGamePaused) return // Skip if paused

        vibrateOnHit()
        playHitSound()

        lives--
        updateLivesUI()

        if (lives == 0) {
            Toast.makeText(this, "All lives lost! Restarting...", Toast.LENGTH_SHORT).show()
            resetLives()
        } else {
            Toast.makeText(this, "Ouch! Lives left: $lives", Toast.LENGTH_SHORT).show()
        }
    }



    private fun updateLivesUI() {
        for (i in lifeIcons.indices) {
            lifeIcons[i].alpha = if (i < lives) 1f else 0.2f
        }
    }

    private fun resetLives() {
        lives = 3
        updateLivesUI()
    }
    private fun playHitSound() {
        if (hitSound.isPlaying) {
            hitSound.seekTo(0)
        }
        hitSound.start()
    }

    private fun vibrateOnHit() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(300)
        }
    }
    override fun onPause() {
        super.onPause()
        isGamePaused = true
        asteroidSpawnerRunnable?.let {
            asteroidHandler.removeCallbacks(it)
        }
    }

    override fun onResume() {
        super.onResume()
        isGamePaused = false
        asteroidSpawnerRunnable?.let {
            asteroidHandler.post(it)
        }
    }


}
