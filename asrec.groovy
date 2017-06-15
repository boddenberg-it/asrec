#!/usr/bin/groovy

import groovy.swing.SwingBuilder
import groovy.transform.Field

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

// UI
def adbDaemonLabel
def batteryLabel
def brightnessLabel
def choosenDeviceLabel

def batteryValue
def brightnessValue

def setBatteryButton
def brightnessSetButton
def initButton
def installApkButton
def refreshButton
def resetBatteryButton
def toggleAirplaneModeButton
def takeScreenshotButton
def recordVideoButton

def batterySlider
def brightnessSlider

def chooseDeviceMenu

// non-UI
def devices = []
def uiDevices = []
def init = true
def uiState = true
def serial = ""

def uiElements = []
uiElements.add(toggleAirplaneModeButton)
uiElements.add(installApkButton)
uiElements.add(takeScreenshotButton)
uiElements.add(recordVideoButton)
uiElements.add(batterySlider)
uiElements.add(brightnessSlider)

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
			toggleAirplaneModeButton.setEnabled(uiState)
			installApkButton.setEnabled(uiState)
			takeScreenshotButton.setEnabled(uiState)
			recordVideoButton.setEnabled(uiState)
			batterySlider.setEnabled(uiState)
			brightnessSlider.setEnabled(uiState)
		}

		vbox {

			// init/refresh buttons
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
						// necessary to firs obtain disconnected devices to avoid ConcurrentException
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
							println "init: $init"
							if(init) {choosenDeviceLabel.text = "Device: Choose Device"}

							// CLearing "Choose Device" menu and refilling it
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
			choosenDeviceLabel = label 'Device: not connected'
		}

		// brightness slider
		label ' ' //spacer
		hbox {
			brightnessLabel = label 'Brightness: 50 %'

			brightnessSlider = slider()
			brightnessSlider.addChangeListener(new ChangeListener() {
      	public void stateChanged(ChangeEvent event) {
        	brightnessValue = brightnessSlider.getValue()
					brightnessLabel.text = "Brightness: ${brightnessValue} %"
					brightnessSetButton.setEnabled(true)
				}
			})
			brightnessSlider.setEnabled(false)

			brightnessSetButton = button('set', actionPerformed: { event ->
			 println "Setting brightness level to ${brightnessValue}"
			 brightnessSetButton.setEnabled(false)
			})
			brightnessSetButton.setEnabled(false)
		}

		label ' ' // spacer

		// battery slider
		hbox {
			batteryLabel = label 'Battery: 50 %'

			batterySlider = slider()
			batterySlider.addChangeListener(new ChangeListener() {
      	public void stateChanged(ChangeEvent event) {
        	batteryValue = batterySlider.getValue()
					batteryLabel.text = "Battery: ${batteryValue} %"
					setBatteryButton.setEnabled(true)
				}
			})
			batterySlider.setEnabled(false)

			setBatteryButton = button('set', actionPerformed: { event ->
			 println "Setting battery level to ${batteryValue}"
			 setBatteryButton.setEnabled(false)
			 resetBatteryButton.setEnabled(true)
			})
			setBatteryButton.setEnabled(false)
		}

		label ' ' // spacer

		hbox {
			resetBatteryButton = button('Reset Battery', actionPerformed: { event ->
				//adbResetBattery(); // will return the actual battery value, used to reset slider and label:
				batterySlider.value = 50
				batteryLabel.text = "Battery: 50 %"
				resetBatteryButton.setEnabled(false)
				setBatteryButton.setEnabled(false)
			})
			resetBatteryButton.setEnabled(false)
		}

		hbox {
			toggleAirplaneModeButton = button('Toggle Airplane Mode', actionPerformed: { event ->
				adbToggleAirplaneMode();
			})
			toggleAirplaneModeButton.setEnabled(false)
		}

		label ' ' // spacer
		hbox {
			installApkButton = button('Install APK', actionPerformed: { event ->
				installApkButton.text = 'Installing...'
				installApkButton.setEnabled(false)
				adbInstallApk(serial)
				installApkButton.text = 'Install APK'
				installApkButton.setEnabled(true)
			})
			installApkButton.setEnabled(false)
		}

		label ' ' // spacer
		hbox {
			takeScreenshotButton = button('Take Screenshot', actionPerformed: { event ->
				adbTakeScreenshot(serial)
			})
			takeScreenshotButton.setEnabled(false)
		}

		hbox {
			recordVideoButton = button('Record Video', actionPerformed: { event ->
				String pathOfScreenshot = recordVideo()
				String fileDestination = fileDialog("Choose file path to store video")
				if (fileDestination) adbCopyFile(pathOfScreenshot, fileDestination)
			})
			recordVideoButton.setEnabled(false)
		}

		label ' ' // spacer
		label ' ' // spacer
		hbox { label'Copyright 2017 Boddenberg.it' }
  	}
	}
}

frame.pack()
frame.size()
frame.visible = true

// Functions will probably be put in non-ui file:
void adbTakeScreenshot(String serial) {
	String fileName = "392ad98b34f03_17-06-2018-14:30:11.png"
	println "adb serial: $serial"
	"adb -s ${serial} wait-for-device shell screencap /mnt/sdcard/asrec/${fileName}".execute()
	filePath = fileDialog()
	pullErrCode = "adb -s ${serial} wait-for-device pull /mnt/sdcard/asrec/${fileName} ${filePath}".execute()
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
		proc.waitForOrKill(15000)

		try {
			String stdout = proc.text
			println stdout
			// contains !is equal, so we check for "uccess" to "meet" quals condition
			if(stdout.contains("uccess")) {
				println("APK installation successful!")
				inform("APK installation successful!")
			}
		} catch(Exception e) {
			println e.toString()
			alert("APK installation failed!\n\n${apkPath}\n")
		}
	}
}





void updateDevices(List devices){

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

// popups
void alert(String error) {
	JOptionPane.showMessageDialog(null, error, "Asrec", JOptionPane.ERROR_MESSAGE);
}

void inform(String information) {
	JOptionPane.showMessageDialog(null, information, "Asrec", JOptionPane.INFORMATION_MESSAGE);
}
