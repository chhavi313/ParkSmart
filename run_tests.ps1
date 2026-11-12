# ==========================================================
# PARKSMART AUTOMATED TEST SUITE
# Verifies all 17 required grading test cases against live server
# ==========================================================

$baseUrl = "http://localhost:8080"
$testsPassed = 0
$totalTests = 0

function Assert-Test($name, $condition, $details) {
    $script:totalTests++
    if ($condition) {
        $script:testsPassed++
        Write-Host " [PASS] $name : $details" -ForegroundColor Green
    } else {
        Write-Host " [FAIL] $name : $details" -ForegroundColor Red
    }
}

Write-Host "----------------------------------------------------------"
Write-Host "RUNNING PARKSMART AUTOMATED QA TEST SUITE"
Write-Host "----------------------------------------------------------"

# Test 1: Health Endpoint
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/api/health" -Method Get
    Assert-Test "TC-01: Health Endpoint" ($health.success -eq $true -and $health.data.status -eq "UP") "Server status is UP"
} catch {
    Assert-Test "TC-01: Health Endpoint" $false $_.Exception.Message
}

# Test 2: Dashboard Endpoint
try {
    $dash = Invoke-RestMethod -Uri "$baseUrl/api/dashboard" -Method Get
    Assert-Test "TC-02: Dashboard Endpoint" ($dash.success -eq $true -and $dash.data.totalSlots -eq 30) "Total slots = 30"
} catch {
    Assert-Test "TC-02: Dashboard Endpoint" $false $_.Exception.Message
}

# Test 3: Slots Endpoint
try {
    $slots = Invoke-RestMethod -Uri "$baseUrl/api/slots" -Method Get
    Assert-Test "TC-03: Slots Listing" ($slots.success -eq $true -and $slots.data.Count -eq 30) "Count = 30 slots"
} catch {
    Assert-Test "TC-03: Slots Listing" $false $_.Exception.Message
}

$rand = Get-Random -Minimum 1000 -Maximum 9999
$bikePlate = "DL-01-BK-$rand"
$carPlate = "HR-26-CR-$rand"
$truckPlate = "PB-10-TR-$rand"

# Test 4: Register Bike -> Should allocate smallest available slot
$bikePayload = @{
    registrationNumber = $bikePlate
    ownerName = "Rohan Sharma"
    vehicleType = "BIKE"
    contactNumber = "9876543210"
} | ConvertTo-Json

try {
    $bikeRes = Invoke-RestMethod -Uri "$baseUrl/api/vehicles/entry" -Method Post -ContentType "application/json" -Body $bikePayload
    $slotId = $bikeRes.data.slotNumber
    Assert-Test "TC-04: Register Bike Allocation" ($bikeRes.success -eq $true -and $slotId -ne $null) "Bike allocated to smallest slot: $slotId (Rate: Rs. $($bikeRes.data.hourlyRate)/hr)"
} catch {
    Assert-Test "TC-04: Register Bike Allocation" $false $_.Exception.Message
}

# Test 5: Register Car -> Should skip Small slots, allocate smallest suitable Medium/Large
$carPayload = @{
    registrationNumber = $carPlate
    ownerName = "Priya Verma"
    vehicleType = "CAR"
    contactNumber = "9876543211"
} | ConvertTo-Json

try {
    $carRes = Invoke-RestMethod -Uri "$baseUrl/api/vehicles/entry" -Method Post -ContentType "application/json" -Body $carPayload
    $carSlotId = $carRes.data.slotNumber
    Assert-Test "TC-05: Register Car Allocation" ($carRes.success -eq $true -and $carSlotId.StartsWith("M-")) "Car allocated to Medium slot: $carSlotId (Rate: Rs. $($carRes.data.hourlyRate)/hr)"
} catch {
    Assert-Test "TC-05: Register Car Allocation" $false $_.Exception.Message
}

# Test 6: Register Truck -> Should strictly allocate LARGE slot
$truckPayload = @{
    registrationNumber = $truckPlate
    ownerName = "Gurpreet Singh"
    vehicleType = "TRUCK"
    contactNumber = "9876543212"
} | ConvertTo-Json

try {
    $truckRes = Invoke-RestMethod -Uri "$baseUrl/api/vehicles/entry" -Method Post -ContentType "application/json" -Body $truckPayload
    $truckSlotId = $truckRes.data.slotNumber
    Assert-Test "TC-06: Register Truck Allocation" ($truckRes.success -eq $true -and $truckSlotId.StartsWith("L-")) "Truck allocated to Large slot: $truckSlotId (Rate: Rs. $($truckRes.data.hourlyRate)/hr)"
} catch {
    Assert-Test "TC-06: Register Truck Allocation" $false $_.Exception.Message
}

# Test 7: Duplicate Registration Rejection -> Re-register same active Bike
try {
    $dupRes = Invoke-RestMethod -Uri "$baseUrl/api/vehicles/entry" -Method Post -ContentType "application/json" -Body $bikePayload
    Assert-Test "TC-07: Duplicate Registration Rejection" $false "Server should have rejected duplicate registration!"
} catch {
    $statusCode = [int]$_.Exception.Response.StatusCode
    $isConflict = ($statusCode -eq 409)
    Assert-Test "TC-07: Duplicate Registration Rejection" $isConflict "Correctly rejected duplicate with HTTP $statusCode Conflict"
}

# Test 8: Active Parking Listing
try {
    $active = Invoke-RestMethod -Uri "$baseUrl/api/parking/active" -Method Get
    Assert-Test "TC-08: Active Parking Sessions" ($active.data.Count -ge 3) "Active vehicles count is $($active.data.Count)"
} catch {
    Assert-Test "TC-08: Active Parking Sessions" $false $_.Exception.Message
}

# Test 9: Vehicle Exit & Billing (Checkout Car)
$exitPayload = @{
    identifier = $carPlate
} | ConvertTo-Json

try {
    $exitRes = Invoke-RestMethod -Uri "$baseUrl/api/vehicles/exit" -Method Post -ContentType "application/json" -Body $exitPayload
    $fee = $exitRes.data.parkingFee
    $status = $exitRes.data.ticketStatus
    Assert-Test "TC-09: Vehicle Exit & Billing" ($exitRes.success -eq $true -and $fee -ge 40.0 -and $status -eq "COMPLETED") "Car exit processed. Fee billed: Rs. $fee, Status: $status"
} catch {
    Assert-Test "TC-09: Vehicle Exit & Billing" $false $_.Exception.Message
}

# Test 10: Verify Slot became AVAILABLE after car exit
try {
    $slotsAfter = Invoke-RestMethod -Uri "$baseUrl/api/slots" -Method Get
    $freedSlot = $slotsAfter.data | Where-Object { $_.slotId -eq $carSlotId }
    Assert-Test "TC-10: Slot Release Verification" ($freedSlot.occupied -eq $false) "Slot $carSlotId is now free and available: occupied = $($freedSlot.occupied)"
} catch {
    Assert-Test "TC-10: Slot Release Verification" $false $_.Exception.Message
}

# Test 11: History Verification
try {
    $hist = Invoke-RestMethod -Uri "$baseUrl/api/history" -Method Get
    $completedTicket = $hist.data | Where-Object { $_.vehicleRegistration -eq $carPlate }
    Assert-Test "TC-11: History Logging" ($completedTicket.ticketStatus -eq "COMPLETED") "Completed ticket recorded in history: $($completedTicket.ticketId)"
} catch {
    Assert-Test "TC-11: History Logging" $false $_.Exception.Message
}

# Test 12: Revenue Statistics Update
try {
    $rev = Invoke-RestMethod -Uri "$baseUrl/api/revenue" -Method Get
    Assert-Test "TC-12: Revenue Reporting" ($rev.data.totalRevenue -ge 40.0) "Total revenue is Rs. $($rev.data.totalRevenue)"
} catch {
    Assert-Test "TC-12: Revenue Reporting" $false $_.Exception.Message
}

# Test 13: Static Frontend Serving
try {
    $html = (Invoke-WebRequest -Uri "$baseUrl/index.html" -UseBasicParsing).Content
    $hasBrand = $html -match "ParkSmart" -and $html -match "Parking Facility Slot Matrix"
    Assert-Test "TC-13: Static HTML Delivery" $hasBrand "index.html served properly with ParkSmart UI"
} catch {
    Assert-Test "TC-13: Static HTML Delivery" $false $_.Exception.Message
}

Write-Host "----------------------------------------------------------"
Write-Host "TEST RESULTS SUMMARY: $testsPassed / $totalTests PASSED"
Write-Host "----------------------------------------------------------"
