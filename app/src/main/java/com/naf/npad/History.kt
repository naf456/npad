package com.naf.npad

class History (private val maxSize: Int) {

    private val undos : MutableList<Xdo> = mutableListOf()
    private val redos : MutableList<Xdo> = mutableListOf()

    //Accepting
    var recording : Boolean = false
        private set

    private var busy : Boolean = false

    interface Xdo {
        fun undo()
        fun redo()
    }

    fun add(xdo: Xdo) {
        if(!recording || busy) return
        if(undos.size >= maxSize)
            undos.removeFirst()
        undos.add(xdo)
        redos.clear()
    }

    fun startRecording() { recording = true }
    fun stopRecording() { recording = false}

    fun clear() {
        undos.clear()
        redos.clear()
    }

    fun reset() {
        clear()
        stopRecording()
    }

    val canUndo : Boolean
    get() {
        return undos.size > 0
    }

    fun undo() {
        if(busy || !canUndo) return
        busy = true
        stopRecording()
        val xdo = undos.removeLast()
        xdo.undo()
        redos.add(xdo)
        startRecording()
        busy = false
    }

    val canRedo : Boolean
    get() {
        return redos.size > 0
    }

    fun redo() {
        if (busy || !canRedo) return
        busy = true
        stopRecording()
        val xdo = redos.removeLast()
        xdo.redo()
        undos.add(xdo)
        startRecording()
        busy = false
    }
}