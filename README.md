Inimesed
========

Inimesed is an Android app that lets you search your contacts by voice.
Since it uses the Android port of the Pocketsphinx speech recognizer, it does not require an internet connection.
The current version of Inimesed uses Estonian acoustic models.

The [FAQ](https://github.com/Kaljurand/Inimesed/wiki/FAQ) tries to answer often occuring
questions regarding porting Inimesed to other languages and use cases.


PocketSphinx on Android
-----------------------

In order to successfully build:

  - PocketSphinx libraries must be installed
  - swig must be installed
  - Android NDK must be installed (tested with: android-ndk-r8e)
  - Android SDK must be installed
  - change `SPHINX_PATH` in `jni/Android.mk` to match your configuration
    - pocketsphinx (PocketSphinx 0.7 / PocketSphinx v0.5.99)
    - sphinxbase (Sphinxbase-0.7)

Follow <http://cmusphinx.sourceforge.net/2011/05/building-pocketsphinx-on-android/>
to get everything installed.


Building for release
--------------------

Run these commands in the app-directory. You need to have the files:

  - local.properties
  - speak.keystore
  - assets/hmm/ should contain the acoustic models (see: <https://github.com/alumae/et-pocketsphinx-tutorial/tree/master/models/hmm>)

First time:

	# Delete bin/ and gen/
	ant clean

	# This is needed only for the first time,
	# and is run in jni-directory
	cd jni/

	# Create Java wrappers and pocketsphinx_wrap.c.
	# They are already in the repository (generated using swig 2.0.4) so you do not
	# necessarily need to run this.
	# Note that swig 2.0.5+ breaks ndk_build. There is probably
	# a simple solution to it but I haven't found it.
	make swig_build

	# Compile the c-files.
	# First set an environment variable `ANDROID_NDK` to point to the Android NDK
	make ndk_build

	# Go back into the main directory
	cd ..

	# Compile and package everything for the release
	# (asks for the release keys)
	ant release

	# Install
	adb install -r bin/Inimesed-release.apk


Next time:

	ant clean release
	adb install -r bin/Inimesed-release.apk


### Tags

Version tags are set by e.g.

	git tag -a v0.8.18 -m 'version 0.8.18'

The last number should be even.


### Lint

	lint --html report.html .
