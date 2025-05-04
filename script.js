// Datastrukturer för att lagra information
let tasks = [];
let employees = [
    { id: 1, name: "Anna Andersson", phone: "070-1234567" },
    { id: 2, name: "Bengt Bengtsson", phone: "070-7654321" }
];
let customers = [
    { id: 1, name: "Karl Karlsson", phone: "08-123456", address: "Storgatan 1" },
    { id: 2, name: "Lisa Larsson", phone: "08-654321", address: "Lillvägen 5" }
];
let equipment = [
    { id: 1, name: "Åkgräsklippare", dailyPrice: 500 },
    { id: 2, name: "Trimmer", dailyPrice: 200 }
];
let workDays = [];

// Hjälpfunktioner
function getCustomerName(id) {
    const customer = customers.find(c => c.id === id);
    return customer ? customer.name : 'Okänd kund';
}

function getEmployeeName(id) {
    const employee = employees.find(e => e.id === id);
    return employee ? employee.name : 'Okänd medarbetare';
}

function getTaskById(id) {
    return tasks.find(t => t.id === id);
}

function getWorkDaysForTask(taskId) {
    return workDays.filter(wd => wd.taskId === taskId);
}

function getEquipmentList(equipmentIds) {
    if (!equipmentIds || !equipmentIds.length) return 'Ingen';
    return equipmentIds.map(id => {
        const eq = equipment.find(e => e.id === id);
        return eq ? eq.name : 'Okänd';
    }).join(', ');
}

function convertTimeToMinutes(timeStr) {
    if (!timeStr) return 0;
    const [h, m] = timeStr.split(':').map(Number);
    return h * 60 + m;
}

function generateTimeOptions(selectedTime = '08:00') {
    let options = '';
    for (let h = 6; h < 20; h++) {
        for (let m = 0; m < 60; m += 15) {
            const time = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
            options += `<option value="${time}" ${time === selectedTime ? 'selected' : ''}>${time}</option>`;
        }
    }
    return options;
}

function getAvailableEmployees() {
    const selects = document.querySelectorAll('#employee-time-table .employee-select');
    const addedIds = Array.from(selects).map(select => select.value).filter(Boolean);
    return employees.filter(emp => !addedIds.includes(emp.id.toString()));
}

function updateEmployeeDropdowns() {
    const selects = document.querySelectorAll('#employee-time-table .employee-select');
    
    selects.forEach(select => {
        const currentValue = select.value;
        const availableEmployees = getAvailableEmployees();
        
        // Inkludera den nuvarande valda medarbetaren om den finns
        if (currentValue) {
            const currentEmployee = employees.find(e => e.id.toString() === currentValue);
            if (currentEmployee && !availableEmployees.some(e => e.id === currentEmployee.id)) {
                availableEmployees.push(currentEmployee);
            }
        }
        
        select.innerHTML = `
            <option value="" ${!currentValue ? 'selected disabled' : ''}>Välj medarbetare</option>
            ${availableEmployees.map(e => `
                <option value="${e.id}" ${e.id.toString() === currentValue ? 'selected' : ''}>
                    ${e.name}
                </option>
            `).join('')}
        `;
    });
}

// Funktioner för medarbetarhantering i registrera arbetsdag
function addEmployeeRow() {
    const tbody = document.getElementById('employee-time-table').querySelector('tbody');
    const availableEmployees = getAvailableEmployees();
    
    if (availableEmployees.length === 0) {
        alert('Alla medarbetare är redan tillagda');
        return;
    }

    // Hämta tider från första raden om den finns
    const firstRow = tbody.querySelector('tr');
    let defaultStart = '08:00';
    let defaultEnd = '16:00';
    let defaultLunch = '30';

    if (firstRow) {
        const firstStart = firstRow.querySelector('.start-time').value;
        const firstEnd = firstRow.querySelector('.end-time').value;
        const firstLunch = firstRow.querySelector('.lunch-minutes').value;
        
        if (firstStart) defaultStart = firstStart;
        if (firstEnd) defaultEnd = firstEnd;
        if (firstLunch) defaultLunch = firstLunch;
    }

    const row = document.createElement('tr');
    row.innerHTML = `
        <td>
            <select class="form-select employee-select">
                <option value="" selected disabled>Välj medarbetare</option>
                ${availableEmployees.map(e => `<option value="${e.id}">${e.name}</option>`).join('')}
            </select>
        </td>
        <td>
            <select class="form-control start-time">
                ${generateTimeOptions(defaultStart)}
            </select>
        </td>
        <td>
            <select class="form-control end-time">
                ${generateTimeOptions(defaultEnd)}
            </select>
        </td>
        <td>
            <select class="form-control lunch-minutes">
                <option value="0">0 min</option>
                <option value="15">15 min</option>
                <option value="30" ${defaultLunch === '30' ? 'selected' : ''}>30 min</option>
                <option value="45">45 min</option>
                <option value="60">60 min</option>
            </select>
        </td>
        <td><input type="checkbox" class="form-check-input is-driver"></td>
        <td><input type="number" class="form-control drive-time" min="0" step="0.25" value="0" disabled></td>
        <td class="worked-time">0 tim</td>
        <td><button type="button" class="btn btn-sm btn-outline-danger remove-employee">Ta bort</button></td>
    `;

    tbody.appendChild(row);
    attachRowEventHandlers(row);
    updateEmployeeDropdowns();
}

function attachRowEventHandlers(row) {
    const startSelect = row.querySelector('.start-time');
    const endSelect = row.querySelector('.end-time');
    const lunchSelect = row.querySelector('.lunch-minutes');
    const isDriver = row.querySelector('.is-driver');
    const driveTime = row.querySelector('.drive-time');
    const workedTime = row.querySelector('.worked-time');
    const employeeSelect = row.querySelector('.employee-select');
    const removeBtn = row.querySelector('.remove-employee');

    const calculateWorkTime = () => {
        if (!startSelect.value || !endSelect.value) return;
        
        const startMins = convertTimeToMinutes(startSelect.value);
        const endMins = convertTimeToMinutes(endSelect.value);
        const lunchMins = parseInt(lunchSelect.value) || 0;
        const driveMins = (parseFloat(driveTime.value) || 0) * 60; // Konvertera timmar till minuter
        
        if (endMins <= startMins) {
            workedTime.textContent = "Ogiltig tid";
            return;
        }
        
        // Beräkna total arbetad tid (inklusive körtid)
        const workMins = endMins - startMins - lunchMins;
        const totalMins = workMins + driveMins;
        const totalHours = totalMins / 60;
        
        workedTime.textContent = `${totalHours.toFixed(2)} tim`;
    };

    // Förarkontroll
    isDriver.addEventListener('change', () => {
        driveTime.disabled = !isDriver.checked;
        if (!isDriver.checked) driveTime.value = 0;
        calculateWorkTime();
    });

    // Tidsuppdatering
    [startSelect, endSelect, lunchSelect, driveTime].forEach(el => {
        el.addEventListener('change', calculateWorkTime);
    });

    employeeSelect.addEventListener('change', updateEmployeeDropdowns);

    removeBtn.addEventListener('click', () => {
        row.remove();
        updateEmployeeDropdowns();
    });

    // Beräkna initial tid
    calculateWorkTime();
}

function setSameStartTime() {
    const time = prompt('Ange starttid för alla (HH:MM):', '08:00');
    if (time) {
        document.querySelectorAll('#employee-time-table .start-time').forEach(select => {
            select.value = time;
            const event = new Event('change');
            select.dispatchEvent(event);
        });
    }
}

function setSameEndTime() {
    const time = prompt('Ange sluttid för alla (HH:MM):', '16:00');
    if (time) {
        document.querySelectorAll('#employee-time-table .end-time').forEach(select => {
            select.value = time;
            const event = new Event('change');
            select.dispatchEvent(event);
        });
    }
}

// Initiera dropdown-menyer
function initSelects() {
    // Kunddropdown
    const customerSelect = document.getElementById('customer');
    if (customerSelect) {
        customerSelect.innerHTML = '<option value="" selected disabled>Välj kund</option>' + 
            customers.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
    }
    
    // Uppdragsdropdown
    const taskSelect = document.getElementById('task-select');
    if (taskSelect) {
        taskSelect.innerHTML = '<option value="" selected disabled>Välj uppdrag</option>' + 
            tasks.map(t => `<option value="${t.id}">${t.number} - ${getCustomerName(t.customerId)}</option>`).join('');
    }
    
    // Arbetsledardropdown
    const supervisorSelect = document.getElementById('supervisor-select');
    if (supervisorSelect) {
        supervisorSelect.innerHTML = '<option value="none">Ingen</option>' + 
            employees.map(e => `<option value="${e.id}">${e.name}</option>`).join('');
    }
    
    // Utrustningscheckboxar
    const equipmentContainer = document.getElementById('equipment-container');
    if (equipmentContainer) {
        equipmentContainer.innerHTML = equipment.map(eq => `
            <div class="form-check form-check-inline equipment-checkbox">
                <input class="form-check-input" type="checkbox" id="eq-${eq.id}" value="${eq.id}">
                <label class="form-check-label" for="eq-${eq.id}">${eq.name}</label>
            </div>
        `).join('');
    }
    
    // Rapporter dropdowns
    const employeeSelect = document.getElementById('employee-select');
    if (employeeSelect) {
        employeeSelect.innerHTML = employees.map(e => `<option value="${e.id}">${e.name}</option>`).join('');
    }
    
    const reportCustomerSelect = document.getElementById('report-customer-select');
    if (reportCustomerSelect) {
        reportCustomerSelect.innerHTML = customers.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
    }
}

// Rendera listor
function renderActiveTasks() {
    const tbody = document.getElementById('active-tasks');
    if (!tbody) return;
    
    tbody.innerHTML = tasks.map(task => `
        <tr data-task-id="${task.id}" class="task-row" style="cursor: pointer;">
            <td>${task.number}</td>
            <td>${getCustomerName(task.customerId)}</td>
            <td>${task.startDate || 'Ej startad'}</td>
        </tr>
    `).join('');
    
    document.querySelectorAll('.task-row').forEach(row => {
        row.addEventListener('click', function() {
            const taskId = parseInt(this.dataset.taskId);
            showTaskDetails(taskId);
        });
    });
}

function renderEmployees() {
    const tbody = document.getElementById('employees-table');
    if (!tbody) return;
    
    tbody.innerHTML = employees.map(emp => `
        <tr>
            <td>${emp.id}</td>
            <td>${emp.name}</td>
            <td>${emp.phone}</td>
            <td>
                <button type="button" class="btn btn-sm btn-outline-danger" onclick="deleteEmployee(${emp.id})">Ta bort</button>
            </td>
        </tr>
    `).join('');
}

function renderEquipment() {
    const tbody = document.getElementById('equipment-table');
    if (!tbody) return;
    
    tbody.innerHTML = equipment.map(eq => `
        <tr>
            <td>${eq.name}</td>
            <td>${eq.dailyPrice} SEK</td>
            <td>
                <button type="button" class="btn btn-sm btn-outline-danger" onclick="deleteEquipment(${eq.id})">Ta bort</button>
            </td>
        </tr>
    `).join('');
}

// Visa detaljer om ett uppdrag
function showTaskDetails(taskId) {
    const task = getTaskById(taskId);
    const taskWorkDays = getWorkDaysForTask(taskId);
    
    if (!task) return;
    
    const totalHours = taskWorkDays.reduce((sum, wd) => {
        return sum + wd.employeeTimes.reduce((daySum, et) => {
            const startMins = convertTimeToMinutes(et.startTime);
            const endMins = convertTimeToMinutes(et.endTime);
            const lunchMins = parseInt(et.lunchMinutes) || 0;
            return daySum + ((endMins - startMins - lunchMins) / 60);
        }, 0);
    }, 0);
    
    const totalDriveHours = taskWorkDays.reduce((sum, wd) => {
        return sum + wd.employeeTimes.reduce((daySum, et) => {
            return daySum + (parseFloat(et.driveTime) || 0);
        }, 0);
    }, 0);
    
    const modalTitle = document.getElementById('taskDetailsModalLabel');
    const modalBody = document.getElementById('taskDetailsModalContent');
    
    modalTitle.textContent = `Uppdrag: ${task.number} - ${getCustomerName(task.customerId)}`;
    
    let daysHTML = '';
    if (taskWorkDays.length > 0) {
        daysHTML = `
            <h5>Registrerade arbetsdagar</h5>
            <table class="table table-striped">
                <thead>
                    <tr>
                        <th>Datum</th>
                        <th>Arbetsledare</th>
                        <th>Antal arbetare</th>
                        <th>Arbetstid</th>
                        <th>Körtid</th>
                        <th>Utrustning</th>
                        <th>Åtgärd</th>
                    </tr>
                </thead>
                <tbody>
                    ${taskWorkDays.map(day => {
                        const dayTotalHours = day.employeeTimes.reduce((sum, et) => {
                            const startMins = convertTimeToMinutes(et.startTime);
                            const endMins = convertTimeToMinutes(et.endTime);
                            const lunchMins = parseInt(et.lunchMinutes) || 0;
                            return sum + ((endMins - startMins - lunchMins) / 60);
                        }, 0);
                        
                        const dayDriveHours = day.employeeTimes.reduce((sum, et) => {
                            return sum + (parseFloat(et.driveTime) || 0);
                        }, 0);
                        
                        const supervisorName = day.supervisorId ? 
                            getEmployeeName(parseInt(day.supervisorId)) : 'Ingen';
                        
                        return `
                            <tr data-workday-id="${day.id}" class="workday-row" style="cursor: pointer;">
                                <td>${day.date}</td>
                                <td>${supervisorName}</td>
                                <td>${day.employeeTimes.length}</td>
                                <td>${dayTotalHours.toFixed(1)} timmar</td>
                                <td>${dayDriveHours.toFixed(1)} timmar</td>
                                <td>${getEquipmentList(day.equipmentIds)}</td>
                                <td>
                                    <button type="button" class="btn btn-sm btn-outline-primary view-day-btn" data-workday-id="${day.id}">
                                        Visa
                                    </button>
                                    <button type="button" class="btn btn-sm btn-outline-warning edit-day-btn" data-workday-id="${day.id}">
                                        Ändra
                                    </button>
                                </td>
                            </tr>
                        `;
                    }).join('')}
                </tbody>
            </table>
        `;
    } else {
        daysHTML = '<p class="text-muted">Inga arbetsdagar registrerade på detta uppdrag.</p>';
    }
    
    modalBody.innerHTML = `
        <div class="card mb-3">
            <div class="card-header">Uppdragssammanfattning</div>
            <div class="card-body">
                <p><strong>Uppdragsnummer:</strong> ${task.number}</p>
                <p><strong>Kund:</strong> ${getCustomerName(task.customerId)}</p>
                <p><strong>Startdatum:</strong> ${task.startDate}</p>
                <p><strong>Beskrivning:</strong> ${task.description || 'Ingen beskrivning'}</p>
                <p><strong>Total arbetstid:</strong> ${totalHours.toFixed(1)} timmar</p>
                <p><strong>Total körtid:</strong> ${totalDriveHours.toFixed(1)} timmar</p>
            </div>
        </div>
        ${daysHTML}
    `;
    
    setTimeout(() => {
        document.querySelectorAll('.view-day-btn').forEach(btn => {
            btn.addEventListener('click', function(e) {
                e.stopPropagation();
                const workdayId = parseInt(this.dataset.workdayId);
                showDayDetails(workdayId);
            });
        });
        
        document.querySelectorAll('.edit-day-btn').forEach(btn => {
            btn.addEventListener('click', function(e) {
                e.stopPropagation();
                const workdayId = parseInt(this.dataset.workdayId);
                editWorkDay(workdayId);
            });
        });
        
        document.querySelectorAll('.workday-row').forEach(row => {
            row.addEventListener('click', function() {
                const workdayId = parseInt(this.dataset.workdayId);
                showDayDetails(workdayId);
            });
        });
    }, 100);
    
    const taskDetailsModal = document.getElementById('taskDetailsModal');
    const bsTaskModal = bootstrap.Modal.getInstance(taskDetailsModal) || new bootstrap.Modal(taskDetailsModal);
    bsTaskModal.show();
}

// Visa detaljer om en arbetsdag
function showDayDetails(workdayId) {
    const workday = workDays.find(wd => wd.id === workdayId);
    if (!workday) return;
    
    const task = getTaskById(workday.taskId);
    
    const modalTitle = document.getElementById('dayDetailsModalLabel');
    const modalBody = document.getElementById('dayDetailsModalContent');
    
    modalTitle.textContent = `Arbetsdag: ${workday.date}`;
    
    const totalHours = workday.employeeTimes.reduce((sum, et) => {
        const startMins = convertTimeToMinutes(et.startTime);
        const endMins = convertTimeToMinutes(et.endTime);
        const lunchMins = parseInt(et.lunchMinutes) || 0;
        return sum + ((endMins - startMins - lunchMins) / 60);
    }, 0);
    
    const totalDriveHours = workday.employeeTimes.reduce((sum, et) => {
        return sum + (parseFloat(et.driveTime) || 0);
    }, 0);
    
    const supervisorName = workday.supervisorId ? 
        getEmployeeName(parseInt(workday.supervisorId)) : 'Ingen';
    
    modalBody.innerHTML = `
        <div class="card mb-3">
            <div class="card-header">Dagssammanfattning</div>
            <div class="card-body">
                <p><strong>Datum:</strong> ${workday.date}</p>
                <p><strong>Uppdrag:</strong> ${task ? `${task.number} - ${getCustomerName(task.customerId)}` : 'Okänt uppdrag'}</p>
                <p><strong>Arbetsledare:</strong> ${supervisorName}</p>
                <p><strong>Total arbetstid:</strong> ${totalHours.toFixed(1)} timmar</p>
                <p><strong>Total körtid:</strong> ${totalDriveHours.toFixed(1)} timmar</p>
                <p><strong>Utrustning:</strong> ${getEquipmentList(workday.equipmentIds)}</p>
            </div>
        </div>
        
        <h5>Medarbetare</h5>
        <table class="table table-striped">
            <thead>
                <tr>
                    <th>Namn</th>
                    <th>Starttid</th>
                    <th>Sluttid</th>
                    <th>Lunch (min)</th>
                    <th>Förare</th>
                    <th>Körtid (tim)</th>
                    <th>Arbetad tid</th>
                </tr>
            </thead>
            <tbody>
                ${workday.employeeTimes.map(et => {
                    const employee = employees.find(e => e.id === et.employeeId);
                    const startMins = convertTimeToMinutes(et.startTime);
                    const endMins = convertTimeToMinutes(et.endTime);
                    const lunchMins = parseInt(et.lunchMinutes) || 0;
                    const workHours = (endMins - startMins - lunchMins) / 60;
                    
                    return `
                        <tr>
                            <td>${employee ? employee.name : 'Okänd medarbetare'}</td>
                            <td>${et.startTime}</td>
                            <td>${et.endTime}</td>
                            <td>${et.lunchMinutes} min</td>
                            <td>${et.isDriver ? 'Ja' : 'Nej'}</td>
                            <td>${et.isDriver ? `${parseFloat(et.driveTime).toFixed(1)} tim` : '-'}</td>
                            <td>${workHours.toFixed(2)} tim</td>
                        </tr>
                    `;
                }).join('')}
            </tbody>
        </table>
        
        <div class="d-flex justify-content-end mt-3">
            <button type="button" class="btn btn-warning me-2" onclick="editWorkDay(${workdayId})">Ändra</button>
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Stäng</button>
        </div>
    `;
    
    const taskDetailsModal = document.getElementById('taskDetailsModal');
    const bsTaskModal = bootstrap.Modal.getInstance(taskDetailsModal);
    if (bsTaskModal) bsTaskModal.hide();
    
    const dayDetailsModal = document.getElementById('dayDetailsModal');
    const bsDayModal = bootstrap.Modal.getInstance(dayDetailsModal) || new bootstrap.Modal(dayDetailsModal);
    bsDayModal.show();
}

// Redigera en arbetsdag
function editWorkDay(workdayId) {
    const workday = workDays.find(wd => wd.id === workdayId);
    if (!workday) return;
    
    const modalTitle = document.getElementById('editDayModalLabel');
    const modalBody = document.getElementById('editDayModalContent');
    
    modalTitle.textContent = `Redigera arbetsdag: ${workday.date}`;
    
    modalBody.innerHTML = `
        <form id="edit-day-form" data-workday-id="${workdayId}">
            <div class="alert alert-warning">
                <strong>Varning:</strong> Ändringar av arbetsdag påverkar tidrapportering och fakturering.
            </div>
            
            <div class="row mb-3">
                <div class="col-md-4">
                    <label for="edit-work-date" class="form-label">Datum</label>
                    <input type="date" class="form-control" id="edit-work-date" required value="${workday.date}">
                </div>
                <div class="col-md-4">
                    <label for="edit-task-select" class="form-label">Uppdrag</label>
                    <select class="form-select" id="edit-task-select" required>
                        ${tasks.map(t => `
                            <option value="${t.id}" ${t.id === workday.taskId ? 'selected' : ''}>
                                ${t.number} - ${getCustomerName(t.customerId)}
                            </option>
                        `).join('')}
                    </select>
                </div>
                <div class="col-md-4">
                    <label for="edit-supervisor-select" class="form-label">Arbetsledare</label>
                    <select class="form-select" id="edit-supervisor-select">
                        <option value="none" ${!workday.supervisorId ? 'selected' : ''}>Ingen</option>
                        ${employees.map(e => `
                            <option value="${e.id}" ${workday.supervisorId === e.id.toString() ? 'selected' : ''}>
                                ${e.name}
                            </option>
                        `).join('')}
                    </select>
                </div>
            </div>
            
            <div class="mb-3">
                <label class="form-label">Utrustning</label>
                <div id="edit-equipment-container">
                    ${equipment.map(eq => `
                        <div class="form-check form-check-inline equipment-checkbox">
                            <input class="form-check-input" type="checkbox" id="edit-eq-${eq.id}" value="${eq.id}" 
                                ${workday.equipmentIds && workday.equipmentIds.includes(eq.id) ? 'checked' : ''}>
                            <label class="form-check-label" for="edit-eq-${eq.id}">${eq.name}</label>
                        </div>
                    `).join('')}
                </div>
            </div>
            
            <div class="mb-3">
                <h5>Medarbetare</h5>
                <table class="table" id="edit-employee-time-table">
                    <thead>
                        <tr>
                            <th>Namn</th>
                            <th>Starttid</th>
                            <th>Sluttid</th>
                            <th>Lunch (min)</th>
                            <th>Förare</th>
                            <th>Körtid (tim)</th>
                            <th>Arbetad tid</th>
                            <th>Åtgärd</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${workday.employeeTimes.map((et, index) => {
                            const employee = employees.find(e => e.id === et.employeeId);
                            const startMins = convertTimeToMinutes(et.startTime);
                            const endMins = convertTimeToMinutes(et.endTime);
                            const lunchMins = parseInt(et.lunchMinutes) || 0;
                            const workHours = (endMins - startMins - lunchMins) / 60;
                            
                            return `
                                <tr data-employee-id="${et.employeeId}" class="edit-employee-row">
                                    <td>
                                        <select class="form-select edit-employee-select" data-original-value="${et.employeeId}">
                                            <option value="${et.employeeId}" selected>${employee ? employee.name : 'Okänd'}</option>
                                        </select>
                                    </td>
                                    <td>
                                        <select class="form-control edit-start-time">
                                            ${generateTimeOptions(et.startTime)}
                                        </select>
                                    </td>
                                    <td>
                                        <select class="form-control edit-end-time">
                                            ${generateTimeOptions(et.endTime)}
                                        </select>
                                    </td>
                                    <td>
                                        <select class="form-control edit-lunch-minutes">
                                            <option value="0" ${et.lunchMinutes === '0' ? 'selected' : ''}>0 min</option>
                                            <option value="15" ${et.lunchMinutes === '15' ? 'selected' : ''}>15 min</option>
                                            <option value="30" ${et.lunchMinutes === '30' ? 'selected' : ''}>30 min</option>
                                            <option value="45" ${et.lunchMinutes === '45' ? 'selected' : ''}>45 min</option>
                                            <option value="60" ${et.lunchMinutes === '60' ? 'selected' : ''}>60 min</option>
                                        </select>
                                    </td>
                                    <td>
                                        <input type="checkbox" class="form-check-input edit-is-driver" ${et.isDriver ? 'checked' : ''}>
                                    </td>
                                    <td>
                                        <input type="number" class="form-control edit-drive-time" min="0" step="0.25" 
                                            value="${et.driveTime || 0}" ${!et.isDriver ? 'disabled' : ''}>
                                    </td>
                                    <td class="edit-worked-time">${workHours.toFixed(2)} tim</td>
                                    <td>
                                        <button type="button" class="btn btn-sm btn-outline-danger edit-remove-employee">Ta bort</button>
                                    </td>
                                </tr>
                            `;
                        }).join('')}
                    </tbody>
                </table>
                <button type="button" class="btn btn-workday" id="edit-add-employee-btn">Ny medarbetare</button>
                <button type="button" class="btn btn-workday" id="edit-same-start-btn">Start samtidigt</button>
                <button type="button" class="btn btn-workday" id="edit-same-end-btn">Slut samtidigt</button>
            </div>
            
            <div class="d-flex justify-content-between mt-4">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Avbryt</button>
                <button type="submit" class="btn btn-primary">Spara ändringar</button>
            </div>
        </form>
    `;
    
    const taskDetailsModal = document.getElementById('taskDetailsModal');
    const bsTaskModal = bootstrap.Modal.getInstance(taskDetailsModal);
    if (bsTaskModal) bsTaskModal.hide();
    
    const dayDetailsModal = document.getElementById('dayDetailsModal');
    const bsDayModal = bootstrap.Modal.getInstance(dayDetailsModal);
    if (bsDayModal) bsDayModal.hide();
    
    const editDayModal = document.getElementById('editDayModal');
    const bsEditModal = bootstrap.Modal.getInstance(editDayModal) || new bootstrap.Modal(editDayModal);
    bsEditModal.show();
    
    setTimeout(() => {
        document.getElementById('edit-day-form').addEventListener('submit', function(e) {
            e.preventDefault();
            
            if (!confirm('Är du säker på att du vill ändra denna arbetsdag?')) return;
            if (!confirm('Är du verkligen säker? Denna åtgärd påverkar tidrapportering och fakturering.')) return;
            
            const workdayId = parseInt(this.dataset.workdayId);
            const date = document.getElementById('edit-work-date').value;
            const taskId = parseInt(document.getElementById('edit-task-select').value);
            const supervisorId = document.getElementById('edit-supervisor-select').value;
            
            const equipmentIds = Array.from(document.querySelectorAll('#edit-equipment-container input[type="checkbox"]:checked'))
                .map(cb => parseInt(cb.value));
            
            const employeeTimes = [];
            document.querySelectorAll('#edit-employee-time-table .edit-employee-row').forEach(row => {
                const employeeId = parseInt(row.querySelector('.edit-employee-select').value);
                const startTime = row.querySelector('.edit-start-time').value;
                const endTime = row.querySelector('.edit-end-time').value;
                const lunchMinutes = row.querySelector('.edit-lunch-minutes').value;
                const isDriver = row.querySelector('.edit-is-driver').checked;
                const driveTime = row.querySelector('.edit-drive-time').value;
                
                employeeTimes.push({
                    employeeId: employeeId,
                    startTime: startTime,
                    endTime: endTime,
                    lunchMinutes: lunchMinutes,
                    isDriver: isDriver,
                    driveTime: isDriver ? driveTime : 0
                });
            });
            
            const index = workDays.findIndex(wd => wd.id === workdayId);
            if (index !== -1) {
                workDays[index] = {
                    ...workDays[index],
                    date: date,
                    taskId: taskId,
                    supervisorId: supervisorId === 'none' ? null : supervisorId,
                    equipmentIds: equipmentIds,
                    employeeTimes: employeeTimes
                };
                
                alert('Arbetsdag uppdaterad!');
                bsEditModal.hide();
                showDayDetails(workdayId);
            }
        });
        
        document.getElementById('edit-add-employee-btn').addEventListener('click', addEmployeeToEditForm);
        document.getElementById('edit-same-start-btn').addEventListener('click', setSameStartTimeForEdit);
        document.getElementById('edit-same-end-btn').addEventListener('click', setSameEndTimeForEdit);
        
        document.querySelectorAll('.edit-employee-row').forEach(row => {
            attachEditRowEventHandlers(row);
        });
        
        updateEditEmployeeDropdowns();
    }, 100);
}

function addEmployeeToEditForm() {
    const tbody = document.getElementById('edit-employee-time-table').querySelector('tbody');
    const rows = tbody.querySelectorAll('tr');
    
    let defaultStart = '08:00';
    let defaultEnd = '16:00';
    let defaultLunch = '30';
    
    if (rows.length > 0) {
        const firstRow = rows[0];
        defaultStart = firstRow.querySelector('.edit-start-time').value || defaultStart;
        defaultEnd = firstRow.querySelector('.edit-end-time').value || defaultEnd;
        defaultLunch = firstRow.querySelector('.edit-lunch-minutes').value || defaultLunch;
    }
    
    const editRows = Array.from(document.querySelectorAll('#edit-employee-time-table .edit-employee-select'));
    const selectedEmployeeIds = editRows.map(select => select.value).filter(Boolean);
    const availableEmployees = employees.filter(emp => !selectedEmployeeIds.includes(emp.id.toString()));
    
    if (availableEmployees.length === 0) {
        alert('Alla medarbetare är redan tillagda');
        return;
    }
    
    const row = document.createElement('tr');
    row.className = 'edit-employee-row';
    row.dataset.employeeId = '';
    row.innerHTML = `
        <td>
            <select class="form-select edit-employee-select">
                <option value="" selected disabled>Välj medarbetare</option>
                ${availableEmployees.map(e => `<option value="${e.id}">${e.name}</option>`).join('')}
            </select>
        </td>
        <td>
            <select class="form-control edit-start-time">
                ${generateTimeOptions(defaultStart)}
            </select>
        </td>
        <td>
            <select class="form-control edit-end-time">
                ${generateTimeOptions(defaultEnd)}
            </select>
        </td>
        <td>
            <select class="form-control edit-lunch-minutes">
                <option value="0">0 min</option>
                <option value="15">15 min</option>
                <option value="30" ${defaultLunch === '30' ? 'selected' : ''}>30 min</option>
                <option value="45">45 min</option>
                <option value="60">60 min</option>
            </select>
        </td>
        <td><input type="checkbox" class="form-check-input edit-is-driver"></td>
        <td><input type="number" class="form-control edit-drive-time" min="0" step="0.25" value="0" disabled></td>
        <td class="edit-worked-time">0 tim</td>
        <td><button type="button" class="btn btn-sm btn-outline-danger edit-remove-employee">Ta bort</button></td>
    `;
    
    tbody.appendChild(row);
    attachEditRowEventHandlers(row);
    updateEditEmployeeDropdowns();
}

function attachEditRowEventHandlers(row) {
    const startSelect = row.querySelector('.edit-start-time');
    const endSelect = row.querySelector('.edit-end-time');
    const lunchSelect = row.querySelector('.edit-lunch-minutes');
    const isDriver = row.querySelector('.edit-is-driver');
    const driveTime = row.querySelector('.edit-drive-time');
    const workedTime = row.querySelector('.edit-worked-time');
    const employeeSelect = row.querySelector('.edit-employee-select');
    const removeBtn = row.querySelector('.edit-remove-employee');

    const calculateWorkTime = () => {
        if (!startSelect.value || !endSelect.value) return;
        
        const startMins = convertTimeToMinutes(startSelect.value);
        const endMins = convertTimeToMinutes(endSelect.value);
        const lunchMins = parseInt(lunchSelect.value) || 0;
        const driveMins = (parseFloat(driveTime.value) || 0) * 60; // Konvertera timmar till minuter
        
        if (endMins <= startMins) {
            workedTime.textContent = "Ogiltig tid";
            return;
        }
        
        // Beräkna total arbetad tid (inklusive körtid)
        const workMins = endMins - startMins - lunchMins;
        const totalMins = workMins + driveMins;
        const totalHours = totalMins / 60;
        
        workedTime.textContent = `${totalHours.toFixed(2)} tim`;
    };

    isDriver.addEventListener('change', () => {
        driveTime.disabled = !isDriver.checked;
        if (!isDriver.checked) driveTime.value = 0;
        calculateWorkTime();
    });

    [startSelect, endSelect, lunchSelect, driveTime].forEach(el => {
        el.addEventListener('change', calculateWorkTime);
    });

    employeeSelect.addEventListener('change', function() {
        if (this.value) {
            row.dataset.employeeId = this.value;
            updateEditEmployeeDropdowns();
        }
    });

    removeBtn.addEventListener('click', () => {
        row.remove();
        updateEditEmployeeDropdowns();
    });

    calculateWorkTime();
}

function setSameStartTimeForEdit() {
    const time = prompt('Ange starttid (HH:MM):', '08:00');
    if (time) {
        document.querySelectorAll('.edit-start-time').forEach(input => {
            input.value = time;
            const event = new Event('change');
            input.dispatchEvent(event);
        });
    }
}

function setSameEndTimeForEdit() {
    const time = prompt('Ange sluttid (HH:MM):', '16:00');
    if (time) {
        document.querySelectorAll('.edit-end-time').forEach(input => {
            input.value = time;
            const event = new Event('change');
            input.dispatchEvent(event);
        });
    }
}

function updateEditEmployeeDropdowns() {
    const selects = document.querySelectorAll('#edit-employee-time-table .edit-employee-select');
    
    selects.forEach(select => {
        const currentValue = select.value;
        const originalValue = select.dataset.originalValue;
        
        const preserveIds = originalValue ? [originalValue] : [];
        
        const otherSelects = Array.from(selects).filter(s => s !== select);
        const otherSelectedIds = otherSelects
            .map(s => s.value)
            .filter(Boolean);
        
        const availableForThisSelect = employees.filter(
            emp => !otherSelectedIds.includes(emp.id.toString()) || 
                  preserveIds.includes(emp.id.toString())
        );
        
        let options = `<option value="" ${!currentValue ? 'selected disabled' : ''}>Välj medarbetare</option>`;
        options += availableForThisSelect.map(e => `
            <option value="${e.id}" ${e.id.toString() === currentValue ? 'selected' : ''}>
                ${e.name}
            </option>
        `).join('');
        
        select.innerHTML = options;
    });
}

// Hantera formulär
function handleNewTask(e) {
    e.preventDefault();
    const taskNumber = document.getElementById('task-number').value;
    const customerId = parseInt(document.getElementById('customer').value);
    const description = document.getElementById('task-description').value;
    
    if (!taskNumber || !customerId) {
        alert('Uppdragsnummer och kund måste anges');
        return;
    }
    
    const newTask = {
        id: tasks.length + 1,
        number: taskNumber,
        customerId: customerId,
        description: description,
        startDate: new Date().toISOString().split('T')[0],
        status: 'active'
    };
    
    tasks.push(newTask);
    renderActiveTasks();
    initSelects();
    e.target.reset();
    alert(`Uppdrag ${taskNumber} skapat för ${getCustomerName(customerId)}`);
}

function handleRegisterDay(e) {
    e.preventDefault();
    const date = document.getElementById('work-date').value;
    const taskId = parseInt(document.getElementById('task-select').value);
    const supervisorId = document.getElementById('supervisor-select').value;
    
    if (!date || !taskId) {
        alert('Datum och uppdrag måste anges');
        return;
    }
    
    const equipmentIds = Array.from(document.querySelectorAll('#equipment-container input[type="checkbox"]:checked'))
        .map(cb => parseInt(cb.value));
    
    const employeeTimes = [];
    document.querySelectorAll('#employee-time-table tbody tr').forEach(row => {
        const employeeId = parseInt(row.querySelector('.employee-select').value);
        if (!employeeId) return;
        
        const startTime = row.querySelector('.start-time').value;
        const endTime = row.querySelector('.end-time').value;
        const lunchMinutes = row.querySelector('.lunch-minutes').value;
        const isDriver = row.querySelector('.is-driver').checked;
        const driveTime = row.querySelector('.drive-time').value;
        
        employeeTimes.push({
            employeeId: employeeId,
            startTime: startTime,
            endTime: endTime,
            lunchMinutes: lunchMinutes,
            isDriver: isDriver,
            driveTime: isDriver ? driveTime : 0
        });
    });
    
    if (employeeTimes.length === 0) {
        alert('Du måste lägga till minst en medarbetare');
        return;
    }
    
    const newWorkDay = {
        id: workDays.length + 1,
        date: date,
        taskId: taskId,
        supervisorId: supervisorId === 'none' ? null : supervisorId,
        equipmentIds: equipmentIds,
        employeeTimes: employeeTimes
    };
    
    workDays.push(newWorkDay);
    alert('Arbetsdag registrerad!');
    e.target.reset();
    
    // Återställ medarbetartabellen
    const tbody = document.getElementById('employee-time-table').querySelector('tbody');
    tbody.innerHTML = '';
    addEmployeeRow(); // Lägg till en tom rad
}

function saveNewCustomer() {
    const name = document.getElementById('customer-name').value;
    if (!name) {
        alert('Namn måste anges');
        return;
    }
    
    const newCustomer = {
        id: customers.length + 1,
        name: name,
        phone: document.getElementById('customer-phone').value,
        address: document.getElementById('customer-address').value
    };
    
    customers.push(newCustomer);
    initSelects();
    
    const customerModal = document.getElementById('addCustomerModal');
    const bsCustomerModal = bootstrap.Modal.getInstance(customerModal);
    if (bsCustomerModal) bsCustomerModal.hide();
    
    document.getElementById('add-customer-form').reset();
    alert(`Kund ${name} tillagd`);
}

function saveNewEmployee() {
    const name = document.getElementById('employee-name').value;
    if (!name) {
        alert('Namn måste anges');
        return;
    }
    
    const newEmployee = {
        id: employees.length + 1,
        name: name,
        phone: document.getElementById('employee-phone').value
    };
    
    employees.push(newEmployee);
    renderEmployees();
    initSelects();
    
    const employeeModal = document.getElementById('addEmployeeModal');
    const bsEmployeeModal = bootstrap.Modal.getInstance(employeeModal);
    if (bsEmployeeModal) bsEmployeeModal.hide();
    
    document.getElementById('add-employee-form').reset();
    alert(`Medarbetare ${name} tillagd`);
}

function deleteEmployee(id) {
    if (!confirm('Är du säker på att du vill ta bort denna medarbetare?')) return;
    
    const index = employees.findIndex(e => e.id === id);
    if (index !== -1) {
        employees.splice(index, 1);
        renderEmployees();
        initSelects();
        alert('Medarbetare borttagen');
    }
}

function handleNewEquipment(e) {
    e.preventDefault();
    const name = document.getElementById('equipment-name').value;
    const dailyPrice = parseFloat(document.getElementById('equipment-price').value);
    
    if (!name || isNaN(dailyPrice)) {
        alert('Namn och pris måste anges');
        return;
    }
    
    const newEquipment = {
        id: equipment.length + 1,
        name: name,
        dailyPrice: dailyPrice
    };
    
    equipment.push(newEquipment);
    renderEquipment();
    initSelects();
    e.target.reset();
    alert(`Utrustning ${name} tillagd`);
}

function deleteEquipment(id) {
    if (!confirm('Är du säker på att du vill ta bort denna utrustning?')) return;
    
    const index = equipment.findIndex(e => e.id === id);
    if (index !== -1) {
        equipment.splice(index, 1);
        renderEquipment();
        initSelects();
        alert('Utrustning borttagen');
    }
}

// Rapporter
function handleTimeSearch(e) {
    e.preventDefault();
    const searchType = document.querySelector('input[name="searchType"]:checked').value;
    const date = document.getElementById('report-date').value;
    
    let results = '';
    if (searchType === 'employee') {
        const employeeId = parseInt(document.getElementById('employee-select').value);
        const employee = employees.find(e => e.id === employeeId);
        const workHours = calculateEmployeeWorkHours(employeeId, date);
        
        results = `
            <h5>Arbetstid för ${employee.name}</h5>
            <p>Datum: ${date || 'Alla datum'}</p>
            <table class="table">
                <thead>
                    <tr>
                        <th>Uppdrag</th>
                        <th>Arbetade timmar</th>
                        <th>Körtid</th>
                    </tr>
                </thead>
                <tbody>
                    ${workHours.map(wh => `
                        <tr>
                            <td>${wh.taskNumber}</td>
                            <td>${wh.totalHours.toFixed(1)} timmar</td>
                            <td>${wh.driveTime.toFixed(1)} timmar</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
    } else {
        const customerId = parseInt(document.getElementById('report-customer-select').value);
        const customer = customers.find(c => c.id === customerId);
        const customerWork = calculateCustomerWork(customerId, date);
        
        results = `
            <h5>Arbetstid för ${customer.name}</h5>
            <p>Datum: ${date || 'Alla datum'}</p>
            <table class="table">
                <thead>
                    <tr>
                        <th>Datum</th>
                        <th>Uppdrag</th>
                        <th>Total tid</th>
                        <th>Antal arbetare</th>
                    </tr>
                </thead>
                <tbody>
                    ${customerWork.map(cw => `
                        <tr>
                            <td>${cw.date}</td>
                            <td>${cw.taskNumber}</td>
                            <td>${cw.totalHours.toFixed(1)} timmar</td>
                            <td>${cw.employeeCount}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
    }
    
    document.getElementById('report-results').innerHTML = results;
}

function handleMonthlyReport(e) {
    e.preventDefault();
    const month = document.getElementById('report-month').value;
    
    const monthlyReport = employees.map(emp => {
        const workData = calculateEmployeeWorkHours(emp.id, null, month);
        const totalHours = workData.reduce((sum, wh) => sum + wh.totalHours, 0);
        const totalDriveHours = workData.reduce((sum, wh) => sum + wh.driveTime, 0);
        
        return {
            employee: emp.name,
            totalHours,
            totalDriveHours
        };
    });
    
    const results = `
        <h5>Månadsrapport för ${month}</h5>
        <table class="table">
            <thead>
                <tr>
                    <th>Medarbetare</th>
                    <th>Total arbetstid</th>
                    <th>Körtid</th>
                </tr>
            </thead>
            <tbody>
                ${monthlyReport.map(mr => `
                    <tr>
                        <td>${mr.employee}</td>
                        <td>${mr.totalHours.toFixed(1)} timmar</td>
                        <td>${mr.totalDriveHours.toFixed(1)} timmar</td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
    
    document.getElementById('report-results').innerHTML = results;
}

function calculateEmployeeWorkHours(employeeId, dateFilter = null, monthFilter = null) {
    return workDays
        .filter(wd => {
            const matchesDate = !dateFilter || wd.date === dateFilter;
            const matchesMonth = !monthFilter || wd.date.startsWith(monthFilter);
            return matchesDate && matchesMonth;
        })
        .filter(wd => wd.employeeTimes.some(et => et.employeeId === employeeId))
        .map(wd => {
            const employeeTime = wd.employeeTimes.find(et => et.employeeId === employeeId);
            const task = tasks.find(t => t.id === wd.taskId);
            
            const startMins = convertTimeToMinutes(employeeTime.startTime);
            const endMins = convertTimeToMinutes(employeeTime.endTime);
            const lunchMins = parseInt(employeeTime.lunchMinutes) || 0;
            const workHours = (endMins - startMins - lunchMins) / 60;
            
            return {
                date: wd.date,
                taskNumber: task ? task.number : 'Okänt uppdrag',
                totalHours: workHours,
                driveTime: parseFloat(employeeTime.driveTime) || 0
            };
        });
}

function calculateCustomerWork(customerId, dateFilter = null) {
    return workDays
        .filter(wd => !dateFilter || wd.date === dateFilter)
        .map(wd => {
            const task = tasks.find(t => t.id === wd.taskId && t.customerId === customerId);
            if (!task) return null;
            
            const totalHours = wd.employeeTimes.reduce((sum, et) => {
                const startMins = convertTimeToMinutes(et.startTime);
                const endMins = convertTimeToMinutes(et.endTime);
                const lunchMins = parseInt(et.lunchMinutes) || 0;
                return sum + ((endMins - startMins - lunchMins) / 60);
            }, 0);
            
            return {
                date: wd.date,
                taskNumber: task.number,
                totalHours,
                employeeCount: wd.employeeTimes.length
            };
        })
        .filter(Boolean);
}

function toggleSearchType() {
    const type = document.querySelector('input[name="searchType"]:checked').value;
    document.getElementById('employee-search-container').classList.toggle('d-none', type !== 'employee');
    document.getElementById('customer-search-container').classList.toggle('d-none', type !== 'customer');
}

// Initialisering
document.addEventListener('DOMContentLoaded', function() {
    initSelects();
    renderActiveTasks();
    renderEmployees();
    renderEquipment();
    
    // Lägg till första medarbetarraden
    addEmployeeRow();
    
    // Formulärhanterare
    document.getElementById('new-task-form').addEventListener('submit', handleNewTask);
    document.getElementById('register-day-form').addEventListener('submit', handleRegisterDay);
    document.getElementById('new-equipment-form').addEventListener('submit', handleNewEquipment);
    document.getElementById('search-time-form').addEventListener('submit', handleTimeSearch);
    document.getElementById('monthly-report-form').addEventListener('submit', handleMonthlyReport);
    
    // Knapphanterare
    document.getElementById('add-employee-btn').addEventListener('click', addEmployeeRow);
    document.getElementById('same-start-btn').addEventListener('click', setSameStartTime);
    document.getElementById('same-end-btn').addEventListener('click', setSameEndTime);
    document.getElementById('save-customer-btn').addEventListener('click', saveNewCustomer);
    document.getElementById('save-employee-btn').addEventListener('click', saveNewEmployee);
    
    // Växling av söktyp
    document.querySelectorAll('input[name="searchType"]').forEach(radio => {
        radio.addEventListener('change', toggleSearchType);
    });
    
    // Initialisera modaler
    [
        'addEmployeeModal',
        'addCustomerModal',
        'taskDetailsModal',
        'dayDetailsModal',
        'editDayModal'
    ].forEach(modalId => {
        const modalEl = document.getElementById(modalId);
        if (modalEl) new bootstrap.Modal(modalEl);
    });
});

// Gör funktioner tillgängliga globalt för HTML-attribut
window.deleteEmployee = deleteEmployee;
window.deleteEquipment = deleteEquipment;
window.editWorkDay = editWorkDay;