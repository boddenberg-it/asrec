#!/usr/bin/groovy
// License: GPL
// Author: andre@boddenberg.it
// Copyright: boddenberg.it
// Date: 18.07.2107

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
// buttons
def adbWifiEnablerButton
def brightnessDownButton
def brightnessUpButton
def connectTcpButton
def disconnectTcpButton
def initButton
def installApkButton
def recordVideoButton
def resetBatteryButton
def resetChargingButton
def setBatteryButton
def setBrightnessButton
def takeScreenshotButton
def toggleAirplaneModeButton
def toggleChargingButton
// slider
def batterySlider
// menu
def chooseDeviceMenu

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
				log "Adding $it to devices"
				devices.add(it)
			}
		}
		// Remove disconnect devices
		List disconnectedDevices = []
		devices.each {
			if (connectedDevices.indexOf(it) == -1) {
				log "Removing $it from devices"
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
			println "Removing disconnected Device: $it"
			devices.removeAll(it)
		}
		// recall if no device left
		if(devices.size() == 0) {
			log "No device connected"
			choosenDeviceLabel.text = "Device: Not connected"
			serial = ""
		}

		log "Using device: $serial"
		println ""
	}

	def oneDeviceFound = {
		serial = devices[0]
		choosenDeviceLabel.text = "Device: $serial"
		chooseDeviceMenu.setEnabled(false)
		uiState = true
		enableUi()
	}

	def moreThanOneFound = {
		if(init) {choosenDeviceLabel.text = "Device: Choose Device"}

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
			menuItem('Preferences', actionPerformed: { event ->
				alert("HALLO WELT!")
			})
			menuItem('Help', actionPerformed: { event ->
				alert("HALLO WELT!")
			})
			menuItem('Info', actionPerformed: { event ->
				alert("Asrec: $version")
			})
		}
		chooseDeviceMenu = menu('Choose Device') {}
		// disabled, only enabled if multiple devices.
		chooseDeviceMenu.setEnabled(false)
	}

	// compoments appear in UI as in script
	panel {
		vbox {

			// init/refresh
			label ' '
			hbox {
				initButton = button('Initialise', actionPerformed: { event ->
					adbDaemonLabel.text = "ADB daemon: Initialising..."
					// check wheter adb is installed and launching daemon (if not launched already)
					if(!"adb devices".execute().text.contains("List of devices attached")){
						alert("adb not found. Is it installed?\nCheck Asrec -> Help for details!")
						return
					}
					adbDaemonLabel.text = "ADB daemon: Initialised"
					log "adb daemon initialised"
					initButton.text = "Refresh"

					updateDevices()
					if (devices.size() == 1) { oneDeviceFound() }
					if (devices.size() > 1) { moreThanOneFound() }
				})
			}

			label ' '
			hbox { adbDaemonLabel = label "ADB daemon: not initialised" }

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
					if(!adbCheckRecordCapability) {
						alert("adb shell screenrecord not supported by device")
						recordVideoButton.setEnabled(false)
						return
					}
					if(!recordState) {
						recordState = true

						adbStartRecording(serial)
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
			hbox{ label '~ Testing Features ~' }
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
			 		resetBatteryButton.setEnabled(true)
				})
				setBatteryButton.setEnabled(false)
			}

			// reset battery and charging mode
			label ' '
			hbox {
				resetBatteryButton = button('Reset Battery', actionPerformed: { event ->
					adbResetBattery(serial);
					batterySlider.value = 50
					batteryLabel.text = "Battery: n/a %"
					resetBatteryButton.setEnabled(false)
					setBatteryButton.setEnabled(false)
				})
				resetBatteryButton.setEnabled(false)

				label ' '
				resetChargingButton = button('Reset Charging', actionPerformed: { event ->
					adbResetChargingMode(serial)
					resetChargingButton.setEnabled(false)
				})
				resetChargingButton.setEnabled(false)
			}

			// toggle charging mode
			hbox {
				toggleChargingButton = button('Toggle Charging Mode', actionPerformed: { event ->
					adbToggleChargingMode(serial)
					resetChargingButton.setEnabled(true)
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
					initButton.doClick()
					enableUi()
				})
				connectTcpButton.setEnabled(false)

				label ' '
				disconnectTcpButton = button('Disconnect ADB WiFi', actionPerformed: { event ->
					if(resetChargingButton.isEnabled() || resetBatteryButton.isEnabled()) {
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
				adbWifiEnablerButton =  button('Enable ADB over WiFi', actionPerformed: { event ->
					if(!connectTcpButton.isEnabled()) {
						connectTcpButton.setEnabled(true)
						adbWifiEnablerButton.setEnabled(false)
					} else {
						alert("You need first to click \"ADB WiFi OFF\"")
					}
				})
			}
			label ' '
			hbox { label'Copyright 2017 Boddenberg.it' }
  	}
		enableUi()
	}
	initButton.doClick()
}

frame.pack()
frame.size()
frame.visible = true

// adb functions
String adbConnectTcp(String serial) {
	try {
		def ip = "adb -s ${serial} wait-for-device shell ip -f inet addr show wlan0".execute().text
		ip = ip.substring(ip.indexOf("inet")+4, ip.indexOf("/")).trim()
		log "ADB-WiFi: Try connecting to ${ip}:5555..."
		println "adb -s ${serial} wait-for-device shell setprop service.adb.tcp.port 5555".execute().text
		sleep(500)
		println "adb -s ${serial} wait-for-device shell 'stop adbd; start adbd'".execute().text
		sleep(500)
		println "adb connect ${ip}:5555".execute().text
		return "${ip}:5555"
	} catch(Exception e) {
		return serial
	}
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
	"adb -s ${serial} wait-for-device pull /mnt/sdcard/asrec.png ${filePath}".execute().text
	log "Pulling of screenshot on $serial done"
}

void adbResetBattery(String serial){
	log "Reset battery: $serial"
	"adb -s ${serial} wait-for-device shell dumpsys battery reset".execute()
}

void adbInstallApk(String serial) {
	apkPath = fileDialog()
	log "Installing APK on $serial: $apkPath"
	// checking whether not null and ends with apk
	if(apkPath && !apkPath.endsWith("apk")) {
		log "ERROR: wrong file extension (!*.apk)"
		alert("File does not hold \"*.apk\" file extension")
		return
	}

	def proc =  "adb -s ${serial} install -r -d ${apkPath}".execute()
	proc.waitForOrKill(30000)

	//try {
		if(proc.text.contains("uccess")) {
			log "APK installation successful on $serial: $apkPath"
			inform("APK installation successful!")
		}
		else { // instead of try-catch
	//} catch(Exception e) {
		log "APK installation failed on $serial: $apkPath"
		alert("APK installation failed!\n\n${apkPath}\n\n${e.toString()}")
	}
}

void adbStartRecording(String seria) {
	log "Start recording on $serial"
	recordProcess = "adb -s ${serial} wait-for-device shell screenrecord --bugreport --size \"1280x720\" /sdcard/asrec.mp4".execute()
}

void adbStopRecording(String serial) {
	log "Stop recording on $serial"
	def videoPath = fileDialog()
	log "Pulling video on $serial to $videoPath"
	"adb -s ${serial} wait-for-device pull /sdcard/asrec.mp4 ${videoPath}".execute()
	log "Pulling video sucessfully on $serial to $videoPath"
}

void adbResetChargingMode(String serial) {
	log "Reset charging mode of $serial"
	"adb -s ${serial} wait-for-device shell dumpsys battery reset".execute()
}

void adbToggleChargingMode(String serial) {
	if("adb -s ${serial} wait-for-device shell dumpsys battery".execute().text.contains("USB powered: true")) {
		log "Set charging mode to TRUE"
		"adb -s ${serial} wait-for-device shell dumpsys battery set usb 0".execute()
	} else {
		log "Set charging mode to FALSE"
		"adb -s ${serial} wait-for-device shell dumpsys battery set usb 1".execute()
	}
}

boolean adbCheckRecordCapability(String serial) {
	def recordSanity = "adb -s ${serial} wait-for-device shell screenrecord --help".execute().text
	if(recordSanity.contains("screenrecord: not found")) return true
	log "ERROR: device $serial does NOT support \"adb shell screenrecord\""
	return false
}

// check out the airplaneModeOn and Off script from XYZ
void adbToggleAirplaneMode(serial) {
	def oldState = "adb -s ${serial} wait-for-device shell settings get global airplane_mode_on".execute().text
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
		log "Toggling airplane mode succeed"
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
