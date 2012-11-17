# Bash script that creates Android icons (png) of different sizes
# from a single input SVG file.
#
# Icon name prefixes determine the type (and the size) of the icon.
#
# Icons: ic: ic_star.png
# Launcher icons: ic_launcher: ic_launcher_calendar.png
# Menu icons: ic_menu: ic_menu_archive.png
# Status bar icons: ic_stat_notify: ic_stat_notify_msg.png
# Tab icons: ic_tab: ic_tab_recent.png
# Dialog icons: ic_dialog: ic_dialog_info.png
#
# @author Kaarel Kaljurand
# @version 2012-04-29
# @work_in_progress

APP_SRC=".."

dir_svg=${APP_SRC}/images/
dir_png=${APP_SRC}/res/

dir_png_xhdpi=${dir_png}/drawable-xhdpi/
dir_png_hdpi=${dir_png}/drawable-hdpi/
dir_png_mdpi=${dir_png}/drawable-mdpi/
dir_png_ldpi=${dir_png}/drawable-ldpi/

# Launcher icon
launcher_logo_w=512
launcher_logo_h=512

launcher_xhdpi_w=96
launcher_xhdpi_h=96

launcher_hdpi_w=72
launcher_hdpi_h=72

launcher_mdpi_w=48
launcher_mdpi_h=48

launcher_ldpi_w=36
launcher_ldpi_h=36

echo "Generating launcher icon:"
for path in ${dir_svg}ic_launcher.svg
do
	filename=$(basename $path)
	file=${filename%.*}
	echo "$file"
	rsvg-convert -f png -w ${launcher_logo_w} -h ${launcher_logo_h} -o ${dir_svg}/$file.png $path
	rsvg-convert -f png -w ${launcher_xhdpi_w} -h ${launcher_xhdpi_h} -o ${dir_png_xhdpi}/$file.png $path
	rsvg-convert -f png -w ${launcher_hdpi_w} -h ${launcher_hdpi_h} -o ${dir_png_hdpi}/$file.png $path
	rsvg-convert -f png -w ${launcher_mdpi_w} -h ${launcher_mdpi_h} -o ${dir_png_mdpi}/$file.png $path
done
