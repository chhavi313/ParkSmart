/**
 * PARKSMART - FRONTEND APPLICATION JAVASCRIPT
 * Connects directly to Java HttpServer backend REST APIs
 * STRICT FRONTEND IMPLEMENTATION - Zero backend changes
 */

// Application State
const state = {
    currentTab: 'dashboard',
    dashboardData: null,
    slotsData: [],
    activeParking: [],
    historyData: [],
    revenueData: null,
    slotFilter: 'all',
    selectedExitTicket: null,
    activityView: 'entries'
};

// Initialize Application on DOM Ready
document.addEventListener('DOMContentLoaded', () => {
    initNavigation();
    initClock();
    initGlobalSearch();
    initSlotFilters();
    loadAllData();

    // Auto-refresh stats periodically every 15 seconds
    setInterval(() => {
        refreshDashboardData();
    }, 15000);
});

// ========================================================
// NAVIGATION & TABS
// ========================================================
function initNavigation() {
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        item.addEventListener('click', () => {
            const tabId = item.getAttribute('data-tab');
            navigateTo(tabId);
        });
    });

    const refreshBtn = document.getElementById('btn-refresh');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', () => {
            showToast('Syncing facility telemetry...', 'info');
            loadAllData();
        });
    }
}

function navigateTo(tabId) {
    if (!tabId) return;

    // Update active nav button
    document.querySelectorAll('.nav-item').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-tab') === tabId);
    });

    // Update active pane
    document.querySelectorAll('.tab-pane').forEach(pane => {
        pane.classList.remove('active');
    });

    const targetPane = document.getElementById(`tab-${tabId}`);
    if (targetPane) {
        targetPane.classList.add('active');
    }

    // Update page title
    const titles = {
        'dashboard': 'Dashboard Overview',
        'slots': 'Parking Slot Grid',
        'entry': 'Vehicle Entry & Registration',
        'active': 'Active Parking Sessions',
        'exit': 'Vehicle Exit & Billing',
        'history': 'Parking History Audit',
        'revenue': 'Revenue & Financial Reports'
    };
    const titleEl = document.getElementById('page-title');
    if (titleEl) {
        titleEl.innerText = titles[tabId] || 'ParkSmart';
    }
    state.currentTab = tabId;

    // Trigger tab-specific refresh
    if (tabId === 'slots') fetchSlots();
    if (tabId === 'active') fetchActiveParking();
    if (tabId === 'history') fetchHistory();
    if (tabId === 'revenue') fetchRevenue();
    if (tabId === 'entry') updateEntryAvailabilityNotice();
}

// ========================================================
// LIVE CLOCK & TELEMETRY
// ========================================================
function initClock() {
    const clockEl = document.getElementById('live-clock');
    if (!clockEl) return;
    const updateTime = () => {
        const now = new Date();
        const options = { 
            weekday: 'short', 
            year: 'numeric', 
            month: 'short', 
            day: 'numeric', 
            hour: '2-digit', 
            minute: '2-digit', 
            second: '2-digit' 
        };
        clockEl.innerText = now.toLocaleDateString('en-IN', options);
    };
    updateTime();
    setInterval(updateTime, 1000);
}

// ========================================================
// GLOBAL SEARCH
// ========================================================
function initGlobalSearch() {
    const searchInput = document.getElementById('global-search-input');
    if (searchInput) {
        searchInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                const query = searchInput.value.trim();
                if (query) {
                    navigateTo('history');
                    const historySearch = document.getElementById('history-search-input');
                    if (historySearch) {
                        historySearch.value = query;
                    }
                    searchHistory(query);
                }
            }
        });
    }

    const activeSearch = document.getElementById('active-search-input');
    if (activeSearch) {
        activeSearch.addEventListener('input', (e) => {
            filterActiveParkingTable(e.target.value.trim().toLowerCase());
        });
    }

    const historySearch = document.getElementById('history-search-input');
    if (historySearch) {
        historySearch.addEventListener('input', (e) => {
            searchHistory(e.target.value.trim());
        });
    }
}

// ========================================================
// DATA LOADING
// ========================================================
async function loadAllData() {
    await Promise.all([
        fetchDashboard(),
        fetchSlots(),
        fetchActiveParking(),
        fetchHistory(),
        fetchRevenue()
    ]);
}

async function refreshDashboardData() {
    await fetchDashboard();
    if (state.currentTab === 'slots') await fetchSlots();
    if (state.currentTab === 'active') await fetchActiveParking();
}

// 1. DASHBOARD API
async function fetchDashboard() {
    try {
        const res = await fetch('/api/dashboard');
        const json = await res.json();
        if (json.success && json.data) {
            state.dashboardData = json.data;
            renderDashboard(json.data);
        }
    } catch (err) {
        console.error('Failed to fetch dashboard data:', err);
    }
}

function renderDashboard(data) {
    const totalSlots = data.totalSlots || 30;
    const availSlots = data.availableSlots ?? 0;
    const occSlots = data.occupiedSlots ?? 0;
    const occPct = data.occupancyPercentage || 0;

    const elTotal = document.getElementById('stat-total-slots');
    if (elTotal) elTotal.innerText = totalSlots;

    const elAvail = document.getElementById('stat-available-slots');
    if (elAvail) elAvail.innerText = availSlots;

    const elOcc = document.getElementById('stat-occupied-slots');
    if (elOcc) elOcc.innerText = occSlots;

    const elActive = document.getElementById('stat-active-vehicles');
    if (elActive) elActive.innerText = data.activeVehicles ?? 0;

    const elRev = document.getElementById('stat-today-revenue');
    if (elRev) elRev.innerText = `₹${(data.todayRevenue || 0).toFixed(2)}`;

    const elCompletedMeta = document.getElementById('stat-completed-meta');
    if (elCompletedMeta) elCompletedMeta.innerText = `${data.completedSessions || 0} sessions billed`;

    // Sidebar badges
    const navFree = document.getElementById('nav-free-slots');
    if (navFree) navFree.innerText = `${availSlots} Free`;

    const navActive = document.getElementById('nav-active-count');
    if (navActive) navActive.innerText = data.activeVehicles ?? 0;

    // Occupancy Dial & Bar
    const occBadge = document.getElementById('occupancy-badge');
    if (occBadge) occBadge.innerText = `${occPct}% Occupied`;

    const dialVal = document.getElementById('dial-percent-val');
    if (dialVal) dialVal.innerText = `${Math.round(occPct)}%`;

    const dialMeter = document.getElementById('occupancy-dial-meter');
    if (dialMeter) {
        // Circumference for r=40 is 2 * PI * 40 ~= 251.2
        const maxOffset = 251.2;
        const offset = maxOffset - (maxOffset * (Math.min(100, occPct) / 100));
        dialMeter.style.strokeDashoffset = offset;
    }

    const occBar = document.getElementById('occupancy-bar');
    if (occBar) occBar.style.width = `${Math.min(100, Math.max(0, occPct))}%`;

    const legOcc = document.getElementById('leg-occupied');
    if (legOcc) legOcc.innerText = occSlots;

    const legAvail = document.getElementById('leg-available');
    if (legAvail) legAvail.innerText = availSlots;

    const summaryText = document.getElementById('occupancy-summary-text');
    if (summaryText) {
        if (occPct === 0) {
            summaryText.innerText = 'Facility Vacant — All 30 Bays Free';
        } else if (occPct < 40) {
            summaryText.innerText = 'Optimal Capacity — Plentiful Slots Available';
        } else if (occPct < 80) {
            summaryText.innerText = 'Moderate Load — Free Spaces Across Zones';
        } else {
            summaryText.innerText = 'High Utilization — Limited Bay Availability';
        }
    }

    // Category bars
    if (data.slotsByType) {
        const small = data.slotsByType.SMALL || { occupied: 0, total: 10 };
        const med = data.slotsByType.MEDIUM || { occupied: 0, total: 15 };
        const large = data.slotsByType.LARGE || { occupied: 0, total: 5 };

        const elSmallCnt = document.getElementById('cat-small-count');
        if (elSmallCnt) elSmallCnt.innerText = `${small.occupied} / ${small.total} Occupied`;
        const elSmallBar = document.getElementById('cat-small-bar');
        if (elSmallBar) elSmallBar.style.width = `${(small.occupied / small.total) * 100}%`;

        const elMedCnt = document.getElementById('cat-medium-count');
        if (elMedCnt) elMedCnt.innerText = `${med.occupied} / ${med.total} Occupied`;
        const elMedBar = document.getElementById('cat-medium-bar');
        if (elMedBar) elMedBar.style.width = `${(med.occupied / med.total) * 100}%`;

        const elLrgCnt = document.getElementById('cat-large-count');
        if (elLrgCnt) elLrgCnt.innerText = `${large.occupied} / ${large.total} Occupied`;
        const elLrgBar = document.getElementById('cat-large-bar');
        if (elLrgBar) elLrgBar.style.width = `${(large.occupied / large.total) * 100}%`;
    }

    // Recent Entries
    const entriesTbody = document.getElementById('recent-entries-tbody');
    if (entriesTbody) {
        if (data.recentEntries && data.recentEntries.length > 0) {
            entriesTbody.innerHTML = data.recentEntries.map(t => `
                <tr>
                    <td><span class="ticket-badge">${t.ticketId}</span></td>
                    <td><strong class="plate-badge">${t.vehicleRegistration}</strong></td>
                    <td><span class="chip ${getVehicleChipClass(t.vehicleType)}">${t.vehicleType}</span></td>
                    <td><span class="slot-pill">${t.slotNumber}</span></td>
                    <td style="color: var(--text-secondary); font-family: var(--font-mono); font-size: 0.78rem;">${t.entryTime || '--'}</td>
                    <td>
                        <button class="btn btn-sm btn-secondary" onclick="openQuickExitModal('${t.vehicleRegistration}', '${t.slotNumber}')">
                            Settle & Exit
                        </button>
                    </td>
                </tr>
            `).join('');
        } else {
            entriesTbody.innerHTML = `<tr><td colspan="6" class="empty-cell">No recent vehicle entries recorded.</td></tr>`;
        }
    }

    // Recent Exits
    const exitsTbody = document.getElementById('recent-exits-tbody');
    if (exitsTbody) {
        if (data.recentExits && data.recentExits.length > 0) {
            exitsTbody.innerHTML = data.recentExits.map(t => `
                <tr>
                    <td><span class="ticket-badge">${t.ticketId}</span></td>
                    <td><strong class="plate-badge">${t.vehicleRegistration}</strong></td>
                    <td><span class="chip chip-active">${t.parkingDuration || '--'}</span></td>
                    <td style="font-family: var(--font-mono); font-weight: 700; color: var(--accent-lime-text);">₹${(t.parkingFee || 0).toFixed(2)}</td>
                    <td style="color: var(--text-secondary); font-family: var(--font-mono); font-size: 0.78rem;">${t.exitTime || '--'}</td>
                    <td><span class="chip chip-completed">Settled</span></td>
                </tr>
            `).join('');
        } else {
            exitsTbody.innerHTML = `<tr><td colspan="6" class="empty-cell">No vehicles have exited yet.</td></tr>`;
        }
    }
}

// Activity View Toggle (Recent Entries vs Exits)
function toggleActivityView(view) {
    state.activityView = view;
    const btnEntries = document.getElementById('btn-toggle-entries');
    const btnExits = document.getElementById('btn-toggle-exits');
    const panelEntries = document.getElementById('activity-entries-panel');
    const panelExits = document.getElementById('activity-exits-panel');

    if (view === 'entries') {
        if (btnEntries) btnEntries.classList.add('active');
        if (btnExits) btnExits.classList.remove('active');
        if (panelEntries) panelEntries.classList.remove('hidden');
        if (panelExits) panelExits.classList.add('hidden');
    } else {
        if (btnEntries) btnEntries.classList.remove('active');
        if (btnExits) btnExits.classList.add('active');
        if (panelEntries) panelEntries.classList.add('hidden');
        if (panelExits) panelExits.classList.remove('hidden');
    }
}

// 2. PARKING SLOTS API
async function fetchSlots() {
    try {
        const res = await fetch('/api/slots');
        const json = await res.json();
        if (json.success && json.data) {
            state.slotsData = json.data;
            renderSlotsGrid();
            updateSlotFilterCounts();
            updateEntryAvailabilityNotice();
        }
    } catch (err) {
        console.error('Failed to fetch slots:', err);
    }
}

function initSlotFilters() {
    const filterBtns = document.querySelectorAll('.slot-filter-buttons .filter-btn');
    filterBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            filterBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            state.slotFilter = btn.getAttribute('data-filter');
            renderSlotsGrid();
        });
    });
}

function updateSlotFilterCounts() {
    const avail = state.slotsData.filter(s => !s.occupied).length;
    const occ = state.slotsData.filter(s => s.occupied).length;
    const availBtn = document.getElementById('btn-count-avail');
    const occBtn = document.getElementById('btn-count-occ');
    if (availBtn) availBtn.innerText = avail;
    if (occBtn) occBtn.innerText = occ;
}

function renderSlotsGrid() {
    const container = document.getElementById('slots-container');
    if (!container) return;

    let filtered = state.slotsData;
    if (state.slotFilter === 'available') {
        filtered = filtered.filter(s => !s.occupied);
    } else if (state.slotFilter === 'occupied') {
        filtered = filtered.filter(s => s.occupied);
    } else if (['SMALL', 'MEDIUM', 'LARGE'].includes(state.slotFilter)) {
        filtered = filtered.filter(s => s.slotType === state.slotFilter);
    }

    if (filtered.length === 0) {
        container.innerHTML = `<div class="empty-cell" style="grid-column: 1/-1;">No parking bays match the selected filter criteria.</div>`;
        return;
    }

    container.innerHTML = filtered.map(slot => {
        const isOcc = slot.occupied;
        const statusClass = isOcc ? 'occupied' : 'available';

        return `
            <div class="slot-bay-card ${statusClass}" onclick="handleSlotCardClick('${slot.slotId}', ${isOcc}, '${slot.vehicleRegistration || ''}')">
                <div class="slot-bay-header">
                    <span class="slot-bay-id">${slot.slotId}</span>
                    <span class="slot-bay-type">${slot.slotType}</span>
                </div>
                <div class="slot-bay-body">
                    ${isOcc ? `
                        <div class="slot-bay-occupied-plate">${slot.vehicleRegistration}</div>
                        <div class="slot-bay-occupied-meta">${slot.vehicleType || 'Vehicle'} • ${formatSlotTime(slot.occupiedAt)}</div>
                    ` : `
                        <div class="slot-bay-vacant">
                            <span class="legend-dot dot-lime"></span>
                            <span>Vacant Bay</span>
                        </div>
                        <div class="slot-bay-vacant-hint">Click to reserve</div>
                    `}
                </div>
                <div class="slot-bay-footer">
                    <span>${isOcc ? '<span style="color: var(--status-occ);">Occupied</span>' : '<span style="color: var(--status-avail);">Ready</span>'}</span>
                    <span>${isOcc ? 'Action &rarr;' : 'Reserve &rarr;'}</span>
                </div>
            </div>
        `;
    }).join('');
}

function handleSlotCardClick(slotId, isOccupied, vehicleRegistration) {
    if (isOccupied && vehicleRegistration) {
        openQuickExitModal(vehicleRegistration, slotId);
    } else {
        navigateTo('entry');
    }
}

function formatSlotTime(ts) {
    if (!ts) return '';
    try {
        const parts = ts.split(' ');
        return parts.length > 1 ? parts[1] : ts;
    } catch (e) {
        return ts;
    }
}

// 3. VEHICLE ENTRY / REGISTRATION
function selectVehicleCategoryCard(cat) {
    document.querySelectorAll('.category-radio-card').forEach(card => {
        card.classList.toggle('active', card.getAttribute('data-val') === cat);
    });
    const nativeSelect = document.getElementById('entry-vehicle-type');
    if (nativeSelect) {
        nativeSelect.value = cat;
        updateEntryAvailabilityNotice();
    }
}

async function handleVehicleEntry(e) {
    e.preventDefault();

    const regInput = document.getElementById('entry-reg-no');
    const ownerInput = document.getElementById('entry-owner-name');
    const typeSelect = document.getElementById('entry-vehicle-type');
    const contactInput = document.getElementById('entry-contact-no');
    const submitBtn = document.getElementById('btn-submit-entry');

    const reg = regInput.value.trim().toUpperCase();
    const owner = ownerInput.value.trim();
    const type = typeSelect.value;
    const contact = contactInput.value.trim();

    if (!reg) {
        showToast('Please enter vehicle registration plate.', 'error');
        regInput.focus();
        return;
    }

    if (!owner) {
        showToast('Please enter driver / owner name.', 'error');
        ownerInput.focus();
        return;
    }

    submitBtn.disabled = true;
    submitBtn.innerHTML = `<span>Reserving Bay...</span>`;

    try {
        const res = await fetch('/api/vehicles/entry', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                registrationNumber: reg,
                ownerName: owner,
                vehicleType: type,
                contactNumber: contact
            })
        });

        const json = await res.json();

        if (res.ok && json.success) {
            const ticket = json.data;
            showToast(`Vehicle ${ticket.vehicleRegistration} allocated to Bay ${ticket.slotNumber}!`, 'success');
            renderNewlyIssuedTicket(ticket);
            document.getElementById('vehicle-entry-form').reset();
            // Reset category selector to default CAR
            selectVehicleCategoryCard('CAR');
            loadAllData();
        } else {
            showToast(json.error || 'Failed to allocate parking slot.', 'error');
        }
    } catch (err) {
        showToast('Network error during vehicle registration.', 'error');
        console.error(err);
    } finally {
        submitBtn.disabled = false;
        submitBtn.innerHTML = `
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
            <span>Allocate Bay & Generate Entry Pass</span>
        `;
    }
}

function updateEntryAvailabilityNotice() {
    const noticeEl = document.getElementById('entry-slot-notice-text');
    const nativeSelect = document.getElementById('entry-vehicle-type');
    const noticeBox = document.getElementById('entry-slot-notice');
    if (!nativeSelect || !noticeEl || !noticeBox) return;

    const type = nativeSelect.value;
    if (!state.slotsData || state.slotsData.length === 0) return;

    let availableCount = 0;
    let allowedTypes = [];

    if (type === 'BIKE') {
        availableCount = state.slotsData.filter(s => !s.occupied).length;
        allowedTypes = ['Small', 'Medium', 'Large'];
    } else if (type === 'CAR') {
        availableCount = state.slotsData.filter(s => !s.occupied && (s.slotType === 'MEDIUM' || s.slotType === 'LARGE')).length;
        allowedTypes = ['Medium', 'Large'];
    } else if (type === 'TRUCK') {
        availableCount = state.slotsData.filter(s => !s.occupied && s.slotType === 'LARGE').length;
        allowedTypes = ['Large only'];
    }

    if (availableCount > 0) {
        noticeEl.innerHTML = `<strong>${availableCount} suitable bays available</strong> for ${type} (${allowedTypes.join(' or ')}). Smallest fit allocated first.`;
        noticeBox.style.borderColor = 'var(--accent-lime-border)';
        noticeBox.style.backgroundColor = 'var(--accent-lime-subtle)';
        noticeBox.style.color = 'var(--accent-lime-text)';
    } else {
        noticeEl.innerHTML = `<strong>Warning: No slots available</strong> for ${type}! Facility is at maximum category capacity.`;
        noticeBox.style.borderColor = 'var(--status-occ-border)';
        noticeBox.style.backgroundColor = 'var(--status-occ-subtle)';
        noticeBox.style.color = 'var(--status-occ)';
    }
}

function renderNewlyIssuedTicket(ticket) {
    const container = document.getElementById('latest-ticket-content');
    if (!container) return;

    container.innerHTML = `
        <div class="issued-ticket-container">
            <div class="ticket-notch-top"></div>
            <div class="ticket-pass-header">
                <div>
                    <h4 style="font-size: 1.05rem; font-weight: 800; color: #ffffff;">PARKSMART PASS</h4>
                    <span style="font-size: 0.72rem; color: var(--text-muted); font-family: var(--font-mono);">${ticket.entryTime}</span>
                </div>
                <span class="chip chip-completed">Active Ticket</span>
            </div>

            <div class="ticket-slot-hero">
                <div class="ticket-slot-label">Reserved Parking Bay</div>
                <div class="ticket-slot-number">${ticket.slotNumber}</div>
                <span style="font-size: 0.75rem; color: var(--text-secondary);">${ticket.slotType} Size Zone</span>
            </div>

            <div class="ticket-meta-grid">
                <div class="ticket-data-row">
                    <span>Ticket Ref:</span>
                    <strong class="ticket-badge">${ticket.ticketId}</strong>
                </div>
                <div class="ticket-data-row">
                    <span>License Plate:</span>
                    <strong class="plate-badge" style="font-size: 0.95rem;">${ticket.vehicleRegistration}</strong>
                </div>
                <div class="ticket-data-row">
                    <span>Vehicle Category:</span>
                    <span class="chip ${getVehicleChipClass(ticket.vehicleType)}">${ticket.vehicleType}</span>
                </div>
                <div class="ticket-data-row">
                    <span>Driver / Owner:</span>
                    <strong>${ticket.ownerName}</strong>
                </div>
                <div class="ticket-data-row">
                    <span>Billing Tariff:</span>
                    <strong style="color: var(--accent-lime-text); font-family: var(--font-mono);">₹${ticket.hourlyRate}/hr</strong>
                </div>
            </div>

            <button class="btn btn-secondary btn-block" onclick="printSimpleTicket('${ticket.ticketId}', '${ticket.vehicleRegistration}', '${ticket.slotNumber}', '${ticket.entryTime}', '${ticket.vehicleType}', '${ticket.hourlyRate}')">
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 6 2 18 2 18 9"></polyline><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"></path><rect x="6" y="14" width="12" height="8"></rect></svg>
                <span>Print Parking Pass</span>
            </button>
        </div>
    `;
}

function printSimpleTicket(id, reg, slot, time, type, rate) {
    const w = window.open('', '_blank', 'width=420,height=520');
    w.document.write(`
        <html>
        <head>
            <title>ParkSmart Entry Pass - ${id}</title>
            <style>
                body { font-family: 'Courier New', monospace; padding: 24px; text-align: center; color: #000; background: #fff; }
                .ticket { border: 2px dashed #000; padding: 20px; border-radius: 8px; }
                h2 { margin: 0 0 4px 0; font-size: 22px; }
                .sub { font-size: 11px; color: #666; margin-bottom: 12px; }
                .slot-box { font-size: 28px; font-weight: bold; background: #eee; padding: 10px; margin: 12px 0; border: 1px solid #ccc; }
                .row { display: flex; justify-content: space-between; margin: 6px 0; font-size: 13px; }
                .footer { font-size: 11px; margin-top: 14px; border-top: 1px dashed #999; padding-top: 10px; }
            </style>
        </head>
        <body onload="window.print()">
            <div class="ticket">
                <h2>PARKSMART</h2>
                <div class="sub">Intelligent Parking Operations</div>
                <div class="slot-box">BAY ${slot}</div>
                <div class="row"><span>Ticket Ref:</span><strong>${id}</strong></div>
                <div class="row"><span>Plate:</span><strong>${reg}</strong></div>
                <div class="row"><span>Category:</span><strong>${type}</strong></div>
                <div class="row"><span>Tariff Rate:</span><strong>₹${rate}/hr</strong></div>
                <div class="row"><span>Check-In:</span><span>${time}</span></div>
                <div class="footer">
                    Retain this entry pass for checkout.<br>
                    Minimum 1 hr tariff applies.
                </div>
            </div>
        </body>
        </html>
    `);
    w.document.close();
}

// 4. ACTIVE PARKING
async function fetchActiveParking() {
    try {
        const res = await fetch('/api/parking/active');
        const json = await res.json();
        if (json.success && json.data) {
            state.activeParking = json.data;
            renderActiveParkingTable(json.data);
            const navActive = document.getElementById('nav-active-count');
            if (navActive) navActive.innerText = json.data.length;
        }
    } catch (err) {
        console.error('Failed to fetch active parking sessions:', err);
    }
}

function renderActiveParkingTable(activeList) {
    const tbody = document.getElementById('active-parking-tbody');
    if (!tbody) return;

    if (!activeList || activeList.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="empty-cell">No active parking sessions found. All slots are vacant.</td></tr>`;
        return;
    }

    tbody.innerHTML = activeList.map(t => {
        const durationStr = calculateLiveDuration(t.entryTime);
        return `
            <tr>
                <td><span class="ticket-badge">${t.ticketId}</span></td>
                <td><strong class="plate-badge">${t.vehicleRegistration}</strong></td>
                <td><span class="chip ${getVehicleChipClass(t.vehicleType)}">${t.vehicleType}</span></td>
                <td>${t.ownerName}</td>
                <td><span class="slot-pill">${t.slotNumber}</span></td>
                <td style="font-family: var(--font-mono); font-size: 0.78rem; color: var(--text-secondary);">${t.entryTime}</td>
                <td><span class="chip chip-active">${durationStr}</span></td>
                <td>
                    <button class="btn btn-sm btn-danger" onclick="openQuickExitModal('${t.vehicleRegistration}', '${t.slotNumber}')">
                        Settle & Exit
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function filterActiveParkingTable(query) {
    if (!query) {
        renderActiveParkingTable(state.activeParking);
        return;
    }
    const filtered = state.activeParking.filter(t => 
        (t.vehicleRegistration && t.vehicleRegistration.toLowerCase().includes(query)) ||
        (t.ticketId && t.ticketId.toLowerCase().includes(query)) ||
        (t.ownerName && t.ownerName.toLowerCase().includes(query)) ||
        (t.slotNumber && t.slotNumber.toLowerCase().includes(query))
    );
    renderActiveParkingTable(filtered);
}

function calculateLiveDuration(entryTimeStr) {
    if (!entryTimeStr) return '--';
    try {
        const entry = new Date(entryTimeStr.replace(' ', 'T'));
        const now = new Date();
        const diffMs = now - entry;
        const diffMins = Math.max(0, Math.floor(diffMs / 60000));
        const hrs = Math.floor(diffMins / 60);
        const mins = diffMins % 60;
        if (hrs === 0) return `${mins}m`;
        return `${hrs}h ${mins}m`;
    } catch (e) {
        return '< 1 min';
    }
}

// 5. VEHICLE EXIT & BILLING
function lookupVehicleForExit() {
    const input = document.getElementById('exit-search-input');
    const query = input.value.trim().toUpperCase();
    if (!query) {
        showToast('Please enter registration plate or ticket ID.', 'error');
        return;
    }

    const ticket = state.activeParking.find(t => 
        (t.vehicleRegistration && t.vehicleRegistration.toUpperCase() === query) || 
        (t.ticketId && t.ticketId.toUpperCase() === query)
    );

    if (!ticket) {
        showToast(`No active parking found for '${query}'.`, 'error');
        const previewBox = document.getElementById('exit-billing-preview');
        if (previewBox) previewBox.classList.add('hidden');
        return;
    }

    state.selectedExitTicket = ticket;
    showExitBillingPreview(ticket);
}

function showExitBillingPreview(ticket) {
    const previewBox = document.getElementById('exit-billing-preview');
    if (!previewBox) return;
    previewBox.classList.remove('hidden');

    const durationStr = calculateLiveDuration(ticket.entryTime);
    const entry = new Date(ticket.entryTime.replace(' ', 'T'));
    const now = new Date();
    const diffMins = Math.max(1, Math.floor((now - entry) / 60000));
    const billedHours = Math.max(1, Math.ceil(diffMins / 60));
    const rate = ticket.hourlyRate || 40.0;
    const estimatedFee = (billedHours * rate).toFixed(2);

    document.getElementById('preview-ticket-id').innerText = ticket.ticketId;
    document.getElementById('preview-reg-no').innerText = ticket.vehicleRegistration;
    document.getElementById('preview-type').innerText = `${ticket.vehicleType} (₹${rate}/hr)`;
    document.getElementById('preview-slot').innerText = ticket.slotNumber;
    document.getElementById('preview-entry-time').innerText = ticket.entryTime;
    document.getElementById('preview-now-time').innerText = new Date().toLocaleTimeString();
    document.getElementById('preview-duration').innerText = `${durationStr} (${billedHours} billed hour${billedHours > 1 ? 's' : ''})`;
    document.getElementById('preview-rate').innerText = `₹${rate} / hr`;
    document.getElementById('preview-total-fee').innerText = `₹${estimatedFee}`;
}

function cancelExitPreview() {
    const previewBox = document.getElementById('exit-billing-preview');
    if (previewBox) previewBox.classList.add('hidden');
    state.selectedExitTicket = null;
}

async function confirmVehicleExit() {
    if (!state.selectedExitTicket) return;
    const ticketId = state.selectedExitTicket.ticketId;

    try {
        const res = await fetch('/api/vehicles/exit', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ identifier: ticketId })
        });

        const json = await res.json();
        if (res.ok && json.success) {
            const receipt = json.data;
            showToast(`Vehicle ${receipt.vehicleRegistration} checkout settled: ₹${receipt.parkingFee.toFixed(2)}`, 'success');
            renderDigitalReceipt(receipt);
            cancelExitPreview();
            const input = document.getElementById('exit-search-input');
            if (input) input.value = '';
            loadAllData();
        } else {
            showToast(json.error || 'Failed to process vehicle exit.', 'error');
        }
    } catch (err) {
        showToast('Network error during vehicle checkout.', 'error');
        console.error(err);
    }
}

function renderDigitalReceipt(receipt) {
    const area = document.getElementById('receipt-printable-area');
    const printBtn = document.getElementById('btn-print-receipt');
    if (printBtn) printBtn.disabled = false;
    if (!area) return;

    area.innerHTML = `
        <div class="receipt-paper-wrap">
            <div class="receipt-header-center">
                <h2>PARKSMART</h2>
                <p>INTELLIGENT PARKING & VEHICLE MANAGEMENT</p>
                <p style="margin-top: 4px; font-weight: 700; color: #0f172a;">TAX INVOICE & OFFICIAL RECEIPT</p>
            </div>
            <div class="receipt-meta-section">
                <div class="receipt-row"><span>Ticket Reference:</span><strong>${receipt.ticketId}</strong></div>
                <div class="receipt-row"><span>Registration Plate:</span><strong>${receipt.vehicleRegistration}</strong></div>
                <div class="receipt-row"><span>Vehicle Category:</span><span>${receipt.vehicleType}</span></div>
                <div class="receipt-row"><span>Driver / Owner:</span><span>${receipt.ownerName}</span></div>
                <div class="receipt-row"><span>Allocated Bay:</span><strong>${receipt.slotNumber} (${receipt.slotType})</strong></div>
            </div>
            <div class="receipt-meta-section">
                <div class="receipt-row"><span>Check-In Timestamp:</span><span>${receipt.entryTime}</span></div>
                <div class="receipt-row"><span>Check-Out Timestamp:</span><span>${receipt.exitTime}</span></div>
                <div class="receipt-row"><span>Elapsed Duration:</span><span>${receipt.parkingDuration} (${receipt.durationMinutes} mins)</span></div>
                <div class="receipt-row"><span>Billed Units:</span><span>${receipt.billedHours} hr(s) (Ceiling rounded)</span></div>
                <div class="receipt-row"><span>Tariff Rate:</span><span>₹${receipt.hourlyRate.toFixed(2)} / hr</span></div>
            </div>
            <div class="receipt-grand-total">
                <span>TOTAL SETTLED</span>
                <span>₹${receipt.parkingFee.toFixed(2)}</span>
            </div>
            <div class="receipt-footer-center">
                <p>Status: Payment Settled at Counter (Cash/POS)</p>
                <p>Bay ${receipt.slotNumber} has been successfully released into available pool.</p>
                <p style="margin-top: 6px;">Thank you for parking with ParkSmart Facilities.</p>
            </div>
        </div>
    `;
}

function printReceipt() {
    window.print();
}

// 6. QUICK EXIT MODAL
function openQuickExitModal(regNumber, slotId) {
    const ticket = state.activeParking.find(t => t.vehicleRegistration === regNumber);
    if (!ticket) {
        showToast(`No active session found for ${regNumber}`, 'error');
        return;
    }

    const durationStr = calculateLiveDuration(ticket.entryTime);
    const entry = new Date(ticket.entryTime.replace(' ', 'T'));
    const now = new Date();
    const diffMins = Math.max(1, Math.floor((now - entry) / 60000));
    const billedHours = Math.max(1, Math.ceil(diffMins / 60));
    const rate = ticket.hourlyRate || 40.0;
    const fee = (billedHours * rate).toFixed(2);

    document.getElementById('modal-exit-reg').innerText = ticket.vehicleRegistration;
    document.getElementById('modal-exit-slot').innerText = `Bay ${slotId}`;
    document.getElementById('modal-exit-entry').innerText = ticket.entryTime;
    document.getElementById('modal-exit-duration').innerText = `${durationStr} (${billedHours}h billed)`;
    document.getElementById('modal-exit-rate').innerText = `₹${rate}/hr`;
    document.getElementById('modal-exit-fee').innerText = `₹${fee}`;

    const confirmBtn = document.getElementById('modal-btn-confirm-exit');
    confirmBtn.onclick = async () => {
        confirmBtn.disabled = true;
        confirmBtn.innerText = 'Settling...';
        try {
            const res = await fetch('/api/vehicles/exit', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ identifier: ticket.ticketId })
            });
            const json = await res.json();
            if (res.ok && json.success) {
                closeQuickExitModal();
                showToast(`Vehicle ${ticket.vehicleRegistration} exited. Settled: ₹${json.data.parkingFee.toFixed(2)}`, 'success');
                navigateTo('exit');
                renderDigitalReceipt(json.data);
                loadAllData();
            } else {
                showToast(json.error || 'Failed to process exit.', 'error');
            }
        } catch (e) {
            showToast('Network error during checkout.', 'error');
        } finally {
            confirmBtn.disabled = false;
            confirmBtn.innerText = 'Confirm & Settle';
        }
    };

    const modal = document.getElementById('quick-exit-modal');
    if (modal) modal.classList.remove('hidden');
}

function closeQuickExitModal() {
    const modal = document.getElementById('quick-exit-modal');
    if (modal) modal.classList.add('hidden');
}

// 7. PARKING HISTORY
async function fetchHistory() {
    try {
        const res = await fetch('/api/history');
        const json = await res.json();
        if (json.success && json.data) {
            state.historyData = json.data;
            renderHistoryTable(json.data);
        }
    } catch (err) {
        console.error('Failed to fetch history:', err);
    }
}

async function searchHistory(query) {
    try {
        const res = await fetch(`/api/history?search=${encodeURIComponent(query)}`);
        const json = await res.json();
        if (json.success && json.data) {
            state.historyData = json.data;
            filterHistoryTable();
        }
    } catch (err) {
        console.error('Failed to search history:', err);
    }
}

function filterHistoryTable() {
    const typeFilter = document.getElementById('history-type-filter').value;
    const statusFilter = document.getElementById('history-status-filter').value;
    const searchVal = document.getElementById('history-search-input').value.trim().toLowerCase();

    let list = state.historyData;

    if (typeFilter !== 'ALL') {
        list = list.filter(t => t.vehicleType === typeFilter);
    }

    if (statusFilter !== 'ALL') {
        list = list.filter(t => t.ticketStatus === statusFilter);
    }

    if (searchVal) {
        list = list.filter(t => 
            (t.ticketId && t.ticketId.toLowerCase().includes(searchVal)) ||
            (t.vehicleRegistration && t.vehicleRegistration.toLowerCase().includes(searchVal)) ||
            (t.ownerName && t.ownerName.toLowerCase().includes(searchVal)) ||
            (t.slotNumber && t.slotNumber.toLowerCase().includes(searchVal))
        );
    }

    renderHistoryTable(list);
}

function renderHistoryTable(list) {
    const tbody = document.getElementById('history-tbody');
    if (!tbody) return;

    if (!list || list.length === 0) {
        tbody.innerHTML = `<tr><td colspan="10" class="empty-cell">No matching parking records found in audit log.</td></tr>`;
        return;
    }

    tbody.innerHTML = list.map(t => {
        const isCompleted = t.ticketStatus === 'COMPLETED';
        const statusChip = isCompleted ? 'chip-completed' : 'chip-active';

        return `
            <tr>
                <td><span class="ticket-badge">${t.ticketId}</span></td>
                <td><strong class="plate-badge">${t.vehicleRegistration}</strong></td>
                <td><span class="chip ${getVehicleChipClass(t.vehicleType)}">${t.vehicleType}</span></td>
                <td><span class="slot-pill">${t.slotNumber}</span></td>
                <td>${t.ownerName || '--'}</td>
                <td style="font-family: var(--font-mono); font-size: 0.78rem; color: var(--text-secondary);">${t.entryTime || '--'}</td>
                <td style="font-family: var(--font-mono); font-size: 0.78rem; color: var(--text-secondary);">${t.exitTime || '<span style="color: var(--status-warn);">Active</span>'}</td>
                <td><span class="chip chip-active">${t.parkingDuration || '--'}</span></td>
                <td style="font-family: var(--font-mono); font-weight: 700; ${isCompleted ? 'color: var(--accent-lime-text);' : ''}">₹${(t.parkingFee || 0).toFixed(2)}</td>
                <td><span class="chip ${statusChip}">${t.ticketStatus}</span></td>
            </tr>
        `;
    }).join('');
}

// 8. REVENUE & REPORTS
async function fetchRevenue() {
    try {
        const res = await fetch('/api/revenue');
        const json = await res.json();
        if (json.success && json.data) {
            state.revenueData = json.data;
            renderRevenueReports(json.data);
        }
    } catch (err) {
        console.error('Failed to fetch revenue:', err);
    }
}

function renderRevenueReports(rev) {
    const elToday = document.getElementById('rev-stat-today');
    if (elToday) elToday.innerText = `₹${(rev.todayRevenue || 0).toFixed(2)}`;

    const elTotal = document.getElementById('rev-stat-total');
    if (elTotal) elTotal.innerText = `₹${(rev.totalRevenue || 0).toFixed(2)}`;

    const elSessions = document.getElementById('rev-stat-sessions');
    if (elSessions) elSessions.innerText = rev.totalCompletedSessions || 0;

    const elAvg = document.getElementById('rev-stat-avg');
    if (elAvg) elAvg.innerText = `₹${(rev.averageFeePerSession || 0).toFixed(2)}`;

    const byType = rev.revenueByType || { BIKE: 0, CAR: 0, TRUCK: 0 };
    const total = Math.max(1, rev.totalRevenue || 1); // avoid divide by zero

    const bikeRev = byType.BIKE || 0;
    const carRev = byType.CAR || 0;
    const truckRev = byType.TRUCK || 0;

    const elBikeVal = document.getElementById('rev-bike-val');
    if (elBikeVal) elBikeVal.innerText = `₹${bikeRev.toFixed(2)}`;
    const elCarVal = document.getElementById('rev-car-val');
    if (elCarVal) elCarVal.innerText = `₹${carRev.toFixed(2)}`;
    const elTruckVal = document.getElementById('rev-truck-val');
    if (elTruckVal) elTruckVal.innerText = `₹${truckRev.toFixed(2)}`;

    const elBikeBar = document.getElementById('rev-bike-bar');
    if (elBikeBar) elBikeBar.style.width = `${(bikeRev / total) * 100}%`;
    const elCarBar = document.getElementById('rev-car-bar');
    if (elCarBar) elCarBar.style.width = `${(carRev / total) * 100}%`;
    const elTruckBar = document.getElementById('rev-truck-bar');
    if (elTruckBar) elTruckBar.style.width = `${(truckRev / total) * 100}%`;
}

// ========================================================
// UTILITIES & NOTIFICATIONS
// ========================================================
function getVehicleChipClass(type) {
    if (!type) return 'chip-car';
    switch (type.toUpperCase()) {
        case 'BIKE': return 'chip-bike';
        case 'CAR': return 'chip-car';
        case 'TRUCK': return 'chip-truck';
        default: return 'chip-car';
    }
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    let iconSvg = '';
    if (type === 'success') {
        iconSvg = `<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="var(--accent-lime)" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>`;
    } else if (type === 'error') {
        iconSvg = `<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="var(--status-occ)" stroke-width="2.5"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>`;
    } else {
        iconSvg = `<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#38bdf8" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>`;
    }

    toast.innerHTML = `
        ${iconSvg}
        <span>${message}</span>
    `;

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(20px)';
        toast.style.transition = 'all 0.25s ease';
        setTimeout(() => toast.remove(), 250);
    }, 4000);
}
