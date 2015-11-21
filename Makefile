
LIB_SYSTEM=app/src/main/jniLibs/armeabi/libsystem.so

all: $(LIB_SYSTEM)

.PHONY: clean

clean:
	rm -rf libs
	rm -rf app/src/main/jniLibs
	$(ANDROID_NDK_HOME)/ndk-build clean

$(LIB_SYSTEM): jni/system.cpp jni/Android.mk
	if [ a == a$(ANDROID_NDK_HOME) ]; then \
		echo ANDROID_NDK_HOME is not set ;\
		exit 1 ;\
	fi ;\
	pushd jni/xSocks || exit 1 ;\
	dist-build/android-x86.sh || exit 1 ;\
	dist-build/android-armv7-a.sh || exit 1 ;\
	popd ;\
	pushd jni ;\
	$(ANDROID_NDK_HOME)/ndk-build NDK_LOG=1 V=0 || exit 1 ;\
	popd ;\
	rm -rf app/src/main/assets/x86 ;\
	rm -rf app/src/main/assets/armeabi-v7a ;\
	mkdir -p app/src/main/assets/x86 ;\
	mkdir -p app/src/main/assets/armeabi-v7a ;\
	install -d app/src/main/jniLibs/x86 ;\
	install -d app/src/main/jniLibs/armeabi-v7a ;\
	install libs/x86/pdnsd app/src/main/assets/x86 ;\
	install libs/x86/tun2socks app/src/main/assets/x86 ;\
	install libs/x86/libsystem.so app/src/main/jniLibs/x86 ;\
	install libs/armeabi-v7a/pdnsd app/src/main/assets/armeabi-v7a ;\
	install libs/armeabi-v7a/tun2socks app/src/main/assets/armeabi-v7a ;\
	install libs/armeabi-v7a/libsystem.so app/src/main/jniLibs/armeabi-v7a ;\
	install jni/xSocks/xSocks-android-i686/xSocks app/src/main/assets/x86 ;\
	install jni/xSocks/xSocks-android-i686/xForwarder app/src/main/assets/x86 ;\
	install jni/xSocks/xSocks-android-armv7-a/xSocks app/src/main/assets/armeabi-v7a ;\
	install jni/xSocks/xSocks-android-armv7-a/xForwarder app/src/main/assets/armeabi-v7a ;
