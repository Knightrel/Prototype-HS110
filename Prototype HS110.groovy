/*
TP-Link HS-110 Device Handler, Version 3.0

Copyright 2017 Dave Gutheinz

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this 
file except in compliance with the License. You may obtain a copy of the License at:

		http://www.apache.org/licenses/LICENSE-2.0
        
Unless required by applicable law or agreed to in writing, software distributed under 
the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
ANY KIND, either express or implied. See the License for the specific language governing 
permissions and limitations under the License.

Supported models and functions:  This device supports the TP-Link HS100, HS105, HS110, and
HS200 devices.  It supports the on/off function only.

Notes: 
1.	This Device Handler requires an operating Windows 10 PC Bridge running version 3.0 of
	'TP-LinkServer.js'.  It is NOT fully compatible with earlier versions.
2.	This device supports the TP-Link HS110.  It supports on/off as well as daily, weekly,
	and monthly energy usage data display.

Update History
	06/01/2017	- Initial release of HS-110 handler
*/
metadata {
	definition (name: "TP-Link HS110 Prototype", namespace: "test", author: "Dave Gutheinz") {
		capability "Switch"
		capability "refresh"
        capability "polling"
        capability "powerMeter"
        capability "Sensor"
		capability "Actuator"
        command "getWkMonStats"
        attribute "monthTotalE", "string"
        attribute "monthAvgE", "string"
        attribute "weekTotalE", "string"
        attribute "weekAvgE", "string"
        attribute "engrToday", "string"
        attribute "dateUpdate", "string"
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
                attributeState "offline", label:'Comms Error', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#e86d13",
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
        	state "engrToday", label: 'Todays Usage\n\r${currentValue} KWH'
        }
		valueTile("monthTotal", "device.monthTotalE", decoration: "flat", height: 1, width: 2) {
			state "monthTotalE", label: '30 Day Total\n\r ${currentValue} KWH'
		}
		valueTile("monthAverage", "device.monthAvgE", decoration: "flat", height: 1, width: 2) {
			state "monthAvgE", label: '30 Day Avg\n\r ${currentValue} KWH'
		}
		valueTile("weekTotal", "device.weekTotalE", decoration: "flat", height: 1, width: 2) {
			state "weekTotalE", label: '7 Day Total\n\r ${currentValue} KWH'
		}
		valueTile("weekAverage", "device.weekAvgE", decoration: "flat", height: 1, width: 2) {
			state "weekAvgE", label: '7 Day Avg\n\r ${currentValue} KWH'
		}
        
		main("switch")
		details("switch", "power", "weekTotal", "monthTotal", "engrToday", "weekAverage", "monthAverage", "refresh" ,"refreshStats")
    }
}
preferences {
	input("deviceIP", "text", title: "Device IP", required: true, displayDuringSetup: true)
	input("gatewayIP", "text", title: "Gateway IP", required: true, displayDuringSetup: true)
}
//	---------------------------------------------------------------------------
//	----- RUN WHEN PREFERENCES ARE UPDATES ------------------------------------
def updated() {
	log.info "Running Updated"
    unschedule()
    runEvery15Minutes(refresh)
	schedule("0 15 0 * * ?", setCurrentDate)
	schedule("0 30 0 * * ?", getWkMonStats)
	runIn(3, setCurrentDate)
    runIn(6, refresh)
    runIn(10, getWkMonStats)
}
//	---------------------------------------------------------------------------
//	----- BASIC PLUG COMMANDS -------------------------------------------------
def on() {
	sendCmdtoServer('{"system":{"set_relay_state":{"state": 1}}}', "onOffResponse")
}
def off() {
	sendCmdtoServer('{"system":{"set_relay_state":{"state": 0}}}', "onOffResponse")
}
def refresh(){
	log.info "Refreshing ${device.name} ${device.label}"
	sendCmdtoServer('{"system":{"get_sysinfo":{}}}', "refreshResponse")
}
def onOffResponse(response){
	if (response.headers["cmd-response"] == "commError") {
		log.error "$device.name $device.label: Communications Error"
		sendEvent(name: "switch", value: "offline", descriptionText: "ERROR - OffLine - mod onOffResponse", isStateChange: true)
     } else {
		log.info "On/Off command response received from server!"
		refresh()
	}
}
def refreshResponse(response){
	if (response.headers["cmd-response"] == "commError") {
		log.error "$device.name $device.label: Communications Error"
		sendEvent(name: "switch", value: "offline", descriptionText: "ERROR - OffLine - mod refreshResponse", isStateChange: true)
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
     	getEngeryMeter()
	}
}
//	---------------------------------------------------------------------------
//	----- CURRENT ENERGY METER DATA -------------------------------------------
def getEngeryMeter(){
	sendCmdtoServer('{"emeter":{"get_realtime":{}}}', "energyMeterResponse")
}
def energyMeterResponse(response) {
	def cmdResponse = parseJson(response.headers["cmd-response"])
    if (cmdResponse["emeter"].err_code == -1) {
    	log.error "This DH Only Supports the HS110 plug"
		sendEvent(name: "power", value: powerConsumption, descriptionText: "Bulb is not a HS110", isStateChange: true)
    } else {
	    def state = cmdResponse["emeter"]["get_realtime"]
		def powerConsumption = state.power
		sendEvent(name: "power", value: powerConsumption, isStateChange: true)
	    log.info "Updating Current Power to $powerConsumption"
		getUseToday()
    }
}
//	---------------------------------------------------------------------------
//	----- USE TODAY DATA ------------------------------------------------------
def getUseToday(){
	getDateData()
	sendCmdtoServer("""{"emeter":{"get_daystat":{"month": ${state.monthToday}, "year": ${state.yearToday}}}}""", "useTodayResponse")
}
def useTodayResponse(response) {
	def engrToday
	def cmdResponse = parseJson(response.headers["cmd-response"])
    def dayList = cmdResponse["emeter"]["get_daystat"].day_list
	for (int i = 0; i < dayList.size(); i++) {
    	def engrData = dayList[i]
		if(engrData.day == state.dayToday) {
        	engrToday = Math.round(1000*engrData.energy) / 1000
        }
   }
    sendEvent(name: "engrToday", value: engrToday, isStateChange: true)
    log.info "Updated Today's Usage to $engrToday"
}
//	---------------------------------------------------------------------------
//	----- WEEKLY AND MONTHLY STATISTICS ---------------------------------------
def getWkMonStats() {
    state.monTotEnergy = 0
	state.monTotDays = 0
    state.wkTotEnergy = 0
    getDateData()
	sendCmdtoServer("""{"emeter":{"get_daystat":{"month": ${state.monthToday}, "year": ${state.yearToday}}}}""", "engrStatsResponse")
    runIn(2, getPrevMonth)
    schedule("0 30 0/6 * * ?", getWkMonStats)
}
def getPrevMonth() {
    getDateData()
	if (state.dayToday < 31) {
	    def month = state.monthToday
		def year = state.yearToday
		if (month == 1) {
	    	year -= 1
	        month = 12
			sendCmdtoServer("""{"emeter":{"get_daystat":{"month": ${month}, "year": ${year}}}}""", "engrStatsResponse")
	    } else {
			month -= 1
			sendCmdtoServer("""{"emeter":{"get_daystat":{"month": ${month}, "year": ${year}}}}""", "engrStatsResponse")
	    }
	}
}
def engrStatsResponse(response) {
	def monTotEnergy = state.monTotEnergy
	def wkTotEnergy = state.wkTotEnergy
	def monTotDays = state.monTotDays
	Calendar calendar = GregorianCalendar.instance
	calendar.set(state.yearToday, state.monthToday, 1)
	def prevMonthDays = calendar.getActualMaximum(GregorianCalendar.DAY_OF_MONTH)
	def weekEnd = state.dayToday + prevMonthDays - 1
	def weekStart = weekEnd - 6
	def cmdResponse = parseJson(response.headers["cmd-response"])
	if (cmdResponse["emeter"].err_code == -1) {
	    log.error "This DH Only Supports the HS110 plug"
		sendEvent(name: "monthTotalE", value: 0, descriptionText: "Bulb is not a HS110", isStateChange: true)
    } else {
		def dayList = cmdResponse["emeter"]["get_daystat"].day_list
		for (int i = 0; i < dayList.size(); i++) {
		    def engrData = dayList[i]
			if(engrData.day == state.dayToday && engrData.month == state.monthToday) {
		        monTotDays -= 1
		    } else {
		    	monTotEnergy += engrData.energy
		    }
		    def adjEngDay = engrData.day + prevMonthDays
			if (engrData.day + prevMonthDays <= weekEnd && engrData.day + prevMonthDays >= weekStart) {
		        wkTotEnergy += engrData.energy
			}
		}
		monTotDays += dayList.size()
		state.monTotDays = monTotDays
		state.monTotEnergy = monTotEnergy
		state.wkTotEnergy = wkTotEnergy
		if (state.dayToday == 31 || state.monthToday -1 == dayList[1].month) {
			log.info "Updated 7 and 30 day energy consumption statistics"
	        wkTotEnergy = Math.round(1000*wkTotEnergy) / 1000
	        monTotEnergy = Math.round(1000*monTotEnergy) / 1000
			def wkAvgEnergy = Math.round((1000*wkTotEnergy)/7) / 1000
			def monAvgEnergy = Math.round((1000*monTotEnergy)/monTotDays) / 1000
			sendEvent(name: "monthTotalE", value: monTotEnergy, isStateChange: true)
			sendEvent(name: "monthAvgE", value: monAvgEnergy, isStateChange: true)
			sendEvent(name: "weekTotalE", value: wkTotEnergy, isStateChange: true)
			sendEvent(name: "weekAvgE", value: wkAvgEnergy, isStateChange: true)
        }
	}
}
//	---------------------------------------------------------------------------
//	----- SET CURRENT DATE AND GET DATE DATA FOR PROCESSING -------------------
def setCurrentDate() {
	sendCmdtoServer('{"time":{"get_time":null}}', "currentDateResponse")
}
def currentDateResponse(response) {
	def cmdResponse = parseJson(response.headers["cmd-response"])
	def setDate =  cmdResponse["time"]["get_time"]
    updateDataValue("dayToday", "$setDate.mday")
    updateDataValue("monthToday", "$setDate.month")
    updateDataValue("yearToday", "$setDate.year")
    sendEvent(name: "dateUpdate", value: "$setDate.hour : $setDate.min")
    log.info "Current Date Updated at $setDate.hour : $setDate.min"
}
def getDateData(){
	state.dayToday = getDataValue("dayToday") as int
    state.monthToday = getDataValue("monthToday") as int
    state.yearToday = getDataValue("yearToday") as int
}
//	---------------------------------------------------------------------------
//	----- SEND COMMAND DATA TO THE SERVER -------------------------------------
private sendCmdtoServer(command, action){
	def headers = [:] 
	headers.put("HOST", "$gatewayIP:8082")	//	SET TO VALUE IN JAVA SCRIPT PKG.
	headers.put("tplink-iot-ip", deviceIP)
    headers.put("tplink-command", command)
	headers.put("command", "deviceCommand")
	sendHubCommand(new physicalgraph.device.HubAction([
		headers: headers],
		device.deviceNetworkId,
		[callback: action]
	))
}