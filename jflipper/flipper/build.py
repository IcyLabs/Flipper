import subprocess
import glob
import os

SOURCE_DIRS = ["src/flipper/usb", "src/flipper/jtag", "src/flipper/gui", "src/flipper/console", "src/flipper"]
TARGETS = ["penguino"]
TARGETS_DIR = "src/flipper/targets"
TARGETS_DEST = "plugins"

manifest_file = "flipper-mac.mf"
jar_file = "flipper-mac.jar"
classpath = ["lib/libusb4j.jar", "lib/jna.jar"]
classpath += ["lib/org.eclipse.swt.cocoa.macosx.x86_64_3.6.1.v3655c.jar", "lib/swt-mac.jar"]

os.environ["CLASSPATH"] = ':'.join(classpath)

java_files = []
for source_dir in SOURCE_DIRS:
	java_files.extend(glob.glob(source_dir+"/*.java"))

subprocess.check_call(["javac", "-d", "classes"] + java_files)
subprocess.check_call(["jar", "cmf", manifest_file, jar_file, "-C", "classes", "flipper"])

for target in TARGETS:
	target_classpath = classpath + [jar_file]
	os.environ["CLASSPATH"] = ':'.join(target_classpath)

	target_dir = os.path.join(TARGETS_DIR, target) 
	java_files = glob.glob(target_dir + "/*.java")
	jar_file = os.path.join(TARGETS_DEST, target + ".jar")
	subprocess.check_call(["javac", "-d", "classes"] + java_files)
	subprocess.check_call(["jar", "cf", jar_file, "-C", "classes", "flipper/targets/" + target])

