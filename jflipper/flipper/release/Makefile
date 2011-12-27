all:
	cp ../flipper-*.jar .
	cp ../plugins/*.jar plugins/
	cp ../lib/*.jar lib/

runmac:
	java -jar flipper-mac.jar -XstartOnFirstThread

dropbox: all
	install_name_tool -change "/usr/local/lib/libusb-1.0.0.dylib" "libusb-1.0.0.dylib" libusb.dylib
	cp *.jar ~/Dropbox/Icy/Flipper\ Java\ Release/
	cp *.dylib ~/Dropbox/Icy/Flipper\ Java\ Release/
	cp plugins/*.jar ~/Dropbox/Icy/Flipper\ Java\ Release/plugins/
	cp flip ~/Dropbox/Icy/Flipper\ Java\ Release/
