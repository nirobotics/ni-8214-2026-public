import { NT4_Client } from "./nt4.js";

const TABLE_LIST_TOPIC = '/shootingcalculator/tableList';
const CURRENT_TABLE_ID_TOPIC = '/shootingcalculator/currentTableId';
const CURRENT_DISTANCE_TOPIC = '/shootingcalculator/currentDistance';

class ShootingCalculator {
    constructor() {
        this.nt4Client = null;
        this.chart = null;

        // 动态表管理
        this.registeredTables = []; // [{id, method}, ...]
        this.tableDataMap = {}; // {tableId: [{distance, xVel, yVel}, ...]}
        this.undoStackMap = {}; // {tableId: [history]}
        this.redoStackMap = {}; // {tableId: [history]}

        this.currentDistance = 0;
        // 支持多个shooter的速度数据: { shooterName: { xVel, yVel } }
        this.shooterVelocities = {};
        this.isConnected = false;
        this.currentDistanceAnnotation = null;
        this.hasUnappliedChanges = false;
        this.hoverLineX = null;
        this.viewingTableId = null; // Which table we're viewing/editing
        this.robotTableId = null; // Which table robot is using

        // Performance optimization: RAF throttling
        this.rafPending = false;
        this.pendingChartUpdate = false;
        this.pendingSpeedUpdate = false;

        // Cache DOM elements
        this.cachedElements = {};

        this.init();
    }

    init() {
        this.setupChart();
        this.setupEventListeners();
        this.setupModalEventListeners();
        this.connectToNetworkTables();
    }

    getCurrentTableData() {
        if (!this.viewingTableId || !this.tableDataMap[this.viewingTableId]) {
            return [];
        }
        return this.tableDataMap[this.viewingTableId];
    }

    // Get cached DOM element
    getElement(id) {
        if (!this.cachedElements[id]) {
            this.cachedElements[id] = document.getElementById(id);
        }
        return this.cachedElements[id];
    }

    // Schedule chart update using RAF for better performance
    scheduleChartUpdate(updateSpeed = false) {
        if (updateSpeed) {
            this.pendingSpeedUpdate = true;
        } else {
            this.pendingChartUpdate = true;
        }

        if (this.rafPending) {
            return; // Already scheduled
        }

        this.rafPending = true;
        requestAnimationFrame(() => {
            this.rafPending = false;

            // Execute both updates if both are pending
            if (this.pendingChartUpdate) {
                this.pendingChartUpdate = false;
                this.updateChart();
            } else if (this.pendingSpeedUpdate) {
                this.pendingSpeedUpdate = false;
                this.updateCurrentSpeedPoint();
            }
        });
    }

    getCurrentTableMethod() {
        if (!this.viewingTableId) {
            return null;
        }
        const table = this.registeredTables.find(t => t.id === this.viewingTableId);
        return table ? table.method : null;
    }

    getCurrentUndoStack() {
        if (!this.viewingTableId || !this.undoStackMap[this.viewingTableId]) {
            return [];
        }
        return this.undoStackMap[this.viewingTableId];
    }

    getCurrentRedoStack() {
        if (!this.viewingTableId || !this.redoStackMap[this.viewingTableId]) {
            return [];
        }
        return this.redoStackMap[this.viewingTableId];
    }

    setupChart() {
        const ctx = document.getElementById('interpolationChart').getContext('2d');

        // Create custom plugin for hover line
        const hoverLinePlugin = {
            id: 'hoverLine',
            afterDraw: (chart) => {
                const ctx = chart.ctx;
                const xScale = chart.scales.x;
                const yScale = chart.scales.y;

                // Draw hover line (black dashed)
                if (this.hoverLineX !== null) {
                    const xPixel = xScale.getPixelForValue(this.hoverLineX);
                    ctx.save();
                    ctx.setLineDash([5, 5]);
                    ctx.strokeStyle = 'rgba(0, 0, 0, 0.7)';
                    ctx.lineWidth = 2;
                    ctx.beginPath();
                    ctx.moveTo(xPixel, yScale.top);
                    ctx.lineTo(xPixel, yScale.bottom);
                    ctx.stroke();
                    ctx.restore();
                }

                // Draw current distance line (shared across all tables)
                if (this.currentDistance > 0) {
                    const xPixel = xScale.getPixelForValue(this.currentDistance);
                    ctx.save();
                    ctx.setLineDash([5, 5]);
                    ctx.strokeStyle = 'rgba(40, 167, 69, 0.9)';
                    ctx.lineWidth = 3;
                    ctx.beginPath();
                    ctx.moveTo(xPixel, yScale.top);
                    ctx.lineTo(xPixel, yScale.bottom);
                    ctx.stroke();
                    ctx.restore();
                }

                // Draw horizontal dashed lines for current velocities of all shooters
                const shooterNames = Object.keys(this.shooterVelocities);
                shooterNames.forEach((shooterName, index) => {
                    const vel = this.shooterVelocities[shooterName];
                    if (!vel) return;

                    // Use different colors for different shooters (cycle through colors)
                    const colors = [
                        { x: 'rgba(255, 99, 132, 0.8)', y: 'rgba(54, 162, 235, 0.8)' },
                        { x: 'rgba(255, 159, 64, 0.8)', y: 'rgba(75, 192, 192, 0.8)' },
                        { x: 'rgba(153, 102, 255, 0.8)', y: 'rgba(255, 205, 86, 0.8)' }
                    ];
                    const colorSet = colors[index % colors.length];

                    // Current X velocity horizontal line
                    if (vel.xVel !== undefined && vel.xVel >= 0) {
                        const xVelPixel = yScale.getPixelForValue(vel.xVel);
                        ctx.save();
                        ctx.setLineDash([8, 4]);
                        ctx.strokeStyle = colorSet.x;
                        ctx.lineWidth = 2;
                        ctx.beginPath();
                        ctx.moveTo(xScale.left, xVelPixel);
                        ctx.lineTo(xScale.right, xVelPixel);
                        ctx.stroke();
                        ctx.restore();

                        // Draw label for X velocity with offset
                        const xLabelOffset = xScale.left + 5 + (index * 150); // Horizontal offset for different shooters
                        const yLabelOffset = xVelPixel - 12; // Offset upward
                        ctx.save();
                        ctx.fillStyle = colorSet.x;
                        ctx.font = 'bold 12px Arial';
                        ctx.fillText(`${shooterName} X: ${vel.xVel.toFixed(3)}`, xLabelOffset, yLabelOffset);
                        ctx.restore();
                    }

                    // Current Y velocity horizontal line
                    if (vel.yVel !== undefined && vel.yVel >= 0) {
                        const yVelPixel = yScale.getPixelForValue(vel.yVel);
                        ctx.save();
                        ctx.setLineDash([8, 4]);
                        ctx.strokeStyle = colorSet.y;
                        ctx.lineWidth = 2;
                        ctx.beginPath();
                        ctx.moveTo(xScale.left, yVelPixel);
                        ctx.lineTo(xScale.right, yVelPixel);
                        ctx.stroke();
                        ctx.restore();

                        // Draw label for Y velocity with offset
                        const xLabelOffset = xScale.left + 5 + (index * 150); // Horizontal offset for different shooters
                        const yLabelOffset = yVelPixel + 12; // Offset downward
                        ctx.save();
                        ctx.fillStyle = colorSet.y;
                        ctx.font = 'bold 12px Arial';
                        ctx.fillText(`${shooterName} Y: ${vel.yVel.toFixed(3)}`, xLabelOffset, yLabelOffset);
                        ctx.restore();
                    }
                });
            }
        };

        this.chart = new Chart(ctx, {
            type: 'line',
            data: {
                datasets: [
                    {
                        label: 'X Velocity (m/s)',
                        borderColor: 'rgb(255, 99, 132)',
                        backgroundColor: 'rgba(255, 99, 132, 0.1)',
                        data: [],
                        tension: 0.1,
                        pointRadius: 8,
                        pointHoverRadius: 10,
                        pointBackgroundColor: 'rgb(255, 99, 132)',
                        pointBorderColor: '#fff',
                        pointBorderWidth: 2,
                        dragData: true
                    },
                    {
                        label: 'Y Velocity (m/s)',
                        borderColor: 'rgb(54, 162, 235)',
                        backgroundColor: 'rgba(54, 162, 235, 0.1)',
                        data: [],
                        tension: 0.1,
                        pointRadius: 8,
                        pointHoverRadius: 10,
                        pointBackgroundColor: 'rgb(54, 162, 235)',
                        pointBorderColor: '#fff',
                        pointBorderWidth: 2,
                        dragData: true
                    },
                    {
                        label: 'Current Speed (当前速度)',
                        borderColor: 'rgba(40, 167, 69, 0)',
                        backgroundColor: 'rgba(40, 167, 69, 0)',
                        data: [],
                        showLine: false,
                        pointRadius: 10,
                        pointHoverRadius: 12,
                        pointBackgroundColor: 'rgba(40, 167, 69, 0.9)',
                        pointBorderColor: 'rgba(40, 167, 69, 1)',
                        pointBorderWidth: 3,
                        dragData: false,
                        pointStyle: 'rectRot',
                        order: -1
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: {
                    mode: 'point',
                    intersect: true
                },
                plugins: {
                    dragData: {
                        round: 2,
                        showTooltip: true,
                        dragX: true, // Enable horizontal dragging
                        onDragStart: (e, datasetIndex, index, value) => {
                            const tableData = this.getCurrentTableData();
                            // Store original data in case we need to revert
                            this.dragOriginalData = {
                                distance: tableData[index].distance,
                                xVel: tableData[index].xVel,
                                yVel: tableData[index].yVel
                            };
                        },
                        onDrag: (e, datasetIndex, index, value) => {
                            const tableData = this.getCurrentTableData();
                            // Update both X and Y values simultaneously
                            if (datasetIndex === 0) {
                                tableData[index].xVel = Math.max(0, value.y);
                                tableData[index].distance = Math.max(0, value.x);
                                // Update the other dataset's X coordinate
                                this.chart.data.datasets[1].data[index].x = value.x;
                            } else if (datasetIndex === 1) {
                                tableData[index].yVel = Math.max(0, value.y);
                                tableData[index].distance = Math.max(0, value.x);
                                // Update the other dataset's X coordinate
                                this.chart.data.datasets[0].data[index].x = value.x;
                            }
                            this.updateLiveData();
                        },
                        onDragEnd: (e, datasetIndex, index, value) => {
                            const tableData = this.getCurrentTableData();
                            // Final update
                            if (datasetIndex === 0) {
                                tableData[index].xVel = Math.max(0, value.y);
                                tableData[index].distance = Math.max(0, value.x);
                            } else if (datasetIndex === 1) {
                                tableData[index].yVel = Math.max(0, value.y);
                                tableData[index].distance = Math.max(0, value.x);
                            }
                            this.saveToHistory();
                            this.markAsModified();
                            this.updateChart();
                        }
                    },
                    title: {
                        display: true,
                        text: '速度插值表 (拖动点进行调整)',
                        font: {
                            size: 18,
                            weight: 'bold'
                        }
                    },
                    tooltip: {
                        enabled: true,
                        mode: 'point',
                        intersect: true,
                        callbacks: {
                            label: (context) => {
                                return context.dataset.label + ': ' + context.parsed.y.toFixed(3) + ' m/s @ ' + context.parsed.x.toFixed(3) + ' m';
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        type: 'linear',
                        title: {
                            display: true,
                            text: 'Distance (m)',
                            font: {
                                size: 14,
                                weight: 'bold'
                            }
                        },
                        min: 0,
                        max: 16,
                        ticks: {
                            stepSize: 1
                        }
                    },
                    y: {
                        title: {
                            display: true,
                            text: 'Velocity (m/s)',
                            font: {
                                size: 14,
                                weight: 'bold'
                            }
                        },
                        min: 0,
                        max: 20
                    }
                }
            },
            plugins: [hoverLinePlugin]
        });

        // Add right-click and double-click handling on canvas
        const canvas = ctx.canvas;

        canvas.addEventListener('contextmenu', (e) => {
            e.preventDefault();

            // Get the clicked point
            const rect = canvas.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;

            const elements = this.chart.getElementsAtEventForMode(e, 'nearest', { intersect: true }, false);

            if (elements.length > 0) {
                const element = elements[0];
                const datasetIndex = element.datasetIndex;
                const index = element.index;

                // Only handle editable datasets (0 and 1)
                if (datasetIndex === 0 || datasetIndex === 1) {
                    this.showPointEditDialog(index);
                }
            }

            return false;
        });

        canvas.addEventListener('mousemove', (e) => {
            const rect = canvas.getBoundingClientRect();
            const x = e.clientX - rect.left;

            const xValue = this.chart.scales.x.getValueForPixel(x);
            const tableData = this.getCurrentTableData();

            if (xValue !== undefined && xValue >= 0 && xValue <= 12 && tableData.length > 0) {
                // Calculate interpolated values
                const xVel = this.interpolate(tableData, xValue, 'xVel');
                const yVel = this.interpolate(tableData, xValue, 'yVel');
                const angle = (Math.atan2(yVel, xVel) * 180 / Math.PI).toFixed(3);
                const totalVel = Math.hypot(xVel, yVel).toFixed(3);

                // Update hover info (using cached elements)
                this.getElement('hoverDistance').textContent = xValue.toFixed(3);
                this.getElement('hoverX').textContent = xVel.toFixed(3);
                this.getElement('hoverY').textContent = yVel.toFixed(3);
                this.getElement('hoverAngle').textContent = angle;
                this.getElement('hoverTotal').textContent = totalVel;

                // Show the hover info box (fixed at top-right)
                this.getElement('hoverInfo').style.display = 'block';

                // Draw vertical dashed line
                this.hoverLineX = xValue;
                this.chart.update('none');
            }
        });

        canvas.addEventListener('mouseleave', () => {
            this.getElement('hoverInfo').style.display = 'none';
            this.hoverLineX = null;
            this.chart.update('none');
        });

        canvas.addEventListener('dblclick', (e) => {
            const rect = canvas.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;

            const xValue = this.chart.scales.x.getValueForPixel(x);
            const tableData = this.getCurrentTableData();

            if (xValue !== undefined && xValue >= 0) {
                let newDistance, newXVel, newYVel;

                if (tableData.length < 2) {
                    // If less than 2 points, use clicked position
                    const yValue = this.chart.scales.y.getValueForPixel(y);
                    newDistance = Math.max(0, xValue);
                    newXVel = Math.max(0, yValue);
                    newYVel = Math.max(0, yValue);
                } else {
                    // Find the two adjacent points surrounding the clicked position
                    let lower = null;
                    let upper = null;

                    for (let i = 0; i < tableData.length; i++) {
                        if (tableData[i].distance < xValue) {
                            lower = tableData[i];
                        }
                        if (tableData[i].distance > xValue && upper === null) {
                            upper = tableData[i];
                            break;
                        }
                    }

                    // Calculate new distance as midpoint of adjacent points
                    if (lower && upper) {
                        // Clicked between two points - use their midpoint
                        newDistance = (lower.distance + upper.distance) / 2;
                    } else if (lower && !upper) {
                        // Clicked after last point - use last point's distance + 1
                        newDistance = lower.distance + 1.0;
                    } else if (!lower && upper) {
                        // Clicked before first point - use first point's distance - 1
                        newDistance = Math.max(0.1, upper.distance - 1.0);
                    } else {
                        // Shouldn't happen, but fallback
                        newDistance = Math.max(0, xValue);
                    }

                    // Calculate velocities using interpolation at the new distance
                    newXVel = this.interpolate(tableData, newDistance, 'xVel');
                    newYVel = this.interpolate(tableData, newDistance, 'yVel');
                }

                const newPoint = {
                    distance: newDistance,
                    xVel: newXVel,
                    yVel: newYVel
                };

                tableData.push(newPoint);
                tableData.sort((a, b) => a.distance - b.distance);
                this.saveToHistory();
                this.updateChart();
                this.markAsModified();
                this.updateDataPointCount();
            }
        });

        this.updateChart();
    }

    updateChart() {
        const tableData = this.getCurrentTableData();
        // Sort by distance
        tableData.sort((a, b) => a.distance - b.distance);

        // Update chart data with actual data points
        const xPoints = tableData.map(d => ({ x: d.distance, y: d.xVel }));
        const yPoints = tableData.map(d => ({ x: d.distance, y: d.yVel }));

        this.chart.data.datasets[0].data = xPoints;
        this.chart.data.datasets[1].data = yPoints;

        // Update current speed point data (without triggering chart.update)
        this.updateCurrentSpeedPointData();

        // Single chart update for better performance
        this.chart.update('none');
        this.updateLiveData();
    }

    updateCurrentSpeedPointData() {
        // Update the current speed data (now shown as horizontal lines via plugin)
        const shooterNames = Object.keys(this.shooterVelocities);

        // Update main shooter data (or first available)
        if (shooterNames.length > 0 && (shooterNames.includes('main') || shooterNames.length > 0)) {
            const mainShooter = shooterNames.includes('main') ? 'main' : shooterNames[0];
            const mainVel = this.shooterVelocities[mainShooter];

            const currentAngle = (Math.atan2(mainVel.yVel, mainVel.xVel) * 180 / Math.PI).toFixed(3);
            const currentTotal = Math.hypot(mainVel.xVel, mainVel.yVel).toFixed(3);

            // Update main shooter status bar
            this.getElement('statusCurrentXVel').textContent = mainVel.xVel.toFixed(3);
            this.getElement('statusCurrentYVel').textContent = mainVel.yVel.toFixed(3);
            this.getElement('statusCurrentAngle').textContent = currentAngle;
            this.getElement('statusCurrentTotal').textContent = currentTotal;
        } else {
            this.getElement('statusCurrentXVel').textContent = '--';
            this.getElement('statusCurrentYVel').textContent = '--';
            this.getElement('statusCurrentAngle').textContent = '--';
            this.getElement('statusCurrentTotal').textContent = '--';
        }

        // Update secondary shooter data (if exists)
        if (shooterNames.includes('secondary')) {
            const secondaryVel = this.shooterVelocities['secondary'];
            const secondaryAngle = (Math.atan2(secondaryVel.yVel, secondaryVel.xVel) * 180 / Math.PI).toFixed(3);
            const secondaryTotal = Math.hypot(secondaryVel.xVel, secondaryVel.yVel).toFixed(3);

            this.getElement('statusSecondaryXVel').textContent = secondaryVel.xVel.toFixed(3);
            this.getElement('statusSecondaryYVel').textContent = secondaryVel.yVel.toFixed(3);
            this.getElement('statusSecondaryAngle').textContent = secondaryAngle;
            this.getElement('statusSecondaryTotal').textContent = secondaryTotal;
        } else {
            this.getElement('statusSecondaryXVel').textContent = '--';
            this.getElement('statusSecondaryYVel').textContent = '--';
            this.getElement('statusSecondaryAngle').textContent = '--';
            this.getElement('statusSecondaryTotal').textContent = '--';
        }
    }

    updateCurrentSpeedPoint() {
        // Update current speed point and trigger chart update
        this.updateCurrentSpeedPointData();
        this.chart.update('none');
    }

    updateCurrentDistanceMarker() {
        // This method is no longer needed as we use datasets instead
    }

    interpolate(data, distance, property) {
        if (data.length === 0) return 0;
        if (data.length === 1) return data[0][property];

        // Clamp to bounds
        if (distance <= data[0].distance) return data[0][property];
        if (distance >= data[data.length - 1].distance) {
            return data[data.length - 1][property];
        }

        // Find surrounding entries
        let lower = data[0];
        let upper = data[data.length - 1];

        for (let i = 0; i < data.length - 1; i++) {
            if (data[i].distance <= distance && data[i + 1].distance >= distance) {
                lower = data[i];
                upper = data[i + 1];
                break;
            }
        }

        // Linear interpolation
        if (upper.distance === lower.distance) return lower[property];
        const t = (distance - lower.distance) / (upper.distance - lower.distance);
        return lower[property] + t * (upper[property] - lower[property]);
    }

    switchViewingTable(tableId) {
        this.viewingTableId = tableId;
        const method = this.getCurrentTableMethod();

        if (method === 'constant') {
            this.showConstantUI();
        } else {
            this.showInterpolatedUI();
        }

        this.updateChart();
        this.updateHistoryButtonStates();
    }

    showConstantUI() {
        this.getElement('chartContainer').style.display = 'none';
        this.getElement('constantContainer').style.display = 'block';

        const tableData = this.getCurrentTableData();
        if (tableData.length > 0) {
            const xVel = tableData[0].xVel;
            const yVel = tableData[0].yVel;

            // 设置 XY 输入框
            this.getElement('constantXVel').value = xVel.toFixed(3);
            this.getElement('constantYVel').value = yVel.toFixed(3);

            // 计算并设置角度输入框
            const angle = Math.atan2(yVel, xVel) * 180 / Math.PI;
            const total = Math.hypot(xVel, yVel);
            this.getElement('constantAngleInput').value = angle.toFixed(3);
            this.getElement('constantTotalInput').value = total.toFixed(3);
        }
    }

    showInterpolatedUI() {
        this.getElement('chartContainer').style.display = 'block';
        this.getElement('constantContainer').style.display = 'none';
    }

    syncXYToAngle() {
        // 从 XY 计算角度和合速度
        const xVel = parseFloat(this.getElement('constantXVel').value) || 0;
        const yVel = parseFloat(this.getElement('constantYVel').value) || 0;
        const angle = Math.atan2(yVel, xVel) * 180 / Math.PI;
        const total = Math.hypot(xVel, yVel);

        this.getElement('constantAngleInput').value = angle.toFixed(3);
        this.getElement('constantTotalInput').value = total.toFixed(3);
    }

    syncAngleToXY() {
        // 从角度和合速度计算 XY
        const angle = parseFloat(this.getElement('constantAngleInput').value) || 0;
        const total = parseFloat(this.getElement('constantTotalInput').value) || 0;
        const angleRad = angle * Math.PI / 180;
        const xVel = total * Math.cos(angleRad);
        const yVel = total * Math.sin(angleRad);

        this.getElement('constantXVel').value = xVel.toFixed(3);
        this.getElement('constantYVel').value = yVel.toFixed(3);
    }

    updateDataPointCount() {
        // Info bar removed - data point count shown in chart only
    }

    showPointEditDialog(index) {
        const tableData = this.getCurrentTableData();
        const point = tableData[index];

        // Store the index for later use
        this.editingPointIndex = index;

        // Calculate current angle and total velocity
        const angle = (Math.atan2(point.yVel, point.xVel) * 180 / Math.PI);
        const total = Math.hypot(point.xVel, point.yVel);

        // Show modal
        const modal = this.getElement('pointEditModal');
        modal.style.display = 'flex';

        // Set initial values
        this.getElement('editDistance').value = point.distance.toFixed(3);
        this.getElement('editXVel').value = point.xVel.toFixed(3);
        this.getElement('editYVel').value = point.yVel.toFixed(3);
        this.getElement('editAngle').value = angle.toFixed(3);
        this.getElement('editTotal').value = total.toFixed(3);

        // Update calculated values for both modes
        // XY mode - calculate angle and total
        this.getElement('calcAngle').textContent = angle.toFixed(3);
        this.getElement('calcTotal').textContent = total.toFixed(3);

        // Angle mode - calculate X and Y
        this.getElement('calcXVel').textContent = point.xVel.toFixed(3);
        this.getElement('calcYVel').textContent = point.yVel.toFixed(3);

        // Show/hide delete button based on point count
        const deleteBtn = this.getElement('btnDelete');
        deleteBtn.style.display = tableData.length > 2 ? 'block' : 'none';
    }

    setupModalEventListeners() {
        const modal = document.getElementById('pointEditModal');

        // Close modal handlers
        const closeModal = () => {
            modal.style.display = 'none';
            this.editingPointIndex = null;
        };

        document.getElementById('modalClose').addEventListener('click', closeModal);
        document.getElementById('btnCancel').addEventListener('click', closeModal);

        // Click outside modal to close
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                closeModal();
            }
        });

        // Tab switching
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const mode = e.target.dataset.mode;

                // Update tab buttons
                document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
                e.target.classList.add('active');

                // Update mode panels
                document.querySelectorAll('.input-mode').forEach(m => m.classList.remove('active'));
                if (mode === 'xy') {
                    document.getElementById('xyMode').classList.add('active');
                } else {
                    document.getElementById('angleMode').classList.add('active');
                }
            });
        });

        // XY mode inputs - update angle/total
        document.getElementById('editXVel').addEventListener('input', () => {
            this.updateModalCalculatedValues();
        });
        document.getElementById('editYVel').addEventListener('input', () => {
            this.updateModalCalculatedValues();
        });

        // Angle mode inputs - update X/Y
        document.getElementById('editAngle').addEventListener('input', () => {
            this.updateModalCalculatedValues();
        });
        document.getElementById('editTotal').addEventListener('input', () => {
            this.updateModalCalculatedValues();
        });

        // Delete button
        document.getElementById('btnDelete').addEventListener('click', () => {
            if (this.editingPointIndex !== null) {
                const tableData = this.getCurrentTableData();
                if (tableData.length > 2) {
                    tableData.splice(this.editingPointIndex, 1);
                    this.saveToHistory();
                    this.updateChart();
                    this.markAsModified();
                    this.updateDataPointCount();
                    closeModal();
                }
            }
        });

        // Save button
        document.getElementById('btnSave').addEventListener('click', () => {
            if (this.editingPointIndex !== null) {
                const tableData = this.getCurrentTableData();
                const distance = parseFloat(document.getElementById('editDistance').value);

                // Check which mode is active
                const xyMode = document.getElementById('xyMode').classList.contains('active');

                let xVel, yVel;
                if (xyMode) {
                    xVel = parseFloat(document.getElementById('editXVel').value);
                    yVel = parseFloat(document.getElementById('editYVel').value);
                } else {
                    const angle = parseFloat(document.getElementById('editAngle').value);
                    const total = parseFloat(document.getElementById('editTotal').value);
                    const angleRad = angle * Math.PI / 180;
                    xVel = total * Math.cos(angleRad);
                    yVel = total * Math.sin(angleRad);
                }

                // Validate
                if (isNaN(distance) || isNaN(xVel) || isNaN(yVel)) {
                    alert('输入无效！请输入有效的数字。');
                    return;
                }

                if (distance < 0 || xVel < 0 || yVel < 0) {
                    alert('值不能为负数！');
                    return;
                }

                // Update point
                tableData[this.editingPointIndex].distance = distance;
                tableData[this.editingPointIndex].xVel = xVel;
                tableData[this.editingPointIndex].yVel = yVel;

                this.saveToHistory();
                this.updateChart();
                this.markAsModified();
                closeModal();
            }
        });
    }

    updateModalCalculatedValues() {
        // Check which mode is active
        const xyMode = this.getElement('xyMode').classList.contains('active');

        if (xyMode) {
            // Calculate angle and total from X/Y
            const xVel = parseFloat(this.getElement('editXVel').value) || 0;
            const yVel = parseFloat(this.getElement('editYVel').value) || 0;
            const angle = (Math.atan2(yVel, xVel) * 180 / Math.PI).toFixed(3);
            const total = Math.hypot(xVel, yVel).toFixed(3);

            this.getElement('calcAngle').textContent = angle;
            this.getElement('calcTotal').textContent = total;
        } else {
            // Calculate X/Y from angle and total
            const angle = parseFloat(this.getElement('editAngle').value) || 0;
            const total = parseFloat(this.getElement('editTotal').value) || 0;
            const angleRad = angle * Math.PI / 180;
            const xVel = (total * Math.cos(angleRad)).toFixed(3);
            const yVel = (total * Math.sin(angleRad)).toFixed(3);

            this.getElement('calcXVel').textContent = xVel;
            this.getElement('calcYVel').textContent = yVel;
        }
    }

    setupEventListeners() {
        // Table type selector - local switch only, doesn't change robot's table
        document.getElementById('tableType').addEventListener('change', (e) => {
            this.switchViewingTable(e.target.value);
        });

        // Constant speed inputs - XY 模式
        // XY 输入框改变 -> 同步到角度输入框 -> 更新数据
        document.getElementById('constantXVel').addEventListener('input', () => {
            this.syncXYToAngle();
            this.updateConstantTableData();
        });

        document.getElementById('constantYVel').addEventListener('input', () => {
            this.syncXYToAngle();
            this.updateConstantTableData();
        });

        // 角度输入框改变 -> 同步到 XY 输入框 -> 更新数据
        document.getElementById('constantAngleInput').addEventListener('input', () => {
            this.syncAngleToXY();
            this.updateConstantTableData();
        });

        document.getElementById('constantTotalInput').addEventListener('input', () => {
            this.syncAngleToXY();
            this.updateConstantTableData();
        });

        // Undo button
        document.getElementById('undo').addEventListener('click', () => {
            this.undo();
        });

        // Redo button
        document.getElementById('redo').addEventListener('click', () => {
            this.redo();
        });

        // Apply changes button
        document.getElementById('applyChanges').addEventListener('click', () => {
            this.applyChangesToRobot();
        });

        // Copy JSON button
        document.getElementById('copyJson').addEventListener('click', () => {
            this.copyJsonToClipboard();
        });

        // Keyboard shortcuts
        document.addEventListener('keydown', (e) => {
            if (e.ctrlKey && e.key === 'Enter') {
                e.preventDefault();
                this.applyChangesToRobot();
            } else if (e.ctrlKey && e.key === 'z' && !e.shiftKey) {
                e.preventDefault();
                this.undo();
            } else if (e.ctrlKey && (e.key === 'y' || (e.key === 'z' && e.shiftKey))) {
                e.preventDefault();
                this.redo();
            }
        });
    }

    updateConstantTableData() {
        // 两组输入框始终同步，直接读取 XY 值即可
        const xVel = parseFloat(this.getElement('constantXVel').value) || 0;
        const yVel = parseFloat(this.getElement('constantYVel').value) || 0;

        const tableData = this.getCurrentTableData();
        if (tableData.length > 0) {
            tableData[0].xVel = xVel;
            tableData[0].yVel = yVel;
            this.saveToHistory();
            this.markAsModified();
        }
    }

    saveToHistory() {
        // Deep copy current state
        const tableData = this.getCurrentTableData();
        const undoStack = this.getCurrentUndoStack();
        const redoStack = this.getCurrentRedoStack();
        const state = JSON.parse(JSON.stringify(tableData));
        undoStack.push(state);
        // Clear redo stack when new change is made
        redoStack.length = 0;
        this.updateHistoryButtonStates();
    }

    undo() {
        const undoStack = this.getCurrentUndoStack();
        const redoStack = this.getCurrentRedoStack();
        if (undoStack.length <= 1) return; // Keep at least initial state

        // Save current state to redo stack
        const tableData = this.getCurrentTableData();
        const currentState = JSON.parse(JSON.stringify(tableData));
        redoStack.push(currentState);

        // Pop from undo stack
        undoStack.pop();
        const previousState = undoStack[undoStack.length - 1];

        // Restore previous state
        this.tableDataMap[this.viewingTableId] = JSON.parse(JSON.stringify(previousState));

        const method = this.getCurrentTableMethod();
        if (method === 'constant') {
            if (previousState.length > 0) {
                this.getElement('constantXVel').value = previousState[0].xVel.toFixed(3);
                this.getElement('constantYVel').value = previousState[0].yVel.toFixed(3);
                this.syncXYToAngle();
            }
        } else {
            this.updateChart();
        }

        this.markAsModified();
        this.updateDataPointCount();
        this.updateHistoryButtonStates();
    }

    redo() {
        const undoStack = this.getCurrentUndoStack();
        const redoStack = this.getCurrentRedoStack();
        if (redoStack.length === 0) return;

        // Save current state to undo stack
        const tableData = this.getCurrentTableData();
        const currentState = JSON.parse(JSON.stringify(tableData));
        undoStack.push(currentState);

        // Pop from redo stack
        const nextState = redoStack.pop();

        // Restore next state
        this.tableDataMap[this.viewingTableId] = JSON.parse(JSON.stringify(nextState));

        const method = this.getCurrentTableMethod();
        if (method === 'constant') {
            if (nextState.length > 0) {
                this.getElement('constantXVel').value = nextState[0].xVel.toFixed(3);
                this.getElement('constantYVel').value = nextState[0].yVel.toFixed(3);
                this.syncXYToAngle();
            }
        } else {
            this.updateChart();
        }

        this.markAsModified();
        this.updateDataPointCount();
        this.updateHistoryButtonStates();
    }

    updateHistoryButtonStates() {
        const undoStack = this.getCurrentUndoStack();
        const redoStack = this.getCurrentRedoStack();
        const undoBtn = this.getElement('undo');
        const redoBtn = this.getElement('redo');

        undoBtn.disabled = undoStack.length <= 1;
        redoBtn.disabled = redoStack.length === 0;
    }

    markAsModified() {
        this.hasUnappliedChanges = true;
        const btn = this.getElement('applyChanges');
        btn.style.backgroundColor = '#ff9800';
        btn.style.fontWeight = 'bold';
        btn.textContent = '⬆️ 应用修改到机器人 *';
    }

    applyChangesToRobot() {
        if (!this.hasUnappliedChanges) {
            return;
        }

        this.publishTableToRobot();
        this.hasUnappliedChanges = false;

        const btn = this.getElement('applyChanges');
        btn.style.backgroundColor = '#28a745';
        btn.textContent = '✅ 已应用！';

        setTimeout(() => {
            btn.style.backgroundColor = '';
            btn.style.fontWeight = '';
            btn.textContent = '⬆️ 应用修改到机器人';
        }, 2000);
    }

    copyJsonToClipboard() {
        const tableData = this.getCurrentTableData();
        const tableId = this.viewingTableId || 'unknown';
        const tableMethod = this.getCurrentTableMethod() || 'interpolated';

        // Generate Java registerTable format
        let javaCode = `registerTable(\n`;
        javaCode += `        "${tableId}",\n`;
        javaCode += `        "${tableMethod}",\n`;
        javaCode += `        "["\n`;

        // Add each data point as a separate line with 3 decimal places
        tableData.forEach((point, index) => {
            const jsonStr = `{\\\"distance\\\":${point.distance.toFixed(3)},\\\"xVel\\\":${point.xVel.toFixed(3)},\\\"yVel\\\":${point.yVel.toFixed(3)}}`;
            const isLast = index === tableData.length - 1;
            const comma = isLast ? '' : ',';
            javaCode += `            + "${jsonStr}${comma}"\n`;
        });

        javaCode += `            + "]");`;

        // Try modern clipboard API first, fall back to legacy method
        const copySuccess = () => {
            const btn = this.getElement('copyJson');
            const originalText = btn.textContent;
            btn.textContent = '✅ 已复制！';
            btn.style.backgroundColor = '#28a745';
            setTimeout(() => {
                btn.textContent = originalText;
                btn.style.backgroundColor = '';
            }, 2000);
        };

        const copyError = (err) => {
            alert('复制失败: ' + err);
        };

        // Check if modern clipboard API is available (HTTPS or localhost)
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(javaCode).then(copySuccess).catch(copyError);
        } else {
            // Fallback to legacy method for HTTP contexts
            try {
                const textArea = document.createElement('textarea');
                textArea.value = javaCode;
                textArea.style.position = 'fixed';
                textArea.style.left = '-999999px';
                textArea.style.top = '-999999px';
                document.body.appendChild(textArea);
                textArea.focus();
                textArea.select();

                const successful = document.execCommand('copy');
                document.body.removeChild(textArea);

                if (successful) {
                    copySuccess();
                } else {
                    copyError('execCommand failed');
                }
            } catch (err) {
                copyError(err);
            }
        }
    }

    connectToNetworkTables() {
        this.nt4Client = new NT4_Client(
            window.location.hostname,
            'ShootingCalculator',
            (topic) => this.onTopicAnnounce(topic),
            (topic) => this.onTopicUnannounce(topic),
            (topic, timestamp, value) => this.onNewTopicData(topic, timestamp, value),
            () => this.onConnect(),
            () => this.onDisconnect()
        );

        // Create publishing topics
        this.nt4Client.connect();
    }

    onConnect() {
        this.isConnected = true;
        this.getElement('connection-indicator').className = 'connected';
        this.getElement('connection-text').textContent = '已连接';

        // Subscribe to topics (will subscribe to table topics once we receive table list)
        this.subscribeToTopics();
    }

    onDisconnect() {
        this.isConnected = false;
        this.getElement('connection-indicator').className = 'disconnected';
        this.getElement('connection-text').textContent = '未连接';
    }

    subscribeToTopics() {
        // Subscribe to table list, current table ID, and current distance
        const topics = [
            TABLE_LIST_TOPIC,
            CURRENT_TABLE_ID_TOPIC,
            CURRENT_DISTANCE_TOPIC,
            '/shootingcalculator/shooters/main/xVel',
            '/shootingcalculator/shooters/main/yVel',
            '/shootingcalculator/shooters/secondary/xVel',
            '/shootingcalculator/shooters/secondary/yVel'
        ];

        // Subscribe to all registered table topics
        for (const table of this.registeredTables) {
            topics.push(`/shootingcalculator/tables/${table.id}`);
        }

        this.nt4Client.subscribe(topics, false, false, 0.1);
    }

    onTopicAnnounce(topic) {
        // Auto-subscribe to shooter velocity topics when they appear
        if (topic.name.startsWith('/shootingcalculator/shooters/') &&
            (topic.name.endsWith('/xVel') || topic.name.endsWith('/yVel'))) {
            this.nt4Client.subscribe([topic.name], false, false, 0.1);
        }
    }

    onTopicUnannounce(topic) {
        // Remove shooter data when topic disappears
        if (topic.name.startsWith('/shootingcalculator/shooters/')) {
            const parts = topic.name.split('/');
            const shooterName = parts[3];
            if (shooterName && this.shooterVelocities[shooterName]) {
                delete this.shooterVelocities[shooterName];
                this.scheduleChartUpdate(true);
            }
        }
    }

    onNewTopicData(topic, timestamp, value) {
        if (topic.name === TABLE_LIST_TOPIC) {
            // Receive table list from robot
            try {
                const tableList = JSON.parse(value);
                if (Array.isArray(tableList) && tableList.length > 0) {
                    this.registeredTables = tableList;
                    this.initializeTablesFromList(tableList);
                    this.populateTableSelector();
                    this.subscribeToTopics(); // Re-subscribe with new table topics
                }
            } catch (err) {
                console.error('Failed to parse table list JSON:', err);
            }
        } else if (topic.name === CURRENT_TABLE_ID_TOPIC) {
            // Robot tells us which table it's currently using
            this.robotTableId = value;
            // Also switch the viewing table to match robot
            if (value && this.viewingTableId !== value) {
                this.viewingTableId = value;
                // Update the selector
                const selector = document.getElementById('tableType');
                if (selector) {
                    selector.value = value;
                }

                const method = this.getCurrentTableMethod();
                if (method === 'constant') {
                    this.showConstantUI();
                } else {
                    this.showInterpolatedUI();
                    this.updateChart();
                }

                this.updateDataPointCount();
                this.updateHistoryButtonStates();
            }
            this.updateChart(); // Refresh to show/hide green line
        } else if (topic.name === CURRENT_DISTANCE_TOPIC) {
            // Update current distance
            this.currentDistance = value;
            this.scheduleChartUpdate();
        } else if (topic.name.startsWith('/shootingcalculator/shooters/')) {
            // Handle shooter velocity updates
            const parts = topic.name.split('/');
            if (parts.length >= 5) {
                const shooterName = parts[3];
                const velocityType = parts[4]; // 'xVel' or 'yVel'

                if (!this.shooterVelocities[shooterName]) {
                    this.shooterVelocities[shooterName] = { xVel: 0, yVel: 0 };
                }

                if (velocityType === 'xVel') {
                    this.shooterVelocities[shooterName].xVel = value;
                } else if (velocityType === 'yVel') {
                    this.shooterVelocities[shooterName].yVel = value;
                }

                this.scheduleChartUpdate(true);
            }
        } else if (topic.name.startsWith('/shootingcalculator/tables/')) {
            // Receive table data
            const tableId = topic.name.replace('/shootingcalculator/tables/', '');
            try {
                const data = JSON.parse(value);
                if (Array.isArray(data) && data.length > 0) {
                    this.tableDataMap[tableId] = data;
                    // Initialize history for this table only if not exists
                    if (!this.undoStackMap[tableId] || this.undoStackMap[tableId].length === 0) {
                        this.undoStackMap[tableId] = [JSON.parse(JSON.stringify(data))];
                        this.redoStackMap[tableId] = [];
                    }


                    // If viewing this table, update chart
                    if (this.viewingTableId === tableId) {
                        const method = this.getCurrentTableMethod();
                        if (method === 'constant') {
                            this.showConstantUI();
                        } else {
                            this.showInterpolatedUI();
                            this.updateChart();
                        }
                        this.updateDataPointCount();
                        this.updateHistoryButtonStates();
                    }
                }
            } catch (err) {
                console.error(`Failed to parse table '${tableId}' JSON:`, err);
            }
        }
    }

    initializeTablesFromList(tableList) {
        // Initialize data structures for each table
        for (const table of tableList) {
            if (!this.tableDataMap[table.id]) {
                this.tableDataMap[table.id] = [];
            }
            if (!this.undoStackMap[table.id]) {
                this.undoStackMap[table.id] = [];
            }
            if (!this.redoStackMap[table.id]) {
                this.redoStackMap[table.id] = [];
            }
        }

        // Set default viewing table if not set
        if (!this.viewingTableId && tableList.length > 0) {
            this.viewingTableId = tableList[0].id;
        }
    }

    populateTableSelector() {
        const selector = document.getElementById('tableType');
        selector.innerHTML = ''; // Clear existing options

        for (const table of this.registeredTables) {
            const option = document.createElement('option');
            option.value = table.id;
            const displayName = table.id.charAt(0).toUpperCase() + table.id.slice(1);
            const methodLabel = table.method === 'constant' ? ' (定速)' : ' (插值)';
            option.textContent = displayName + methodLabel;
            if (table.id === this.viewingTableId) {
                option.selected = true;
            }
            selector.appendChild(option);
        }
    }

    updateLiveData() {
        // Always use robot's current table for live data, not the viewing table
        const statusBar = document.querySelector('.status-bar');

        if (!this.robotTableId || this.currentDistance <= 0) {
            // Dim status bar when no robot table or distance
            if (statusBar) {
                statusBar.style.opacity = '0.5';
            }
            this.getElement('robotTableName').textContent = '--';
            this.getElement('statusDistance').textContent = '--';
            this.getElement('statusXVel').textContent = '--';
            this.getElement('statusYVel').textContent = '--';
            this.getElement('statusAngle').textContent = '--';
            this.getElement('statusTotal').textContent = '--';
            this.getElement('statusCurrentXVel').textContent = '--';
            this.getElement('statusCurrentYVel').textContent = '--';
            this.getElement('statusCurrentAngle').textContent = '--';
            this.getElement('statusCurrentTotal').textContent = '--';
            return;
        }

        const robotTableData = this.tableDataMap[this.robotTableId];
        if (!robotTableData || robotTableData.length === 0) {
            // Dim status bar when table data not available
            if (statusBar) {
                statusBar.style.opacity = '0.5';
            }
            return;
        }

        const xVel = this.interpolate(robotTableData, this.currentDistance, 'xVel');
        const yVel = this.interpolate(robotTableData, this.currentDistance, 'yVel');
        const angle = (Math.atan2(yVel, xVel) * 180 / Math.PI).toFixed(3);
        const totalVel = Math.hypot(xVel, yVel).toFixed(3);

        // Update status bar
        if (statusBar) {
            statusBar.style.opacity = '1';
        }
        const tableName = this.robotTableId.charAt(0).toUpperCase() + this.robotTableId.slice(1);
        this.getElement('robotTableName').textContent = tableName;
        this.getElement('statusDistance').textContent = this.currentDistance.toFixed(3);
        this.getElement('statusXVel').textContent = xVel.toFixed(3);
        this.getElement('statusYVel').textContent = yVel.toFixed(3);
        this.getElement('statusAngle').textContent = angle;
        this.getElement('statusTotal').textContent = totalVel;
    }

    publishTableToRobot() {
        if (!this.nt4Client || !this.isConnected || !this.viewingTableId) return;

        const tableData = this.getCurrentTableData();
        const jsonData = JSON.stringify(tableData);

        // Publish to the modified topic for this table
        const modifiedTopic = `/shootingcalculator/tables/${this.viewingTableId}/modified`;

        // Ensure topic is published
        this.nt4Client.publishTopic(modifiedTopic, 'string');
        this.nt4Client.addSample(modifiedTopic, jsonData);
    }
}

// Initialize when page loads
window.addEventListener('DOMContentLoaded', () => {
    new ShootingCalculator();
});