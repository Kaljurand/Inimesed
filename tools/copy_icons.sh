
sdk=${HOME}/myapps/android-sdk-linux_x86/
platform="15"
res=${sdk}/platforms/android-${platform}/data/res/
target_res="../res/"

for d in "ldpi" "mdpi" "hdpi" "xhdpi";
do
  for image in "ic_menu_help" "ic_menu_preferences" "ic_menu_sort_by_size";
  do
    echo "${res}/drawable-${d}/${image}.png -> ${target_res}/drawable-${d}/"
    cp ${res}/drawable-${d}/${image}.png ${target_res}/drawable-${d}/
  done
done
