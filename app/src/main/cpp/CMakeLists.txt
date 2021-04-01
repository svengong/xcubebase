# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)
project("base")


find_library(log-lib log)

# 编译gumjs
add_library(gumjs STATIC IMPORTED)
set_target_properties(gumjs
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libfrida-gumjs.a)


# 添加到apk
add_library(xcubebase SHARED frida-gumjs.cpp NativeEntry.cpp)
target_link_libraries(xcubebase gumjs ${log-lib})
# 添加到apk
add_library(hellofrida SHARED HelloFrida.cpp)
target_link_libraries(hellofrida ${log-lib})

add_executable(shellcmd shellcmd.cpp)
target_link_libraries(shellcmd ${log-lib})