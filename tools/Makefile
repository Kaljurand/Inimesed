# Shorcuts for common testing tasks

hmm_local = ../assets/hmm/
dir = /sdcard/Android/data/ee.ioc.phon.android.inimesed/files/
log = /sdcard/Android/data/ee.ioc.phon.android.inimesed/files/pocketsphinx.log
jsgf = /sdcard/Android/data/ee.ioc.phon.android.inimesed/files/lm/et_EE/lm.jsgf
dict = /sdcard/Android/data/ee.ioc.phon.android.inimesed/files/lm/et_EE/lm.dic

help:
	cat Makefile | grep ^[a-z].*:$

adb_ls:
	adb shell ls -R $(dir)

adb_cat_dict:
	adb shell cat $(dict)

adb_diff_dict:
	adb shell cat $(dict) | sort > lm.dic
	diff gold_lm.dic lm.dic

adb_cat_jsgf:
	adb shell cat $(jsgf)

adb_cat_log:
	adb shell cat $(log)

# TODO: one could also use 'play'
#play -r 16000 -s -2 000000000.raw
# For the following to work:
# the device must be connected via USB,
# Inimesed must be running,
# some utterances must have been performed.
test_raw:
	adb pull $(dir)/raw/ .
	aplay --format=S16_LE --file-type raw --channels 1 --rate 16000 *.raw

compare:
	md5sum $(hmm_local)/*
	adb pull $(dir)/hmm/et_EE/16000/sendump .
	md5sum sendump
