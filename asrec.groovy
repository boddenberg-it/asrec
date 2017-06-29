#!/usr/bin/groovy
/*
	license GPL 3.0
	copyright 2017 boddenberg.it
*/

import groovy.swing.SwingBuilder
import javax.swing.JOptionPane
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

// UI components
// labels
def adbDaemonLabel
def batteryLabel
def brightnessLabel
def choosenDeviceLabel
def testingFeaturesLabel
// buttons
def adbWifiEnablerButton
def brightnessDownButton
def brightnessUpButton
def connectTcpButton
def disconnectTcpButton
def initButton
def installApkButton
def resetBatteryAndChargingModeButton
def setBatteryButton
def setBrightnessButton
def takeScreenshotButton
def toggleAirplaneModeButton
def toggleChargingButton
// slider
def batterySlider
// menu
def chooseDeviceMenu
// frame sizes (normal-advanced mode)
int am_width
int nm_width
int am_height
int nm_height

boolean init = true
boolean uiState = false
boolean adbOverTcpState = false
boolean recordState = false
int batteryValue
String serial = ""
List devices = []
Process recordProcess

String version = "0.1"
log "Initialising asrec..."

swing = new SwingBuilder()
frame = swing.frame(title:'Android Screen RECorder') {

	// functions within UI (mostly to set UI according to current state)
	// helper closures
		def enableUi = {
		toggleChargingButton.setEnabled(uiState)
		installApkButton.setEnabled(uiState)
		takeScreenshotButton.setEnabled(uiState)
		recordVideoButton.setEnabled(uiState)
		batterySlider.setEnabled(uiState)
		brightnessDownButton.setEnabled(uiState)
		brightnessUpButton.setEnabled(uiState)

		if(!uiState) connectTcpButton.setEnabled(uiState)

		def deviceLabel = choosenDeviceLabel.text

		if(!deviceLabel.contains(":5555") && !deviceLabel.contains("Not connected")) {
			toggleAirplaneModeButton.setEnabled(true)
			if(adbOverTcpState){
				connectTcpButton.setEnabled(true)
				disconnectTcpButton.setEnabled(false)
			} else {
				adbWifiEnablerButton.setEnabled(true)
			}
		} else {
			disconnectTcpButton.setEnabled(true)
			connectTcpButton.setEnabled(false)
			adbWifiEnablerButton.setEnabled(false)
			toggleAirplaneModeButton.setEnabled(false)
		}

		if(choosenDeviceLabel.text.contains("onnected")) {
			serial = ""
			toggleAirplaneModeButton.setEnabled(false)
			disconnectTcpButton.setEnabled(false)
			adbWifiEnablerButton.setEnabled(false)
		}
	}

	String.metaClass.browse = { ->
		java.awt.Desktop.desktop.browse(new URI(delegate))
	}

	// updateDevices
	def updateDevices = {
		def connectedDevices = []
		// obtaining connected devices
		"adb devices".execute().text.eachLine {
		if (it && (it.contains("unknown") || \
			(it.contains("device") && !it.contains("devices")))) {
				connectedDevices.add(it.substring(0, it.indexOf("\t")))
			}
		}
		// Adding new devices
		connectedDevices.each {
			if(devices.indexOf(it) == -1) {
				// log that one device has been added
				log "Adding $it"
				devices.add(it)
			}
		}
		// Remove disconnect devices
		List disconnectedDevices = []
		devices.each {
			if (connectedDevices.indexOf(it) == -1) {
				log "Removing $it"
				disconnectedDevices.add(it)
			}
		}
		disconnectedDevices.each() {
			// will be overriden in case of one device being present
			if(serial == it) {
				choosenDeviceLabel.text = "Device: Choose Device"
				uiState = false
				enableUi()
			}
			devices.removeAll(it)
		}
		// recall if no device left
		if(devices.size() == 0) {
			log "No device connected"
			choosenDeviceLabel.text = "Device: Not connected"
			serial = ""
		}
	}

	def oneDeviceFound = {
		serial = devices[0]
		choosenDeviceLabel.text = "Device: $serial"
		chooseDeviceMenu.setEnabled(false)
		uiState = true
		enableUi()
	}

	def moreThanOneFound = {
		if(init) {
			choosenDeviceLabel.text = "Device: Choose Device"
			init = false
		}

		// Updating "Choose Device" menu
		chooseDeviceMenu.removeAll()
		devices.each() {

			def item = menuItem(it, actionPerformed: { fire ->
				serial = it
				choosenDeviceLabel.text = "Device: $serial"
				uiState = true
				enableUi()
			})
			chooseDeviceMenu.add(item)
		}
	chooseDeviceMenu.setEnabled(true)
}

	// actual UI compoments
	menuBar {
		menu('Asrec') {

			menuItem('Help', actionPerformed: { event ->
				"https://github.com/boddenberg-it/asrec/blob/master/README.md".browse()
			})

			menuItem('Info', actionPerformed: { event ->
				inform("""Android Screen RECorder

					version: $version
					author: André Boddenberg

					license: GPL 3.0
					https://www.gnu.org/licenses/gpl-3.0.en.html

					copyright 2017 boddenberg.it""")
			})

		}
		menu('Mode') {
			menuItem('Normal', actionPerformed: { event ->
				frame.resize(nm_width, nm_height)
				testingFeaturesLabel.text = "Copyright Boddenberg.it 2017"
			})
			menuItem('Advanced', actionPerformed: { event ->
				frame.resize(am_width, am_height)
				testingFeaturesLabel.text = "~ Testing Features ~"
			})
		}

		chooseDeviceMenu = menu('Choose Device') {}
		// disabled, only enabled if multiple devices are connected.
		chooseDeviceMenu.setEnabled(false)
	}

	// compoments appear in UI as in script
	panel {
		vbox {

			// init/refresh
			label ' '
			hbox {
				initButton = button('Initialise', actionPerformed: { event ->

					// check wheter adb is installed and launching daemon (if not launched already)
					if(!"adb devices".execute().text.contains("List of devices attached")){
						adbDaemonLabel.text = "ADB daemon: Not initialised"
						alert("adb not found. Is it installed?\nCheck Asrec -> Help for details!")
						return
					}
					if(adbDaemonLabel.text.equals("ADB daemon: Initialising...")) {
						log "adb daemon initialised"
						adbDaemonLabel.text = "ADB daemon: Initialised"
						initButton.text = "Refresh"
					}

					updateDevices()
					if (devices.size() == 1) { oneDeviceFound() }
					if (devices.size() > 1) { moreThanOneFound() }
					log "Using device: $serial"
					// separating each "Refresh" lick with a newline
					// to easily spot "Using device: xyz"
					println ""
				})
			}

			label ' '
			hbox { adbDaemonLabel = label "ADB daemon: Initialising..." }

			label ' '
			hbox { choosenDeviceLabel = label 'Device: Not connected' }

			// install APK
			label ' '
			hbox {
				installApkButton = button('Install APK', actionPerformed: { event ->
					installApkButton.text = 'Installing...'
					installApkButton.setEnabled(false)
					adbInstallApk(serial)
					installApkButton.text = 'Install APK'
					installApkButton.setEnabled(true)
				})
			}

			label ' ' // spacer
			hbox {
				takeScreenshotButton = button('Take Screenshot', actionPerformed: { event ->
					takeScreenshotButton.text = "Taking Screenshot..."
					takeScreenshotButton.setEnabled(false)
					adbTakeScreenshot(serial)
					takeScreenshotButton.setEnabled(true)
					takeScreenshotButton.text = "Take Screenshot"
				})
			}

			hbox {
				recordVideoButton = button('Record Video', actionPerformed: { event ->
					//def recordSanity = "adb -s ${serial} wait-for-device shell screenrecord --help".execute().text
					//if(recordSanity.contains("screenrecord: not found")) {
					if(!adbCheckRecordCapability(serial)) {
						alert("adb shell screenrecord not supported by device")
						recordVideoButton.setEnabled(false)
						return
					}
					if(!recordState) {
						recordState = true

						recordProcess = adbStartRecording(serial)
						recordVideoButton.text = "Stop Recording"
					} else {
						recordProcess.destroy()
						recordVideoButton.text = "Pulling *.mp4..."
						recordVideoButton.setEnabled(false)

						adbStopRecording(serial)
						recordVideoButton.text = "Record Video"
						recordVideoButton.setEnabled(true)
						recordState = false
					}
				})
			}

			label ' '
			label ' '
			hbox { testingFeaturesLabel = label 'Copyright Boddenberg.it 2017' }
			label ' '

			// brightness
			hbox {
				brightnessDownButton = button('Brightness Down', actionPerformed: { event ->
					adbBrightnessDown(serial)
				})
				label ' ' // spacer
				brightnessUpButton = button('Brightness Up', actionPerformed: { event ->
					adbBrightnessUp(serial)
				})
			}

			// battery slider
			label ' '
			hbox {
				label ' '
				batteryLabel = label "Battery: n/a %"

				batterySlider = slider()
				batterySlider.addChangeListener(new ChangeListener() {
      		public void stateChanged(ChangeEvent event) {
        		batteryValue = batterySlider.getValue()
						batteryLabel.text = "Battery: ${batteryValue} %"
						setBatteryButton.setEnabled(true)
					}
				})

				setBatteryButton = button('set', actionPerformed: { event ->
			 		log "Setting battery level to ${batteryValue}"
			 		adbSetBatteryLevel(serial, batterySlider.getValue())
			 		setBatteryButton.setEnabled(false)
					resetBatteryAndChargingModeButton.setEnabled(true)
				})
				setBatteryButton.setEnabled(false)
			}

			// reset battery and charging mode
			label ' '
			hbox {
				resetBatteryAndChargingModeButton = button('Reset Battery', actionPerformed: { event ->
					adbResetBatteryAndChargingMode(serial);
					batterySlider.value = 50
					batteryLabel.text = "Battery: n/a %"
					resetBatteryAndChargingModeButton.setEnabled(false)
					setBatteryButton.setEnabled(false)
				})
				resetBatteryAndChargingModeButton.setEnabled(false)

				label ' '
				resetBatteryAndChargingModeButton = button('Reset Charging', actionPerformed: { event ->
					adbResetBatteryAndChargingMode(serial)
					resetBatteryAndChargingModeButton.setEnabled(false)
				})
				resetBatteryAndChargingModeButton.setEnabled(false)
			}

			// toggle charging mode
			hbox {
				toggleChargingButton = button('Toggle Charging Mode', actionPerformed: { event ->
					adbToggleChargingMode(serial)
					resetBatteryAndChargingModeButton.setEnabled(true)
				})
			}

			// toggle airplane mode
			hbox {
				toggleAirplaneModeButton = button('Toggle Airplane Mode', actionPerformed: { event ->
					adbToggleAirplaneMode(serial);
				})
			}

			hbox {
				connectTcpButton = button('Connect ADB WiFi', actionPerformed: { event ->
					serial = adbConnectTcp(serial)
					choosenDeviceLabel.text = "Device: $serial"
					adbOverTcpState = true
					uiState = true
					enableUi()
				})
				connectTcpButton.setEnabled(false)

				label ' '
				disconnectTcpButton = button('Disconnect ADB WiFi', actionPerformed: { event ->
					if(resetBatteryAndChargingModeButton.isEnabled()) {
						alert("You need to reset battery and charging modde, before disconnecting!")
					} else {
						adbDisconnectTcp(serial)
						adbOverTcpState = false
						initButton.doClick()
						enableUi()
					}
		 		})
				disconnectTcpButton.setEnabled(false)
			}

			// "Enable ADB over WiFi"
			hbox{
				adbWifiEnablerButton =  button('Enable ADB over WiFi (rooted?)', actionPerformed: { event ->
					if(!connectTcpButton.isEnabled()) {
						log "Enabling 'ADB over WiFi' mode"
						connectTcpButton.setEnabled(true)
						adbWifiEnablerButton.setEnabled(false)
					} else {
						alert("You need first to click \"ADB WiFi OFF\"")
					}
				})
			}
			label ' '
			hbox { label'Copyright Boddenberg.it 2017' }
  	}
		enableUi()
	}
	initButton.doClick()
}

// get host specific size
frame.pack()
def s = frame.getContentPane().getSize()
// widths testing and normal mode
am_width = s.width * 1.05
nm_width = s.width * 0.8
// heights testing and normal mode
am_height = s.height * 1.1
nm_height = s.height * 0.6
// init with normal mode
frame.resize(nm_width, nm_height)
frame.visible = true

// adb functions
String adbConnectTcp(String serial) {
	log "ADB-WiFi connecting attempt on $serial"
	try {
		def ip = "adb -s ${serial} wait-for-device shell ip -f inet addr show wlan0".execute().text
		ip = ip.substring(ip.indexOf("inet")+4, ip.indexOf("/")).trim()
		log "ADB-WiFi try connecting to ${ip}:5555..."
		println "adb -s ${serial} wait-for-device shell setprop service.adb.tcp.port 5555".execute().text
		sleep(500)
		println "adb -s ${serial} wait-for-device shell 'stop adbd; start adbd'".execute().text
		sleep(500)
		println "adb connect ${ip}:5555".execute().text
		return "${ip}:5555"

	} catch(java.lang.StringIndexOutOfBoundsException e) {
		log "ADB-WiFi device $serial has no IP address"
		alert("""Device does not have an IP address $serial

			Is 'airplane mode' enabled?""")
	} catch(Exception e) {
		println "Stacktrace: ${e.toString()}"
	}
	return serial
}

void adbDisconnectTcp(String serial) {
	log "ADB-WiFi: Disconnecting $serial"
	"adb -s ${serial} wait-for-device shell setprop service.adb.tcp.port -1".execute()
	"adb disconnect ${serial}".execute()
}

void adbTakeScreenshot(String serial) {
	log "Taking screenshot of $serial"
	"adb -s ${serial} wait-for-device shell screencap /mnt/sdcard/asrec.png".execute()
	def filePath = fileDialog()
	log "Pulling screenshot of $serial to $filePath"
	"adb -s ${serial} wait-for-device pull /mnt/sdcard/asrec.png ${filePath}.png".execute().text
	log "Pulling of screenshot on $serial done"
}

void adbResetBatteryAndChargingMode(String serial){
	log "Reset battery and charging mode of $serial"
	"adb -s ${serial} wait-for-device shell dumpsys battery reset".execute()
}

void adbInstallApk(String serial) {
	apkPath = fileDialog()
	log "Installing APK on $serial $apkPath"
	// checking whether not null and ends with apk
	if(apkPath && !apkPath.endsWith("apk")) {
		log "ERROR: wrong file extension (!*.apk)"
		alert("File does not hold \"*.apk\" file extension")
		return
	}

	def proc =  "adb -s ${serial} install -r -d ${apkPath}".execute()
	// TODO: think about preferences menu to set "install timeout in seconds"
	proc.waitForOrKill(30000)
		if(proc.text.contains("uccess")) {
			log "APK installation successful on $serial $apkPath"
			inform("APK installation successful!")
		} else {
			log "APK installation failed on $serial $apkPath"
			alert("APK installation failed!\n\n${apkPath}\n\n${e.toString()}")
		}
}

Process adbStartRecording(String serial) {
	//log "Clear logcat on $serial"
	//"adb -s ${serial} wait-for-device logcat -c"
	log "Start recording on $serial"
	"adb -s ${serial} wait-for-device shell screenrecord --bugreport --size \"1280x720\" /sdcard/asrec.mp4".execute()
}

void adbStopRecording(String serial) {
	log "Stop recording on $serial"
	//log "Fetching logcat of $serial"
	//def logcat_content = "adb -s ${serial} wait-for-device logcat -d".execute().text

	def filePath = fileDialog()
	//def logcat = new File("${filePath}_logcat.txt")
	//logcat << logcat_content
	//log "Logcat of $serial fetched to ${filePath}_logcat.txt"

	log "Pulling video on $serial to $filePath"
	"adb -s ${serial} wait-for-device pull /sdcard/asrec.mp4 ${filePath}.mp4".execute()
	log "Pulling video sucessfully on $serial to $filePath"
}

void adbToggleChargingMode(String serial) {
	if("adb -s ${serial} wait-for-device shell dumpsys battery".execute().text.contains("USB powered: true")) {
		log "Set charging mode to FALSE"
		"adb -s ${serial} wait-for-device shell dumpsys battery set usb 0".execute()
	} else {
		log "Set charging mode to TRUE"
		"adb -s ${serial} wait-for-device shell dumpsys battery set usb 1".execute()
	}
}

boolean adbCheckRecordCapability(String serial) {
	def recordSanity = "adb -s ${serial} wait-for-device shell screenrecord --help".execute().text
	if(!recordSanity.contains("screenrecord: not found")) return true
	log "ERROR: device $serial does NOT support \"adb shell screenrecord\""
	return false
}

void adbSetBatteryLevel(String serial, int level) {
	log "Setting battery level of $serial to $level"
	"adb -s ${serial} wait-for-device shell dumpsys battery set level $level".execute()
}

// check out the airplaneModeOn and Off script from XYZ
void adbToggleAirplaneMode(serial) {
	def oldState = "adb -s ${serial} wait-for-device shell settings get global airplane_mode_on".execute().text.trim()
	log "Toggling airplane mode on $serial - $oldState"

	"adb -s ${serial} wait-for-device shell am start -a android.settings.AIRPLANE_MODE_SETTINGS".execute()
	sleep(3000)
	// if already toggled previously one ENTER is sufficient.
	"adb -s ${serial} wait-for-device shell input keyevent KEYCODE_ENTER".execute()
	sleep(200)
	def newState = "adb -s ${serial} wait-for-device shell settings get global airplane_mode_on".execute().text
	if(newState == oldState) {
		// first time toggling, Airplane mode switch needs to be focused
		"adb -s ${serial} shell input keyevent KEYCODE_ENTER".execute()
	}
	"adb -s ${serial} wait-for-device shell input keyevent KEYCODE_HOME".execute()
	// verification
	newState = "adb -s ${serial} wait-for-device shell settings get global airplane_mode_on".execute().text
	if(newState == oldState) {
		log "Toggling airplane mode failed"
		alert "\"Toggle Airplane Mode\" didn't succeed!\nnewState: ${newState}oldState: $oldState"
	} else {
		log "Toggling airplane mode succesfully"
	}
}

void adbBrightnessDown(String serial) {
	log "Lowering brightness on $serial"
	"adb -s ${serial} wait-for-device shell input keyevent KEYCODE_BRIGHTNESS_DOWN".execute()
}

void adbBrightnessUp(String serial) {
	log "Increasing brightness on $serial"
	"adb -s ${serial} wait-for-device shell input keyevent KEYCODE_BRIGHTNESS_UP".execute()
}

// fileDialog used for Install APK, Take Screenshot and Record Video
String fileDialog(String title) {
	def dialog = swing.fileChooser(dialogTitle: "${title}")
	// approve returns 0, cancel returns 1
	if (dialog.showOpenDialog() == 0) {
		return dialog.selectedFile
	}
	null
}

// pop ups
void alert(String error) {
	JOptionPane.showMessageDialog(null, error, "Asrec", JOptionPane.ERROR_MESSAGE);
}

void inform(String information) {
	JOptionPane.showMessageDialog(null, information, "Asrec", JOptionPane.INFORMATION_MESSAGE);
}

// log helper
void log(String msg) {
	println "[${new Date()}] $msg"
}
