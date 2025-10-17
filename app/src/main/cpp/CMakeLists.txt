# Minimum CMake version required
cmake_minimum_required(VERSION 3.18)

# Project name
project(tokenizer_native)

# Import prebuilt DJL tokenizer library
add_library(
        djl_tokenizer           # CMake name
        SHARED                  # It’s a shared library (.so)
        IMPORTED                 # We are importing, not building from source
)

# Specify path to prebuilt .so for each ABI
set_target_properties(
        djl_tokenizer
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libdjl_tokenizer.so
)

# Import prebuilt libc++_shared.so from NDK r22
add_library(
        cxx_shared
        SHARED
        IMPORTED
)

set_target_properties(
        cxx_shared
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libc++_shared.so
)

# Find log library from Android NDK
find_library(
        log-lib log
)

# Link libraries
target_link_libraries(
        djl_tokenizer
        ${log-lib}   # Needed for Android logging
        cxx_shared   # Link C++ runtime
)
