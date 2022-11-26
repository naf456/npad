package com.naf.npad.views

import android.graphics.Canvas
import android.widget.EdgeEffect
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.RecyclerView

private const val OVERSCROLL_TRANSLATION_MAGNITUDE = 0.5f

/** The magnitude of translation distance when the list reaches the edge on fling. */
private const val FLING_TRANSLATION_MAGNITUDE = 1f

class BounceEdgeEffect(private val recyclerView: RecyclerView): RecyclerView.EdgeEffectFactory() {

    override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
        return object : EdgeEffect(recyclerView.context) {

            // A reference to the [SpringAnimation] for this RecyclerView used to bring the item back after the over-scroll effect.
            var anim: SpringAnimation? = null

            var isHorizontal : Boolean = false

            override fun onPull(deltaDistance: Float) {
                super.onPull(deltaDistance)
                handlePull(deltaDistance)
            }

            override fun onPull(deltaDistance: Float, displacement: Float) {
                super.onPull(deltaDistance, displacement)
                handlePull(deltaDistance)
            }

            private fun handlePull(deltaDistance: Float) {
                // Translate the recyclerView with the distance
                val sign = if (direction == DIRECTION_BOTTOM || direction == DIRECTION_RIGHT) -1 else 1
                val horizontal = direction == DIRECTION_LEFT || direction == DIRECTION_RIGHT
                val translationDelta = recyclerView.width * deltaDistance * OVERSCROLL_TRANSLATION_MAGNITUDE
                if(horizontal) {
                    recyclerView.translationX += sign * translationDelta
                    isHorizontal = true
                }
                else {
                    recyclerView.translationY += sign * translationDelta
                    isHorizontal = false
                }

                anim?.cancel()
            }
            override fun onRelease() {
                super.onRelease()
                // The finger is lifted. Start the animation to bring translation back to the resting state.
                if(isHorizontal) {
                    if (recyclerView.translationX != 0f) {
                        anim = createAnim()?.also { it.start() }
                    }
                }
                else {
                    if (recyclerView.translationY != 0f) {
                        anim = createAnim()?.also { it.start() }
                    }
                }
            }

            override fun onAbsorb(velocity: Int) {
                super.onAbsorb(velocity)

                // The list has reached the edge on fling.
                val sign = if (direction == DIRECTION_BOTTOM || direction == DIRECTION_RIGHT) -1 else 1
                val translationVelocity = sign * velocity * FLING_TRANSLATION_MAGNITUDE
                anim?.cancel()
                anim = createAnim().setStartVelocity(translationVelocity)?.also { it.start() }
            }

            private fun createAnim() =
                SpringAnimation(recyclerView, if (isHorizontal) SpringAnimation.TRANSLATION_X else SpringAnimation.TRANSLATION_Y)
                    .setSpring(
                        SpringForce()
                        .setFinalPosition(0f)
                        .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY)
                        .setStiffness(SpringForce.STIFFNESS_MEDIUM))

            override fun draw(canvas: Canvas?): Boolean {
                // don't paint the usual edge effect
                return false
            }

            override fun isFinished(): Boolean {
                // Without this, will skip future calls to onAbsorb()
                return anim?.isRunning?.not() ?: true
            }

        }
    }
}