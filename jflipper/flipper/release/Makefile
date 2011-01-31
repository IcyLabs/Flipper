all:
	cp ../flipper-*.jar .
	cp ../plugins/*.jar plugins/
	cp ../lib/*.jar lib/

runmac:
	java -jar flipper-mac.jar -XstartOnFirstThread

dropbox: all
	cp *.jar ~/Dropbox/Icy/Flipper\ Java\ Release/
	cp plugins/*.jar ~/Dropbox/Icy/Flipper\ Java\ Release/plugins/
