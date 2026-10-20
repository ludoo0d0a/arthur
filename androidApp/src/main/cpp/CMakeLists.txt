cmake_minimum_required(VERSION 3.22.1)

project("executorch_midi_jni")

add_library(
    executorch_midi_jni
    SHARED
    executorch_midi_bridge.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    executorch_midi_jni
    ${log-lib}
)
