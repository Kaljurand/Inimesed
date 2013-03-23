all: swig_build ndk_build

ndk_build:
	@echo NDK build using $(ANDROID_NDK)
	$(ANDROID_NDK)/ndk-build -B

swig_build:
	@echo SWIG build
	swig -java -package edu.cmu.pocketsphinx -outdir edu/cmu/pocketsphinx/ pocketsphinx.i
