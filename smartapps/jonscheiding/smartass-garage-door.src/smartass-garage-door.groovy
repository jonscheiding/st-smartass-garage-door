/**
 *  Smartass Garage Door
 *
 *  Copyright 2016 Jon Scheiding
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Smartass Garage Door",
    namespace: "jonscheiding",
    author: "Jon Scheiding",
    description: "Garage door app with the smarts to get by in this world.",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Transportation/transportation13-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Transportation/transportation13-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Transportation/transportation13-icn@2x.png")


preferences {
	section("Garage Door") {
		input "doorSwitch", "capability.momentary", title: "Opener", required: true
		input "doorContactSensor", "capability.contactSensor", title: "Open/Close Sensor", required: true
        input "doorAccelerationSensor", "capability.accelerationSensor",  title: "Movement Sensor", required: false
	}
    section("Car / Driver") {
    	input "driver", "capability.presenceSensor", title: "Presence Sensor", required: true
	}
    section("Interior Door") {
    	input "interiorDoor", "capability.contactSensor", title: "Open/Close Sensor", required: false
    }
    section("Notifications") {
    	input "shouldSendPush", "bool", title: "Send Push Notifications"
    }
    section("Behavior") {
        input "openOnArrival", "bool", title: "Open On Arrival"
    	input "closeOnDeparture", "bool", title: "Close On Departure"
        input "closeOnEntry", "bool", title: "Close On Interior Door Entry"
    }
}

def onDriverArrived(evt) {
	state.lastArrival = now()
    
    if(openOnArrival)
		pushDoorSwitch("open", "Opening ${doorSwitch.displayName} due to arrival of ${driver.displayName}.")
}

def onDriverDeparted(evt) {
    if(closeOnDeparture)
    	pushDoorSwitch("closed", "Closing ${doorSwitch.displayName} due to departure of ${driver.displayName}.")
}

def onInteriorDoorOpened(evt) {
	if(!closeOnEntry) return
    
    def expirationMinutes = 15
    
	if(state.lastArrival < state.lastClosed)
    	return
    if(state.lastArrival < (now() - (expirationMinutes * 60 * 1000)))
    	return
    
    pushDoorSwitch("closed", "Closing ${doorSwitch.displayName} due to entry into ${interiorDoor.displayName}.")
}

def onGarageDoorClosed(evt) {
    state.lastClosed = now()
}

def pushDoorSwitch(desiredState, msg) {
	log.info msg
	if(doorContactSensor.currentContact == desiredState) {
    	log.info "Door will not be triggered because it is already ${desiredState}."
        return
    }
    if(doorAccelerationSensor && doorAccelerationSensor.currentAcceleration == "active") {
    	log.info "Door will not be triggered because it is currently in motion."
        return
    }
    
    notifyIfNecessary msg
	doorSwitch.push()
}

def notifyIfNecessary(msg) {
    if(shouldSendPush)
    	sendPush msg
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
    subscribe(driver, "presence.present", onDriverArrived)
    subscribe(driver, "presence.not present", onDriverDeparted)

    subscribe(doorContactSensor, "contact.closed", onGarageDoorClosed)

	if(interiorDoor)
    	subscribe(interiorDoor, "contact.open", onInteriorDoorOpened)
}