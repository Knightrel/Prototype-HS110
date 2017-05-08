/*
TP-Link HS110 PROTOTYPE
Increment 1 - Current Power


Copyright 2017 Dave Gutheinz

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this 
file except in compliance with the License. You may obtain a copy of the License at:

		http://www.apache.org/licenses/LICENSE-2.0
        
Unless required by applicable law or agreed to in writing, software distributed under 
the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
ANY KIND, either express or implied. See the License for the specific language governing 
permissions and limitations under the License.

Notes: This is a prototype HS-110 DH.  It requires version 2.1 of "TP-LinkServer.js" running
on a bridge (PC, etc.)
*/
metadata {
	definition (name: "Prototpe HS110", namespace: "test", author: "Dave Gutheinz") {
		capability "Switch"
		capability "refresh"
        capability "powerMeter"
	}
	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:'st.switches.light.on', backgroundColor:"#00a0dc",
				nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff",
				nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#e86d13",
				nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#e86d13",
				nextState:"turningOn"
                attributeState "offline", label:'Bulb Offline', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"##e86d13",
                nextState:"turningOn"
			}
			tileAttribute ("device.power", key: "SECONDARY_CONTROL") {
				attributeState "power", label:'Current Power: ${currentValue}W'
			}
		}
		standardTile("refresh", "capability.refresh", width: 2, height: 2,  decoration: "flat") {
			state ("default", label:"Refresh", action:"refresh.refresh", icon:"st.secondary.refresh")
		}         
		main("switch")
		details(["switch", "refresh"])
    }
}
preferences {
	input("deviceIP", "text", title: "Device IP", required: true, displayDuringSetup: true)
	input("gatewayIP", "text", title: "Gateway IP", required: true, displayDuringSetup: true)
}
def on() {
	log.info "${device.name} ${device.label}: Turning ON"
	sendCmdtoServer('{"system":{"set_relay_state":{"state": 1}}}', "onOffResponse")
}
def off() {
	log.info "${device.name} ${device.label}: Turning OFF"
	sendCmdtoServer('{"system":{"set_relay_state":{"state": 0}}}', "onOffResponse")
}
def refresh(){
	log.info "Polling ${device.name} ${device.label}"
	sendCmdtoServer('{"system":{"get_sysinfo":{}}}', "refreshResponse")
}
def getCurrentPower(){
log.debug "getting current power"
	sendCmdtoServer('{"emeter":{"get_realtime":{}}}', "getRealTimeResponse")
}
private sendCmdtoServer(command, action){
	def headers = [:] 
	headers.put("HOST", "$gatewayIP:8082")   // port 8082 must be same as value in TP-LInkServerLite.js
	headers.put("tplink-iot-ip", deviceIP)
    headers.put("tplink-command", command)
	headers.put("command", "deviceCommand")
	sendHubCommand(new physicalgraph.device.HubAction([
		headers: headers],
		device.deviceNetworkId,
		[callback: action]
	))
}
def getRealTimeResponse(response) {
	def cmdResponse = parseJson(response.headers["cmd-response"])
log.debug cmdResponse
    def state = cmdResponse.emeter.get_realtime
	def powerConsumption = state.power_mw / 1000
//    def powerConsumption = 11.1
	sendEvent(name: "power", value: powerConsumption, isStateChange: true)
}
def onOffResponse(response){
	log.info "On/Off command response received from server!"
	refresh()
}
def refreshResponse(response){
	if (response.headers["cmd-response"] == "TcpTimeout") {
		log.error "$device.name $device.label: TCP Timeout"
		sendEvent(name: "switch", value: "offline", isStateChange: true)
	} else if (response.headers["cmd-response"] == "invalidServerCmd") {
		log.error "$device.name $device.label: Server Received an Invalid Command"
    } else {
		def cmdResponse = parseJson(response.headers["cmd-response"])
		def status = cmdResponse.system.get_sysinfo.relay_state
		if (status == 1) {
			status = "on"
		} else {
   	     status = "off"
		}
		log.info "${device.name} ${device.label}: Power: ${status}"
		sendEvent(name: "switch", value: status, isStateChange: true)
	}
	getCurrentPower()
}