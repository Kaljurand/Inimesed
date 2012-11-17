ndk_dir = /home/kaarel/myapps/android-ndk-r8b/

all: swig_build ndk_build

ndk_build:
	@echo NDK build
	$(ndk_dir)/ndk-build -B

swig_build:
	@echo SWIG build
	swig -java -package edu.cmu.pocketsphinx -outdir edu/cmu/pocketsphinx/ pocketsphinx.i
