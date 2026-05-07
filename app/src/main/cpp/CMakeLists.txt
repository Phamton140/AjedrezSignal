cmake_minimum_required(VERSION 3.22.1)

project("ajedrezsignal")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Optimizaciones de rendimiento de alto nivel
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -flto -fno-exceptions -fno-rtti")
set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -flto")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -flto")

if(${ANDROID_ABI} STREQUAL "arm64-v8a")
    add_definitions(-DUSE_NEON)
endif()

# Definimos el nombre de la librería como 'ajedrezsignal'
add_library(
        ajedrezsignal
        SHARED
        native-lib.cpp
)

find_library(log-lib log)

target_link_libraries(
        ajedrezsignal
        ${log-lib}
)
