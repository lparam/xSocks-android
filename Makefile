all: app/src/main/jniLibs/armeabi/libsystem.so

.PHONY: clean

clean:
	rm -rf libs
	rm -rf app/src/main/jniLibs
	rm -rf jni/xsocks/xsocks-android-armv7-a

app/src/main/jniLibs/armeabi/libsystem.so: jni/system.cpp jni/Android.mk
	if [ a == a$(ANDROID_NDK_HOME) ]; then \
		echo ANDROID_NDK_HOME is not set ;\
		exit 1 ;\
	fi ;\
	pushd jni/xsocks || exit 1 ;\
	if [ ! -f xsocks-android-i686/xsocks ]; then \
		dist-build/android-x86.sh || exit 1 ;\
	fi ;\
	if [ ! -f xsocks-android-armv7-a/xsocks ]; then \
		dist-build/android-armv7-a.sh || exit 1 ;\
	fi ;\
	popd ;\
	pushd jni ;\
	$(ANDROID_NDK_HOME)/ndk-build NDK_LOG=1 V=1 || exit 1 ;\
	popd ;\
	rm -rf app/src/main/assets/x86 ;\
	rm -rf app/src/main/assets/armeabi-v7a ;\
	mkdir -p app/src/main/assets/x86 ;\
	mkdir -p app/src/main/assets/armeabi-v7a ;\
	install -d app/src/main/jniLibs/x86 ;\
	install -d app/src/main/jniLibs/armeabi-v7a ;\
	install libs/x86/libsystem.so app/src/main/jniLibs/x86 ;\
	install libs/x86/tun2socks app/src/main/assets/x86 ;\
	install libs/x86/pdnsd app/src/main/assets/x86 ;\
	install jni/xsocks/xsocks-android-i686/xsocks app/src/main/assets/x86 ;\
	install jni/xsocks/xsocks-android-i686/xforwarder app/src/main/assets/x86 ;\
	install libs/armeabi-v7a/tun2socks app/src/main/assets/armeabi-v7a ;\
	install libs/armeabi-v7a/pdnsd app/src/main/assets/armeabi-v7a ;\
	install libs/armeabi-v7a/libsystem.so app/src/main/jniLibs/armeabi-v7a ;\
	install jni/xsocks/xsocks-android-armv7-a/xsocks app/src/main/assets/armeabi-v7a ;\
	install jni/xsocks/xsocks-android-armv7-a/xforwarder app/src/main/assets/armeabi-v7a ;

