wav="wav.wav"
data="../data"

#arecord --format=S16_LE --file-type wav --channels 1 --rate 16000 > $wav
#-lm /usr/local/share/pocketsphinx/model/lm/en/turtle.DMP \

pocketsphinx_continuous \
-fwdflat no \
-bestpath no \
-jsgf $data/lm/lm.jsgf \
-dict $data/lm/lm.dic \
-hmm $data/acoustic/est16k-pocketsphinx.cd_ptm_1000 \
-samprate 16000 \
-nfft 256 \
-infile $wav
