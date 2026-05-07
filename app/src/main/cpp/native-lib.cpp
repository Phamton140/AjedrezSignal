#include <jni.h>
#include <string>
#include <thread>
#include <chrono>

class StockfishInstance {
public:
    void setOption(const std::string& name, const std::string& value) {}
    std::string analyze(const std::string& fen, int moveTimeMs) {
        std::this_thread::sleep_for(std::chrono::milliseconds(moveTimeMs));
        return "e7e5"; // Respuesta simulada sincronizada con el flujo háptico
    }
    void stop() {}
};

static StockfishInstance engine;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_ajedrezsignal_engine_StockfishBridge_initEngine(
        JNIEnv* env, jobject thiz, jint moveTimeMs) {
    engine.setOption("Hash", "32");
    engine.setOption("Threads", "2");
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_ajedrezsignal_engine_StockfishBridge_getBestMove(
        JNIEnv* env, jobject thiz, jstring fen) {
    const char *nativeFen = env->GetStringUTFChars(fen, 0);
    std::string bestMove = engine.analyze(nativeFen, 2000);
    env->ReleaseStringUTFChars(fen, nativeFen);
    return env->NewStringUTF(bestMove.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_ajedrezsignal_engine_StockfishBridge_stopEngine(
        JNIEnv* env, jobject thiz) {
    engine.stop();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_ajedrezsignal_MainActivity_stringFromJNI(
        JNIEnv* env, jobject thiz) {
    return env->NewStringUTF("Motor Háptico Activo");
}
