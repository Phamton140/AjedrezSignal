package com.example.ajedrezsignal.engine

/**
 * Interface JNI para comunicarse con el binario de Stockfish 16.
 */
class StockfishBridge {

    companion object {
        init {
            // Nombre de la librería definido en CMakeLists.txt
            System.loadLibrary("ajedrezsignal")
        }
    }

    /**
     * Inicializa el motor con límites de búsqueda y memoria.
     */
    external fun initEngine(moveTimeMs: Int): Boolean

    /**
     * Envía una posición FEN y retorna el bestmove UCI tras el análisis.
     */
    external fun getBestMove(fen: String): String

    /**
     * Detiene los hilos de cálculo (Kill Switch).
     */
    external fun stopEngine()
}
