package com.example.kaimera.sphereqix

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20

class SphereQixGame : Game() {

    override fun create() {
        // Initialize the game screen
        setScreen(GameScreen(this))
    }

    override fun render() {
        // Clear the screen with a dark background color
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        
        super.render()
    }
}
