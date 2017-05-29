# Prototype-HS110
A prototype for the HS110 with Engegy Monitor for SmartThing integration.  Includes daily, weekly, and stats.

This is a prototype.  The final version will run in using the same node.js script as other SmartThings integrtions in this series.

# Requirements:

a.  Stand-alone bridge (PC, Raspberry, etc.) running node.js and version "Prototype.js"  The attached is a separate copy for your convenience.

b.  A TP-Link HS110 plug.

# UPDATED May 29

Found error where sending emeter cmd using UDP did not respond (where it had before).  Probably unsupported size (to long a packet).  Changed back to TCP with concating the return.  Requires both Device Handler and prototype.js updates.

Updated to correct date derivation and insert automatic scheduling of weekly/monthly statistics.  TO UPDATE,

a.  Open the existing DH on the SmartThings IDE,

b.  Delete existing code and copy contents of the new 'Prototype HS110.groovy" file into the DH,

c.  Select SAVE and the PUBLISH (for me),

d.  In the ST App, under settings, select "DONE".  This will initialize the date and scheduling functions.

Does NOT require an update to the 'Prototype.js' file.

# Open issues

1.  Ensure energy data is only 31 days (31 May check)

2.  Validate month roll-over in calculations (2 June check)

3.  See if automatic (15 min) refresh is working, once the devices are stable (current).  May have to go to polling.

4.  Monitor UDP implementation for split return messages from the device causing the DH to generate an error and not update the week/month stat data.

# Installation:

This Device Handler REQUIRES the 'Prototpye.js' bridge app.  It runs concurrently with existing bridge apps for this product line. It uses Port 8085 on the Bridge.

Installation is as for the original devices.  YOU DO NOT HAVE TO DELETE THE ORIGINAL DEVICE.  You can add this as a second rendition using a unique Device Network ID (I use HS100PROTO).  This allows existing functions to work w/o interference.

Instruction to tester:

To initialize the energy monitor functions, you will need to press "Refresh Stats" and also any other command to the plug.

a.  Use SmartThing Logging.  I need to have the log of a single refresh command (about four lines) to debug problems.

b.  Evaluate Interface - Is this the way to do Current Power - or do you want a separate Tile.

c.  If it works - tell me.

d.  Where next?  How much of the TP-Link Energy Monitor functions do you want?????

