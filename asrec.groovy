#!/usr/bin/groovy

import groovy.swing.SwingBuilder
import groovy.transform.Field

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.JOptionPane
import javax.swing.JCheckBox
import javax.swing.SwingUtilities

// UI
def adbDaemonLabel
def batteryLabel
def brightnessLabel
def choosenDeviceLabel

def batteryValue
def brightnessValue

def setBatteryButton
def setBrightnessButton
def brightnessDownButton
def brightnessUpButton
def initButton
def installApkButton
def resetBatteryButton
def resetChargingButton
def connectTcpButton
def disconnectTcpButton
def adbWifiEnablerButton
def toggleAirplaneModeButton
def toggleChargingButton
def takeScreenshotButton
def recordVideoButton

def batterySlider
def brightnessSlider

def chooseDeviceMenu

// non-UI
def batteryLevel
def devices = []
def init = true
def uiState = false
def serial = ""
def recordState = false
def recordProcess
def adbOverTcpState = false

swing = new SwingBuilder()

println "init: $init"
frame = swing.frame(title:'Android Screen RECorder') {
	menuBar {
		menu('Asrec') {
			menuItem('Preferences', actionPerformed: { event ->
				alert("HALLO WELT!")
			})
			menuItem 'Load Config'
			menuItem 'Help'
			menuItem 'Info'
		}
		chooseDeviceMenu = menu('Choose Device') {}
		// disabled, only enabled if multiple devices.
		chooseDeviceMenu.setEnabled(false)
	}

	panel {

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
				println "FOOOOOOOOOOOOOoo"
				serial = ""
				toggleAirplaneModeButton.setEnabled(false)
				disconnectTcpButton.setEnabled(false)
				adbWifiEnablerButton.setEnabled(false)
			}

		}

		vbox {
			// init/refresh button
			label ' '
			hbox {
				initButton = button('Initialise', actionPerformed: { event ->

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
									println "Adding $it to devices"
									devices.add(it)
								}
							}
							// Remove disconnect devices
							List disconnectedDevices = []
							devices.each {
								if (connectedDevices.indexOf(it) == -1) {
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
							if(devices.size() == 0) {
								choosenDeviceLabel.text = "Device: Not connected"
								serial = ""
							}
						}

						def oneDeviceFound = {
							serial = devices[0]
							println "serial: $serial"
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

					initButton.setEnabled(false)
					adbDaemonLabel.text = "ADB daemon: Initialising..."

					// check wheter adb is installed and launching daemon (if not launched already)
					if(!"adb devices".execute().text.contains("List of devices attached")){
						alert("adb not found. Is it installed?\nCheck Asrec -> Help for details!")
						return
					}

					adbDaemonLabel.text = "ADB daemon: Initialised"
					initButton.text = "Refresh"

					updateDevices()
					if (devices.size() == 1) { oneDeviceFound() }
					if (devices.size() > 1) { moreThanOneFound() }

					initButton.setEnabled(true)
					if(init) {init = false}
				})
			}

			label ' ' // spacer
			hbox {
				adbDaemonLabel = label "ADB daemon: not initialised"
			}

		label ' ' // spacer
		hbox {
			choosenDeviceLabel = label 'Device: Not connected'
		}

		label ' ' // spacer
		hbox {
			brightnessDownButton = button('Brightness Down', actionPerformed: { event ->
				"adb -s ${serial} wait-for-device shell input keyevent KEYCODE_BRIGHTNESS_DOWN".execute()
			})
			label ' ' // spacer
			brightnessUpButton = button('Brightness Up', actionPerformed: { event ->
				"adb -s ${serial} wait-for-device shell input keyevent KEYCODE_BRIGHTNESS_UP".execute()
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
			 println "Setting battery level to ${batteryValue}"
			 println serial
			 adbSetBatteryLevel(serial, batterySlider.getValue())
			 setBatteryButton.setEnabled(false)
			 resetBatteryButton.setEnabled(true)
			})
			setBatteryButton.setEnabled(false)
			label ' '
		}

		label ' ' // spacer

		hbox {
			resetBatteryButton = button('Reset Battery', actionPerformed: { event ->
				adbResetBattery(serial);
				batterySlider.value = 50
				batteryLabel.text = "Battery: n/a %"
				resetBatteryButton.setEnabled(false)
				setBatteryButton.setEnabled(false)
			})
			resetBatteryButton.setEnabled(false)

			label ' ' // spacer

			resetChargingButton = button('Reset Charging', actionPerformed: { event ->
				"adb -s ${serial} wait-for-device shell dumpsys battery reset".execute()
				resetChargingButton.setEnabled(false)
			})
			resetChargingButton.setEnabled(false)
		}

		hbox {
			toggleChargingButton = button('Toggle Charging Mode', actionPerformed: { event ->
				if("adb -s ${serial} wait-for-device shell dumpsys battery".execute().text.contains("USB powered: true")) {
					"adb -s ${serial} wait-for-device shell dumpsys battery set usb 0".execute()
				} else {
					println "discharging"
					"adb -s ${serial} wait-for-device shell dumpsys battery set usb 1".execute()
				}
				resetChargingButton.setEnabled(true)
			})
		}

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

		label ' ' // spacer
		label ' ' // spacer

		hbox {
			installApkButton = button('Install APK', actionPerformed: { event ->
				// some UI feedback within button
				installApkButton.text = 'Installing...'
				adbInstallApk(serial)
				installApkButton.text = 'Install APK'
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
				if(!recordState) {
					recordProcess = "adb -s ${serial} wait-for-device shell screenrecord /mnt/sdcard/asrec.mp4".execute()
					recordVideoButton.text = "Stop Recording"
					recordState = true
				} else {
					recordProcess.destroy()
					recordVideoButton.text = "Pulling *.mp4..."
					recordVideoButton.setEnabled(false)
					"adb -s ${serial} wait-for-device pull /mnt/sdcard/asrec.mp4 ${fileDialog()}".execute()
					recordVideoButton.text = "Record Video"
					recordVideoButton.setEnabled(true)
					recordState = false
				}
			})
		}

		label ' ' // spacer
		label ' ' // spacer
		hbox { label'Copyright 2017 Boddenberg.it' }
  	}
		// init
		uiState = false
		enableUi()
	}
	initButton.doClick()
}

frame.pack()
frame.size()
frame.visible = true

// functions
String adbConnectTcp(String serial) {
	try {
		def ip = "adb -s ${serial} wait-for-device shell ip -f inet addr show wlan0".execute().text
		ip = ip.substring(ip.indexOf("inet")+4, ip.indexOf("/")).trim()
		println "Try connecting to ${ip}:5555..."
		"adb -s ${serial} wait-for-device shell setprop service.adb.tcp.port 5555".execute()
		sleep(500)
		"adb -s ${serial} wait-for-device shell 'stop adbd; start adbd'".execute()
		sleep(500)
		println "adb connect ${ip}:5555".execute().text
		return "${ip}:5555"
	} catch(Exception e) {
		return serial
	}
}

void adbDisconnectTcp(String serial) {
	"adb -s ${serial} wait-for-device shell setprop service.adb.tcp.port -1".execute()
	"adb disconnect ${serial}".execute()
}

void adbTakeScreenshot(String serial) {
	println "adb serial: $serial"
	"adb -s ${serial} wait-for-device shell screencap /mnt/sdcard/asrec.png".execute()
	"adb -s ${serial} wait-for-device pull /mnt/sdcard/asrec.png ${fileDialog()}".execute().text
}

void adbResetBattery(String serial){
	println "resetBattery call: $serial"
	"adb -s ${serial} wait-for-device shell dumpsys battery reset".execute()
}

void adbSetBatteryLevel(String serial, int level) {
	println "adbSetBatteryLevel call: $serial"
	"adb -s ${serial} wait-for-device shell dumpsys battery set level $level".execute()
}
// void because try-catch within function
void adbInstallApk(String serial) {

	apkPath = fileDialog()
	// checking whether not null and ends with apk
	if(apkPath && !apkPath.endsWith("apk")) {
		alert("File does not hold \"*.apk\" file extension")
		return
	}

	if (apkPath) {
		println "here it comes: ${apkPath}"
		def proc =  "adb -s ${serial} install -r -d ${apkPath}".execute()
		proc.waitForOrKill(30000)

		try {
			String stdout = proc.text
			println stdout
			// contains !is equal, so we check for "uccess" to "meet" quals condition
			if(stdout.contains("uccess")) {
				println("APK installation successful!")
				inform("APK installation successful!")
			}
		} catch(Exception e) {
			alert("APK installation failed!\n\n${apkPath}\n\n${e.toString()}")
		}
	}
}

// check out the airplaneModeOn and Off script from XYZ
void adbToggleAirplaneMode(serial) {
	def oldState = "adb -s ${serial} wait-for-device shell settings get global airplane_mode_on".execute().text
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
		alert "\"Toggle Airplane Mode\" didn't succeed!\nnewState: ${newState}oldState: $oldState"
	}
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
