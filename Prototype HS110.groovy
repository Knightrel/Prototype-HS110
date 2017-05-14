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

Notes: This is a prototype HS-110 DH.  Read the Installation Instructions for explicit
instructions.
*/
metadata {
	definition (name: "Prototpe HS110", namespace: "test", author: "Dave Gutheinz") {
		capability "Switch"
		capability "refresh"
        capability "powerMeter"
        command "getWkMonStats"
        attribute "monthTotalE", "string"
        attribute "monthAvgE", "string"
        attribute "weekTotalE", "string"
        attribute "weekAvgE", "string"
        attribute "engrToday", "string"
	}
	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:'st.switches.switch.on', backgroundColor:"#00a0dc",
				nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff",
				nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#e86d13",
				nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#e86d13",
				nextState:"turningOn"
                attributeState "offline", label:'Bulb Offline', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"##e86d13",
                nextState:"turningOn"
			}
		}
		standardTile("refresh", "capability.refresh", width: 2, height: 2,  decoration: "flat") {
			state ("default", label:"Refresh", action:"refresh.refresh", icon:"st.secondary.refresh")
		}         
		standardTile("refreshStats", "Refresh Statistics", width: 2, height: 2,  decoration: "flat") {
			state ("refreshStats", label:"Refresh Stats", action:"getWkMonStats", backgroundColor:"#ffffff")
		}         
		valueTile("power", "device.power", decoration: "flat", height: 1, width: 2) {
			state "power", label: 'Current Power \n\r ${currentValue} W'
		}
        valueTile("engrToday", "device.engrToday", decoration: "flat", height: 1, width: 2) {
        	state "engrToday", label: 'Todays Energy\n\r${currentValue} KWH'
        }
		valueTile("monthTotal", "device.monthTotalE", decoration: "flat", height: 1, width: 2) {
			state "monthTotalE", label: '30 Day Total\n\r ${currentValue} KWH'
		}
		valueTile("monthAverage", "device.monthAvgE", decoration: "flat", height: 1, width: 2) {
			state "monthAvgE", label: '30 Day Average\n\r ${currentValue} KWH'
		}
		valueTile("weekTotal", "device.weekTotalE", decoration: "flat", height: 1, width: 2) {
			state "weekTotalE", label: 'Week Total\n\r ${currentValue} KWH'
		}
		valueTile("weekAverage", "device.weekAvgE", decoration: "flat", height: 1, width: 2) {
			state "weekAvgE", label: 'Week Average\n\r ${currentValue} KWH'
		}
        
		main("switch")
		details("switch", "power", "weekTotal", "monthTotal", "engrToday", "weekAverage", "monthAverage", "refresh" ,"refreshStats")
    }
}
preferences {
	input("deviceIP", "text", title: "Device IP", required: true, displayDuringSetup: true)
	input("gatewayIP", "text", title: "Gateway IP", required: true, displayDuringSetup: true)
}
//	------------------------------------------------------------------------------
def on() {
	log.info "${device.name} ${device.label}: Turning ON"
	sendCmdtoServer('{"system":{"set_relay_state":{"state": 1}}}', "onOffResponse")
}
//	------------------------------------------------------------------------------
def off() {
	log.info "${device.name} ${device.label}: Turning OFF"
	sendCmdtoServer('{"system":{"set_relay_state":{"state": 0}}}', "onOffResponse")
}
//	------------------------------------------------------------------------------
def refresh(){
	log.info "Polling ${device.name} ${device.label}"
	sendCmdtoServer('{"system":{"get_sysinfo":{}}}', "refreshResponse")
}
//	------------------------------------------------------------------------------
//	Energy Monitor Functions for HS-110
//	Get current consumption rate
def getEngeryMeter(){
	sendCmdtoServer('{"emeter":{"get_realtime":{}}}', "energyMeterResponse")
}
//	------------------------------------------------------------------------------
//	Get today's total consumption
def getTodayUse(){
	initEngMon()
    def month = state.monthToday
	def year = state.yearToday
	sendCmdtoServer("""{"emeter":{"get_daystat":{"month": ${month}, "year": ${year}}}}""", "engrTodayResponse")
}
//	------------------------------------------------------------------------------
//	Get the weekly and monthly statistics
def getWkMonStats() {
	log.info "Refreshing Weekly and Monthly Energry Statistics"
    unschedule(getWkMonStats)
    runEvery3Hours(getWkMonStats)
	initEngMon()
    def month = state.monthToday
	def year = state.yearToday
	sendCmdtoServer("""{"emeter":{"get_daystat":{"month": ${month}, "year": ${year}}}}""", "engrStatsResponse")
	if (month == 1) {
    	year -= 1
        month = 12
    } else {
		month -= 1
    }
	sendCmdtoServer("""{"emeter":{"get_daystat":{"month": ${month}, "year": ${year}}}}""", "engrStatsResponse")
}
//	------------------------------------------------------------------------------
def initEngMon() {
    state.monTotEnergy = 0
	state.monTotDays = 0
    state.wkTotEnergy = 0
    def today = new Date().format('yyyyMMdd')
    state.yearToday = today.substring(0,4) as int
    state.monthToday = today.substring(4,6) as int
    state.dayToday = today.substring(6,8) as int
}
//	------------------------------------------------------------------------------
//	Standard send command to server function.
private sendCmdtoServer(command, action){
	def headers = [:] 
	headers.put("HOST", "$gatewayIP:8085")   // port 8082 must be same as value in TP-LInkServerLite.js
	headers.put("tplink-iot-ip", deviceIP)
    headers.put("tplink-command", command)
	headers.put("command", "deviceCommand")
	sendHubCommand(new physicalgraph.device.HubAction([
		headers: headers],
		device.deviceNetworkId,
		[callback: action]
	))
}
//	------------------------------------------------------------------------------
def onOffResponse(response){
	log.info "On/Off command response received from server!"
	refresh()
}
//	------------------------------------------------------------------------------
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
    getEngeryMeter()
    runIn(1, getTodayUse())
}
//	------------------------------------------------------------------------------
//	Energy Monitor Response Parsing
//	Get the current consumption rate
def energyMeterResponse(response) {
	def cmdResponse = parseJson(response.headers["cmd-response"])
    if (cmdResponse.emeter.err_code == -1) {
    	log.error "Energy Monitor not supported on Device.  Only Supported on HS110"
    } else {
    	def state = cmdResponse["emeter"]["get_realtime"]
		def powerConsumption = state.power_mw / 1000
		sendEvent(name: "power", value: powerConsumption, isStateChange: true)
    	log.info "Updated Current Power to $powerConsumption W"
    }
}
//	------------------------------------------------------------------------------
//	Get consumption today
def engrTodayResponse(response) {
	def cmdResponse = parseJson(response.headers["cmd-response"])
    if (cmdResponse.emeter.err_code == -1) {
    	log.error "Energy Monitor not supported on Device.  Only Supported on HS110"
    } else {
		def monTotEnergy = state.monTotEnergy
		def engrToday
	    def dayList = cmdResponse["emeter"]["get_daystat"].day_list
		for (int i = 0; i < dayList.size(); i++) {
	    	def engrData = dayList[i]
			if(engrData.day == state.dayToday) {
	        	engrToday = engrData.energy_wh/1000
	        }
	   }
	    sendEvent(name: "engrToday", value: engrToday, isStateChange: true)
	    log.info "Updated Current power useag to $engrToday"
	}
}
//	------------------------------------------------------------------------------
//	Get monthly and weekly consumption statistics
def engrStatsResponse(response) {
	def cmdResponse = parseJson(response.headers["cmd-response"])
    if (cmdResponse.emeter.err_code == -1) {
    	log.error "Energy Monitor not supported on Device.  Only Supported on HS110"
    } else {
		def monTotEnergy = state.monTotEnergy
	    def wkTotEnergy = state.wkTotEnergy
		def monTotDays = state.monTotDays
//	Determine start and stop parameters for the weekly data.
		Calendar calendar = GregorianCalendar.instance
		calendar.set(state.yearToday, state.monthToday, 1)
	    def prevMonthDays = calendar.getActualMaximum(GregorianCalendar.DAY_OF_MONTH)
		def weekEnd = state.dayToday + prevMonthDays - 1
	    def weekStart = weekEnd - 6
//	Read sent data and extract data.
	    def dayList = cmdResponse["emeter"]["get_daystat"].day_list
		for (int i = 0; i < dayList.size(); i++) {
	    	def engrData = dayList[i]
//	Get monthly total energy (all date except current day)
			if(engrData.day == state.dayToday) {
	        	monTotDays -= 1
	        } else {
	    		monTotEnergy += engrData.energy_wh
	        }
//	Get weekly total energy - from yesterday and back
	        def adjEngDay = engrData.day + prevMonthDays
			if (engrData.day + prevMonthDays <= weekEnd && engrData.day + prevMonthDays >= weekStart) {
	        	wkTotEnergy += engrData.energy_wh
			}
	    }
//	Update state values, calculate averages, and update display through sendEvent
	    monTotDays += dayList.size()
	    state.monTotDays = monTotDays
		state.monTotEnergy = monTotEnergy
	    state.wkTotEnergy = wkTotEnergy
		def monAvgEnergy = Math.round(monTotEnergy/(monTotDays-1))/1000
	    def wkAvgEnergy = Math.round(wkTotEnergy/7)/1000
		sendEvent(name: "monthTotalE", value: monTotEnergy/1000, isStateChange: true)
	    sendEvent(name: "monthAvgE", value: monAvgEnergy, isStateChange: true)
		sendEvent(name: "weekTotalE", value: wkTotEnergy/1000, isStateChange: true)
	    sendEvent(name: "weekAvgE", value: wkAvgEnergy, isStateChange: true)
	    log.info "Updated Weekly and Monthly energy consumption statistics"
	}
}